#ifndef ASYNC_ROOM_SERVICE_HPP
#define ASYNC_ROOM_SERVICE_HPP

#include <message.grpc.pb.h>
#include <message.pb.h>
#include <grpcpp/grpcpp.h>

#include <cache/redis_async_client.hpp>
#include <database/postgreSQL_async_client.hpp>
#include <messaging/nats_async_client.hpp>
#include "pqxx_pool.hpp"
#include <logger.hpp>
#include <memory>
#include <atomic>

class AsyncRoomService final : public room_service::RoomService::Service {
public:
    AsyncRoomService(std::shared_ptr<RedisAsyncClient> redis_client,
                    std::shared_ptr<PostgreSQLAsyncClient> pg_client,
                    std::shared_ptr<NatsAsyncClient> nats_client,
                    std::shared_ptr<PqxxConnectionPool> pqxx_pool);
    ~AsyncRoomService() override;

    void start();
    void shutdown();
    bool isRunning() const { return is_running_; }

    // gRPC Methods
    grpc::Status ComputeIntervals(grpc::ServerContext* context, 
                                 const room_service::ComputeIntervalsRequest* request,
                                 room_service::ComputeIntervalsResponse* response) override;

    grpc::Status Validate(grpc::ServerContext* context,
                         const room_service::ValidateRequest* request,
                         room_service::ValidateResponse* response) override;

    grpc::Status OccupiedIntervals(grpc::ServerContext* context,
                                 const room_service::OccupiedIntervalsRequest* request,
                                 room_service::OccupiedIntervalsResponse* response) override;

    grpc::Status FindRoomChain(grpc::ServerContext* context,
                             const room_service::FindRoomChainRequest* request,
                             room_service::FindRoomChainResponse* response) override;

private:
    std::shared_ptr<RedisAsyncClient> redis_client_;
    std::shared_ptr<PostgreSQLAsyncClient> pg_client_;
    std::shared_ptr<NatsAsyncClient> nats_client_;
    std::shared_ptr<PqxxConnectionPool> pqxx_pool_;
    
    std::unique_ptr<grpc::Server> server_;
    std::atomic<bool> is_running_{false};

    std::thread redis_thread_;
};

#endif
