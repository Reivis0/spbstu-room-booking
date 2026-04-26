#include "postgreSQL_async_client.hpp"

#include "logger.hpp"
#include <fstream>
#include <iostream>
#include <sstream>
#include <poll.h>
#include <optional>
#include <cstring>

static void mapResultToBookings(const PostgreSQLAsyncClient::Result& r, std::vector<Booking>& out)
{
  out.clear();
  out.reserve(r.size());
  for (auto& row : r) {
    if (row.size() < 6) continue;
    Booking b;
    b.id         = row[0];
    b.start_time = row[1];
    b.end_time   = row[2];
    b.user_id    = row[3];
    b.notes      = row[4];
    b.status     = row[5];
    out.emplace_back(std::move(b));
  }
}

PostgreSQLAsyncClient::Connector::Connector()
{
  auto cfg = read_config();
  host     = cfg["booking_service.host"];
  port     = cfg["booking_service.port"];
  db_name  = cfg["booking_service.database_name"];
  username = cfg["booking_service.username"];
  password = cfg["booking_service.password"];
}

std::map<std::string, std::string> PostgreSQLAsyncClient::Connector::read_config()
{
  std::map<std::string, std::string> cfg;
  std::ifstream f("configs/pg_config.ini");
  if (!f.is_open())
  {
    LOG_ERROR("PG: Failed to open config file");
    return cfg;
  }
  std::string line, section;
  auto trim = [](std::string& s)
  {
    auto a = s.find_first_not_of(" \t\r\n"); 
    if (a==std::string::npos)
    {
      s.clear();
      return;
    }
    auto b = s.find_last_not_of(" \t\r\n"); s = s.substr(a, b-a+1);
  };
  while (std::getline(f, line))
  {
    trim(line);
    if (line.empty() || line[0]==';' || line[0]=='#') 
    {
      continue;
    }
    if (line.front()=='[' && line.back()==']')
    {
      section = line.substr(1, line.size()-2);
      continue;
    }
    auto p = line.find('=');
    if (p!=std::string::npos) {
      std::string k = line.substr(0,p), v = line.substr(p+1);
      trim(k); trim(v);
      cfg[section + "." + k] = v;
    }
  }
  return cfg;
}

std::string PostgreSQLAsyncClient::Connector::get_connecting_str()
{
  std::ostringstream s;
  s << "host=" << host
    << " port=" << port
    << " dbname=" << db_name
    << " user=" << username
    << " password=" << password;
  return s.str();
}

PostgreSQLAsyncClient::PostgreSQLAsyncClient()
{
  start();
}

PostgreSQLAsyncClient::~PostgreSQLAsyncClient()
{
  stop();
  disconnect();
}

void PostgreSQLAsyncClient::start()
{
  if (m_running) return;
  m_running = true;
  begin_async_connect();
  m_loop_thread = std::thread([this]{ run_loop(); });
}

void PostgreSQLAsyncClient::stop()
{
  m_running = false;
  if (m_loop_thread.joinable()) m_loop_thread.join();
}

void PostgreSQLAsyncClient::begin_async_connect()
{
  Connector connector;
  m_connect.connection = PQconnectStart(connector.get_connecting_str().c_str());
  if (!m_connect.connection) {
    m_connect.status = ConnectStatus::ERROR;
    m_connect.error_msg = "Failed to create connection object";
    LOG_ERROR("PG: " + m_connect.error_msg);
    return;
  }
  if (PQsetnonblocking(m_connect.connection, 1) != 0)
  {
    m_connect.error_msg = std::string("Failed to set non-blocking mode: ") + PQerrorMessage(m_connect.connection);
    m_connect.status = ConnectStatus::ERROR;
    PQfinish(m_connect.connection);
    m_connect.connection = nullptr;
    return;
  }
  m_connect.socket_fd = PQsocket(m_connect.connection);
  if (m_connect.socket_fd < 0)
  {
    m_connect.error_msg = "Invalid socket descriptor";
    m_connect.status = ConnectStatus::ERROR;
    PQfinish(m_connect.connection);
    m_connect.connection = nullptr;
    return;
  }
  arm_connect_poll();
}

void PostgreSQLAsyncClient::arm_connect_poll()
{
  auto st = PQconnectPoll(m_connect.connection);
  switch (st)
  {
    case PGRES_POLLING_READING:
      m_connect.status = ConnectStatus::NEED_READ;
      update_interests(true,false);
      break;
    case PGRES_POLLING_WRITING:
      m_connect.status = ConnectStatus::NEED_WRITE;
      update_interests(false,true);
      break;
    case PGRES_POLLING_OK:
      m_connect.status = ConnectStatus::CONNECTED;
      m_connect.is_connected = true;
      update_interests(false,false);
      LOG_INFO("PG: Connected PG");
      try_send_next();
      break;
    case PGRES_POLLING_FAILED:
    default:
      m_connect.status = ConnectStatus::ERROR;
      m_connect.error_msg = std::string("Connection failed: ") + PQerrorMessage(m_connect.connection);
      update_interests(false,false);
      LOG_ERROR("PG: " + m_connect.error_msg);
      break;
  }
}

