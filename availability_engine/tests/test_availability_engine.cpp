#include "async_room_service.hpp"
#include "logger.hpp"
#include <gtest/gtest.h>
#include <memory>

class AsyncRoomServiceTest : public ::testing::Test {
protected:
    void SetUp() override {
        redis_client = std::make_shared<RedisAsyncClient>();
        pg_client = std::make_shared<PostgreSQLAsyncClient>();
        nats_client = std::make_shared<NatsAsyncClient>();
        service = std::make_unique<AsyncRoomService>(redis_client, pg_client, nats_client);
    }

    void TearDown() override {
        service->shutdown();
    }

    std::shared_ptr<RedisAsyncClient> redis_client;
    std::shared_ptr<PostgreSQLAsyncClient> pg_client;
    std::shared_ptr<NatsAsyncClient> nats_client;
    std::unique_ptr<AsyncRoomService> service;
};

TEST_F(AsyncRoomServiceTest, StartAndStopService) {
    EXPECT_NO_THROW(service->start());
    EXPECT_TRUE(service->isRunning());

    service->shutdown();
    EXPECT_FALSE(service->isRunning());
}

TEST_F(AsyncRoomServiceTest, HandleSignal) {
    EXPECT_NO_THROW(service->start());
    EXPECT_TRUE(service->isRunning());

    // Simulate signal handling
    raise(SIGINT);
    std::this_thread::sleep_for(std::chrono::seconds(1));

    // Add a timeout to prevent hanging
    auto start_time = std::chrono::steady_clock::now();
    while (service->isRunning()) {
        if (std::chrono::steady_clock::now() - start_time > std::chrono::seconds(5)) {
            FAIL() << "Service did not stop within the timeout period.";
            break;
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }

    EXPECT_FALSE(service->isRunning());
}

int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}