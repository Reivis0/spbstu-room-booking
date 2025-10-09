#ifndef POSTGRESQL_ASYNC_CLIENT_HPP
#define POSTGRESQL_ASYNC_CLIENT_HPP

#include <libpq-fe.h>
#include <string>
#include <map>


struct BookingRecord
{
  std::string id;
  std::string room_id;
  std::string start_time;
  std::string end_time;
  std::string status;
  std::string user_id;
  std::string created_at;
  std::string update_at;
};

struct ScheduleRecord
{
  std::string id;
  std::string room_id;
  std::string start_time;
  std::string end_time;
  std::string confidence; //imported, manual
  std::string source; //ruz, manual
  std::string imported_at;
};

class PostgreSQLAsyncClient
{
public: 
  PostgreSQLAsyncClient();
  ~PostgreSQLAsyncClient();

private:
  struct Connector
  {
    Connector();
    std::map<std::string, std::string> read_config();
    std::string get_connecting_str();

    std::string host;
    std::string port;
    std::string db_name;
    std::string username;
    std::string password;
  };

  enum class ConnectStatus
  {
    NEED_READ,
    NEED_WRITE,
    CONNECTED,
    ERROR
  };

  struct ConnectResult
  {
    PGconn* connection;
    ConnectStatus status;
    std::string error_msg;
    int socket_fd;
    bool is_connected;
  };

  void begin_async_connect();
  void disconnect();

  ConnectResult m_connect {};
};

#endif