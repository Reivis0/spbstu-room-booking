#ifndef POSTGRESQL_ASYNC_CLIENT_HPP
#define POSTGRESQL_ASYNC_CLIENT_HPP

#include <string>
#include <vector>
#include <map>
#include <memory>
#include <thread>
#include <atomic>
#include <mutex>
#include <optional>
#include <functional>
#include <libpq-fe.h>
#include <queue>
#include <condition_variable>
#include "postgreSQL_connection_pool.hpp"

struct Booking {
    std::string id;
    std::string start_time;
    std::string end_time;
    std::string user_id;
    std::string notes;
    std::string status;
};

struct IBookingsByRoomDateCb {
    virtual ~IBookingsByRoomDateCb() = default;
    virtual void onBookings(Booking* rows, size_t n, bool ok, const char* err) = 0;
};

struct IconflictsCb {
    virtual ~IconflictsCb() = default;
    virtual void onConflicts(Booking* rows, size_t n, bool ok, const char* err) = 0;
};

struct IGenericCb {
    virtual ~IGenericCb() = default;
    virtual void onResult(const std::vector<std::vector<std::string>>& result, bool ok, const char* err) = 0;
};

struct PendingQuery {
    enum Kind { K_BookingsByRoomDate, K_ConflictsByInterval, K_Generic };
    Kind kind { K_Generic };
    std::string sql;
    std::vector<std::string> params;
    std::unique_ptr<IBookingsByRoomDateCb> bookings_cb;
    std::unique_ptr<IconflictsCb> conflicts_cb;
    std::unique_ptr<IGenericCb> generic_cb;
};

class PostgreSQLAsyncClient {
public:
    using Row = std::vector<std::string>;
    using Result = std::vector<Row>;

    PostgreSQLAsyncClient(std::shared_ptr<PostgreSQLConnectionPool> pool);
    PostgreSQLAsyncClient();
    ~PostgreSQLAsyncClient();

    void start();
    void stop();

    void getBookingsByRoomAndDate(const char* room_code, const char* dayISO, std::unique_ptr<IBookingsByRoomDateCb> cb);
    void getConflictsByInterval(const char* room_id, const char* startsAtISO, const char* endsAtISO, std::unique_ptr<IconflictsCb> cb);
    
    void execute(const std::string& sql, const std::vector<std::string>& params, std::unique_ptr<IGenericCb> cb);
    void executeQueryWithTable(const std::string& sql_template, const std::string& table,
                               const std::vector<std::string>& params, std::unique_ptr<IGenericCb> cb);
    void connect();
    void disconnect();
    bool is_connected() const { return m_connect.is_connected; }

private:
    struct Connector {
        Connector();
        std::map<std::string, std::string> read_config();
        std::string get_connecting_str();
        std::string host, port, db_name, username, password;
    };

    enum class ConnectStatus { NEED_READ, NEED_WRITE, CONNECTED, ERROR };

    struct ConnectResult {
        PGconn* connection { nullptr };
        ConnectStatus status { ConnectStatus::ERROR };
        std::string error_msg;
        int socket_fd { -1 };
        bool is_connected { false };
    } m_connect {};

    void begin_async_connect();
    void run_loop();
    void arm_connect_poll();
    void update_interests(bool rd, bool wr);
    void try_send_next();
    void on_readable();
    void drain_results(PendingQuery& q);

    static void FinishBookingsByRoomDate_(PostgreSQLAsyncClient* self, PendingQuery& q, Result&& r, bool ok, const char* err);
    static void FinishConflicts_(PostgreSQLAsyncClient* self, PendingQuery& q, Result&& r, bool ok, const char* err);
    static void FinishGeneric_(PostgreSQLAsyncClient* self, PendingQuery& q, Result&& r, bool ok, const char* err);

    std::thread m_loop_thread;
    std::atomic<bool> m_running { false };
    std::atomic<bool> m_want_read { false };
    std::atomic<bool> m_want_write { false };
    
    std::vector<PendingQuery> m_queue;
    std::optional<PendingQuery> m_inflight;
    
    std::mutex m_queue_mutex; 
    static bool is_table_allowed(const std::string& table);

    std::shared_ptr<PostgreSQLConnectionPool> connection_pool;
};

#endif
