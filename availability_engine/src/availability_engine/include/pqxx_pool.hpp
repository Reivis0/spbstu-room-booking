#ifndef PQXX_POOL_HPP
#define PQXX_POOL_HPP

#include <pqxx/pqxx>
#include <memory>
#include <mutex>
#include <condition_variable>
#include <queue>
#include <string>

class PqxxConnectionPool {
public:
    PqxxConnectionPool(size_t size, const std::string& conn_str) : pool_size_(size), conn_str_(conn_str) {
        for (size_t i = 0; i < size; ++i) {
            pool_.push(std::make_unique<pqxx::connection>(conn_str_));
        }
    }

    std::unique_ptr<pqxx::connection> acquire() {
        std::unique_lock<std::mutex> lock(mutex_);
        cv_.wait(lock, [this] { return !pool_.empty(); });
        auto conn = std::move(pool_.front());
        pool_.pop();
        return conn;
    }

    void release(std::unique_ptr<pqxx::connection> conn) {
        std::lock_guard<std::mutex> lock(mutex_);
        pool_.push(std::move(conn));
        cv_.notify_one();
    }

private:
    size_t pool_size_;
    std::string conn_str_;
    std::queue<std::unique_ptr<pqxx::connection>> pool_;
    std::mutex mutex_;
    std::condition_variable cv_;
};

#endif