void PostgreSQLAsyncClient::update_interests(bool rd, bool wr)
{
  m_want_read.store(rd, std::memory_order_relaxed);
  m_want_write.store(wr, std::memory_order_relaxed);
}

void PostgreSQLAsyncClient::disconnect()
{
  if (m_connect.connection)
  {
    while (auto* r = PQgetResult(m_connect.connection)) PQclear(r);
    PQfinish(m_connect.connection);
    m_connect.connection = nullptr;
  }
  m_connect.socket_fd = -1;
  m_connect.is_connected = false;
  m_connect.status = ConnectStatus::ERROR;
  update_interests(false,false);
  LOG_INFO("PG: disconnect PG");
}

void PostgreSQLAsyncClient::run_loop()
{
  while (m_running)
  {
    if (!m_connect.connection)
    {
      std::this_thread::sleep_for(std::chrono::milliseconds(50));
      continue;
    }
    struct pollfd pfd{};
    pfd.fd = m_connect.socket_fd;
    pfd.events = 0;
    if (m_want_read)  pfd.events |= POLLIN;
    if (m_want_write) pfd.events |= POLLOUT;

    int pr = poll(&pfd, 1, 200);
    if (pr < 0)
    {
      if (errno == EINTR) continue;
      LOG_ERROR("PG: poll error: " + std::string(strerror(errno)));
      continue;
    }
    if (pr == 0)
    {
      try_send_next();
      continue;
    }

    if (pfd.revents & (POLLERR | POLLHUP | POLLNVAL))
    {
      LOG_ERROR("PG: socket error/hup");
      disconnect();
      continue;
    }
    if (m_connect.status == ConnectStatus::NEED_READ && (pfd.revents & POLLIN))
    {
      arm_connect_poll();
      continue;
    }
    if (m_connect.status == ConnectStatus::NEED_WRITE && (pfd.revents & POLLOUT))
    {
      arm_connect_poll();
      continue;
    }
    if (m_connect.status == ConnectStatus::CONNECTED)
    {
      if (pfd.revents & POLLIN)
      {
        on_readable();
      }
    }
  }
}

void PostgreSQLAsyncClient::on_readable()
{
  if (PQconsumeInput(m_connect.connection) != 1)
  {
    LOG_ERROR("PG: " + std::string("PQconsumeInput: ") + PQerrorMessage(m_connect.connection));
    disconnect();
    return;
  }
  if (m_inflight && PQisBusy(m_connect.connection) == 0)
  {
    drain_results(*m_inflight);
  }
}

void PostgreSQLAsyncClient::drain_results(PendingQuery& q) {
    Result out;
    std::string err_msg;
    bool success = true;

    while (PGresult* r = PQgetResult(m_connect.connection)) {
        auto st = PQresultStatus(r);
        if (st == PGRES_TUPLES_OK || st == PGRES_COMMAND_OK) {
            int nf = PQnfields(r);
            int nt = PQntuples(r);
            for (int i = 0; i < nt; ++i) {
                Row row_vec;
                row_vec.reserve(nf);
                for (int j = 0; j < nf; ++j) {
                    if (PQgetisnull(r, i, j)) row_vec.push_back("");
                    else row_vec.push_back(PQgetvalue(r, i, j));
                }
                out.push_back(std::move(row_vec));
            }
        } else {
             if (st == PGRES_FATAL_ERROR) {
                 success = false;
                 err_msg = PQresultErrorMessage(r);
             }
        }
        PQclear(r);
    }
    
    m_inflight.reset();

    if (q.user_cb) {
        switch (q.kind) {
            case PendingQuery::K_BookingsByRoomDate:
                FinishBookingsByRoomDate_(this, q, std::move(out), success, err_msg.c_str());
                break;
            case PendingQuery::K_ConflictsByInterval:
                FinishConflicts_(this, q, std::move(out), success, err_msg.c_str());
                break;
            case PendingQuery::K_Generic:
                FinishGeneric_(this, q, std::move(out), success, err_msg.c_str());
                break;
        }
    }

    try_send_next();
}

