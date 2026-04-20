#include "async_room_service.hpp"
#include <grpcpp/server_builder.h>

AsyncRoomService::AsyncRoomService(std::shared_ptr<RedisAsyncClient> redis_client,
                                 std::shared_ptr<PostgreSQLAsyncClient> pg_client,
                                 std::shared_ptr<NatsAsyncClient> nats_client)
    : redis_client_(std::move(redis_client)),
      pg_client_(std::move(pg_client)),
      nats_client_(std::move(nats_client)) {}

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
    LOG_INFO("gRPC: ComputeIntervals called for room " + request->room_id());
    // TODO: Implement actual logic
    return grpc::Status::OK;
}

grpc::Status AsyncRoomService::Validate(grpc::ServerContext* context,
                                      const room_service::ValidateRequest* request,
                                      room_service::ValidateResponse* response) {
    LOG_INFO("gRPC: Validate called for room=" + request->room_id() + 
             " date=" + request->date() + " interval=" + request->start_time() + "-" + request->end_time());

    std::mutex mtx;
    std::condition_variable cv;
    bool done = false;
    bool has_conflicts = false;
    std::vector<Booking> found_conflicts;

    struct SyncConflictsCb : public IconflictsCb {
        std::mutex& mtx;
        std::condition_variable& cv;
        bool& done;
        bool& has_conflicts;
        std::vector<Booking>& conflicts;

        SyncConflictsCb(std::mutex& m, std::condition_variable& c, bool& d, bool& h, std::vector<Booking>& cf)
            : mtx(m), cv(c), done(d), has_conflicts(h), conflicts(cf) {}

        void onConflicts(Booking* rows, size_t n, bool ok, const char* err) override {
            std::lock_guard<std::mutex> lock(mtx);
            if (ok && n > 0) {
                has_conflicts = true;
                for (size_t i = 0; i < n; ++i) {
                    conflicts.push_back(rows[i]);
                }
            }
            done = true;
            cv.notify_one();
        }
    };

    std::string start_full = request->date() + " " + request->start_time();
    std::string end_full = request->date() + " " + request->end_time();

    pg_client_->getConflictsByInterval(request->room_id().c_str(), 
                                      start_full.c_str(), 
                                      end_full.c_str(),
                                      std::make_unique<SyncConflictsCb>(mtx, cv, done, has_conflicts, found_conflicts));

    std::unique_lock<std::mutex> lock(mtx);
    if (!cv.wait_for(lock, std::chrono::seconds(5), [&] { return done; })) {
        LOG_ERROR("gRPC: Validate TIMEOUT for room=" + request->room_id());
        return grpc::Status(grpc::StatusCode::DEADLINE_EXCEEDED, "PostgreSQL query timeout");
    }

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
        c->set_start_time(b.start_time);
        c->set_end_time(b.end_time);
        c->set_status(b.status);
        c->set_user_id(b.user_id);
    }
    
    return grpc::Status::OK;
}

grpc::Status AsyncRoomService::OcupiedIntervals(grpc::ServerContext* context,
                                              const room_service::OcupiedIntervalsRequest* request,
                                              room_service::OcupiedIntervalsResponce* response) {
    LOG_INFO("gRPC: OcupiedIntervals called for room " + request->room_id());
    return grpc::Status::OK;
}
