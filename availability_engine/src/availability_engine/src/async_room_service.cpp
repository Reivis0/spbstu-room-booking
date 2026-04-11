#include "async_room_service.hpp"
#include <csignal>

namespace {
    AsyncRoomService* global_service_instance = nullptr;

    void signal_handler(int signal) {
        LOG_INFO("Signal handler invoked for signal: " + std::to_string(signal));
        if (global_service_instance) {
            LOG_INFO("Calling shutdown from signal handler.");
            global_service_instance->shutdown();
        } else {
            LOG_WARN("Global service instance is null in signal handler.");
        }
    }
}

AsyncRoomService::AsyncRoomService(
    std::shared_ptr<RedisAsyncClient> redis_client,
    std::shared_ptr<PostgreSQLAsyncClient> pg_client,
    std::shared_ptr<NatsAsyncClient> nats_client)
    : redis_client_(std::move(redis_client)),
      pg_client_(std::move(pg_client)),
      nats_client_(std::move(nats_client)),
      is_running_(false) {}

AsyncRoomService::~AsyncRoomService() {
    shutdown();
}

void AsyncRoomService::start() {
    global_service_instance = this;
    std::signal(SIGINT, signal_handler);

    try {
        is_running_ = true;
        LOG_INFO("AsyncRoomService started.");
        
        if (pg_client_) {
            LOG_INFO("Connecting to PostgreSQL...");
        }
        if (redis_client_) {
            LOG_INFO("Connecting to Redis...");
        }
        if (nats_client_) {
            LOG_INFO("Connecting to NATS...");
        }
    } catch (const std::exception& e) {
        LOG_ERROR(std::string("Error in AsyncRoomService::start: ") + e.what());
        shutdown();
    }
}

void AsyncRoomService::shutdown() {
    static bool is_shutdown = false; // Ensure shutdown is called only once

    if (is_shutdown) {
        LOG_WARN("AsyncRoomService shutdown called multiple times.");
        return;
    }

    if (is_running_) {
        try {
            is_running_ = false;
            LOG_INFO("AsyncRoomService shutting down.");

            if (pg_client_ && pg_client_->is_connected()) {
                LOG_INFO("Disconnecting from PostgreSQL...");
                pg_client_->disconnect();
            }
            if (redis_client_ && redis_client_->is_connected()) {
                LOG_INFO("Disconnecting from Redis...");
                redis_client_->disconnect();
            }
            if (nats_client_ && nats_client_->is_connected()) {
                LOG_INFO("Disconnecting from NATS...");
                nats_client_->disconnect();
            }
        } catch (const std::exception& e) {
            LOG_ERROR(std::string("Error in AsyncRoomService::shutdown: ") + e.what());
        }
    }

    is_shutdown = true;
}

