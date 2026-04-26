#include "async_room_service.hpp"
#include <grpcpp/server_builder.h>
#include <map>

// Helper to manage trace_id in logs
class TraceContextGuard {
public:
    TraceContextGuard(grpc::ServerContext* context) {
        const auto& metadata = context->client_metadata();
        
        // Try b3 single header first
        auto it = metadata.find("b3");
        if (it != metadata.end()) {
            std::string b3(it->second.data(), it->second.size());
            size_t dash = b3.find('-');
            if (dash != std::string::npos) {
                Logger::setTraceId(b3.substr(0, dash));
                return;
            }
            Logger::setTraceId(b3);
            return;
        }

        // Try x-b3-traceid
        it = metadata.find("x-b3-traceid");
        if (it != metadata.end()) {
            Logger::setTraceId(std::string(it->second.data(), it->second.size()));
            return;
        }

        // Try traceparent
        it = metadata.find("traceparent");
        if (it != metadata.end()) {
            std::string tp(it->second.data(), it->second.size());
            if (tp.size() >= 35 && tp.substr(0, 2) == "00") {
                Logger::setTraceId(tp.substr(3, 32));
                return;
            }
        }
        
        Logger::setTraceId(Logger::generateTraceId());
    }
    ~TraceContextGuard() {
        Logger::clearTraceId();
    }
};

AsyncRoomService::AsyncRoomService(std::shared_ptr<RedisAsyncClient> redis_client,
                                 std::shared_ptr<PostgreSQLAsyncClient> pg_client,
                                 std::shared_ptr<NatsAsyncClient> nats_client,
                                 std::shared_ptr<PqxxConnectionPool> pqxx_pool)
    : redis_client_(std::move(redis_client)),
      pg_client_(std::move(pg_client)),
      nats_client_(std::move(nats_client)),
      pqxx_pool_(std::move(pqxx_pool)) {}

AsyncRoomService::~AsyncRoomService() {
    shutdown();
}

void AsyncRoomService::start() {
    if (is_running_.exchange(true)) return;

    if (pg_client_) {
        pg_client_->start();
    }
    if (redis_client_) {
        redis_thread_ = std::thread([this] {
            redis_client_->connect();
            redis_client_->run_event_loop();
        });
    }

    std::string server_address("0.0.0.0:50051");
    grpc::ServerBuilder builder;
    builder.AddListeningPort(server_address, grpc::InsecureServerCredentials());
    builder.RegisterService(this);
    
    server_ = builder.BuildAndStart();
    LOG_INFO("gRPC RoomService started on " + server_address);
}

void AsyncRoomService::shutdown() {
    if (server_) {
        LOG_INFO("Shutting down gRPC RoomService...");
        server_->Shutdown();
        server_.reset();
    }
    if (pg_client_) {
        pg_client_->stop();
    }
    if (redis_client_) {
        redis_client_->stop_event_loop();
        if (redis_thread_.joinable()) {
            redis_thread_.join();
        }
    }
    is_running_ = false;
}

