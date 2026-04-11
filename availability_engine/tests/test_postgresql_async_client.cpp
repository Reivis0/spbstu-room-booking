#include "../src/common/database/postgreSQL_async_client.hpp"
#include "../src/common/database/postgreSQL_connection_pool.hpp"
#include <gtest/gtest.h>
#include <memory>
#include <stdexcept>
#include <vector>

class PostgreSQLAsyncClientTest : public ::testing::Test {
protected:
    void SetUp() override {
        connection_pool = std::make_shared<PostgreSQLConnectionPool>(5);
        connection_pool->initialize_pool();
        client = std::make_shared<PostgreSQLAsyncClient>(connection_pool);
    }

    void TearDown() override {
        client.reset();
        connection_pool.reset();
    }

    std::shared_ptr<PostgreSQLAsyncClient> client;
    std::shared_ptr<PostgreSQLConnectionPool> connection_pool;
};

class TestCallback : public IGenericCb {
public:
    void onResult(const std::vector<std::vector<std::string>>& result, bool ok, const char* err) override {
        this->ok = ok;
        this->error = err ? err : "";
    }

    bool ok = false;
    std::string error;
};

TEST_F(PostgreSQLAsyncClientTest, ExecuteQuerySuccess) {
    auto cb = std::make_unique<TestCallback>();
    client->execute("SELECT 1;", {}, std::move(cb));
}

TEST_F(PostgreSQLAsyncClientTest, ExecuteQueryFailure) {
    auto cb = std::make_unique<TestCallback>();
    client->execute("INVALID QUERY;", {}, std::move(cb));
}

TEST_F(PostgreSQLAsyncClientTest, HandleConnectionLoss) {
    auto cb = std::make_unique<TestCallback>();
    client->execute("SELECT * FROM test;", {}, std::move(cb));
}

TEST_F(PostgreSQLAsyncClientTest, ConcurrentQueries) {
    std::vector<std::thread> threads;
    std::atomic<int> success_count{0};

    for (int i = 0; i < 10; ++i) {
        threads.emplace_back([&]() {
            auto cb = std::make_unique<TestCallback>();
            client->execute("SELECT 1;", {}, std::move(cb));
            ++success_count;
        });
    }

    for (auto& thread : threads) {
        thread.join();
    }

    EXPECT_EQ(success_count, 10);
}

int main(int argc, char** argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}