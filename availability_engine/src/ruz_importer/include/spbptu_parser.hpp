#ifndef SPBPTU_PARSER_HPP
#define SPBPTU_PARSER_HPP

#include "university_parser.hpp"
#include "http_client.hpp"

class SpbptuParser : public UniversityParser {
public:
    std::vector<BuildingInfo> fetchBuildingList() override;
    std::vector<RoomInfo> fetchRoomList(const std::string& building_id) override;
    std::vector<ScheduleRecord> fetchRoomSchedule(const RoomInfo& room, const std::string& date_from, const std::string& date_to) override;
    std::vector<ScheduleRecord> parse(const std::string& json) override;
    std::string getUniversityID() const override { return "spbptu"; }
    
private:
    HttpClient http_;
};

#endif
