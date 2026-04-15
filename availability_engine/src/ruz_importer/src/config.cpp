#include "config.hpp"
#include <logger.hpp>
#include <fstream>
#include <sstream>
#include <algorithm>

static std::string trim(std::string s) {
    s.erase(std::remove(s.begin(), s.end(), '\r'), s.end());
    s.erase(std::remove(s.begin(), s.end(), '\n'), s.end());
    size_t first = s.find_first_not_of(" \t");
    if (std::string::npos == first) return "";
    size_t last = s.find_last_not_of(" \t");
    return s.substr(first, (last - first + 1));
}

std::map<std::string, std::string> load_config()
{
  std::map<std::string, std::string> config;

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
    line = trim(line);

    if (line.empty() || line[0] == ';' || line[0] == '#')
    {
      continue;
    }

    if (line[0] == '[' && line.back() == ']')
    {
      section = trim(line.substr(1, line.size() - 2));
      LOG_INFO("CONFIG: Found section [" + section + "]");
      continue;
    }

    size_t pos = line.find('=');
    if (pos != std::string::npos)
    {
      std::string key = trim(line.substr(0, pos));
      std::string value = trim(line.substr(pos + 1));

      std::string full_key = section.empty() ? key : section + "." + key;
      config[full_key] = value;
      LOG_INFO("CONFIG: Loaded key " + full_key + " = " + value);
    }
  }

  LOG_INFO("RUS_IMPORTER: CONFIG: Configuration loaded successfully. Total keys: " + std::to_string(config.size()));
  return config;
}