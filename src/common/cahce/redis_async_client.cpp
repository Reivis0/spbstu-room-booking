#include "redis_acync_client.hpp"

#include <fstream>

std::map<std::string, std::string> RedisAsyncClient::RedisConnector::read_config()
{
  std::map<std::string, std::string> config;
  std::ifstream file("/home/anns/поликек/3 курс/Конструирование ПО/spbstu-room-booking/include/configs/redis_config.ini");
  std::string line, section;
    
  while (getline(file, line)) 
  {
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
        std::string key = section + "." + line.substr(0, pos);
        std::string value = line.substr(pos + 1);
        config[key] = value;
      }
    }
  }
    
    return config;
}

RedisAsyncClient::RedisConnector::RedisConnector()
{
    auto config = read_config();
    host = config["booking_service.host"];
    port = std::stoi(config["booking_service.port"]);
    connection_timeout = std::stoi(config["booking_service.connection_timeout"]);
    is_connected = false;
}

void RedisAsyncClient::begin_async_connect()
{
   m_connector.context=  redisAsyncConnect(m_connector.host.c_str(), m_connector.port);
  if(m_connector.context == nullptr || m_connector.context->err)
  {
    m_connector.error_msg = "Fail connection";
  }
  m_connector.is_connected = true;
}

RedisAsyncClient::RedisAsyncClient()
{
  begin_async_connect();
}

void RedisAsyncClient::disconect()
{
  redisAsyncDisconnect(m_connector.context);
  if(m_connector.is_connected);
}

RedisAsyncClient::~RedisAsyncClient()
{
  disconect();
}