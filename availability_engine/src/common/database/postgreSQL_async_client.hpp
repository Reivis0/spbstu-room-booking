#ifndef POSTGRESQL_ASYNC_CLIENT_HPP
#define POSTGRESQL_ASYNC_CLIENT_HPP

#include <string>
#include <vector>
#include <map>
#include <memory>
#include <thread>
#include <atomic>
#include <mutex>
#include <libpq-fe.h>
#include <optional>
#include <functional>

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
    void* user_cb { nullptr };
};

class PostgreSQLAsyncClient {
public:
    using Row = std::vector<std::string>;
    using Result = std::vector<Row>;

    PostgreSQLAsyncClient();
    ~PostgreSQLAsyncClient();

    void start();
    void stop();

    void getBookingsByRoomAndDate(const char* room_code, const char* dayISO, IBookingsByRoomDateCb* cb);
    void getConflictsByInterval(const char* room_id, const char* startsAtISO, const char* endsAtISO, IconflictsCb* cb);
    
    void execute(const std::string& sql, const std::vector<std::string>& params, IGenericCb* cb);

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
    void disconnect();
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
};

#endif
