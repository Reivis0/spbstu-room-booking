#include "../src/common/database/postgreSQL_async_client.hpp"
#include <future>
#include <gtest/gtest.h>
#include <thread>
#include <vector>

class PostgreSQLConnectionPoolTest : public ::testing::Test {
protected:
    void SetUp() override {
        pool = std::make_shared<PostgreSQLConnectionPool>(5); // Use std::make_shared for proper memory management
        pool->initialize_pool(); // Explicitly initialize the pool after creation
    }

    void TearDown() override {
        pool.reset(); // Reset the shared pointer
    }

    std::shared_ptr<PostgreSQLConnectionPool> pool;
};

TEST_F(PostgreSQLConnectionPoolTest, AcquireAndReleaseConnections) {
    std::vector<std::shared_ptr<PostgreSQLAsyncClient>> connections;

    for (int i = 0; i < 5; ++i) {
        auto conn = pool->acquire();
        EXPECT_NE(conn, nullptr);
        connections.push_back(conn);
    }

    auto future = std::async(std::launch::async, [&]() {
        return pool->acquire();
    });
    EXPECT_EQ(future.wait_for(std::chrono::milliseconds(100)), std::future_status::timeout);

    pool->release(connections.back());
    connections.pop_back();

    auto conn = pool->acquire();
    EXPECT_NE(conn, nullptr);
}

TEST_F(PostgreSQLConnectionPoolTest, ConcurrentAccess) {
    std::vector<std::thread> threads;
    std::atomic<int> acquired_count{0};

    for (int i = 0; i < 10; ++i) {
        threads.emplace_back([&]() {
            auto conn = pool->acquire();
            if (conn) {
                ++acquired_count;
                std::this_thread::sleep_for(std::chrono::milliseconds(50));
                pool->release(conn);
            }
        });
    }

    for (auto& thread : threads) {
        thread.join();
    }

    EXPECT_EQ(acquired_count, 10);
}

int main(int argc, char** argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}