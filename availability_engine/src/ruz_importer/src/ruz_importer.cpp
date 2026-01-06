#include "ruz_importer.hpp"
#include "http_client.hpp"
#include "data_processor.hpp"
#include "config.hpp"
#include "logger.hpp"
#include <thread>
#include <chrono>
#include <future>
#include <sstream>
#include <algorithm>
#include <iomanip>
#include <ctime>


std::string get_current_date_string() {
    auto now = std::chrono::system_clock::now();
    std::time_t now_c = std::chrono::system_clock::to_time_t(now);
    std::tm now_tm = *std::localtime(&now_c);
    
    std::stringstream ss;
    ss << std::put_time(&now_tm, "%Y-%m-%d");
    return ss.str();
}

struct SyncGenericCb : public IGenericCb {
    std::promise<std::pair<bool, std::string>> promise;
    std::vector<std::vector<std::string>> result;

    void onResult(const std::vector<std::vector<std::string>>& r, bool ok, const char* err) override {
        result = r;
        promise.set_value({ok, err ? err : ""});
    }
};


RuzImporter::RuzImporter(std::shared_ptr<PostgreSQLAsyncClient> pg_client,
                         std::shared_ptr<NatsAsyncClient> nats_client)
    : m_pg_client(pg_client), m_nats_client(nats_client) 
{
    auto config = load_config();
    m_api_url = config.count("api_url") ? config.at("api_url") : "https://ruz.spbstu.ru/api/v1/ruz/scheduler";
    
    if (!m_api_url.empty() && m_api_url.back() == '/') {
        m_api_url.pop_back();
    }

    if (config.count("import_interval_seconds")) m_import_interval_seconds = std::stoi(config.at("import_interval_seconds"));
    if (config.count("retry_attempts")) m_retry_attempts = std::stoi(config.at("retry_attempts"));
    if (config.count("retry_delay_seconds")) m_retry_delay_seconds = std::stoi(config.at("retry_delay_seconds"));

    LOG_INFO("RUZ_IMPORTER: Configured. Base URL: " + m_api_url);
}

RuzImporter::~RuzImporter() {
    shutdown();
}

void RuzImporter::run() {
    LOG_INFO("RUZ_IMPORTER: Entering main loop...");
    std::this_thread::sleep_for(std::chrono::seconds(1)); 
    
    if (!load_rooms_cache()) {
        LOG_ERROR("RUZ_IMPORTER: Failed to load rooms cache. Importer might fail to map rooms.");
    }

    main_loop();
}

void RuzImporter::shutdown() {
    m_shutdown = true;
    LOG_INFO("RUZ_IMPORTER: Shutdown requested");
}

void RuzImporter::main_loop() {
    while (!m_shutdown) {
        import_cycle();
        wait_for_next_cycle();
    }
}

void RuzImporter::wait_for_next_cycle() {
    if (m_shutdown) return;
    LOG_INFO("RUZ_IMPORTER: Waiting " + std::to_string(m_import_interval_seconds) + "s...");
    
    int waited = 0;
    while (waited < m_import_interval_seconds && !m_shutdown) {
        std::this_thread::sleep_for(std::chrono::seconds(1));
        waited++;
    }
}

void RuzImporter::import_cycle() {
    LOG_INFO("RUZ_IMPORTER: Starting import cycle");
    auto start = std::chrono::steady_clock::now();
    
    bool success = false;
    for (int i = 0; i < m_retry_attempts && !m_shutdown; ++i) {
        if (i > 0) {
            LOG_INFO("RUZ_IMPORTER: Retry " + std::to_string(i + 1) + "/" + std::to_string(m_retry_attempts));
            std::this_thread::sleep_for(std::chrono::seconds(m_retry_delay_seconds));
        }
        
        if (fetch_and_process()) {
            success = true;
            break;
        }
    }

    auto end = std::chrono::steady_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    
    if (success) {
        LOG_INFO("RUZ_IMPORTER: Cycle OK. Time: " + std::to_string(ms) + "ms");
    } else {
        LOG_ERROR("RUZ_IMPORTER: Cycle FAILED after attempts.");
    }
}

bool RuzImporter::load_rooms_cache() {
    auto cb = std::make_unique<SyncGenericCb>();
    auto fut = cb->promise.get_future();
    
    m_pg_client->execute("SELECT id, code FROM rooms", {}, cb.get());
    
    if (fut.wait_for(std::chrono::seconds(5)) == std::future_status::timeout) {
         LOG_ERROR("RUZ_IMPORTER: DB Timeout loading rooms");
         return false;
    }

    auto res = fut.get();
    if (!res.first) {
        LOG_ERROR("RUZ_IMPORTER: DB Error loading rooms: " + res.second);
        return false;
    }

    m_room_name_to_id.clear();
    for (const auto& row : cb->result) {
        if (row.size() >= 2) {
            m_room_name_to_id[row[1]] = row[0];
        }
    }
    LOG_INFO("RUZ_IMPORTER: Loaded " + std::to_string(m_room_name_to_id.size()) + " rooms into cache.");
    return true;
}

