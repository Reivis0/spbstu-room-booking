#ifndef REDIS_ASYNC_CLIENT_HPP
#define REDIS_ASYNC_CLIENT_HPP

#include <hiredis/hiredis.h>
#include <hiredis/async.h>
#include <hiredis/adapters/libevent.h>
#include <event2/event.h>
#include <string>
#include <map>
 

class RedisAsyncClient
{
public:
  RedisAsyncClient();
  ~RedisAsyncClient();

private:
  struct RedisConnector
  {
    RedisConnector();
    redisAsyncContext* context;
    event_base* ev_base;
    std::string host;
    int port;
    int connection_timeout;
    bool is_connected;
    std::string error_msg;

    std::map<std::string, std::string> read_config();
  };
  void begin_async_connect();
  void disconect();

  RedisConnector m_connector;
};

#endif