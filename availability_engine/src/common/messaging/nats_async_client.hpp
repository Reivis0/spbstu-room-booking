#ifndef NATS_ASYNC_CLIENT_HPP
#define NATS_ASYNC_CLIENT_HPP

#include <logger.hpp>
#include <nats.h>
#include <string>
#include <mutex>
#include <functional>
#include <memory>
#include <iostream>
#include <fstream>
#include <sstream>
#include <map>

class NatsAsyncClient
{
public:
    NatsAsyncClient();
    ~NatsAsyncClient();

    void connect();
    void disconnect();
    void publish(const std::string& subject, const std::string& data);
    void publishScheduleRefreshed(const std::string& room_id, const std::string& date);
    bool is_connected() const;

private:
    struct NatsConnection
    {
      std::string m_host;
      int m_client_port;
    };
    NatsConnection m_nats_connect;
    std::string trim(const std::string& str);
    std::map<std::string, std::string> read_config();

    std::string m_url;
    natsConnection* m_conn = nullptr;
    natsOptions* m_opts = nullptr;
    mutable std::mutex m_conn_mutex;
    bool m_running = true; // Flag to indicate if the client is running
};

#endif // NATS_ASYNC_CLIENT_HPP