bool RuzImporter::fetch_and_process() {
    if (m_room_name_to_id.empty()) {
        LOG_WARN("RUZ_IMPORTER: No rooms found in DB (cache empty). Nothing to import. Populate 'rooms' table first.");
        return true; 
    }

    HttpClient http;
    DataProcessor proc;
    std::string current_date = get_current_date_string();
    std::vector<Lesson> all_valid_lessons;
    int rooms_processed = 0;

    for (const auto& [room_code, room_uuid] : m_room_name_to_id) {
        if (m_shutdown) break;

        std::string url = m_api_url + "/" + room_code + "?date=" + current_date;

        std::string json = http.fetch_ruz_data(url);
        
        if (json.empty()) {
         LOG_WARN("RUZ_IMPORTER: Empty/Failed response for room code: " + room_code);
            continue;
        }

        std::vector<Lesson> lessons = proc.parse_and_transform(json);
        
        for (auto& l : lessons) {
            l.room_id = room_uuid; 
            
            l.hash = l.compute_hash(); 
            all_valid_lessons.push_back(l);
        }
        rooms_processed++;
        
        std::this_thread::sleep_for(std::chrono::milliseconds(50));
    }

    LOG_INFO("RUZ_IMPORTER: Fetched data for " + std::to_string(rooms_processed) + " rooms. Total lessons: " + std::to_string(all_valid_lessons.size()));

    if (all_valid_lessons.empty()) {
        LOG_WARN("RUZ_IMPORTER: No lessons found for any room.");
        return true;
    }

    try {
        if (!save_lessons_to_db(all_valid_lessons)) return false;
        if (!sync_deletions(all_valid_lessons)) return false;

        send_notification();
        return true;

    } catch (const std::exception& e) {
        LOG_ERROR(std::string("RUZ_IMPORTER: Exception in DB save/sync: ") + e.what());
        return false;
    }
}

bool RuzImporter::save_lessons_to_db(const std::vector<Lesson>& lessons) {
    if (lessons.empty()) return true;

    const size_t BATCH_SIZE = 500;
    for (size_t i = 0; i < lessons.size(); i += BATCH_SIZE) {
        size_t end = std::min(i + BATCH_SIZE, lessons.size());
        
        std::stringstream sql;
        std::vector<std::string> params;
        
        sql << "INSERT INTO schedules_import (room_id, starts_at, ends_at, source, hash, metadata) ";
        sql << "SELECT v.rid::uuid, v.start::timestamptz, v.end_::timestamptz, v.src, v.h, v.meta::jsonb ";
        sql << "FROM (VALUES ";
        
        int p_idx = 1;
        for (size_t j = i; j < end; ++j) {
            if (j > i) sql << ",";
            sql << "($" << p_idx++ << ", $" << p_idx++ << ", $" << p_idx++ << ", 'ruz', $" << p_idx++ << ", $" << p_idx++ << "::jsonb)";
            
            params.push_back(lessons[j].room_id);
            params.push_back(lessons[j].starts_at);
            params.push_back(lessons[j].ends_at);
            params.push_back(lessons[j].hash);
            params.push_back(lessons[j].get_metadata_json());
        }
        
        sql << ") AS v(rid, start, end_, src, h, meta) ";
        sql << "WHERE NOT EXISTS (SELECT 1 FROM schedules_import s WHERE s.hash = v.h)";

        auto cb = std::make_unique<SyncGenericCb>();
        auto fut = cb->promise.get_future();
        
        m_pg_client->execute(sql.str(), params, cb.get());
        
        if (fut.wait_for(std::chrono::seconds(10)) == std::future_status::timeout) {
             LOG_ERROR("RUZ_IMPORTER: DB Timeout inserting batch");
             return false;
        }

        auto res = fut.get();
        if (!res.first) {
            LOG_ERROR("RUZ_IMPORTER: DB Insert Error: " + res.second);
            return false;
        }
    }
    LOG_INFO("RUZ_IMPORTER: Saved/Updated " + std::to_string(lessons.size()) + " lessons.");
    return true;
}

bool RuzImporter::sync_deletions(const std::vector<Lesson>& lessons) {
    if (lessons.empty()) return true;

    std::string min_time = lessons[0].starts_at;
    std::string max_time = lessons[0].ends_at;

    for (const auto& l : lessons) {
        if (l.starts_at < min_time) min_time = l.starts_at;
        if (l.ends_at > max_time) max_time = l.ends_at;
    }

    std::stringstream ss_hashes;
    ss_hashes << "{";
    for (size_t i = 0; i < lessons.size(); ++i) {
        if (i > 0) ss_hashes << ",";
        ss_hashes << lessons[i].hash;
    }
    ss_hashes << "}";
    
    std::string sql = 
        "DELETE FROM schedules_import "
        "WHERE source = 'ruz' "
        "AND starts_at >= $1::timestamptz "
        "AND ends_at <= $2::timestamptz "
        "AND NOT (hash = ANY($3::text[]))"; 

    std::vector<std::string> params = { min_time, max_time, ss_hashes.str() };

    auto cb = std::make_unique<SyncGenericCb>();
    auto fut = cb->promise.get_future();

    m_pg_client->execute(sql, params, cb.get());

    if (fut.wait_for(std::chrono::seconds(10)) == std::future_status::timeout) {
         LOG_ERROR("RUZ_IMPORTER: DB Timeout syncing deletions");
         return false;
    }

    auto res = fut.get();
    if (!res.first) {
        LOG_ERROR("RUZ_IMPORTER: DB Deletion Sync Error: " + res.second);
        return false;
    }

    LOG_INFO("RUZ_IMPORTER: Sync (Cleanup) completed for range " + min_time + " - " + max_time);
    return true;
}

void RuzImporter::send_notification() {
    m_nats_client->publish("events.schedule_refreshed", "{\"event\": \"full_refresh\"}");
    LOG_INFO("RUZ_IMPORTER: Sent NATS notification");
}
