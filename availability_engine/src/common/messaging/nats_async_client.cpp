#include "nats_async_client.hpp"
#include <iostream>

std::map<std::string, std::string> NatsAsyncClient::read_config()
{
  std::map<std::string, std::string> config;
  std::ifstream file("configs/nats_config.ini");
  
  if (!file.is_open()) {
    LOG_ERROR("Failed to open config file");
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

NatsAsyncClient::NatsAsyncClient()
{
    auto config = read_config();
    m_nats_connect.m_host = config["booking_service.host"];
    if (m_nats_connect.m_host.empty()) m_nats_connect.m_host = "localhost";

    try {
        m_nats_connect.m_client_port = std::stoi(config["booking_service.client_port"]);
    } catch(...) {
        m_nats_connect.m_client_port = 4222;
    }

    // Construct m_url using host and port
    m_url = "nats://" + m_nats_connect.m_host + ":" + std::to_string(m_nats_connect.m_client_port);

    // Log the constructed URL
    LOG_INFO("Constructed NATS URL: " + m_url);

    connect();
}

NatsAsyncClient::~NatsAsyncClient()
{
    disconnect();
    if (m_opts) {
        natsOptions_Destroy(m_opts);
        m_opts = nullptr;
    }
}

std::string NatsAsyncClient::trim(const std::string& str)
{
    size_t first = str.find_first_not_of(" \t\r\n");
    if (std::string::npos == first) return str;
    size_t last = str.find_last_not_of(" \t\r\n");
    return str.substr(first, (last - first + 1));
}

void NatsAsyncClient::connect()
{
    std::lock_guard<std::mutex> lock(m_conn_mutex);
    natsStatus s = natsOptions_Create(&m_opts);
    if (s == NATS_OK)
    {
        natsOptions_SetURL(m_opts, m_url.c_str());
        s = natsConnection_Connect(&m_conn, m_opts);
    }

    if (s != NATS_OK) {
        LOG_ERROR("Failed to connect to NATS at " + m_url + ": " + natsStatus_GetText(s));
    } else {
        LOG_INFO("Connected to NATS at " + m_url);
    }
}

void NatsAsyncClient::disconnect() {
    static bool is_disconnected = false; // Ensure disconnect is called only once

    if (is_disconnected) {
        LOG_WARN("NATS disconnect called multiple times.");
        return;
    }

    std::lock_guard<std::mutex> lock(m_conn_mutex);
    if (m_conn) {
        natsConnection_Destroy(m_conn);
        m_conn = nullptr;
        LOG_INFO("Disconnected from NATS.");
    }

    is_disconnected = true;
}

void NatsAsyncClient::publish(const std::string& subject, const std::string& data)
{
    std::lock_guard<std::mutex> lock(m_conn_mutex);
    if (!m_conn) return;

    natsStatus s = natsConnection_Publish(m_conn, subject.c_str(), data.c_str(), data.size());
    if (s != NATS_OK) {
        LOG_ERROR(std::string("NATS Publish error: ") + natsStatus_GetText(s));
    }
}

void NatsAsyncClient::publishScheduleRefreshed(const std::string& room_id, const std::string& date)
{
    std::string payload = "{ \"event\": \"schedule_refreshed\", \"room_id\": \"" + room_id + "\", \"date\": \"" + date + "\" }";
    publish("events.schedule_refreshed", payload);
}

bool NatsAsyncClient::is_connected() const {
    std::lock_guard<std::mutex> lock(m_conn_mutex);
    return m_conn != nullptr; // Проверяем, установлено ли соединение
}