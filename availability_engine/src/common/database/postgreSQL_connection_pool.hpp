#include "postgreSQL_async_client.hpp"

class PostgreSQLAsyncClient; // Forward declaration

#ifndef POSTGRESQL_CONNECTION_POOL_HPP
#define POSTGRESQL_CONNECTION_POOL_HPP

#include <memory>
#include <queue>
#include <mutex>
#include <condition_variable>

class PostgreSQLConnectionPool : public std::enable_shared_from_this<PostgreSQLConnectionPool> {
public:
    PostgreSQLConnectionPool(size_t pool_size);
    ~PostgreSQLConnectionPool();

    std::shared_ptr<PostgreSQLAsyncClient> acquire();
    void release(std::shared_ptr<PostgreSQLAsyncClient> connection);

private:
    void initialize_pool();

    size_t m_pool_size;
    std::queue<std::shared_ptr<PostgreSQLAsyncClient>> m_pool;
    std::mutex m_pool_mutex;
    std::condition_variable m_condition;

    friend class PostgreSQLConnectionPoolTest; // Grant access to the test class
};

#endif // POSTGRESQL_CONNECTION_POOL_HPP