#include "async_room_service.hpp"
#include "logger.hpp"
#include <iostream>
#include <signal.h>

std::unique_ptr<AsyncRoomService> service;

void SignalHandler(int signal) 
{
    LOG_INFO("AVAILABILITY_ENGINE: ASINC_ROOM_SERVICE:Received signal " + std::to_string(signal) + ", shutting down...");
    if (service)
    {
        service->shutdown();
    }
}

int main()
{
    signal(SIGINT, SignalHandler);
    signal(SIGTERM, SignalHandler);
    
    try
    {
        Logger::getInstance().init();

        auto redis_client = std::make_shared<RedisAsyncClient>();
        auto pg_client = std::make_shared<PostgreSQLAsyncClient>();
        auto nats_client = std::make_shared<NatsAsyncClient>();
        
        service = std::make_unique<AsyncRoomService>(
            redis_client, pg_client, nats_client);
        
        LOG_INFO("AVAILABILITY_ENGINE: ASINC_ROOM_SERVICE: Async Room Service starting...");
        service->start();
        
    } catch (const std::exception& e)
    {
        LOG_ERROR("AVAILABILITY_ENGINE: ASINC_ROOM_SERVICE: " + std::string("Exception: ") + e.what());
        return EXIT_FAILURE;
    }
    
    LOG_INFO("AVAILABILITY_ENGINE: ASINC_ROOM_SERVICE: Async Room Service stopped.");
    return EXIT_SUCCESS;
}
