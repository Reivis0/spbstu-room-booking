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

  // Defaults
  config["ruz_importer.api_url"] = "https://ruz.spbstu.ru/api/v1/ruz";
  config["import_interval_seconds"] = "1800"; 
  config["retry_attempts"] = "3";
  config["retry_delay_seconds"] = "5";

  // Load from file if exists
  std::ifstream file("configs/ruz_importer_config.ini");
  if (file.is_open())
  {
    std::string line, section;
    while (std::getline(file, line))
    {
      line = trim(line);
      if (line.empty() || line[0] == ';' || line[0] == '#') continue;
      if (line[0] == '[' && line.back() == ']') {
        section = trim(line.substr(1, line.size() - 2));
        continue;
      }
      size_t pos = line.find('=');
      if (pos != std::string::npos) {
        std::string key = trim(line.substr(0, pos));
        std::string value = trim(line.substr(pos + 1));
        std::string full_key = section.empty() ? key : section + "." + key;
        config[full_key] = value;
      }
    }
    LOG_INFO("RUS_IMPORTER: Config loaded from file.");
  }

  // Override with Environment Variables (RUZ_*)
  auto override_env = [&](const std::string& key, const char* env_name) {
      if (const char* env_p = std::getenv(env_name)) {
          config[key] = std::string(env_p);
          LOG_INFO("CONFIG: Override " + key + " from ENV " + env_name);
      }
  };

  override_env("ruz_importer.api_url", "RUZ_API_URL");
  override_env("import_interval_seconds", "RUZ_IMPORT_INTERVAL");
  override_env("retry_attempts", "RUZ_RETRY_ATTEMPTS");
  override_env("retry_delay_seconds", "RUZ_RETRY_DELAY");

  return config;
}