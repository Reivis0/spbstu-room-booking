#include <gtest/gtest.h>
#include "spbptu_parser.hpp"
#include "spbgu_parser.hpp"
#include "leti_parser.hpp"
#include "parser_factory.hpp"

TEST(ParserFactoryTest, CreateParsers) {
    auto p1 = ParserFactory::create("spbptu");
    EXPECT_EQ(p1->getUniversityID(), "spbptu");

    auto p2 = ParserFactory::create("spbgu");
    EXPECT_EQ(p2->getUniversityID(), "spbgu");

    auto p3 = ParserFactory::create("leti");
    EXPECT_EQ(p3->getUniversityID(), "leti");

    EXPECT_THROW(ParserFactory::create("unknown"), std::runtime_error);
}

TEST(SpbptuParserTest, ParseValidJson) {
    SpbptuParser parser;
    std::string json = R"({
        "days": [{
            "date": "2026-04-20",
            "lessons": [{
                "subject": "Mathematics",
                "time_start": "09:00",
                "time_end": "10:35",
                "typeObj": {"name": "Lecture"},
                "teachers": [{"full_name": "Ivanov I.I."}],
                "auditories": [{"name": "101", "building": {"name": "Main Building"}}]
            }]
        }]
    })";

    auto records = parser.parse(json);
    ASSERT_EQ(records.size(), 1);
    EXPECT_EQ(records[0].subject, "Mathematics");
    EXPECT_EQ(records[0].starts_at, "2026-04-20 09:00");
    EXPECT_EQ(records[0].room_name, "101");
}

TEST(SpbguParserTest, ParseValidJson) {
    SpbguParser parser;
    std::string json = R"({
        "Days": [{
            "Events": [{
                "Subject": "Physics",
                "Start": "2026-04-20T10:00:00",
                "End": "2026-04-20T11:30:00",
                "EducatorsDisplayText": "Petrov P.P.",
                "LocationsDisplayText": "Room 202"
            }]
        }]
    })";

    auto records = parser.parse(json);
    ASSERT_EQ(records.size(), 1);
    EXPECT_EQ(records[0].subject, "Physics");
    EXPECT_EQ(records[0].starts_at, "2026-04-20T10:00:00");
    EXPECT_EQ(records[0].room_name, "Room 202");
}

int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}
