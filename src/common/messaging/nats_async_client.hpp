#ifndef NATS_ASYNC_CLIENT_HPP
#define NATS_ASYNC_CLIENT_HPP

#include <nats/nats.h>
#include <string>
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
    void publish(const std::string& subject, const std::string& data);
    void publishScheduleRefreshed(const std::string& room_id, const std::string& date);

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
};

#endif // NATS_ASYNC_CLIENT_HPP
