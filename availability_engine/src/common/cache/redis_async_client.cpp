#include "redis_async_client.hpp"

#include <iostream>
#include <fstream>
#include <cstring>

std::map<std::string, std::string> RedisAsyncClient::RedisConnector::read_config()
{
    std::map<std::string, std::string> config;
    std::ifstream file("configs/redis_config.ini");
    
    if (!file.is_open()) {
        LOG_ERROR("REDIS: Failed to open config file");
        return config;
    }
    
    std::string line, section;
    while (getline(file, line)) 
    {
        line.erase(0, line.find_first_not_of(" \t"));
        line.erase(line.find_last_not_of(" \t") + 1);
        
        if (line.empty() || line[0] == ';') continue;
            
        if (line[0] == '[' && line.back() == ']')
        {
            section = line.substr(1, line.size() - 2);
        } 
        else 
        {
            size_t pos = line.find('=');
            if (pos != std::string::npos)
            {
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
    return config;
}

bool RedisAsyncClient::RedisConnector::setup_event_base()
{
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
    
    if (!setup_event_base())
    {
        LOG_ERROR("REDIS: Failed to create event base");
    }
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
        LOG_ERROR("REDIS: ✗ " + m_connector.error_msg);
        return;
    }

    m_connector.context->data = this;
    if (redisLibeventAttach(m_connector.context, m_connector.ev_base) != REDIS_OK)
    {
        m_connector.error_msg = "Failed to attach to libevent";
        redisAsyncFree(m_connector.context);
        m_connector.context = nullptr;
        m_connector.is_connected = false;
        LOG_ERROR("REDIS: ✗ " + m_connector.error_msg);
        return;
    }

    redisAsyncSetConnectCallback(m_connector.context, connectCallback);
    redisAsyncSetDisconnectCallback(m_connector.context, disconnectCallback);
    LOG_INFO("REDIS: Redis connection started");
}

RedisAsyncClient::RedisAsyncClient()
{
    begin_async_connect();
}

void RedisAsyncClient::disconnect()
{
    if (m_connector.is_connected && m_connector.context)
    {
        redisAsyncDisconnect(m_connector.context);
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
        client->handleDisconnect(status);
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
        LOG_INFO("REDIS: ✓ Successfully connected to Redis");
        
        if (!m_connector.password.empty())
        {
            LOG_INFO("REDIS: Authenticating...");
            redisAsyncCommand(m_connector.context, authCallback, this, "AUTH %s", 
                            m_connector.password.c_str());
        }
        else
        {
            LOG_INFO("REDIS: Sending PING...");
            redisAsyncCommand(m_connector.context, pingCallback, this, "PING");
        }
    }
    else
    {
        m_connector.is_connected = false;
        m_connector.error_msg = "Connection failed: " + std::string(m_connector.context->errstr);
        LOG_ERROR("REDIS: ✗ " + m_connector.error_msg);
        stop_event_loop();
    }
}

void RedisAsyncClient::handleDisconnect(int status)
{
    m_connector.is_connected = false;
    if (status == REDIS_OK) {
        LOG_INFO("REDIS: Disconnected from Redis (normal)");
    } else {
        LOG_INFO("REDIS: Disconnected from Redis (error)");
    }
}

void RedisAsyncClient::handleAuth(redisReply* reply)
{
    if (reply == nullptr)
    {
        m_connector.error_msg = "AUTH failed (no reply)";
        LOG_ERROR("REDIS: ✗ " + m_connector.error_msg);
        stop_event_loop();
        return;
    }

    if (reply->type == REDIS_REPLY_STATUS && strcasecmp(reply->str, "OK") == 0)
    {
        LOG_INFO("REDIS: ✓ Authentication successful");
        LOG_INFO("REDIS: Sending PING after auth...");
        redisAsyncCommand(m_connector.context, pingCallback, this, "PING");
    }
    else if (reply->type == REDIS_REPLY_ERROR)
    {
        m_connector.error_msg = std::string("AUTH error: ") + reply->str;
        LOG_ERROR("REDIS: ✗ " + m_connector.error_msg);
        stop_event_loop();
    }
    else
    {
        m_connector.error_msg = "Unexpected AUTH response";
        LOG_ERROR("REDIS: ✗ " + m_connector.error_msg);
        stop_event_loop();
    }
}

void RedisAsyncClient::handlePing(redisReply* reply)
{
    if (reply == nullptr)
    {
        m_connector.error_msg = "PING failed (no reply)";
        LOG_ERROR("REDIS: ✗ " + m_connector.error_msg);
    }
    else if (reply->type == REDIS_REPLY_STATUS && strcasecmp(reply->str, "PONG") == 0) {
        LOG_INFO("REDIS: ✓ PING successful - Redis is responding");
    }
    else if (reply->type == REDIS_REPLY_ERROR)
    {
        m_connector.error_msg = std::string("PING error: ") + reply->str;
        LOG_ERROR("REDIS: ✗ " + m_connector.error_msg);
    }
    else
    {
        m_connector.error_msg = "Unexpected PING response";
        LOG_ERROR("REDIS: ✗ " + m_connector.error_msg);
    }
}

void RedisAsyncClient::run_event_loop()
{
    if (m_connector.ev_base)
    {
        LOG_INFO("REDIS: Starting Redis event loop...");
        event_base_dispatch(m_connector.ev_base);
        LOG_INFO("REDIS: Redis event loop finished");
    }
}

void RedisAsyncClient::stop_event_loop()
{
    if (m_connector.ev_base)
    {
        LOG_INFO("REDIS: Calling event_base_loopbreak...");
        event_base_loopexit(m_connector.ev_base, nullptr);
    }
}

void RedisAsyncClient::genericCallback(redisAsyncContext* context, void* reply, void* privdata)
{
    IRedisCallback* cb = static_cast<IRedisCallback*>(privdata);
    if(cb)
    {
        cb->onRedisReply(static_cast<redisReply*>(reply));
    }
}

void RedisAsyncClient::get(const std::string& key, IRedisCallback* cb)
{
    if(!is_connected() || !cb)
    {
        if(cb)
        {
            cb->onRedisReply(nullptr);
            delete cb;
        }
        return;
    }
    redisAsyncCommand(m_connector.context, genericCallback, cb,"GET %s", key.c_str());
}

void RedisAsyncClient::setex(const std::string& key, int ttl_seconds, const std::string& value, IRedisCallback* cb)
{
    if(!is_connected())
    {
        if(cb)
        {
            cb->onRedisReply(nullptr);
            delete cb;
        }
        return;
    }
    redisAsyncCommand(m_connector.context, cb ? genericCallback : nullptr, cb, "SETEX %s %d %s", key.c_str(), ttl_seconds, value.c_str());
}
