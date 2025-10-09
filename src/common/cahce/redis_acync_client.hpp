#ifndef REDIS_ASYNC_CLIENT_HPP
#define REDIS_ASYNC_CLIENT_HPP

#include <hiredis/hiredis.h>
#include <hiredis/async.h>
#include <hiredis/adapters/libevent.h>
#include <event2/event.h>
 

class RedisAsyncClient
{
public:
  RedisAsyncClient();
  ~RedisAsyncClient();

private:
  class ConnectRedis
  {
    redisAsyncContext* context;
    event_base* event_base;
    std::string host;
    int port;
    int connection_timeout;
    bool is_connected;
  }
};

#endif