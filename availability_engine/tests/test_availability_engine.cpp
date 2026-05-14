#include "async_room_service.hpp"
#include "logger.hpp"
#include "../src/common/database/postgreSQL_connection_pool.hpp"
#include "pqxx_pool.hpp"
#include <gtest/gtest.h>
#include <memory>
#include <csignal>
#include <thread>
#include <chrono>

class AsyncRoomServiceTest : public ::testing::Test {
protected:
    void SetUp() override {
        redis_client = std::make_shared<RedisAsyncClient>();
        
        pg_pool = std::make_shared<PostgreSQLConnectionPool>(5);
        pg_pool->initialize_pool();
        pg_client = std::make_shared<PostgreSQLAsyncClient>(pg_pool);
        
        pqxx_pool = std::make_shared<PqxxConnectionPool>(5, "postgresql://postgres:postgres@postgres:5432/booking_mvp_db");
        
        nats_client = std::make_shared<NatsAsyncClient>();
        service = std::make_unique<AsyncRoomService>(redis_client, pg_client, nats_client, pqxx_pool);
    }

    void TearDown() override {
        service->shutdown();
    }

    std::shared_ptr<RedisAsyncClient> redis_client;
    std::shared_ptr<PostgreSQLConnectionPool> pg_pool;
    std::shared_ptr<PostgreSQLAsyncClient> pg_client;
    std::shared_ptr<PqxxConnectionPool> pqxx_pool;
    std::shared_ptr<NatsAsyncClient> nats_client;
    std::unique_ptr<AsyncRoomService> service;
};

TEST_F(AsyncRoomServiceTest, StartAndStopService) {
    EXPECT_NO_THROW(service->start());
    EXPECT_TRUE(service->isRunning());

    service->shutdown();
    EXPECT_FALSE(service->isRunning());
}

// TEST_F(AsyncRoomServiceTest, HandleSignal) removed because it terminates the test runner process.

int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}