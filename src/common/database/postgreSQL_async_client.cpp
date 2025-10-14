#include "postgreSQL_async_client.hpp"

#include <fstream>
#include <exception>
#include <sstream>
#include <iostream>

PostgreSQLAsyncClient::Connector::Connector()
{
  auto config = read_config();
  host = config["booking_service.host"];
  port = config["booking_service.port"];
  db_name = config["booking_service.database_name"];
  username = config["booking_service.username"];
  password = config["booking_service.password"];
}

std::map<std::string, std::string> PostgreSQLAsyncClient::Connector::read_config()
{
  std::map<std::string, std::string> config;
  std::ifstream file("configs/pg_config.ini");
  
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
std::string PostgreSQLAsyncClient::Connector::get_connecting_str()
{
  std::stringstream conn_str;
  conn_str <<"host="+host
           <<" port="+port
           <<" dbname="+db_name
           <<" user="+username
           <<" password="+password;
  return conn_str.str();  
}

void PostgreSQLAsyncClient::begin_async_connect()
{
  Connector connector;

  m_connect.connection = PQconnectStart(connector.get_connecting_str().c_str());

  if (!m_connect.connection) 
  {
    m_connect.status = ConnectStatus::ERROR;
    m_connect.error_msg = "Failed to create connection object";
    std::cout<<m_connect.error_msg<<std::endl;
    return;
  }

  if (PQsetnonblocking(m_connect.connection, 1) != 0) 
  {
    m_connect.error_msg = "Failed to set non-blocking mode: " + std::string(PQerrorMessage(m_connect.connection));
    m_connect.status = ConnectStatus::ERROR;
    PQfinish(m_connect.connection);
    m_connect.connection = nullptr;
    return;
  }

  m_connect.socket_fd = PQsocket(m_connect.connection);
  if (m_connect.socket_fd < 0) 
  {
    m_connect.error_msg = "Invalid socket descriptor";
    m_connect.status = ConnectStatus::ERROR;
    PQfinish(m_connect.connection);
    m_connect.connection = nullptr;
    return;
  }
  PostgresPollingStatusType status = PQconnectPoll(m_connect.connection);
  switch (status)
  {
    case PGRES_POLLING_READING:
      m_connect.status = ConnectStatus::NEED_READ;
      break;
    case PGRES_POLLING_WRITING:
      m_connect.status = ConnectStatus::NEED_WRITE;
      break;
    case PGRES_POLLING_OK:
      m_connect.status = ConnectStatus::CONNECTED;
      m_connect.is_connected = true;
      break;
    case PGRES_POLLING_FAILED: 
     m_connect.status = ConnectStatus::ERROR;
     m_connect.error_msg = "Connection faild " + std::string(PQerrorMessage(m_connect.connection));
      break;
    default:
      m_connect.status = ConnectStatus::ERROR;
      m_connect.error_msg = "Unknown connection status";
      break;
  }
  if(m_connect.status != ConnectStatus::ERROR)
  {
    std::cout<<"Connected PG"<<std::endl;
  }
  else 
  {
    std::cerr<<"Error connecteing PG"<<std::endl;
  }
}

PostgreSQLAsyncClient::PostgreSQLAsyncClient()
{
  std::cout<<"Starting connect to PG\n";
  begin_async_connect();
}


void PostgreSQLAsyncClient::disconnect() 
{
  if (m_connect.connection)
  {
      PQfinish(m_connect.connection);
      m_connect.connection = nullptr;
      m_connect.socket_fd = -1;
      m_connect.is_connected = false;
  }
  std::cout<<"disconnect PG"<<std::endl;
}

PostgreSQLAsyncClient::~PostgreSQLAsyncClient()
{
  disconnect();
}