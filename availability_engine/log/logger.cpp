#include "logger.hpp"
#include <filesystem> // Нужен C++17

namespace fs = std::filesystem;

void Logger::init(const std::string& filename)
{
    std::lock_guard<std::mutex> lock(m_mutex);
    
    if (m_file.is_open()) {
        m_file.close();
    }

    fs::path current_source_path = __FILE__;
    
    fs::path project_root = current_source_path.parent_path().parent_path();
    
    fs::path log_dir = project_root / "logs";

    if (!fs::exists(log_dir))
    {
        try
        {
            fs::create_directories(log_dir);
        } catch (const std::exception& e)
        {
            std::cerr << "[CRITICAL] Failed to create log directory: " << e.what() << std::endl;
            return;
        }
    }

    fs::path full_path = log_dir / filename;

    m_file.open(full_path, std::ios::out | std::ios::trunc);

    if (!m_file.is_open()) {
        std::cerr << "[CRITICAL] Failed to open log file: " << full_path << std::endl;
    } else {
        std::cout << "[LOGGER] Logs will be written to: " << full_path << std::endl;
    }
}

Logger::~Logger()
{
    if (m_file.is_open())
    {
        m_file.close();
    }
}

void Logger::log(LogLevel level, const std::string& message)
{
    std::lock_guard<std::mutex> lock(m_mutex);
    
    std::string timestamp = getTimestamp();
    std::string levelStr = levelToString(level);
    
    std::stringstream ss;
    ss << "[" << timestamp << "] [" << levelStr << "] " << message << "\n";
    std::string logEntry = ss.str();

    if (m_file.is_open())
    {
        m_file << logEntry;
        m_file.flush(); 
    }

    std::cout << logEntry;
}

std::string Logger::getTimestamp()
{
    auto now = std::time(nullptr);
    auto tm = *std::localtime(&now);
    std::stringstream ss;
    ss << std::put_time(&tm, "%Y-%m-%d %H:%M:%S");
    return ss.str();
}

std::string Logger::levelToString(LogLevel level)
{
    switch (level)
    {
        case LogLevel::DEBUG:   return "DEBUG";
        case LogLevel::INFO:    return "INFO ";
        case LogLevel::WARNING: return "WARN ";
        case LogLevel::ERROR:   return "ERROR";
        default: return "UNKNOWN";
    }
}
