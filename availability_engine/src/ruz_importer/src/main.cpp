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
  signal(SIGINT, signalHandler);
  signal(SIGTERM, signalHandler);
  
  try
  {
    Logger::getInstance().init();
    
    LOG_INFO("RUZ_Importer: RUZ Importer Starting");
    
    auto pg_client = std::make_shared<PostgreSQLAsyncClient>();
    auto nats_client = std::make_shared<NatsAsyncClient>();
    g_importer = std::make_unique<RuzImporter>(pg_client, nats_client);
    
    LOG_INFO("RUZ_Importer: Service initialized. Starting main loop...");
    g_importer->run();
    
  }
  catch (const std::exception& e)
  {
    LOG_ERROR(std::string("RUZ_Importer: Fatal exception: ") + e.what());
    return EXIT_FAILURE;
  }
  
  LOG_INFO("RUZ_Importer: RUZ Importer Stopped");
  return EXIT_SUCCESS;
}