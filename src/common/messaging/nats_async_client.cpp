#include "nats_async_client.hpp"

std::map<std::string, std::string> NatsAsyncClient::read_config()
{
  std::map<std::string, std::string> config;
  std::ifstream file("configs/nats_config.ini");
  
  if (!file.is_open()) {
    std::cerr << "Failed to open config file" << std::endl;
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
    m_nats_connect.m_client_port = std::stoi(config["booking_service.client_port"]);

    m_url = "nats://" + m_nats_connect.m_host + ":"+ std::to_string(m_nats_connect.m_client_port);
    
    connect();
}

NatsAsyncClient::~NatsAsyncClient()
{
    if (m_conn)
    {
        natsConnection_Destroy(m_conn);
    }
    if (m_opts) {
        natsOptions_Destroy(m_opts);
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
    natsStatus s = natsOptions_Create(&m_opts);
    if (s == NATS_OK)
    {
        natsOptions_SetURL(m_opts, m_url.c_str());
        s = natsConnection_Connect(&m_conn, m_opts);
    }

    if (s != NATS_OK) {
        std::cerr << "Failed to connect to NATS at " << m_url << ": " << natsStatus_GetText(s) << std::endl;
    } else {
        std::cout << "Connected to NATS at " << m_url << std::endl;
    }
}

void NatsAsyncClient::publish(const std::string& subject, const std::string& data)
{
    if (!m_conn) return;

    natsStatus s = natsConnection_Publish(m_conn, subject.c_str(), data.c_str(), data.size());
    if (s != NATS_OK) {
        std::cerr << "NATS Publish error: " << natsStatus_GetText(s) << std::endl;
    }
}

void NatsAsyncClient::publishScheduleRefreshed(const std::string& room_id, const std::string& date)
{
    std::string payload = "{ \"event\": \"schedule_refreshed\", \"room_id\": \"" + room_id + "\", \"date\": \"" + date + "\" }";
    publish("events.schedule_refreshed", payload);
}
