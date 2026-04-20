#include "ruz_importer.hpp"
#include "http_client.hpp"
#include "data_processor.hpp"
#include "config.hpp"
#include "logger.hpp"
#include <libpq-fe.h>
#include <thread>
#include <chrono>
#include <future>
#include <sstream>
#include <algorithm>
#include <iomanip>
#include <ctime>
#include <csignal>
#include <openssl/evp.h>

std::string get_current_date_string() {
    auto now = std::chrono::system_clock::now();
    std::time_t now_c = std::chrono::system_clock::to_time_t(now);
    std::tm now_tm = {};
    localtime_r(&now_c, &now_tm);
    
    std::stringstream ss;
    ss << std::put_time(&now_tm, "%Y-%m-%d");
    return ss.str();
}

std::string get_future_date_string(int days_ahead) {
    auto now = std::chrono::system_clock::now();
    auto future = now + std::chrono::hours(24 * days_ahead);
    std::time_t future_c = std::chrono::system_clock::to_time_t(future);
    std::tm future_tm = {};
    localtime_r(&future_c, &future_tm);
    
    std::stringstream ss;
    ss << std::put_time(&future_tm, "%Y-%m-%d");
    return ss.str();
}

std::string sha256(const std::string& data) {
    unsigned char hash[EVP_MAX_MD_SIZE];
    unsigned int lengthOfHash = 0;

    EVP_MD_CTX* context = EVP_MD_CTX_new();
    if(context != nullptr) {
        if(EVP_DigestInit_ex(context, EVP_sha256(), nullptr)) {
            EVP_DigestUpdate(context, data.c_str(), data.size());
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

struct SyncGenericCb : public IGenericCb {
    std::promise<std::pair<bool, std::string>> promise;
    std::vector<std::vector<std::string>> result;

    void onResult(const std::vector<std::vector<std::string>>& r, bool ok, const char* err) override {
        result = r;
        promise.set_value({ok, err ? err : ""});
    }
};

namespace {
    RuzImporter* global_importer_instance = nullptr;

    void signal_handler(int signal) {
        LOG_INFO("Signal handler invoked for signal: " + std::to_string(signal));
        if (global_importer_instance) {
            LOG_INFO("Calling shutdown from signal handler.");
            global_importer_instance->shutdown();
        }
    }
}

RuzImporter::RuzImporter(std::shared_ptr<PostgreSQLAsyncClient> pg_client,
                         std::shared_ptr<NatsAsyncClient> nats_client,
                         std::shared_ptr<RedisAsyncClient> redis_client)
    : m_pg_client(std::move(pg_client)),
      m_nats_client(std::move(nats_client)),
      m_redis_client(std::move(redis_client))
{
    auto config = load_config();

    config_.universities = {"leti", "spbptu", "spbgu"};
    if (const char* env_u = std::getenv("RUZ_UNIVERSITIES")) {
        std::stringstream ss(env_u);
        std::string item;
        config_.universities.clear();
        while (std::getline(ss, item, ',')) {
            if (!item.empty()) config_.universities.push_back(item);
        }
    }

    if (config.count("import_interval_seconds")) config_.import_interval_seconds = std::stoi(config.at("import_interval_seconds"));

    LOG_INFO("RUZ_IMPORTER: Configured. Active universities: " + std::to_string(config_.universities.size()));
}

RuzImporter::~RuzImporter() {
    shutdown();
}

void RuzImporter::start() {
    global_importer_instance = this;
    std::signal(SIGINT, signal_handler);
    std::signal(SIGTERM, signal_handler);

    try {
        LOG_INFO("RUZ_Importer: Service initialized. Starting main loop...");
        shutdown_ = false;

        if (m_pg_client) {
            LOG_INFO("RUZ_Importer: Connecting to PostgreSQL...");
            for (int i = 0; i < 20 && !m_pg_client->is_connected(); ++i) {
                if (shutdown_) return;
                std::this_thread::sleep_for(std::chrono::milliseconds(500));
            }
            if (m_pg_client->is_connected()) {
                LOG_INFO("RUZ_Importer: PostgreSQL connected.");
            } else {
                LOG_ERROR("RUZ_Importer: PostgreSQL NOT connected after timeout.");
            }
        }
        main_loop();
    } catch (const std::exception& e) {
        LOG_ERROR(std::string("Error in RuzImporter::start: ") + e.what());
        shutdown();
    }
}

void RuzImporter::shutdown() {
    static bool is_shutdown = false;
    if (is_shutdown) return;
    
    LOG_INFO("RUZ_Importer: Shutting down...");
    shutdown_ = true;
    m_shutdown_cv.notify_all(); 

    if (m_pg_client && m_pg_client->is_connected()) {
        LOG_INFO("Disconnecting from PostgreSQL...");
        m_pg_client->disconnect();
    }
    if (m_nats_client && m_nats_client->is_connected()) {
        LOG_INFO("Disconnecting from NATS...");
        m_nats_client->disconnect();
    }
    
    LOG_INFO("RUZ_Importer: Shutdown completed.");
    is_shutdown = true;
}

void RuzImporter::run() {
    start();
}

void RuzImporter::main_loop() {
    while (!shutdown_) {
        std::vector<std::thread> threads;
        for (const auto& uni : config_.universities) {
            threads.emplace_back([this, uni]() {
                try {
                    runImportCycle(uni);
                } catch (const std::exception& e) {
                    LOG_ERROR("RUZ_IMPORTER: Error in cycle " + uni + ": " + e.what());
                }
            });
        }
        
        for (auto& t : threads) {
            if (t.joinable()) t.join();
        }

        if (shutdown_) break;

        LOG_INFO("RUZ_IMPORTER: Waiting " + std::to_string(config_.import_interval_seconds) + "s...");
        std::unique_lock<std::mutex> lock(m_shutdown_mutex);
        m_shutdown_cv.wait_for(lock, std::chrono::seconds(config_.import_interval_seconds), 
                               [this]() { return shutdown_.load(); });
    }
}

RuzImporter::ImportRange RuzImporter::calcImportRange(const std::string& university_id) {
    return { get_current_date_string(), get_future_date_string(config_.import_days_ahead) };
}

void RuzImporter::runImportCycle(const std::string& university_id) {
    auto parser = ParserFactory::create(university_id);
    auto range = calcImportRange(university_id);
    
    LOG_INFO("RUZ_IMPORTER: Starting cycle for " + university_id + " | " + range.date_from + " -> " + range.date_to);
    
    if (university_id == "leti") {
        auto leti = dynamic_cast<LetiParser*>(parser.get());
        if (!leti) {
            LOG_ERROR("Failed to cast to LetiParser");
            return;
        }
        auto records = leti->fetchAll();
        int64_t saved = saveRecords(university_id, records);
        LOG_INFO("RUZ_IMPORTER: Cycle OK for leti. Saved: " + std::to_string(saved));
        syncRoomsAndBuildings(university_id);
        invalidateCache(university_id);
        return;
    }
    
    auto buildings = parser->fetchBuildingList();
    LOG_INFO(university_id + ": " + std::to_string(buildings.size()) + " buildings found");
    
    int64_t total = 0;
    int building_idx = 0;
    
    for (const auto& building : buildings) {
        if (shutdown_) break;
        building_idx++;
        auto all_rooms = parser->fetchRoomList(building.id);
        for (auto& r : all_rooms) {
            r.building_name = building.name;
            r.building_address = building.address;
        }
        int total_rooms = all_rooms.size();
        int processed = 0;
        
        for (int i = 0; i < total_rooms && !shutdown_; i += config_.room_batch_size) {
            int end = std::min(i + config_.room_batch_size, total_rooms);
            
            for (int j = i; j < end && !shutdown_; j++) {
                const auto& room = all_rooms[j];
                
                if (config_.full_sync_on_empty && isRoomImported(university_id, room.id)) {
                    processed++;
                    continue;
                }
                
                try {
                    auto records = parser->fetchRoomSchedule(room, range.date_from, range.date_to);
                    int64_t saved = saveRecords(university_id, records);
                    total += saved;
                    processed++;
                } catch (const std::exception& e) {
                    LOG_WARN(university_id + ": room " + room.id + " failed: " + e.what());
                }
                
                std::this_thread::sleep_for(std::chrono::milliseconds(config_.rate_limit_ms));
            }
            LOG_INFO(university_id + ": building " + std::to_string(building_idx) + "/" + std::to_string(buildings.size()) + " | rooms " + std::to_string(processed) + "/" + std::to_string(total_rooms) + " | records: " + std::to_string(total));
        }
    }
    LOG_INFO("RUZ_IMPORTER: Cycle OK for " + university_id + ". Saved: " + std::to_string(total));
    syncRoomsAndBuildings(university_id);
    invalidateCache(university_id);
}

bool RuzImporter::isRoomImported(const std::string& uni_id, const std::string& room_id) {
    // В MVP всегда возвращаем false, чтобы сделать full scan
    return false;
}

std::string RuzImporter::computeHash(const ScheduleRecord& r) {
    std::string date = r.starts_at.length() >= 10 ? r.starts_at.substr(0, 10) : "";
    std::string time = r.starts_at.length() > 10 ? r.starts_at.substr(11, 5) : "";
    std::string input = r.university_id + r.subject + date + time + r.ruz_room_id + r.teacher;
    return sha256(input);
}

int64_t RuzImporter::saveRecords(const std::string& university_id, std::vector<ScheduleRecord>& records) {
    if (records.empty()) return 0;
    
    int64_t total = 0;
    std::vector<ScheduleRecord> buf;
    buf.reserve(config_.insert_batch_size);
    
    for (auto& r : records) {
        r.university_id = university_id;
        r.data_hash = computeHash(r);
        buf.push_back(std::move(r));
        
        if (buf.size() >= (size_t)config_.insert_batch_size) {
            insertBatch(buf);
            total += buf.size();
            buf.clear();
        }
    }
    
    if (!buf.empty()) {
        insertBatch(buf);
        total += buf.size();
    }
    
    return total;
}

void RuzImporter::insertBatch(const std::vector<ScheduleRecord>& buf) {
    if (buf.empty()) return;

    std::stringstream sql;
    std::vector<std::string> params;
    
    sql << "INSERT INTO schedules_import (university_id, subject, teacher, starts_at, ends_at, data_hash, raw_source, ruz_room_id, room_name, ruz_building_id, building_name, building_address, source, hash) VALUES ";
    
    for (size_t i = 0; i < buf.size(); ++i) {
        int p = i * 14 + 1;
        if (i > 0) sql << ",";
        sql << "($" << p << ",$" << p+1 << ",$" << p+2 << ",$" << p+3 << "::timestamptz,$" << p+4 << "::timestamptz,$" << p+5 << ",$" << p+6 << "::jsonb, $" << p+7 << ", $" << p+8 << ", $" << p+9 << ", $" << p+10 << ", $" << p+11 << ", $" << p+12 << ", $" << p+13 << ")";
        
        params.push_back(buf[i].university_id);
        params.push_back(buf[i].subject);
        params.push_back(buf[i].teacher);
        params.push_back(buf[i].starts_at);
        params.push_back(buf[i].ends_at);
        params.push_back(buf[i].data_hash);
        params.push_back(buf[i].raw_json.empty() ? "{}" : buf[i].raw_json);
        params.push_back(buf[i].ruz_room_id);   
        params.push_back(buf[i].room_name); 
        params.push_back(buf[i].ruz_building_id);
        params.push_back(buf[i].building_name);
        params.push_back(buf[i].building_address);
        params.push_back(buf[i].university_id); // source
        params.push_back(buf[i].data_hash);     // hash
    }
    
    sql << " ON CONFLICT (university_id, data_hash) DO NOTHING";

    auto cb = std::make_unique<SyncGenericCb>();
    auto fut = cb->promise.get_future();
    
    m_pg_client->execute(sql.str(), params, std::move(cb));
    auto res = fut.get();
    if (!res.first) {
        LOG_ERROR("RuzImporter: DB Insert failed: " + res.second);
    }
}

void RuzImporter::syncRoomsAndBuildings(const std::string& university_id) {
    LOG_INFO("RUZ_IMPORTER: Syncing rooms/buildings for " + university_id + "...");

    // Step 1: UPSERT buildings from schedules_import
    // We use COALESCE(building_name, ruz_building_id) for name if name is missing
    m_pg_client->execute(R"(
        INSERT INTO buildings (id, university_id, ruz_id, name, address, code)
        SELECT
            uuid_generate_v4(),
            university_id,
            ruz_building_id,
            COALESCE(NULLIF(building_name, ''), 'Building ' || ruz_building_id),
            COALESCE(building_address, ''),
            'BUILDING_' || university_id || '_' || ruz_building_id
        FROM (
            SELECT DISTINCT
                university_id,
                ruz_building_id,
                building_name,
                building_address
            FROM schedules_import
            WHERE university_id = $1
              AND ruz_building_id IS NOT NULL
              AND ruz_building_id != ''
        ) sub
        ON CONFLICT (ruz_id, university_id)
        DO UPDATE SET
            name = EXCLUDED.name,
            address = EXCLUDED.address,
            updated_at = now()
    )", {university_id}, std::make_unique<SyncGenericCb>());

    // Step 2: UPSERT rooms from schedules_import
    m_pg_client->execute(R"(
        INSERT INTO rooms (id, university_id, building_id, ruz_id, name, code, capacity)
        SELECT
            uuid_generate_v4(),
            si.university_id,
            b.id,
            si.ruz_room_id,
            COALESCE(NULLIF(si.room_name, ''), 'Room ' || si.ruz_room_id),
            'ROOM_' || si.university_id || '_' || si.ruz_room_id,
            30
        FROM (
            SELECT DISTINCT
                university_id,
                ruz_room_id,
                room_name,
                ruz_building_id
            FROM schedules_import
            WHERE university_id = $1
              AND ruz_room_id IS NOT NULL
              AND ruz_room_id != ''
        ) si
        LEFT JOIN buildings b 
            ON b.ruz_id = si.ruz_building_id 
           AND b.university_id = si.university_id
        ON CONFLICT (ruz_id, university_id)
        DO UPDATE SET
            name = EXCLUDED.name,
            building_id = EXCLUDED.building_id,
            updated_at = now()
    )", {university_id}, std::make_unique<SyncGenericCb>());

    // Step 3: Link room_id in schedules_import
    m_pg_client->execute(R"(
        UPDATE schedules_import si
        SET room_id = r.id
        FROM rooms r
        WHERE r.ruz_id = si.ruz_room_id
          AND r.university_id = si.university_id
          AND si.university_id = $1
          AND si.room_id IS NULL
    )", {university_id}, std::make_unique<SyncGenericCb>());

    LOG_INFO("RUZ_IMPORTER: Sync completed for " + university_id);
}

void RuzImporter::invalidateCache(const std::string& university_id) {
    if (!m_redis_client) return;
    
    LOG_INFO("RUZ_IMPORTER: Invalidating Redis cache for " + university_id);
    
    // Invalidate global lists
    m_redis_client->del("rooms:all", std::make_unique<RedisCallbackImpl>([](redisReply* reply) {
        if (!reply) LOG_WARN("Redis del rooms:all failed");
    }));
    m_redis_client->del("buildings:all", std::make_unique<RedisCallbackImpl>([](redisReply* reply) {
        if (!reply) LOG_WARN("Redis del buildings:all failed");
    }));
    
    // Invalidate university-specific lists (if any)
    m_redis_client->del("rooms:" + university_id, std::make_unique<RedisCallbackImpl>([university_id](redisReply* reply) {
        if (!reply) LOG_WARN("Redis del rooms:" + university_id + " failed");
    }));
    m_redis_client->del("buildings:" + university_id, std::make_unique<RedisCallbackImpl>([university_id](redisReply* reply) {
        if (!reply) LOG_WARN("Redis del buildings:" + university_id + " failed");
    }));
}
