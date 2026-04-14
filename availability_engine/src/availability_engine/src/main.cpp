#include "async_room_service.hpp"
#include "logger.hpp"
#include <iostream>
#include <signal.h>
#include <thread>
#include <atomic>

std::unique_ptr<AsyncRoomService> service;

void SignalHandler(int signal) 
{
    static std::atomic<bool> signal_logged{false};
    if (!signal_logged.exchange(true)) {
        LOG_INFO("Received signal " + std::to_string(signal) + ", shutting down...");
        if (service)
        {
            service->shutdown();
        }
    }
}

int main()
{
    std::cout.setf(std::ios::unitbuf);
    std::cerr.setf(std::ios::unitbuf);
    std::setvbuf(stdout, NULL, _IONBF, 0);
    std::setvbuf(stderr, NULL, _IONBF, 0);
    signal(SIGINT, SignalHandler);
    signal(SIGTERM, SignalHandler);
    
    try
    {
        Logger::getInstance().init();

        auto redis_client = std::make_shared<RedisAsyncClient>();
        
        std::shared_ptr<PostgreSQLAsyncClient> pg_client;
        try {
            pg_client = std::make_shared<PostgreSQLAsyncClient>();
            LOG_INFO("PostgreSQL client created successfully.");
        } catch (const std::exception& e) {
            LOG_ERROR(std::string("Failed to initialize PostgreSQL client: ") + e.what());
            return EXIT_FAILURE;
        }
        
        auto nats_client = std::shared_ptr<NatsAsyncClient>();
        try {
            nats_client = std::make_shared<NatsAsyncClient>();
            LOG_INFO("NATS client created successfully.");
        } catch (const std::exception& e) {
            LOG_ERROR(std::string("Failed to initialize NATS client: ") + e.what());
            return EXIT_FAILURE;
        }
        
        service = std::make_unique<AsyncRoomService>(
            redis_client, pg_client, nats_client);
        
        LOG_INFO("Async Room Service starting...");
        service->start();
        
        while (service && service->isRunning()) {
            std::this_thread::sleep_for(std::chrono::seconds(1));
        }
        
    } catch (const std::exception& e)
    {
        LOG_ERROR(std::string("Exception: ") + e.what());
        return EXIT_FAILURE;
    }
    
    LOG_INFO("Async Room Service stopped.");
    return EXIT_SUCCESS;
}
