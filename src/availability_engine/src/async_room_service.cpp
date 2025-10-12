#include <src/availability_engine/include/async_room_service.hpp>

#include <exception>

//AvailableSlotsCallData

AsyncRoomService::ComputeIntervalsCallData::ComputeIntervalsCallData(
    room_service::RoomService::AsyncService* service,
    grpc::ServerCompletionQueue* cq) :
    m_service(service),  m_cq(cq), m_responder(&m_ctx), m_status(CREATE)
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
    new ComputeIntervalsCallData(m_service, m_cq);
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

AsyncRoomService::ValidateCallData::ValidateCallData(room_service::RoomService::AsyncService* service, grpc::ServerCompletionQueue* cq) :
  m_service(service), m_cq(cq), m_responder(&m_ctx), m_status(CREATE)
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
    new ValidateCallData(m_service, m_cq);
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

AsyncRoomService::OcupiedIntervalsCallData::OcupiedIntervalsCallData(room_service::RoomService::AsyncService* service, grpc::ServerCompletionQueue* cq) :
  m_service(service), m_cq(cq), m_responder(&m_ctx), m_status(CREATE)
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
    new OcupiedIntervalsCallData(m_service, m_cq);
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

