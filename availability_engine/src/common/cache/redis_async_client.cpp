#include "redis_async_client.hpp"
#include "logger.hpp"

#include <iostream>
#include <fstream>
#include <cstring>
#include <vector>
#include <sstream>

std::map<std::string, std::string> RedisAsyncClient::RedisConnector::read_config()
{
    std::map<std::string, std::string> config;
    
    // Default values
    config["booking_service.host"] = "127.0.0.1";
    config["booking_service.port"] = "6379";
    config["booking_service.password"] = "";
    config["booking_service.connection_timeout"] = "5";

    std::ifstream file("configs/redis_config.ini");
    if (file.is_open()) {
        std::string line, section;
        while (getline(file, line)) 
        {
            line.erase(0, line.find_first_not_of(" \t"));
            line.erase(line.find_last_not_of(" \t") + 1);
            if (line.empty() || line[0] == ';') continue;
            if (line[0] == '[' && line.back() == ']') {
                section = line.substr(1, line.size() - 2);
            } else {
                size_t pos = line.find('=');
                if (pos != std::string::npos) {
                    std::string key = line.substr(0, pos);
                    std::string value = line.substr(pos + 1);
                    key.erase(0, key.find_first_not_of(" \t"));
                    key.erase(key.find_last_not_of(" \t") + 1);
                    value.erase(0, value.find_first_not_of(" \t"));
                    value.erase(value.find_last_not_of(" \t") + 1);
                    config[section + "." + key] = value;
                }
            }
        }
    }

    // Override with ENV
    if (const char* host = std::getenv("REDIS_HOST")) config["booking_service.host"] = host;
    if (const char* port = std::getenv("REDIS_PORT")) config["booking_service.port"] = port;
    if (const char* pass = std::getenv("REDIS_PASSWORD")) config["booking_service.password"] = pass;

    return config;
}

bool RedisAsyncClient::RedisConnector::setup_event_base()
{
    if (ev_base) return true;
    ev_base = event_base_new();
    return ev_base != nullptr;
}

RedisAsyncClient::RedisConnector::RedisConnector() 
    : context(nullptr), ev_base(nullptr), is_connected(false)
{
    auto config = read_config();
    host = config["booking_service.host"];
    port = std::stoi(config["booking_service.port"]);
    password = config["booking_service.password"];
    connection_timeout = std::stoi(config["booking_service.connection_timeout"]);
}

RedisAsyncClient::RedisConnector::~RedisConnector()
{
    if (ev_base)
    {
        event_base_free(ev_base);
    }
}

void RedisAsyncClient::begin_async_connect()
{
    if (m_connector.is_connected) return;
    if (!m_connector.setup_event_base()) {
        LOG_ERROR("Failed to create event base");
        return;
    }

    m_connector.context = redisAsyncConnect(m_connector.host.c_str(), m_connector.port);
    if (m_connector.context == nullptr || m_connector.context->err)
    {
        m_connector.error_msg = "Failed to create connection";
        if (m_connector.context)
        {
             m_connector.error_msg += ": " + std::string(m_connector.context->errstr);
             redisAsyncFree(m_connector.context);
             m_connector.context = nullptr;
        }
        m_connector.is_connected = false;
        LOG_ERROR("✗ " + m_connector.error_msg);
        return;
    }

    m_connector.context->data = this;
    if (redisLibeventAttach(m_connector.context, m_connector.ev_base) != REDIS_OK)
    {
        m_connector.error_msg = "Failed to attach to libevent";
        redisAsyncFree(m_connector.context);
        m_connector.context = nullptr;
        m_connector.is_connected = false;
        LOG_ERROR("✗ " + m_connector.error_msg);
        return;
    }

    redisAsyncSetConnectCallback(m_connector.context, connectCallback);
    redisAsyncSetDisconnectCallback(m_connector.context, disconnectCallback);
    LOG_INFO("Redis connection started to " + m_connector.host + ":" + std::to_string(m_connector.port));
}

RedisAsyncClient::RedisAsyncClient() {
    custom_disconnect_callback = nullptr;
}

void RedisAsyncClient::disconnect() {
    if (m_connector.is_connected && m_connector.context) {
        LOG_INFO("Disconnecting from Redis...");
        redisAsyncDisconnect(m_connector.context);
        m_connector.is_connected = false;
    }
}

RedisAsyncClient::~RedisAsyncClient()
{
    disconnect();
}

void RedisAsyncClient::connectCallback(const redisAsyncContext* context, int status)
{
    RedisAsyncClient* client = static_cast<RedisAsyncClient*>(context->data);
    if (client)
    {
        client->handleConnect(status);
    }
}

