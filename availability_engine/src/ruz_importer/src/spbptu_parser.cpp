#include "spbptu_parser.hpp"
#include <json/json.h>
#include <logger.hpp>
#include <chrono>
#include <iomanip>
#include <sstream>

std::vector<BuildingInfo> SpbptuParser::fetchBuildingList() {
    std::vector<BuildingInfo> buildings;
    std::string url = "https://ruz.spbstu.ru/api/v1/ruz/buildings";
    std::string response = http_.fetch_ruz_data(url);

    if (response.empty()) return buildings;

    Json::Value root;
    Json::Reader reader;
    if (!reader.parse(response, root)) {
        LOG_ERROR("SpbptuParser: JSON parse error in buildings");
        return buildings;
    }

    if (!root.isObject() || !root.isMember("buildings")) return buildings;
    const auto& b_list = root["buildings"];
    if (!b_list.isArray()) return buildings;

    for (const auto& b : b_list) {
        if (!b.isObject()) continue;
        BuildingInfo info;
        info.id = std::to_string(b.get("id", 0).asInt());
        info.name = b.get("name", "").asString();
        info.address = b.get("address", "").asString(); 
        if (info.address.empty()) info.address = b.get("abbrev", "").asString();
        buildings.push_back(info);
    }
    return buildings;
}

std::vector<RoomInfo> SpbptuParser::fetchRoomList(const std::string& building_id) {
    std::vector<RoomInfo> rooms;
    std::string url = "https://ruz.spbstu.ru/api/v1/ruz/buildings/" + building_id + "/rooms";
    std::string response = http_.fetch_ruz_data(url);

    if (response.empty()) return rooms;

    Json::Value root;
    Json::Reader reader;
    if (!reader.parse(response, root)) {
        LOG_ERROR("SpbptuParser: JSON parse error in rooms");
        return rooms;
    }

    if (!root.isObject() || !root.isMember("rooms")) return rooms;
    const auto& r_list = root["rooms"];
    if (!r_list.isArray()) return rooms;

    for (const auto& r : r_list) {
        if (!r.isObject()) continue;
        RoomInfo info;
        info.id = std::to_string(r.get("id", 0).asInt());
        info.name = r.get("name", "").asString();
        info.building_id = building_id;
        rooms.push_back(info);
    }
    return rooms;
}

static std::string addDays(const std::string& date_str, int days) {
    std::tm t = {};
    std::istringstream ss(date_str);
    ss >> std::get_time(&t, "%Y-%m-%d");
    if (ss.fail()) {
        LOG_WARN("SpbptuParser: Failed to parse date: " + date_str);
        return ""; 
    }

    auto time_point = std::chrono::system_clock::from_time_t(std::mktime(&t));
    time_point += std::chrono::hours(24 * days);
    
    std::time_t result_time = std::chrono::system_clock::to_time_t(time_point);
    std::tm result_tm = {};
    localtime_r(&result_time, &result_tm);

    std::ostringstream res_ss;
    res_ss << std::put_time(&result_tm, "%Y-%m-%d");
    return res_ss.str();
}

std::vector<ScheduleRecord> SpbptuParser::fetchRoomSchedule(const RoomInfo& room, const std::string& date_from, const std::string& date_to) {
    std::vector<ScheduleRecord> records;
    std::string current_date = date_from;
    int max_weeks = 52; // Safety limit
    int weeks = 0;

    while (current_date <= date_to && !current_date.empty() && weeks < max_weeks) {
        std::string url = "https://ruz.spbstu.ru/api/v1/ruz/buildings/" + room.building_id + "/rooms/" + room.id + "/scheduler?date=" + current_date;
        std::string response = http_.fetch_ruz_data(url);

        if (!response.empty()) {
            auto week_records = parse(response);
            // If the JSON didn't have room info (which is common for room-specific API), 
            // we fill it from our 'room' context.
            for (auto& rec : week_records) {
                if (rec.ruz_room_id.empty()) rec.ruz_room_id = room.id;
                if (rec.room_name.empty()) rec.room_name = room.name;
                rec.ruz_building_id = room.building_id;
                rec.building_name = room.building_name;
                rec.building_address = room.building_address;
                rec.compute_hash();
                records.push_back(std::move(rec));
            }
        }
        
        std::string next_date = addDays(current_date, 7);
        if (next_date == current_date || next_date.empty()) {
            LOG_ERROR("SpbptuParser: Date did not advance: " + current_date);
            break;
        }
        current_date = next_date;
        weeks++;
    }
    return records;
}

std::vector<ScheduleRecord> SpbptuParser::parse(const std::string& json_str) {
    std::vector<ScheduleRecord> records;
    Json::Value root;
    Json::Reader reader;
    if (!reader.parse(json_str, root)) {
        LOG_ERROR("SpbptuParser: JSON parse error");
        return records;
    }

    const Json::Value* days_node = nullptr;
    if (root.isObject() && root.isMember("week") && root["week"].isObject() && root["week"].isMember("days")) {
        days_node = &root["week"]["days"];
    } else if (root.isObject() && root.isMember("days")) {
        days_node = &root["days"];
    }

    if (!days_node || !days_node->isArray()) return records;

    for (const auto& day : *days_node) {
        if (!day.isObject() || !day.isMember("lessons") || !day["lessons"].isArray()) continue;
        
        std::string date = day.get("date", "").asString();
        // SpbSTU API can return full ISO date, we only need the date part
        if (date.size() > 10) date = date.substr(0, 10);

        for (const auto& lesson : day["lessons"]) {
            if (!lesson.isObject()) continue;
            
            ScheduleRecord rec;
            rec.university_id = "spbptu";
            rec.subject = lesson.get("subject", "").asString();
            rec.raw_json = lesson.toStyledString();
            
            if (lesson.isMember("teachers") && lesson["teachers"].isArray() && !lesson["teachers"].empty()) {
                rec.teacher = lesson["teachers"][0].get("full_name", "").asString();
            }
            
            if (lesson.isMember("auditories") && lesson["auditories"].isArray() && !lesson["auditories"].empty()) {
                rec.ruz_room_id = std::to_string(lesson["auditories"][0].get("id", 0).asInt());
                rec.room_name = lesson["auditories"][0].get("name", "").asString();
            }

            std::string t_start = lesson.get("time_start", "").asString();
            std::string t_end = lesson.get("time_end", "").asString();
            
            if (!date.empty()) {
                rec.starts_at = date + " " + t_start;
                rec.ends_at = date + " " + t_end;
            } else {
                rec.starts_at = t_start;
                rec.ends_at = t_end;
            }
            
            rec.compute_hash();
            records.push_back(rec);
        }
    }
    return records;
}