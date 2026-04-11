#include "redis_async_client.hpp"
#include "logger.hpp"
#include "message.pb.h" // Include the generated protobuf header
#include <gtest/gtest.h>
#include <thread>
#include <chrono>
#include <memory>
#include <condition_variable>
#include <mutex>

class RedisAsyncClientTest : public ::testing::Test {
protected:
    RedisAsyncClient client;

    void SetUp() override {
        client.connect();
        std::this_thread::sleep_for(std::chrono::seconds(1)); // Allow connection to establish
    }

    void TearDown() override {
        client.disconnect();
    }
};

TEST_F(RedisAsyncClientTest, TestConnection) {
    ASSERT_TRUE(client.is_connected());
}

TEST_F(RedisAsyncClientTest, TestSetAndGet) {
    std::string key = "test_key";
    std::string value = "test_value";

    auto set_callback = std::make_unique<RedisCallbackImpl>([](redisReply* reply) {
        ASSERT_NE(reply, nullptr);
        ASSERT_EQ(reply->type, REDIS_REPLY_STATUS);
        ASSERT_STREQ(reply->str, "OK");
    });

    client.setex(key, 10, value, std::move(set_callback));

    std::this_thread::sleep_for(std::chrono::seconds(1)); // Allow command to execute

    auto get_callback = std::make_unique<RedisCallbackImpl>([&value](redisReply* reply) {
        ASSERT_NE(reply, nullptr);
        ASSERT_EQ(reply->type, REDIS_REPLY_STRING);
        ASSERT_EQ(reply->str, value);
    });

    client.get(key, std::move(get_callback));

    std::this_thread::sleep_for(std::chrono::seconds(1)); // Allow command to execute
}

TEST_F(RedisAsyncClientTest, TestSetAndGetProtobuf) {
    std::string key = "protobuf_key";
    room_service::ValidateRequest message;
    message.set_room_id("test_room");
    message.set_date("2026-04-11");

    auto set_callback = std::make_unique<RedisCallbackImpl>([](redisReply* reply) {
        ASSERT_NE(reply, nullptr);
        ASSERT_EQ(reply->type, REDIS_REPLY_STATUS);
        ASSERT_STREQ(reply->str, "OK");
    });

    client.setProtobuf(key, 10, message, std::move(set_callback));

    std::this_thread::sleep_for(std::chrono::seconds(1)); // Allow command to execute

    room_service::ValidateResponse retrieved_message;
    auto get_callback = std::make_shared<RedisCallbackImpl>([&retrieved_message](redisReply* reply) {
        ASSERT_NE(reply, nullptr);
        ASSERT_EQ(reply->type, REDIS_REPLY_STRING);
        ASSERT_TRUE(retrieved_message.ParseFromString(reply->str));
    });

    client.getProtobuf(key, retrieved_message, get_callback);

    std::this_thread::sleep_for(std::chrono::seconds(1)); // Allow command to execute
}

TEST_F(RedisAsyncClientTest, TestDisconnect) {
    std::mutex mtx;
    std::condition_variable cv;
    bool callback_invoked = false;

    // Set up a mechanism to wait for the disconnect callback
    client.setDisconnectCallback([&]() {
        std::unique_lock<std::mutex> lock(mtx);
        callback_invoked = true;
        cv.notify_one();
    });

    client.disconnect();

    // Wait for the callback to be invoked or timeout
    {
        std::unique_lock<std::mutex> lock(mtx);
        cv.wait_for(lock, std::chrono::seconds(5), [&]() { return callback_invoked; });
    }

    ASSERT_FALSE(client.is_connected());
}