void RedisAsyncClient::disconnectCallback(const redisAsyncContext* context, int status)
{
    RedisAsyncClient* client = static_cast<RedisAsyncClient*>(context->data);
    if (client)
    {
        LOG_INFO("disconnectCallback invoked. Status: " + std::to_string(status));
        client->handleDisconnect(status);
        if (client->custom_disconnect_callback) {
            client->custom_disconnect_callback();
        }
        LOG_INFO("disconnectCallback completed. is_connected: " + std::to_string(client->m_connector.is_connected));
    }
}

void RedisAsyncClient::authCallback(redisAsyncContext* context, void* reply, void* privdata)
{
    RedisAsyncClient* client = static_cast<RedisAsyncClient*>(privdata);
    if (client)
    {
        client->handleAuth(static_cast<redisReply*>(reply));
    }
}

void RedisAsyncClient::pingCallback(redisAsyncContext* context, void* reply, void* privdata)
{
    RedisAsyncClient* client = static_cast<RedisAsyncClient*>(privdata);
    if (client)
    {
        client->handlePing(static_cast<redisReply*>(reply));
    }
}

void RedisAsyncClient::handleConnect(int status)
{
    if (status == REDIS_OK)
    {
        m_connector.is_connected = true;
        LOG_INFO("✓ Successfully connected to Redis");
        
        if (!m_connector.password.empty())
        {
            LOG_INFO("Authenticating...");
            redisAsyncCommand(m_connector.context, authCallback, this, "AUTH %s", 
                            m_connector.password.c_str());
        }
        else
        {
            LOG_INFO("Sending PING...");
            redisAsyncCommand(m_connector.context, pingCallback, this, "PING");
        }
    }
    else
    {
        m_connector.is_connected = false;
        m_connector.error_msg = "Connection failed: " + std::string(m_connector.context->errstr);
        LOG_ERROR("✗ " + m_connector.error_msg);
        stop_event_loop();
    }
}

void RedisAsyncClient::handleDisconnect(int status)
{
    m_connector.is_connected = false;
    if (status == REDIS_OK) {
        LOG_INFO("Disconnected from Redis (normal). is_connected set to false.");
    } else {
        LOG_INFO("Disconnected from Redis (error). is_connected set to false.");
    }
}

void RedisAsyncClient::handleAuth(redisReply* reply)
{
    if (reply == nullptr)
    {
        m_connector.error_msg = "AUTH failed (no reply)";
        LOG_ERROR("✗ " + m_connector.error_msg);
        stop_event_loop();
        return;
    }

    if (reply->type == REDIS_REPLY_STATUS && strcasecmp(reply->str, "OK") == 0)
    {
        LOG_INFO("✓ Authentication successful");
        LOG_INFO("Sending PING after auth...");
        redisAsyncCommand(m_connector.context, pingCallback, this, "PING");
    }
    else if (reply->type == REDIS_REPLY_ERROR)
    {
        m_connector.error_msg = std::string("AUTH error: ") + reply->str;
        LOG_ERROR("✗ " + m_connector.error_msg);
        stop_event_loop();
    }
    else
    {
        m_connector.error_msg = "Unexpected AUTH response";
        LOG_ERROR("✗ " + m_connector.error_msg);
        stop_event_loop();
    }
}

void RedisAsyncClient::handlePing(redisReply* reply)
{
    if (reply == nullptr)
    {
        m_connector.error_msg = "PING failed (no reply)";
        LOG_ERROR("✗ " + m_connector.error_msg);
    }
    else if (reply->type == REDIS_REPLY_STATUS && strcasecmp(reply->str, "PONG") == 0) {
        LOG_INFO("✓ PING successful - Redis is responding");
    }
    else if (reply->type == REDIS_REPLY_ERROR)
    {
        m_connector.error_msg = std::string("PING error: ") + reply->str;
        LOG_ERROR("✗ " + m_connector.error_msg);
    }
    else
    {
        m_connector.error_msg = "Unexpected PING response";
        LOG_ERROR("✗ " + m_connector.error_msg);
    }
}

void RedisAsyncClient::run_event_loop()
{
    if (m_connector.ev_base)
    {
        LOG_INFO("Starting Redis event loop...");
        event_base_dispatch(m_connector.ev_base);
        LOG_INFO("Redis event loop finished");
    }
}

void RedisAsyncClient::stop_event_loop()
{
    if (m_connector.ev_base)
    {
        LOG_INFO("Calling event_base_loopbreak...");
        event_base_loopexit(m_connector.ev_base, nullptr);
    }

    std::vector<std::unique_ptr<IRedisCallback>> to_invoke;
    {
        std::lock_guard<std::mutex> lock(m_pending_mutex);
        to_invoke.reserve(m_pending_callbacks.size());
        for (auto &kv : m_pending_callbacks) {
            to_invoke.emplace_back(std::move(kv.second));
        }
        m_pending_callbacks.clear();
    }
    for (auto &cb : to_invoke) {
        try { cb->onRedisReply(nullptr); } catch(...) {}
    }
}

