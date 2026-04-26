#include "ruz_importer.hpp"
#include "data_processor.hpp"
#include "postgreSQL_async_client.hpp"
#include "nats_async_client.hpp"
#include <gtest/gtest.h>
#include <memory>
#include <string>

using namespace testing;


TEST(LessonTest, Validate) {
    Lesson l;
    l.room_name = "101";
    l.room_id = "room-1";
    l.starts_at = "2025-01-01T10:00:00";
    l.ends_at = "2025-01-01T11:30:00";
    l.subject = "Math";
    
    EXPECT_TRUE(l.validate());
}

TEST(LessonTest, ComputeHashCheck) {
    Lesson l;
    l.room_name = "101";
    l.room_id = "room-1";
    l.starts_at = "2025-01-01T10:00:00";
    l.ends_at = "2025-01-01T11:30:00";
    l.subject = "Math";
    
    std::string h1 = l.compute_hash();
    
    Lesson l2 = l;
    EXPECT_EQ(h1, l2.compute_hash());
    
    l2.subject = "Physics";
    EXPECT_NE(h1, l2.compute_hash());
}

TEST(LessonTest, InvalidLesson) {
    Lesson l;
    EXPECT_FALSE(l.validate());
}


TEST(RuzImporterTest, Initialization) {
    auto pg_client = std::make_shared<PostgreSQLAsyncClient>();
    auto nats_client = std::make_shared<NatsAsyncClient>();
    
    RuzImporter importer(pg_client, nats_client);
    
    SUCCEED();
}


TEST(DataProcessorTest, EmptyJson) {
    DataProcessor processor;
    
    auto lessons = processor.parse_and_transform("");
    
    EXPECT_TRUE(lessons.empty());
}

TEST(DataProcessorTest, InvalidJson) {
    DataProcessor processor;
    
    auto lessons = processor.parse_and_transform("{ invalid json");
    
    EXPECT_TRUE(lessons.empty());
}
