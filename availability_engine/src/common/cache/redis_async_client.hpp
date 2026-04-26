#ifndef REDIS_ASYNC_CLIENT_HPP
#define REDIS_ASYNC_CLIENT_HPP

#include <logger.hpp>
#include <hiredis/hiredis.h>
#include <hiredis/async.h>
#include <hiredis/adapters/libevent.h>
#include <event2/event.h>
#include <string>
#include <string.h>
#include <map>
#include <iostream>
#include <thread>
#include <chrono>

class IRedisCallback
{
public:
  virtual ~IRedisCallback() = default;
  virtual void onRedisReply(redisReply* reply) = 0;
};

class RedisAsyncClient
{
public:
  RedisAsyncClient();
  ~RedisAsyncClient();

  void run_event_loop();
  void stop_event_loop();
  bool is_connected() const { return m_connector.is_connected; }

  void get(const std::string& key, IRedisCallback* cb);
  void setex(const std::string& key, int ttl_seconds, const std::string& value, IRedisCallback* cb = nullptr);

private:
  struct RedisConnector
  {
    RedisConnector();
    ~RedisConnector();
    
    redisAsyncContext* context;
    event_base* ev_base;
    std::string host;
    int port;
    std::string password;
    int connection_timeout;
    bool is_connected;
    std::string error_msg;

    std::map<std::string, std::string> read_config();
    bool setup_event_base();
  };

  RedisConnector m_connector;
  
  void begin_async_connect();
  void disconnect();
  
  static void connectCallback(const redisAsyncContext* context, int status);
  static void authCallback(redisAsyncContext* context, void* reply, void* privdata);
  static void pingCallback(redisAsyncContext* context, void* reply, void* privdata);
  static void disconnectCallback(const redisAsyncContext* context, int status);
  
  static void genericCallback(redisAsyncContext* context, void* reply, void* privdata);

  void handleConnect(int status);
  void handleAuth(redisReply* reply);
  void handlePing(redisReply* reply);
  void handleDisconnect(int status);
};

#endif