#ifndef RUZ_IMPORTER_HPP
#define RUZ_IMPORTER_HPP

#include <memory>
#include <vector>
#include <string>
#include <atomic>
#include <map>
#include <mutex>
#include <condition_variable>
#include "database/postgreSQL_async_client.hpp"
#include "messaging/nats_async_client.hpp"
#include "cache/redis_async_client.hpp"
#include "lesson.hpp" 

#include "university_parser.hpp"
#include "parser_factory.hpp"

struct ImportConfig {
    int room_batch_size = 10;
    int insert_batch_size = 200;
    int rate_limit_ms = 100;
    int week_step_days = 7;
    int import_days_ahead = 180;
    bool full_sync_on_empty = true;
    int import_interval_seconds = 1800;
    std::vector<std::string> universities;
    
    // Per-university concurrency limits
    std::map<std::string, int> max_parallel_per_uni = {
        {"spbptu", 10},
        {"leti", 5},
        {"spbgu", 2}
    };
    
    int getMaxParallel(const std::string& uni) const {
        auto it = max_parallel_per_uni.find(uni);
        return (it != max_parallel_per_uni.end()) ? it->second : 5;
    }
};

class RuzImporter {
public:
    RuzImporter(std::shared_ptr<PostgreSQLAsyncClient> pg_client,
                std::shared_ptr<NatsAsyncClient> nats_client,
                std::shared_ptr<RedisAsyncClient> redis_client);
    ~RuzImporter();

    void run();
    void shutdown();
    void start();

private:
    void main_loop();
    void runImportCycle(const std::string& university_id);
    int64_t saveRecords(const std::string& university_id, std::vector<ScheduleRecord>& records);
    std::string computeHash(const ScheduleRecord& r);
    bool isRoomImported(const std::string& uni_id, const std::string& room_id);
    void insertBatch(const std::vector<ScheduleRecord>& buf);
    void syncRoomsAndBuildings(const std::string& university_id);
    void invalidateCache(const std::string& university_id);

    struct ImportRange {
        std::string date_from;
        std::string date_to;
    };
    ImportRange calcImportRange(const std::string& university_id);

    std::shared_ptr<PostgreSQLAsyncClient> m_pg_client;
    std::shared_ptr<NatsAsyncClient> m_nats_client;
    std::shared_ptr<RedisAsyncClient> m_redis_client;
    
    std::atomic<bool> shutdown_{false};
    std::mutex m_shutdown_mutex;
    std::mutex m_db_mutex;
    std::condition_variable m_shutdown_cv;
    
    ImportConfig config_;
};

#endif
