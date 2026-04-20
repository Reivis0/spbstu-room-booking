#include "spbgu_parser.hpp"
#include <json/json.h>
#include <logger.hpp>
#include <algorithm>
#include <chrono>
#include <iomanip>
#include <sstream>

static std::string addDaysToIso(const std::string& date_iso, int days) {
    std::tm t = {};
    std::istringstream ss(date_iso.substr(0, 10));
    ss >> std::get_time(&t, "%Y-%m-%d");
    if (ss.fail()) return date_iso;

    auto time_point = std::chrono::system_clock::from_time_t(std::mktime(&t));
    time_point += std::chrono::hours(24 * days);
    
    std::time_t result_time = std::chrono::system_clock::to_time_t(time_point);
    std::tm result_tm = {};
    localtime_r(&result_time, &result_tm);

    std::ostringstream res_ss;
    res_ss << std::put_time(&result_tm, "%Y-%m-%d");
    return res_ss.str();
}

static std::string getMondayOfIso(const std::string& date_iso) {
    std::tm t = {};
    std::istringstream ss(date_iso.substr(0, 10));
    ss >> std::get_time(&t, "%Y-%m-%d");
    if (ss.fail()) return date_iso;

    std::mktime(&t);
    int days_since_monday = (t.tm_wday + 6) % 7;
    return addDaysToIso(date_iso, -days_since_monday);
}

std::vector<BuildingInfo> SpbguParser::fetchBuildingList() {
    std::vector<BuildingInfo> buildings;
    std::string url = "https://timetable.spbu.ru/api/v1/addresses";
    std::string response = http_.fetch_ruz_data(url);

    if (response.empty()) return buildings;

    Json::Value root;
    Json::Reader reader;
    if (!reader.parse(response, root)) {
        LOG_ERROR("SpbguParser: JSON parse error in buildings");
        return buildings;
    }

    if (!root.isArray()) return buildings;

    for (const auto& b : root) {
        if (!b.isObject()) continue;
        BuildingInfo info;
        info.id = b.get("Oid", "").asString();
        info.name = b.get("DisplayName1", b.get("City", "")).asString();
        info.address = b.get("matches", b.get("City", "")).asString(); 
        buildings.push_back(info);
    }
    return buildings;
}

std::vector<RoomInfo> SpbguParser::fetchRoomList(const std::string& building_id) {
    std::vector<RoomInfo> rooms;
    std::string url = "https://timetable.spbu.ru/api/v1/addresses/" + building_id + "/classrooms";
    std::string response = http_.fetch_ruz_data(url);

    if (response.empty()) return rooms;

    Json::Value root;
    Json::Reader reader;
    if (!reader.parse(response, root)) {
        LOG_ERROR("SpbguParser: JSON parse error in rooms");
        return rooms;
    }

    if (!root.isArray()) return rooms;

    for (const auto& r : root) {
        if (!r.isObject()) continue;
        RoomInfo info;
        info.id = r.get("Oid", "").asString();
        info.name = r.get("DisplayName1", "").asString();
        info.building_id = building_id;
        rooms.push_back(info);
    }
    return rooms;
}

std::vector<ScheduleRecord> SpbguParser::fetchRoomSchedule(const RoomInfo& room, const std::string& date_from, const std::string& date_to) {
    std::vector<ScheduleRecord> records;
    
    std::string from = date_from;
    from.erase(std::remove(from.begin(), from.end(), '-'), from.end());
    from += "0000";

    std::string to = date_to;
    to.erase(std::remove(to.begin(), to.end(), '-'), to.end());
    to += "2359";

    std::string url = "https://timetable.spbu.ru/api/v1/classrooms/" + room.id + "/events/" + from + "/" + to;
    std::string response = http_.fetch_ruz_data(url);

    if (!response.empty()) {
        auto parsed = parse(response);
        for (auto& rec : parsed) {
            if (rec.ruz_room_id.empty()) rec.ruz_room_id = room.id;
            if (rec.room_name.empty()) rec.room_name = room.name;
            rec.ruz_building_id = room.building_id;
            rec.building_name = room.building_name;
            rec.building_address = room.building_address;
            rec.compute_hash();
            records.push_back(std::move(rec));
        }
    }
    
    return records;
}

std::vector<ScheduleRecord> SpbguParser::parse(const std::string& json_str) {
    std::vector<ScheduleRecord> records;
    Json::Value root;
    Json::Reader reader;
    if (!reader.parse(json_str, root)) {
        LOG_ERROR("SpbguParser: JSON parse error");
        return records;
    }

    std::string from_iso = root.get("From", "").asString();
    if (from_iso.empty()) return records;
    
    std::string monday_iso = getMondayOfIso(from_iso);

    const Json::Value* days_node = nullptr;
    if (root.isObject() && root.isMember("ClassroomEventsDays")) {
        days_node = &root["ClassroomEventsDays"];
    } else if (root.isArray()) {
        days_node = &root;
    } else if (root.isObject() && root.isMember("Days")) {
        days_node = &root["Days"];
    }

    if (!days_node || !days_node->isArray()) return records;

    for (const auto& day_node : *days_node) {
        int day_idx = day_node.get("Day", 0).asInt();
        if (day_idx < 1 || day_idx > 7) continue;

        std::string current_date = addDaysToIso(monday_iso, day_idx - 1);

        const Json::Value* events_node = nullptr;
        if (day_node.isObject() && day_node.isMember("DayStudyEvents")) events_node = &day_node["DayStudyEvents"];
        else if (day_node.isObject() && day_node.isMember("Events")) events_node = &day_node["Events"];

        if (!events_node || !events_node->isArray()) continue;
        
        for (const auto& event : *events_node) {
            if (!event.isObject()) continue;
            
            ScheduleRecord rec;
            rec.university_id = "spbgu";
            rec.subject = event.get("Subject", "").asString();
            rec.teacher = event.get("EducatorsDisplayText", "").asString();
            rec.room_name = event.get("LocationsDisplayText", "").asString();
            rec.raw_json = event.toStyledString();
            
            std::string t_start = event.get("Start", "").asString();
            std::string t_end = event.get("End", "").asString();
            
            if (t_start.find('-') == std::string::npos) {
                rec.starts_at = current_date + " " + t_start;
                rec.ends_at = current_date + " " + t_end;
            } else {
                rec.starts_at = t_start;
                rec.ends_at = t_end;
            }

            if (rec.starts_at.size() > 19) rec.starts_at = rec.starts_at.substr(0, 19);
            if (rec.ends_at.size() > 19) rec.ends_at = rec.ends_at.substr(0, 19);
            
            rec.compute_hash();
            records.push_back(rec);
        }
    }
    return records;
}
