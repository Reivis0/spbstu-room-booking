#include "logger.hpp"
#include <ctime>
#include <iomanip>
#include <iostream>
#include <chrono>
#include <atomic>

thread_local std::string Logger::t_traceId = "";

void Logger::setTraceId(const std::string& traceId) {
    t_traceId = traceId;
}

void Logger::clearTraceId() {
    t_traceId = "";
}

std::string Logger::getTraceId() {
    return t_traceId.empty() ? "--" : t_traceId;
}

std::string Logger::generateTraceId() {
    auto now = std::chrono::system_clock::now().time_since_epoch().count();
    static std::atomic<uint64_t> counter{0};
    std::stringstream ss;
    ss << std::hex << now << std::setfill('0') << std::setw(4) << (counter.fetch_add(1) & 0xFFFF);
    return ss.str();
}

void Logger::log(LogLevel level, const std::string& message)
{
    std::lock_guard<std::mutex> lock(m_mutex);
    
    std::string timestamp = getTimestamp();
    std::string levelStr = levelToString(level);
    
    std::cout << "[" << timestamp << "] "
              << "[" << m_serviceName << "] "
              << "[" << std::left << std::setw(5) << levelStr << "] "
              << "[trace_id=" << getTraceId() << "] - " 
              << message << std::endl;
}

std::string Logger::getTimestamp()
{
    auto now = std::chrono::system_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(now.time_since_epoch()) % 1000;
    auto timer = std::chrono::system_clock::to_time_t(now);
    std::tm bt = *std::gmtime(&timer);

    std::stringstream ss;
    ss << std::put_time(&bt, "%Y-%m-%dT%H:%M:%S");
    ss << '.' << std::setfill('0') << std::setw(3) << ms.count() << 'Z';
    return ss.str();
}

std::string Logger::levelToString(LogLevel level)
{
    switch (level)
    {
        case LogLevel::DEBUG:   return "DEBUG";
        case LogLevel::INFO:    return "INFO";
        case LogLevel::WARNING: return "WARN";
        case LogLevel::ERROR:   return "ERROR";
        default: return "UNKNOWN";
    }
}
