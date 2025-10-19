#include "postgreSQL_async_client.hpp"

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
    std::cerr << "Failed to open config file\n";
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
    std::cerr << m_connect.error_msg << std::endl;
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
      std::cout << "Connected PG\n";
      try_send_next();
      break;
    case PGRES_POLLING_FAILED:
    default:
      m_connect.status = ConnectStatus::ERROR;
      m_connect.error_msg = std::string("Connection failed: ") + PQerrorMessage(m_connect.connection);
      update_interests(false,false);
      std::cerr << m_connect.error_msg << std::endl;
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
  std::cout << "disconnect PG\n";
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
      std::cerr << "poll error: " << strerror(errno) << std::endl;
      continue;
    }
    if (pr == 0)
    {
      try_send_next();
      continue;
    }

    if (pfd.revents & (POLLERR | POLLHUP | POLLNVAL))
    {
      std::cerr << "socket error/hup\n";
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
    std::cerr << "PQconsumeInput: " << PQerrorMessage(m_connect.connection) << std::endl;
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
  for (PGresult* r = PQgetResult(m_connect.connection); r; r = PQgetResult(m_connect.connection))
  {
    auto st = PQresultStatus(r);
    if (st == PGRES_TUPLES_OK)
    {
      int nf = PQnfields(r), nt = PQntuples(r);
      for (int i=0;i<nt;++i)
      {
        Row row; row.reserve(nf);
        for (int j=0;j<nf;++j)
        {
          row.emplace_back(PQgetisnull(r,i,j) ? "" : PQgetvalue(r,i,j));
        }
        out.emplace_back(std::move(row));
      }
    } else if (st == PGRES_COMMAND_OK)
    {
      // ок без строк
    } 
    else
    {
      std::string err = PQresultErrorMessage(r);
      PQclear(r);
      PendingQuery failed = std::move(*m_inflight);
      m_inflight.reset();
      switch (failed.kind) {
        case PendingQuery::K_BookingsByRoomDate:
          FinishBookingsByRoomDate_(this, failed, {}, false, err.c_str());
          break;
        case PendingQuery::K_ConflictsByInterval:
          FinishConflicts_(this, failed, {}, false, err.c_str());
          break;
        default: break;
      }
      try_send_next();
      return;
    }
    PQclear(r);
  }

  PendingQuery done = std::move(*m_inflight);
  m_inflight.reset();
  switch (done.kind)
  {
    case PendingQuery::K_BookingsByRoomDate:
      FinishBookingsByRoomDate_(this, done, std::move(out), true, nullptr);
      break;
    case PendingQuery::K_ConflictsByInterval:
      FinishConflicts_(this, done, std::move(out), true, nullptr);
      break;
    default: break;
  }
  try_send_next();
}

void PostgreSQLAsyncClient::try_send_next()
{
  if (m_connect.status != ConnectStatus::CONNECTED) return;
  if (m_inflight) return;
  if (m_queue.empty()) return;

  m_inflight.emplace(std::move(m_queue.front()));
  m_queue.erase(m_queue.begin());
  auto& q = *m_inflight;

  std::vector<const char*> vals;
  std::vector<int> lens, fmts;
  vals.reserve(q.params.size());
  lens.reserve(q.params.size());
  fmts.reserve(q.params.size());
  for (auto& p : q.params)
  {
    vals.push_back(p.c_str());
    lens.push_back((int)p.size());
    fmts.push_back(0);
  }

  if (PQsendQueryParams(m_connect.connection, q.sql.c_str(),
                        (int)q.params.size(), nullptr,
                        vals.data(), lens.data(), fmts.data(), 0) != 1)
  {
    std::string err = PQerrorMessage(m_connect.connection);
    PendingQuery bad = std::move(q);
    m_inflight.reset();
    switch (bad.kind) {
      case PendingQuery::K_BookingsByRoomDate:
        FinishBookingsByRoomDate_(this, bad, {}, false, err.c_str());
        break;
      case PendingQuery::K_ConflictsByInterval:
        FinishConflicts_(this, bad, {}, false, err.c_str());
        break;
      default: break;
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
    "WHERE b.room_id = $1::uuid "
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
