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

    if (!root.isObject() || !root.isMember("days")) {
        if (root.isObject() && root.isMember("error") && root["error"].asBool()) {
             LOG_WARN("RUZ_IMPORTER: API returned error: " + root.get("text", "unknown").asString());
             return lessons;
        }
        LOG_ERROR("RUZ_IMPORTER: DATA_PROCESSOR: Expected JSON object with 'days' key");
        return lessons;
    }

    int valid_count = 0;
    int invalid_count = 0;

    const Json::Value days = root["days"];
    for (const auto& day_json : days) {
        std::string date = day_json.get("date", "").asString();
        const Json::Value lessons_json = day_json["lessons"];
        
        for (const auto& lesson_json : lessons_json) {
            try {
                Lesson lesson = parse_single_lesson(lesson_json);
                
                // Set the date if it's separate in the API
                if (!date.empty()) {
                    if (lesson.starts_at.length() <= 8) lesson.starts_at = date + " " + lesson.starts_at;
                    if (lesson.ends_at.length() <= 8)   lesson.ends_at = date + " " + lesson.ends_at;
                }

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
    }

    LOG_INFO("RUZ_IMPORTER: DATA_PROCESSOR: Parsing completed: " + std::to_string(valid_count) + " valid, " +
             std::to_string(invalid_count) + " invalid lessons");
    
    return lessons;
}

Lesson DataProcessor::parse_single_lesson(const Json::Value& lesson_json) {
    Lesson lesson;
    // According to Step 2 analysis and RUZ API standard:
    // subject is often in "subject" or "discipline"
    // teacher is often in "teachers" array (take first)
    // auditorium is in "auditories" array (take first)
    
    lesson.subject = lesson_json.get("subject", "").asString();
    
    const Json::Value auditories = lesson_json["auditories"];
    if (auditories.isArray() && !auditories.empty()) {
        lesson.room_name = auditories[0].get("name", "").asString();
    }

    lesson.starts_at = lesson_json.get("time_start", "").asString();
    lesson.ends_at   = lesson_json.get("time_end", "").asString();

    const Json::Value groups = lesson_json["groups"];
    if (groups.isArray() && !groups.empty()) {
        lesson.group = groups[0].get("name", "").asString();
    }
    
    const Json::Value teachers = lesson_json["teachers"];
    if (teachers.isArray() && !teachers.empty()) {
        lesson.teacher = teachers[0].get("full_name", "").asString();
    }

    return lesson;
}
