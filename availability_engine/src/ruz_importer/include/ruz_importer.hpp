#ifndef RUZ_IMPORTER_HPP
#define RUZ_IMPORTER_HPP

#include <memory>
#include <vector>
#include <string>
#include <atomic>
#include <map>
#include "database/postgreSQL_async_client.hpp"
#include "messaging/nats_async_client.hpp"
#include "lesson.hpp" 

class RuzImporter {
public:
    RuzImporter(std::shared_ptr<PostgreSQLAsyncClient> pg_client,
                std::shared_ptr<NatsAsyncClient> nats_client);
    ~RuzImporter();

    void run();
    void shutdown();

private:
    void main_loop();
    void import_cycle();
    bool fetch_and_process();
    void wait_for_next_cycle();
    
    bool load_rooms_cache();
    bool save_lessons_to_db(const std::vector<Lesson>& lessons);
    bool sync_deletions(const std::vector<Lesson>& lessons);
    
    void send_notification();

    std::shared_ptr<PostgreSQLAsyncClient> m_pg_client;
    std::shared_ptr<NatsAsyncClient> m_nats_client;
    
    std::atomic<bool> m_shutdown{false};
    std::string m_api_url;
    int m_import_interval_seconds{1800};
    int m_retry_attempts{3};
    int m_retry_delay_seconds{5};

    std::map<std::string, std::string> m_room_name_to_id;
};

#endif
