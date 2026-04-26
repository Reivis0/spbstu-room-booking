#include "ruz_importer.hpp"
#include <iostream>
#include <signal.h>
#include <memory>

std::unique_ptr<RuzImporter> g_importer;

void signalHandler(int signal) 
{
  LOG_INFO("RUZ_Importer: RUZ Importer received signal " + std::to_string(signal) + ", shutting down...");
  if (g_importer)
  {
    g_importer->shutdown();
  }
}

int main()
{
  std::cout.setf(std::ios::unitbuf);
  std::cerr.setf(std::ios::unitbuf);
  std::setvbuf(stdout, NULL, _IONBF, 0);
  std::setvbuf(stderr, NULL, _IONBF, 0);
  signal(SIGINT, signalHandler);
  signal(SIGTERM, signalHandler);
  
  try
  {
    Logger::getInstance().init("ruz-importer");
    
    LOG_INFO("RUZ_Importer: RUZ Importer Starting");
    
    // Create and initialize PostgreSQL client
    auto pg_client = std::make_shared<PostgreSQLAsyncClient>();
    pg_client->start(); 

    auto nats_client = std::make_shared<NatsAsyncClient>();
    
    auto redis_client = std::make_shared<RedisAsyncClient>();
    redis_client->connect();

    g_importer = std::make_unique<RuzImporter>(pg_client, nats_client, redis_client);
    
    LOG_INFO("RUZ_Importer: Service initialized. Starting main loop...");
    g_importer->start();
    
  }
  catch (const std::exception& e)
  {
    LOG_ERROR(std::string("RUZ_Importer: Fatal exception: ") + e.what());
    return EXIT_FAILURE;
  }
  
  LOG_INFO("RUZ_Importer: RUZ Importer Stopped");
  return EXIT_SUCCESS;
}