#include "leti_parser.hpp"
#include <json/json.h>
#include <logger.hpp>
#include <chrono>
#include <iomanip>
#include <sstream>

std::vector<BuildingInfo> LetiParser::fetchBuildingList() {
    return { {"leti_main", "ЛЭТИ", "ул. Профессора Попова, 5, Санкт-Петербург"} };
}

std::vector<RoomInfo> LetiParser::fetchRoomList(const std::string& building_id) {
    return {};
}

std::vector<ScheduleRecord> LetiParser::fetchRoomSchedule(const RoomInfo& room, const std::string& date_from, const std::string& date_to) {
    return {};
}

std::vector<ScheduleRecord> LetiParser::fetchAll() {
    std::string url = "https://digital.etu.ru/api/mobile/schedule";
    std::string response = http_.fetch_ruz_data(url);
    if (response.empty()) return {};
    return parse(response);
}

std::vector<ScheduleRecord> LetiParser::parse(const std::string& json_str) {
    std::vector<ScheduleRecord> records;
    Json::Value json;
    Json::Reader reader;
    if (!reader.parse(json_str, json)) {
        LOG_ERROR("LetiParser: JSON parse error");
        return records;
    }

    if (!json.isObject()) return records;

    // Helper to get current week's Monday date
    auto now = std::chrono::system_clock::now();
    std::time_t now_c = std::chrono::system_clock::to_time_t(now);
    std::tm now_tm = *std::localtime(&now_c);
    
    int days_since_monday = (now_tm.tm_wday + 6) % 7;
    auto monday_tp = now - std::chrono::hours(24 * days_since_monday);

    int group_count = 0;
    for (const auto& group_key : json.getMemberNames()) {
        if (++group_count > 1000) break; // Increased from 500

        const auto& group = json[group_key];
        if (!group.isObject() || !group.isMember("days")) continue;

        const auto& days = group["days"];
        if (!days.isObject()) continue;

        for (const auto& day_key : days.getMemberNames()) {
            const auto& day = days[day_key];
            if (!day.isObject() || !day.isMember("lessons")) continue;

            const auto& lessons = day["lessons"];
            if (!lessons.isArray()) continue;

            int day_offset = 0;
            try {
                day_offset = std::stoi(day_key);
            } catch (...) {
                continue;
            }
            
            auto day_tp = monday_tp + std::chrono::hours(24 * day_offset);
            std::time_t day_c = std::chrono::system_clock::to_time_t(day_tp);
            std::tm day_tm = *std::localtime(&day_c);
            
            std::stringstream date_ss;
            date_ss << std::put_time(&day_tm, "%Y-%m-%d");
            std::string lesson_date = date_ss.str();

            for (const auto& lesson : lessons) {
                if (!lesson.isObject()) continue;

                ScheduleRecord rec;
                rec.university_id = "leti";
                rec.ruz_room_id = lesson.get("room", "").asString();
                rec.room_name = lesson.get("room", "").asString();
                rec.ruz_building_id = "leti_main";
                rec.building_name = "ЛЭТИ";
                rec.building_address = "ул. Профессора Попова, 5, Санкт-Петербург";
                rec.subject = lesson.get("name", "").asString();
                rec.teacher = lesson.get("teacher", "").asString();
                rec.raw_json = lesson.toStyledString();
                rec.starts_at = lesson_date + " " + lesson.get("start_time", "").asString();
                rec.ends_at = lesson_date + " " + lesson.get("end_time", "").asString();
                
                rec.compute_hash();
                records.push_back(rec);
            }
        }
    }
    return records;
}
