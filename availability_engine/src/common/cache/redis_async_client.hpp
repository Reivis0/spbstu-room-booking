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
#include <memory>
#include <mutex>
#include <unordered_map>
#include <google/protobuf/message.h>
#include <functional>

class IRedisCallback
{
public:
  virtual ~IRedisCallback() = default;
  virtual void onRedisReply(redisReply* reply) = 0;
};

class RedisCallbackImpl : public IRedisCallback {
public:
    explicit RedisCallbackImpl(const std::function<void(redisReply*)>& callback)
        : callback_(callback) {}

    void onRedisReply(redisReply* reply) override {
        if (callback_) {
            callback_(reply);
        }
    }

private:
    std::function<void(redisReply*)> callback_;
};

class RedisAsyncClient
{
public:
  RedisAsyncClient();
  ~RedisAsyncClient();

  void run_event_loop();
  void stop_event_loop();
  bool is_connected() const; // Declaration only

  void get(const std::string& key, std::unique_ptr<IRedisCallback> cb);
  void setex(const std::string& key, int ttl_seconds, const std::string& value, std::unique_ptr<IRedisCallback> cb = nullptr);
  void setProtobuf(const std::string& key, int ttl_seconds, const google::protobuf::Message& message, std::unique_ptr<IRedisCallback> cb = nullptr);
  void getProtobuf(const std::string& key, google::protobuf::Message& message, std::shared_ptr<IRedisCallback> cb);
  void connect();
  void disconnect();
  void setDisconnectCallback(std::function<void()> callback);

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
    std::atomic<bool> is_connected{false};
    std::string error_msg;

    std::map<std::string, std::string> read_config();
    bool setup_event_base();
  };

  RedisConnector m_connector;
  std::mutex m_pending_mutex;
  std::unordered_map<IRedisCallback*, std::unique_ptr<IRedisCallback>> m_pending_callbacks;
  
  void begin_async_connect();
  
  static void connectCallback(const redisAsyncContext* context, int status);
  static void authCallback(redisAsyncContext* context, void* reply, void* privdata);
  static void pingCallback(redisAsyncContext* context, void* reply, void* privdata);
  static void disconnectCallback(const redisAsyncContext* context, int status);
  
  static void genericCallback(redisAsyncContext* context, void* reply, void* privdata);

  void handleConnect(int status);
  void handleAuth(redisReply* reply);
  void handlePing(redisReply* reply);
  void handleDisconnect(int status);

  std::function<void()> custom_disconnect_callback;
};

#endif