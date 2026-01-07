#ifndef DATA_PROCESSOR_HPP
#define DATA_PROCESSOR_HPP

#include <vector>
#include <string>
#include <json/json.h>
#include "lesson.hpp"

class DataProcessor {
public:
    std::vector<Lesson> parse_and_transform(const std::string& json_data);

private:
    Lesson parse_single_lesson(const Json::Value& lesson_json);
};

#endif
