#include "async_room_service.hpp"
#include "cache/redis_async_client.hpp"
#include "database/postgreSQL_async_client.hpp"
#include "messaging/nats_async_client.hpp"
#include <gtest/gtest.h>
#include <gmock/gmock.h>
#include <memory>
#include <string>
#include <vector>
#include <hiredis/hiredis.h>

using namespace testing;


struct IRedisCallbackStub {
    virtual ~IRedisCallbackStub() = default;
    virtual void onRedisReply(redisReply* reply) = 0;
};

struct INatsSubscriptionCbStub {
    virtual ~INatsSubscriptionCbStub() = default;
    virtual void onMessage(const std::string& subj, const std::string& data) = 0;
};


class MockPgClient : public PostgreSQLAsyncClient {
public:
};

class MockRedisClient : public RedisAsyncClient {
public:
};

class MockNatsClient : public NatsAsyncClient {
public:
};


class AsyncRoomServiceTest : public Test {
protected:
    std::shared_ptr<MockPgClient> pg_mock;
    std::shared_ptr<MockRedisClient> redis_mock;
    std::shared_ptr<MockNatsClient> nats_mock;
    std::unique_ptr<AsyncRoomService> service;

    void SetUp() override {
        pg_mock = std::make_shared<NiceMock<MockPgClient>>();
        redis_mock = std::make_shared<NiceMock<MockRedisClient>>();
        nats_mock = std::make_shared<NiceMock<MockNatsClient>>();

        service = std::make_unique<AsyncRoomService>(redis_mock, pg_mock, nats_mock);
    }

    void TearDown() override {
        service.reset();
        pg_mock.reset();
        redis_mock.reset();
        nats_mock.reset();
    }
};

TEST_F(AsyncRoomServiceTest, ServiceInitialization) {
    ASSERT_NE(service, nullptr);
    
    EXPECT_NE(pg_mock, nullptr);
    EXPECT_NE(redis_mock, nullptr);
    EXPECT_NE(nats_mock, nullptr);
}

TEST_F(AsyncRoomServiceTest, SafeDestructionWithoutStart) {
    {
        auto local_pg = std::make_shared<NiceMock<MockPgClient>>();
        auto local_redis = std::make_shared<NiceMock<MockRedisClient>>();
        auto local_nats = std::make_shared<NiceMock<MockNatsClient>>();
        
        auto local_service = std::make_unique<AsyncRoomService>(local_redis, local_pg, local_nats);
        
        ASSERT_NE(local_service, nullptr);
    }
    
    SUCCEED();
}