grpc::Status AsyncRoomService::ComputeIntervals(grpc::ServerContext* context, 
                                              const room_service::ComputeIntervalsRequest* request,
                                              room_service::ComputeIntervalsResponse* response) {
    TraceContextGuard guard(context);
    LOG_INFO("gRPC: ComputeIntervals called for room " + request->room_id() + 
             " date=" + request->date() + " window=" + request->start_time() + "-" + request->end_time());

    std::string start_full = request->date() + " " + request->start_time() + "+00";
    std::string end_full = request->date() + " " + request->end_time() + "+00";

    std::vector<Booking> conflicts;

    try {
        auto conn = pqxx_pool_->acquire();
        pqxx::work txn(*conn);

        std::string query = 
            "SELECT id::text, starts_at::text, ends_at::text, user_id::text, 'Booking'::text, status::text "
            "FROM bookings "
            "WHERE room_id = $1 "
            "  AND status = 'confirmed' "
            "  AND tstzrange(starts_at, ends_at) && tstzrange($2::timestamptz, $3::timestamptz) "
            "UNION ALL "
            "SELECT id::text, starts_at::text, ends_at::text, '00000000-0000-0000-0000-000000000000'::text, subject, 'confirmed'::text "
            "FROM schedules_import "
            "WHERE room_id = $1 "
            "  AND tstzrange(starts_at, ends_at) && tstzrange($2::timestamptz, $3::timestamptz)";
        
        pqxx::result res = txn.exec_params(query, request->room_id(), start_full, end_full);
        
        for (auto row : res) {
            Booking b;
            b.id = row[0].c_str();
            b.start_time = row[1].c_str();
            b.end_time = row[2].c_str();
            b.user_id = row[3].c_str();
            b.notes = row[4].c_str();
            b.status = row[5].c_str();
            conflicts.push_back(std::move(b));
        }
        
        txn.commit();
        pqxx_pool_->release(std::move(conn));
    } catch (const std::exception& e) {
        LOG_ERROR(std::string("pqxx exception in ComputeIntervals: ") + e.what());
        return grpc::Status(grpc::StatusCode::INTERNAL, "DB query failed");
    }

    // Sort conflicts by start time
    std::sort(conflicts.begin(), conflicts.end(), [](const Booking& a, const Booking& b) {
        return a.start_time < b.start_time;
    });

    // Merge overlapping intervals if any (simplified)
    // Then calculate free gaps
    std::string current_start = request->date() + " " + request->start_time();
    for (const auto& c : conflicts) {
        if (c.start_time > current_start) {
            auto* slot = response->add_available_slots();
            slot->set_start_time(current_start.substr(11, 5));
            slot->set_end_time(c.start_time.substr(11, 5));
        }
        if (c.end_time > current_start) {
            current_start = c.end_time.substr(0, 16);
        }
    }

    std::string request_end = request->date() + " " + request->end_time();
    if (current_start < request_end) {
        auto* slot = response->add_available_slots();
        slot->set_start_time(current_start.substr(11, 5));
        slot->set_end_time(request_end.substr(11, 5));
    }

    return grpc::Status::OK;
}

grpc::Status AsyncRoomService::Validate(grpc::ServerContext* context,
                                      const room_service::ValidateRequest* request,
                                      room_service::ValidateResponse* response) {
    TraceContextGuard guard(context);
    LOG_INFO("gRPC: Validate called for room=" + request->room_id() + 
             " date=" + request->date() + " interval=" + request->start_time() + "-" + request->end_time());

    std::string start_full = request->date() + " " + request->start_time() + "+00";
    std::string end_full = request->date() + " " + request->end_time() + "+00";

    std::vector<Booking> found_conflicts;

    try {
        auto conn = pqxx_pool_->acquire();
        pqxx::work txn(*conn);

        std::string query = 
            "SELECT id::text, starts_at::text, ends_at::text, user_id::text, 'Booking'::text, status::text "
            "FROM bookings "
            "WHERE room_id = $1 "
            "  AND status = 'confirmed' "
            "  AND tstzrange(starts_at, ends_at) && tstzrange($2::timestamptz, $3::timestamptz) "
            "UNION ALL "
            "SELECT id::text, starts_at::text, ends_at::text, '00000000-0000-0000-0000-000000000000'::text, subject, 'confirmed'::text "
            "FROM schedules_import "
            "WHERE room_id = $1 "
            "  AND tstzrange(starts_at, ends_at) && tstzrange($2::timestamptz, $3::timestamptz)";
        
        pqxx::result res = txn.exec_params(query, request->room_id(), start_full, end_full);
        
        for (auto row : res) {
            Booking b;
            b.id = row[0].c_str();
            b.start_time = row[1].c_str();
            b.end_time = row[2].c_str();
            b.user_id = row[3].c_str();
            b.notes = row[4].c_str();
            b.status = row[5].c_str();
            found_conflicts.push_back(std::move(b));
        }
        
        txn.commit();
        pqxx_pool_->release(std::move(conn));
    } catch (const std::exception& e) {
        LOG_ERROR("gRPC: Validate pqxx exception: " + std::string(e.what()));
        return grpc::Status(grpc::StatusCode::INTERNAL, "PostgreSQL query failed");
    }

    bool has_conflicts = !found_conflicts.empty();
    LOG_INFO("gRPC: Validate completed for room=" + request->room_id() + 
             " is_valid=" + std::to_string(!has_conflicts) + 
             " conflicts=" + std::to_string(found_conflicts.size()));

    response->set_is_valid(!has_conflicts);
    auto* details = response->mutable_details();
    details->set_duration_valid(true); // Simplified
    details->set_working_hours_valid(true); // Simplified
    details->set_no_conflicts(!has_conflicts);

    for (const auto& b : found_conflicts) {
        auto* c = response->add_conflicts();
        c->set_booking_id(b.id);
        c->set_start_time(b.start_time.size() > 11 ? b.start_time.substr(11, 5) : b.start_time);
        c->set_end_time(b.end_time.size() > 11 ? b.end_time.substr(11, 5) : b.end_time);
        c->set_status(b.status);
        c->set_user_id(b.user_id);
    }
    
    return grpc::Status::OK;
}

