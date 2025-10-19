#include "async_room_service.hpp"
#include <iostream>
#include <signal.h>

std::unique_ptr<AsyncRoomService> service;

void SignalHandler(int signal) 
{
    std::cout << "Received signal " << signal << ", shutting down..." << std::endl;
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
        auto redis_client = std::make_shared<RedisAsyncClient>();
        auto pg_client = std::make_shared<PostgreSQLAsyncClient>();
        
        // Создаем и запускаем сервис
        service = std::make_unique<AsyncRoomService>(
            redis_client, pg_client);
        
        std::cout << "Async Room Service starting..." << std::endl;
        service->start();
        
    } catch (const std::exception& e)
    {
        std::cerr << "Exception: " << e.what() << std::endl;
        return EXIT_FAILURE;
    }
    
    std::cout << "Async Room Service stopped." << std::endl;
    return EXIT_SUCCESS;
}