void RedisAsyncClient::genericCallback(redisAsyncContext* context, void* reply, void* privdata)
{
    RedisAsyncClient* client = nullptr;
    if (context) client = static_cast<RedisAsyncClient*>(context->data);
    IRedisCallback* raw = static_cast<IRedisCallback*>(privdata);
    std::unique_ptr<IRedisCallback> cbptr;
    if (client && raw) {
        std::lock_guard<std::mutex> lock(client->m_pending_mutex);
        auto it = client->m_pending_callbacks.find(raw);
        if (it != client->m_pending_callbacks.end()) {
            cbptr = std::move(it->second);
            client->m_pending_callbacks.erase(it);
        }
    }
    if (!cbptr && raw) {
        cbptr.reset(raw);
    }
    if (cbptr) {
        try {
            cbptr->onRedisReply(static_cast<redisReply*>(reply));
        } catch (...) {
            LOG_ERROR("Exception in Redis callback");
        }
    }
}

void RedisAsyncClient::get(const std::string& key, std::unique_ptr<IRedisCallback> cb)
{
    if(!is_connected() || !cb)
    {
        if(cb)
        {
            try { cb->onRedisReply(nullptr); } catch(...) {}
        }
        return;
    }
    IRedisCallback* raw = cb.get();
    {
        std::lock_guard<std::mutex> lock(m_pending_mutex);
        m_pending_callbacks.emplace(raw, std::move(cb));
    }
    redisAsyncCommand(m_connector.context, genericCallback, raw, "GET %s", key.c_str());
}

void RedisAsyncClient::del(const std::string& key, std::unique_ptr<IRedisCallback> cb)
{
    if(!is_connected())
    {
        if(cb)
        {
            try { cb->onRedisReply(nullptr); } catch(...) {}
        }
        return;
    }
    if (!cb) {
        redisAsyncCommand(m_connector.context, nullptr, nullptr, "DEL %s", key.c_str());
        return;
    }
    IRedisCallback* raw = cb.get();
    {
        std::lock_guard<std::mutex> lock(m_pending_mutex);
        m_pending_callbacks.emplace(raw, std::move(cb));
    }
    redisAsyncCommand(m_connector.context, genericCallback, raw, "DEL %s", key.c_str());
}

void RedisAsyncClient::setex(const std::string& key, int ttl_seconds, const std::string& value, std::unique_ptr<IRedisCallback> cb)
{
    if(!is_connected())
    {
        if(cb)
        {
            try { cb->onRedisReply(nullptr); } catch(...) {}
        }
        return;
    }
    if (!cb) {
        redisAsyncCommand(m_connector.context, nullptr, nullptr, "SETEX %s %d %s", key.c_str(), ttl_seconds, value.c_str());
        return;
    }
    IRedisCallback* raw = cb.get();
    {
        std::lock_guard<std::mutex> lock(m_pending_mutex);
        m_pending_callbacks.emplace(raw, std::move(cb));
    }
    redisAsyncCommand(m_connector.context, genericCallback, raw, "SETEX %s %d %s", key.c_str(), ttl_seconds, value.c_str());
}

void RedisAsyncClient::setProtobuf(const std::string& key, int ttl_seconds, const google::protobuf::Message& message, std::unique_ptr<IRedisCallback> cb) {
    std::string serialized_data;
    if (!message.SerializeToString(&serialized_data)) {
        LOG_ERROR("Failed to serialize protobuf message");
        if (cb) cb->onRedisReply(nullptr);
        return;
    }
    setex(key, ttl_seconds, serialized_data, std::move(cb));
}

void RedisAsyncClient::getProtobuf(const std::string& key, google::protobuf::Message& message, std::shared_ptr<IRedisCallback> cb) {
    auto wrapper_cb = std::make_unique<RedisCallbackImpl>([&message, cb](redisReply* reply) {
        if (!reply || reply->type != REDIS_REPLY_STRING) {
            LOG_ERROR("Failed to retrieve protobuf data from Redis");
            if (cb) cb->onRedisReply(reply);
            return;
        }
        if (!message.ParseFromString(reply->str)) {
            LOG_ERROR("Failed to deserialize protobuf message");
            if (cb) cb->onRedisReply(reply);
            return;
        }
        if (cb) cb->onRedisReply(reply);
    });
    get(key, std::move(wrapper_cb));
}

void RedisAsyncClient::connect() {
    begin_async_connect();
}

bool RedisAsyncClient::is_connected() const {
    LOG_INFO("is_connected() called. Current state: " + std::to_string(m_connector.is_connected));
    return m_connector.is_connected;
}

void RedisAsyncClient::setDisconnectCallback(std::function<void()> callback) {
    custom_disconnect_callback = std::move(callback);
}
