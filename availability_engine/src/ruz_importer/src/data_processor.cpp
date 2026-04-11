#include "data_processor.hpp"
#include "logger.hpp"
#include <sstream>

std::vector<Lesson> DataProcessor::parse_and_transform(const std::string& json_data) {
    std::vector<Lesson> lessons;
    Json::CharReaderBuilder reader_builder;
    Json::Value root;
    std::string errors;
    std::istringstream json_stream(json_data);

    if (!Json::parseFromStream(reader_builder, json_stream, &root, &errors)) {
        LOG_ERROR("RUZ_IMPORTER: DATA_PROCESSOR: Failed to parse JSON: " + errors);
        return lessons;
    }

    if (!root.isArray()) {
        if (root.isObject() && root.isMember("error")) {
             LOG_WARN("RUZ_IMPORTER: API returned error object");
             return lessons;
        }

        LOG_ERROR("RUZ_IMPORTER: DATA_PROCESSOR: Expected JSON array");
        return lessons;
    }

    int valid_count = 0;
    int invalid_count = 0;

    for (const auto& lesson_json : root) {
        try {
            Lesson lesson = parse_single_lesson(lesson_json);
            
            if (lesson.validate()) {

                lesson.hash = lesson.compute_hash(); 
                lessons.push_back(lesson);
                valid_count++;
            } else {
                invalid_count++;
            }
        } catch (const std::exception& e) {
            LOG_WARN("RUZ_IMPORTER: DATA_PROCESSOR: Failed to parse lesson: " + std::string(e.what()));
            invalid_count++;
        }
    }

    LOG_INFO("RUZ_IMPORTER: DATA_PROCESSOR: Parsing completed: " + std::to_string(valid_count) + " valid, " +
             std::to_string(invalid_count) + " invalid lessons");
    
    return lessons;
}

Lesson DataProcessor::parse_single_lesson(const Json::Value& lesson_json) {
    Lesson lesson;
    lesson.room_name = lesson_json.get("auditorium", "").asString();
    lesson.starts_at = lesson_json.get("beginLesson", "").asString();
    lesson.ends_at   = lesson_json.get("endLesson", "").asString();

    std::string date = lesson_json.get("date", "").asString();
    if (!date.empty()) {
        if (lesson.starts_at.length() <= 8) lesson.starts_at = date + " " + lesson.starts_at;
        if (lesson.ends_at.length() <= 8)   lesson.ends_at = date + " " + lesson.ends_at;
    }

    lesson.group = lesson_json.get("group", "").asString();
    if (lesson.group.empty()) lesson.group = lesson_json.get("stream", "").asString();
    
    lesson.subject = lesson_json.get("discipline", "").asString();
    lesson.teacher = lesson_json.get("lecturer", "").asString();

    return lesson;
}
