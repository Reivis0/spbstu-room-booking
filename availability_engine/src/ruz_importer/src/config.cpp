#include "config.hpp"
#include <logger.hpp>
#include <fstream>
#include <sstream>

std::map<std::string, std::string> load_config()
{
  std::map<std::string, std::string> config;
  
  config["api_url"] = "https://ruz.spbstu.ru/api/v1/ruz/scheduler/";
  config["import_interval_seconds"] = "1800"; 
  config["retry_attempts"] = "3";
  config["retry_delay_seconds"] = "5";
  
  std::ifstream file("configs/ruz_importer_config.ini");
  if (!file.is_open())
  {
    LOG_WARN("RUS_IMPORTER: CONFIG: Config file not found, using defaults");
    return config;
  }
  
  std::string line, section;
  while (std::getline(file, line))
  {
    line.erase(0, line.find_first_not_of(" \t"));
    line.erase(line.find_last_not_of(" \t") + 1);
    
    if (line.empty() || line[0] == ';' || line[0] == '#')
    {
      continue;
    }
    
    if (line[0] == '[' && line.back() == ']')
    {
      section = line.substr(1, line.size() - 2);
      continue;
    }
    
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
  
  LOG_INFO("RUS_IMPORTER: CONFIG: Configuration loaded successfully");
  return config;
}