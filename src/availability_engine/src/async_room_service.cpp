#include <async_room_service.hpp>

#include <exception>

//AvailableSlotsCallData

AsyncRoomService::ComputeIntervalsCallData::ComputeIntervalsCallData(
    room_service::RoomService::AsyncService* service,
    grpc::ServerCompletionQueue* cq, AsyncRoomService* room_service) :
    m_service(service),  m_cq(cq), m_room_service(room_service), m_responder(&m_ctx), m_status(CREATE)
    {
        Proceed();
    }

void AsyncRoomService::ComputeIntervalsCallData::Proceed()
{
  if(m_status == CREATE)
  {
    m_status = PROCESS;
    m_service->RequestComputeIntervals(&m_ctx, &m_request, &m_responder, m_cq, m_cq, this);
  }
  else if(m_status == PROCESS)
  {
    new ComputeIntervalsCallData(m_service, m_cq, m_room_service);
    ProcessRequest();
  }
  else
  {
    delete this;
  }
}

void AsyncRoomService::ComputeIntervalsCallData::ProcessRequest()
{
  try
  {
    //кеш
    CompleteRequest();
  }
  catch(const std::exception& ex)
  {
    m_responder.FinishWithError(grpc::Status(grpc::StatusCode::INTERNAL, ex.what()), this);
    m_status = FINISH;
  }
}

void AsyncRoomService::ComputeIntervalsCallData::ProcessWithCache()
{
  //обработка Redis
}

void AsyncRoomService::ComputeIntervalsCallData::ProcessWithDataBase()
{
  //Обработка PG

}

void AsyncRoomService::ComputeIntervalsCallData::CompleteRequest()
{
  m_status = FINISH;
  m_responder.Finish(m_response, grpc::Status::OK, this);
}

//ValidateCallData

AsyncRoomService::ValidateCallData::ValidateCallData(room_service::RoomService::AsyncService* service, grpc::ServerCompletionQueue* cq,AsyncRoomService* room_service) :
  m_service(service), m_cq(cq),m_room_service(room_service), m_responder(&m_ctx), m_status(CREATE)
  {
    Proceed();
  }
  
void AsyncRoomService::ValidateCallData::Proceed()
{
  if(m_status == CREATE)
  {
    m_status = PROCESS;
    m_service->RequestValidate(&m_ctx, &m_request, &m_responder,m_cq, m_cq, this);
  }
  else if(m_status == PROCESS)
  {
    new ValidateCallData(m_service, m_cq, m_room_service);
    ProcessRequest();
  }
  else
  {
    delete this;
  }
}

void AsyncRoomService::ValidateCallData::ProcessRequest()
{
  try
  {
   //обработка 
  }
  catch(const std::exception& ex)
  {
    m_responder.FinishWithError(grpc::Status(grpc::StatusCode::INTERNAL, ex.what()), this);
    m_status = FINISH;
  }
   
}

void AsyncRoomService::ValidateCallData::CompleteRequest()
{
  m_status = FINISH;
  m_responder.Finish(m_response, grpc::Status::OK, this);
}

//OcupiedIntervalsCallData

AsyncRoomService::OcupiedIntervalsCallData::OcupiedIntervalsCallData(room_service::RoomService::AsyncService* service, grpc::ServerCompletionQueue* cq, AsyncRoomService* room_service) :
  m_service(service), m_cq(cq),m_room_service(room_service), m_responder(&m_ctx), m_status(CREATE)
  {
    Proceed();
  }
  
void AsyncRoomService::OcupiedIntervalsCallData::Proceed()
{
  if(m_status == CREATE)
  {
    m_status = PROCESS;
    m_service->RequestOcupiedIntervals(&m_ctx, &m_request, &m_responder,m_cq, m_cq, this);
  }
  else if(m_status == PROCESS)
  {
    new OcupiedIntervalsCallData(m_service, m_cq, m_room_service);
    ProcessRequest();
  }
  else
  {
    delete this;
  }
}

void AsyncRoomService::OcupiedIntervalsCallData::ProcessRequest()
{
  try
  {
   //обработка 
  }
  catch(const std::exception& ex)
  {
    m_responder.FinishWithError(grpc::Status(grpc::StatusCode::INTERNAL, ex.what()), this);
    m_status = FINISH;
  }
   
}

void AsyncRoomService::OcupiedIntervalsCallData::CompleteRequest()
{
  m_status = FINISH;
  m_responder.Finish(m_response, grpc::Status::OK, this);
}


AsyncRoomService::AsyncRoomService(
  std::shared_ptr<RedisAsyncClient> redis_client,
    std::shared_ptr<PostgreSQLAsyncClient> pg_client
    //std::shared_ptr<NatsAsyncClient> nats_clien
) : m_redis_client(redis_client), m_pg_client(pg_client) 
{
  if (m_redis_client) {
    m_redis_thread = std::thread([this]()
    {
        std::cout << "Starting Redis event loop..." << std::endl;
        m_redis_client->run_event_loop();
        std::cout << "Redis event loop finished" << std::endl;
    });
    m_redis_thread.detach();
}
}

AsyncRoomService::~AsyncRoomService()
{
  shutdown();
  if (m_redis_client)
  {
    m_redis_client->stop_event_loop();
  }
}

void AsyncRoomService::start()
{
  std::string adress ("0.0.0.0:50051");
  grpc::ServerBuilder builder;

  builder.AddListeningPort(adress, grpc::InsecureServerCredentials());
  builder.RegisterService(&m_service);
  m_cq = builder.AddCompletionQueue();
  m_server = builder.BuildAndStart();

  //подписка на nats

  new ComputeIntervalsCallData(&m_service, m_cq.get(), this);
  new ValidateCallData(&m_service, m_cq.get(), this);
  new OcupiedIntervalsCallData(&m_service, m_cq.get(), this);

  void* tag;
  bool ok;
  while(!m_shutdown && m_cq->Next(&tag, &ok))
  {
    if(ok)
    {
      auto call_data = static_cast<CallData*>(tag);
      call_data->Proceed();
    }
  }
}

void AsyncRoomService::shutdown()
{
  m_shutdown = true;

  if (m_server)
  {
    m_server->Shutdown();
  }
  if(m_cq)
  {
    m_cq->Shutdown();
  }
}
