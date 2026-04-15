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
    
    // For now, return always valid to allow SAGA to complete
    response->set_is_valid(true);
    auto* details = response->mutable_details();
    details->set_duration_valid(true);
    details->set_working_hours_valid(true);
    details->set_no_conflicts(true);
    
    return grpc::Status::OK;
}

grpc::Status AsyncRoomService::OcupiedIntervals(grpc::ServerContext* context,
                                              const room_service::OcupiedIntervalsRequest* request,
                                              room_service::OcupiedIntervalsResponce* response) {
    LOG_INFO("gRPC: OcupiedIntervals called for room " + request->room_id());
    return grpc::Status::OK;
}