grpc::Status AsyncRoomService::OccupiedIntervals(grpc::ServerContext* context,
                                              const room_service::OccupiedIntervalsRequest* request,
                                              room_service::OccupiedIntervalsResponse* response) {
    TraceContextGuard guard(context);
    LOG_INFO("gRPC: OccupiedIntervals called for room " + request->room_id() + " date=" + request->date());

    std::string start_full = request->date() + " 00:00:00+00";
    std::string end_full = request->date() + " 23:59:59+00";

    std::vector<Booking> conflicts;

    try {
        auto conn = pqxx_pool_->acquire();
        pqxx::work txn(*conn);

        std::string query = 
            "SELECT id::text, starts_at::text, ends_at::text, user_id::text, 'Booking'::text, status::text "
            "FROM bookings "
            "WHERE room_id = $1 "
            "  AND status = 'confirmed' "
            "  AND tstzrange(starts_at, ends_at) && tstzrange($2::timestamptz, $3::timestamptz) "
            "UNION ALL "
            "SELECT id::text, starts_at::text, ends_at::text, '00000000-0000-0000-0000-000000000000'::text, subject, 'confirmed'::text "
            "FROM schedules_import "
            "WHERE room_id = $1 "
            "  AND tstzrange(starts_at, ends_at) && tstzrange($2::timestamptz, $3::timestamptz)";
        
        pqxx::result res = txn.exec_params(query, request->room_id(), start_full, end_full);
        
        for (auto row : res) {
            Booking b;
            b.id = row[0].c_str();
            b.start_time = row[1].c_str();
            b.end_time = row[2].c_str();
            b.user_id = row[3].c_str();
            b.notes = row[4].c_str();
            b.status = row[5].c_str();
            conflicts.push_back(std::move(b));
        }
        
        txn.commit();
        pqxx_pool_->release(std::move(conn));
    } catch (const std::exception& e) {
        LOG_ERROR(std::string("pqxx exception in OccupiedIntervals: ") + e.what());
        return grpc::Status(grpc::StatusCode::INTERNAL, "DB query failed");
    }

    for (const auto& c : conflicts) {
        auto* interval = response->add_intervals();
        interval->set_booking_id(c.id);
        interval->set_start_time(c.start_time.size() > 11 ? c.start_time.substr(11, 5) : c.start_time);
        interval->set_end_time(c.end_time.size() > 11 ? c.end_time.substr(11, 5) : c.end_time);
        interval->set_user_id(c.user_id);
        interval->set_note(c.notes);
    }

    return grpc::Status::OK;
}

grpc::Status AsyncRoomService::FindRoomChain(grpc::ServerContext* context,
                                           const room_service::FindRoomChainRequest* request,
                                           room_service::FindRoomChainResponse* response) {
    TraceContextGuard guard(context);
    LOG_INFO("gRPC: FindRoomChain called for date=" + request->date() + 
             " building=" + request->building_id());
    
    // NOT IMPLEMENTED in MVP
    return grpc::Status(grpc::StatusCode::UNIMPLEMENTED, "Not implemented in MVP");
}