void PostgreSQLAsyncClient::try_send_next()
{
    if (m_inflight) return;
    if (!m_connect.is_connected) return;

    std::lock_guard<std::mutex> lock(m_queue_mutex);
    if (m_queue.empty()) return;

    m_inflight = std::move(m_queue.front());
    m_queue.erase(m_queue.begin());
    
    PendingQuery& q = *m_inflight;
    
    std::vector<const char*> vals;
    std::vector<int> lens, fmts;
    vals.reserve(q.params.size());
    lens.reserve(q.params.size());
    fmts.reserve(q.params.size());

    for (const auto& p : q.params) {
        vals.push_back(p.c_str());
        lens.push_back((int)p.size());
        fmts.push_back(0);
    }

    int res = PQsendQueryParams(m_connect.connection, q.sql.c_str(),
                                (int)q.params.size(), nullptr,
                                vals.data(), lens.data(), fmts.data(), 0);

    if (res != 1) {
        std::string err = PQerrorMessage(m_connect.connection);
        PendingQuery bad = std::move(*m_inflight);
        m_inflight.reset();
        
        if (bad.user_cb) {
            Result empty;
            if (bad.kind == PendingQuery::K_Generic) {
                FinishGeneric_(this, bad, std::move(empty), false, err.c_str());
            } else if (bad.kind == PendingQuery::K_BookingsByRoomDate) {
                 FinishBookingsByRoomDate_(this, bad, std::move(empty), false, err.c_str());
            } else if (bad.kind == PendingQuery::K_ConflictsByInterval) {
                 FinishConflicts_(this, bad, std::move(empty), false, err.c_str());
            }
        }
        return;
    }

    update_interests(true, m_want_write);
}


void PostgreSQLAsyncClient::getBookingsByRoomAndDate(const char* room_code,
                                                     const char* dayISO,
                                                     IBookingsByRoomDateCb* cb)
{
  static const char* SQL =
  "WITH room AS (SELECT id FROM rooms WHERE code = $1 LIMIT 1) "
  "SELECT b.id, "
  "       b.starts_at, "
  "       b.ends_at, "
  "       b.user_id, "
  "       ''::text AS notes, "
  "       b.status::text "
  "FROM bookings b "
  "JOIN room r ON b.room_id = r.id "
  "WHERE b.status IN ('pending','confirmed') "
  "  AND b.starts_at < ($2::date + INTERVAL '1 day') "
  "  AND b.ends_at   > ($2::date) "
  "ORDER BY b.starts_at";

  PendingQuery q;
  q.kind = PendingQuery::K_BookingsByRoomDate;
  q.sql = SQL;
  q.params = { room_code ? room_code : "", dayISO ? dayISO : "" };
  q.user_cb = cb;
  m_queue.emplace_back(std::move(q));
  try_send_next();
}

void PostgreSQLAsyncClient::getConflictsByInterval(const char* room_id, const char* startsAtISO, const char* endsAtISO, IconflictsCb* cb)
{
  static const char* SQL =
    "SELECT b.id, "
            "b.starts_at, "
            "b.ends_at, "
            "b.user_id, "
            "''::text AS notes, "
            "b.status::text "
    "FROM bookings b "
    "JOIN rooms r ON b.room_id = r.id "
    "WHERE r.code = $1 "
    "   AND b.status = 'confirmed' "
    "   AND tstzrange(b.starts_at, b.ends_at, '[)') "
    " && tstzrange($2::timestamptz, $3::timestamptz, '[)')";

  PendingQuery q;
  q.kind = PendingQuery::K_ConflictsByInterval;
  q.sql = SQL;
  q.params = { room_id ? room_id : "", startsAtISO ? startsAtISO : "", endsAtISO ? endsAtISO : "" };
  q.user_cb = cb;
  m_queue.emplace_back(std::move(q));
  try_send_next();
}

void PostgreSQLAsyncClient::FinishBookingsByRoomDate_(PostgreSQLAsyncClient*, PendingQuery& q, Result&& r, bool ok, const char* err)
{
  auto* cb = static_cast<IBookingsByRoomDateCb*>(q.user_cb);
  if (!cb) 
  {
    return;
  }
  if (!ok)
  {
    cb->onBookings(nullptr, 0, false, err ? err : "query failed"); return;
  }
  std::vector<Booking> tmp;
  mapResultToBookings(r, tmp);
  cb->onBookings(tmp.data(), tmp.size(), true, nullptr);
}

void PostgreSQLAsyncClient::FinishConflicts_(PostgreSQLAsyncClient*, PendingQuery& q, Result&& r, bool ok, const char* err)
{
  auto* cb = static_cast<IconflictsCb*>(q.user_cb);
  if (!cb) 
  {
    return;
  }
  if (!ok) 
  {
    cb->onConflicts(nullptr, 0, false, err ? err : "query failed");
    return;
  }
  std::vector<Booking> tmp;
  mapResultToBookings(r, tmp);
  cb->onConflicts(tmp.data(), tmp.size(), true, nullptr);
}

void PostgreSQLAsyncClient::execute(const std::string& sql, const std::vector<std::string>& params, IGenericCb* cb) {
    PendingQuery q;
    q.kind = PendingQuery::K_Generic;
    q.sql = sql;
    q.params = params;
    q.user_cb = cb;

    {
        std::lock_guard<std::mutex> lock(m_queue_mutex);
        m_queue.emplace_back(std::move(q));
    }
}

void PostgreSQLAsyncClient::FinishGeneric_(PostgreSQLAsyncClient* self, PendingQuery& q, Result&& r, bool ok, const char* err) {
    auto* cb = static_cast<IGenericCb*>(q.user_cb);
    if (cb) cb->onResult(r, ok, err);
}