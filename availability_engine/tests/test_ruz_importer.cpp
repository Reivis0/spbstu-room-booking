#include "../src/ruz_importer/include/ruz_importer.hpp"
#include "../src/common/database/postgreSQL_async_client.hpp"
#include "../src/common/messaging/nats_async_client.hpp"
#include "logger.hpp"
#include <gtest/gtest.h>
#include <memory>
#include <thread>
#include <chrono>

class RuzImporterTest : public ::testing::Test {
protected:
    void SetUp() override {
        Logger::getInstance().init();
        
        // Create and initialize PostgreSQL connection pool
        pg_pool = std::make_shared<PostgreSQLConnectionPool>(5);
        pg_pool->initialize_pool();
        
        pg_client = std::make_shared<PostgreSQLAsyncClient>(pg_pool);
        nats_client = std::make_shared<NatsAsyncClient>();
        importer = std::make_unique<RuzImporter>(pg_client, nats_client);
    }

    void TearDown() override {
        if (importer) {
            importer->shutdown();
        }
    }

    std::shared_ptr<PostgreSQLConnectionPool> pg_pool;
    std::shared_ptr<PostgreSQLAsyncClient> pg_client;
    std::shared_ptr<NatsAsyncClient> nats_client;
    std::unique_ptr<RuzImporter> importer;
};

TEST_F(RuzImporterTest, CreateAndDestroyImporter) {
    EXPECT_NE(importer, nullptr);
}

TEST_F(RuzImporterTest, StartAndShutdownImporter) {
    // Run start() in a separate thread to avoid blocking
    std::thread importer_thread([this]() {
        EXPECT_NO_THROW(importer->start());
    });
    
    // Give it time to initialize
    std::this_thread::sleep_for(std::chrono::milliseconds(200));
    
    // Shutdown from main thread
    EXPECT_NO_THROW(importer->shutdown());
    
    // Wait for thread to finish with timeout
    std::this_thread::sleep_for(std::chrono::milliseconds(500));
    if (importer_thread.joinable()) {
        importer_thread.join();
    }
}

TEST_F(RuzImporterTest, HandleShutdownGracefully) {
    // Run start() in a separate thread to avoid blocking
    std::thread importer_thread([this]() {
        EXPECT_NO_THROW(importer->start());
    });
    
    // Give it time to initialize and run first cycle
    std::this_thread::sleep_for(std::chrono::milliseconds(500));
    
    // Simulate shutdown
    EXPECT_NO_THROW(importer->shutdown());
    
    // Wait for thread to finish
    importer_thread.join();
}

TEST_F(RuzImporterTest, MultipleShutdownCalls) {
    EXPECT_NO_THROW(importer->start());
    std::this_thread::sleep_for(std::chrono::milliseconds(100));
    
    EXPECT_NO_THROW(importer->shutdown());
    EXPECT_NO_THROW(importer->shutdown()); // Should not crash on second call
    EXPECT_NO_THROW(importer->shutdown()); // Should not crash on third call
}

TEST_F(RuzImporterTest, ShutdownWithoutStart) {
    // Should not crash when shutdown is called without start
    EXPECT_NO_THROW(importer->shutdown());
}

TEST_F(RuzImporterTest, ImporterWithNullClients) {
    // Create importer with null clients
    auto importer_null = std::make_unique<RuzImporter>(nullptr, nullptr);
    EXPECT_NE(importer_null, nullptr);
    
    // Should not crash when shutdown is called
    EXPECT_NO_THROW(importer_null->shutdown());
}

TEST_F(RuzImporterTest, StartThenShutdownMultipleTimes) {
    for (int i = 0; i < 3; ++i) {
        auto temp_importer = std::make_unique<RuzImporter>(pg_client, nats_client);
        EXPECT_NO_THROW(temp_importer->start());
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
        EXPECT_NO_THROW(temp_importer->shutdown());
        std::this_thread::sleep_for(std::chrono::milliseconds(50));
    }
}

TEST_F(RuzImporterTest, PostgreSQLClientConnected) {
    EXPECT_NE(pg_client, nullptr);
    
    std::thread importer_thread([this]() {
        EXPECT_NO_THROW(importer->start());
    });
    
    std::this_thread::sleep_for(std::chrono::milliseconds(200));
    EXPECT_NO_THROW(importer->shutdown());
    importer_thread.join();
}

TEST_F(RuzImporterTest, NatsClientConnected) {
    EXPECT_NE(nats_client, nullptr);
    
    std::thread importer_thread([this]() {
        EXPECT_NO_THROW(importer->start());
    });
    
    std::this_thread::sleep_for(std::chrono::milliseconds(200));
    EXPECT_NO_THROW(importer->shutdown());
    importer_thread.join();
}

TEST_F(RuzImporterTest, ImporterLifecycle) {
    // Test full lifecycle: create, start, shutdown, destroy
    EXPECT_NE(importer, nullptr);
    
    std::thread importer_thread([this]() {
        EXPECT_NO_THROW(importer->start());
    });
    
    std::this_thread::sleep_for(std::chrono::milliseconds(300));
    
    EXPECT_NO_THROW(importer->shutdown());
    importer_thread.join();
    
    std::this_thread::sleep_for(std::chrono::milliseconds(100));
    
    importer.reset();
    EXPECT_EQ(importer, nullptr);
}
