#ifndef LOGGER_HPP
#define LOGGER_HPP

#include <string>
#include <mutex>


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

    void init() {};

    void log(LogLevel level, const std::string& message);

private:
    Logger() = default;
    ~Logger() {};

    std::string getTimestamp();
    std::string levelToString(LogLevel level);

    std::mutex m_mutex;
};

#define LOG_DEBUG(msg) Logger::getInstance().log(LogLevel::DEBUG, msg)
#define LOG_INFO(msg)  Logger::getInstance().log(LogLevel::INFO, msg)
#define LOG_WARN(msg)  Logger::getInstance().log(LogLevel::WARNING, msg)
#define LOG_ERROR(msg) Logger::getInstance().log(LogLevel::ERROR, msg)

#endif
