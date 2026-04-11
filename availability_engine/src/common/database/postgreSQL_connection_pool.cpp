#include "postgreSQL_connection_pool.hpp"
#include "postgreSQL_async_client.hpp"
#include "logger.hpp"

PostgreSQLConnectionPool::PostgreSQLConnectionPool(size_t pool_size) : m_pool_size(pool_size) {
    // Constructor no longer calls initialize_pool
}

PostgreSQLConnectionPool::~PostgreSQLConnectionPool() {
    std::lock_guard<std::mutex> lock(m_pool_mutex);
    while (!m_pool.empty()) {
        m_pool.pop();
    }
}

void PostgreSQLConnectionPool::initialize_pool() {
    for (size_t i = 0; i < m_pool_size; ++i) {
        auto connection = std::make_shared<PostgreSQLAsyncClient>(shared_from_this());
        m_pool.push(connection);
    }
}

std::shared_ptr<PostgreSQLAsyncClient> PostgreSQLConnectionPool::acquire() {
    std::unique_lock<std::mutex> lock(m_pool_mutex);
    if (!m_condition.wait_for(lock, std::chrono::seconds(5), [this]() { return !m_pool.empty(); })) {
        LOG_ERROR("Timeout while acquiring connection from pool");
        return nullptr;
    }

    auto connection = m_pool.front();
    m_pool.pop();
    LOG_INFO("Connection acquired from pool");
    return connection;
}

void PostgreSQLConnectionPool::release(std::shared_ptr<PostgreSQLAsyncClient> connection) {
    {
        std::lock_guard<std::mutex> lock(m_pool_mutex);
        m_pool.push(connection);
    }
    m_condition.notify_one();
}