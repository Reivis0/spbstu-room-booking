#ifndef LESSON_HPP
#define LESSON_HPP

#include <string>
#include <iomanip>
#include <sstream>
#include <vector>
#include <json/json.h>
#include <openssl/evp.h> 

struct Lesson {
    std::string room_name;
    std::string room_id; 
    std::string starts_at;
    std::string ends_at;
    std::string group;
    std::string subject;
    std::string teacher;
    std::string hash;

    bool validate() const {
        if (room_name.empty()) return false;
        if (starts_at.empty() || ends_at.empty()) return false;
        if (starts_at >= ends_at) return false;
        return true;
    }

    std::string get_metadata_json() const {
        Json::Value meta;
        if (!group.empty()) meta["group"] = group;
        if (!subject.empty()) meta["subject"] = subject;
        if (!teacher.empty()) meta["teacher"] = teacher;
        
        Json::StreamWriterBuilder builder;
        builder["indentation"] = ""; 
        return Json::writeString(builder, meta);
    }

    std::string compute_hash() const {
        std::string id_to_use = room_id.empty() ? room_name : room_id;
        std::string raw = id_to_use + starts_at + ends_at + group + subject + teacher;

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
        return ss.str();
    }
};

#endif
