#ifndef LOGGER_HPP
#define LOGGER_HPP

#include <string>
#include <mutex>
#include <ctime>
#include <sstream>
#include <iomanip>
#include <iostream>

enum class LogLevel
{
    DEBUG,
    INFO,
    WARNING,
    ERROR
};

class Logger
{
public:
  Logger(const Logger&) = delete;
  Logger& operator=(const Logger&) = delete;

  static Logger& getInstance()
  {
    static Logger instance;
    return instance;
  }

  void init(const std::string& serviceName) {
      m_serviceName = serviceName;
  };
  void log(LogLevel level, const std::string& message);

  static void setTraceId(const std::string& traceId);
  static void clearTraceId();
  static std::string generateTraceId();

private:
  Logger() : m_serviceName("unknown-cpp-service") {}
  ~Logger() {};

  std::string getTimestamp();
  std::string levelToString(LogLevel level);
  std::string getTraceId();

  std::mutex m_mutex;
  std::string m_serviceName;
  static thread_local std::string t_traceId;
};

#define LOG_DEBUG(msg) Logger::getInstance().log(LogLevel::DEBUG, msg)
#define LOG_INFO(msg)  Logger::getInstance().log(LogLevel::INFO, msg)
#define LOG_WARN(msg)  Logger::getInstance().log(LogLevel::WARNING, msg)
#define LOG_ERROR(msg) Logger::getInstance().log(LogLevel::ERROR, msg)

#endif
