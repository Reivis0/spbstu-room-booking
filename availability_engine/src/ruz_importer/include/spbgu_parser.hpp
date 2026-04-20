#ifndef SPBGU_PARSER_HPP
#define SPBGU_PARSER_HPP

#include "university_parser.hpp"
#include "http_client.hpp"

class SpbguParser : public UniversityParser {
public:
    std::vector<BuildingInfo> fetchBuildingList() override;
    std::vector<RoomInfo> fetchRoomList(const std::string& building_id) override;
    std::vector<ScheduleRecord> fetchRoomSchedule(const RoomInfo& room, const std::string& date_from, const std::string& date_to) override;
    std::vector<ScheduleRecord> parse(const std::string& json) override;
    std::string getUniversityID() const override { return "spbgu"; }

private:
    HttpClient http_;
};

#endif
