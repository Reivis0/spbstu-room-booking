#include <gtest/gtest.h>
#include "common/messaging/nats_async_client.hpp"

class NATSClientTest : public ::testing::Test {
protected:
    NatsAsyncClient client;

    void SetUp() override {
        // Initialize the NATS client with default parameters
        client.connect();
    }

    void TearDown() override {
        // Disconnect the NATS client after each test
        client.disconnect();
    }
};

TEST_F(NATSClientTest, TestConnection) {
    ASSERT_TRUE(client.is_connected());
}

TEST_F(NATSClientTest, TestPublish) {
    std::string subject = "test.subject";
    std::string message = "Hello, NATS!";

    ASSERT_NO_THROW(client.publish(subject, message));
}