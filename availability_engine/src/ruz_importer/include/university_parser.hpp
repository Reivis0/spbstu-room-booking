#ifndef UNIVERSITY_PARSER_HPP
#define UNIVERSITY_PARSER_HPP

#include <string>
#include <vector>
#include <iomanip>
#include <sstream>
#include <openssl/evp.h>

struct RoomInfo {
    std::string id;          // ruz_id из API
    std::string name;        // человекочитаемое название
    std::string building_id; // для spbptu — нужен для URL
    std::string building_name;
    std::string building_address;
};

struct BuildingInfo {
    std::string id;
    std::string name;
    std::string address;
};

struct ScheduleRecord {
    std::string university_id;
    std::string ruz_room_id; // id (spbptu) или Oid (spbgu) или room (leti)
    std::string room_name;   // человекочитаемое имя аудитории
    std::string ruz_building_id; 
    std::string building_name;
    std::string building_address;
    std::string subject;
    std::string teacher;
    std::string starts_at;   // формат: "YYYY-MM-DD HH:mm" или ISO8601
    std::string ends_at;
    std::string data_hash;   // SHA-256
    std::string raw_json;

    void compute_hash() {
        std::string raw = university_id + ruz_room_id + ruz_building_id + subject + teacher + starts_at + ends_at;
        
        unsigned char hash[EVP_MAX_MD_SIZE];
        unsigned int lengthOfHash = 0;

        EVP_MD_CTX* context = EVP_MD_CTX_new();
        if(context != nullptr) {
            if(EVP_DigestInit_ex(context, EVP_sha256(), nullptr)) {
                EVP_DigestUpdate(context, raw.c_str(), raw.size());
                EVP_DigestFinal_ex(context, hash, &lengthOfHash);
            }
            EVP_MD_CTX_free(context);
        }

        std::stringstream ss;
        for(unsigned int i = 0; i < lengthOfHash; i++) {
            ss << std::hex << std::setw(2) << std::setfill('0') << (int)hash[i];
        }
        data_hash = ss.str();
    }
};

class UniversityParser {
public:
    virtual ~UniversityParser() = default;
    
    // Список всех зданий/адресов (лёгкий запрос)
    virtual std::vector<BuildingInfo> fetchBuildingList() = 0;
    
    // Список аудиторий в здании (только метаданные, не расписание)
    virtual std::vector<RoomInfo> fetchRoomList(const std::string& building_id) = 0;
    
    // Расписание одной аудитории за диапазон дат
    virtual std::vector<ScheduleRecord> fetchRoomSchedule(const RoomInfo& room, const std::string& date_from, const std::string& date_to) = 0;

    // Парсинг JSON-ответа расписания
    virtual std::vector<ScheduleRecord> parse(const std::string& json) = 0;

    // Получение ID университета
    virtual std::string getUniversityID() const = 0;
};

#endif
