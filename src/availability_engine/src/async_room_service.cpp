#include <src/availability_engine/include/async_room_service.hpp>

#include <exception>

//AvailableSlotsCallData

AsyncRoomService::AvailableSlotsCallData::AvailableSlotsCallData(
    room_service::RoomService::AsyncService* service,
    grpc::ServerCompletionQueue* cq) :
    m_service(service),  m_cq(cq), m_responder(&m_ctx), m_status(CREATE)
    {
        Proceed();
    }

void AsyncRoomService::AvailableSlotsCallData::Proceed()
{
  if(m_status == CREATE)
  {
    m_status = PROCESS;
    m_service->RequestAvailableSlots(&m_ctx, &m_request, &m_responder, m_cq, m_cq, this);
  }
  else if(m_status == PROCESS)
  {
    new AvailableSlotsCallData(m_service, m_cq);
    ProcessRequest();
  }
  else
  {
    delete this;
  }
}

void AsyncRoomService::AvailableSlotsCallData::ProcessRequest()
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

void AsyncRoomService::AvailableSlotsCallData::ProcessWithCache()
{
  //обработка Redis
}

void AsyncRoomService::AvailableSlotsCallData::ProcessWithDataBase()
{
  //Обработка PG

}

void AsyncRoomService::AvailableSlotsCallData::CompleteRequest()
{
  m_status = FINISH;
  m_responder.Finish(m_response, grpc::Status::OK, this);
}

//BookRoomCallData

AsyncRoomService::BookRoomCallData::BookRoomCallData(room_service::RoomService::AsyncService* service, grpc::ServerCompletionQueue* cq) :
  m_service(service), m_cq(cq), m_responder(&m_ctx), m_status(CREATE)
  {
    Proceed();
  }
  
void AsyncRoomService::BookRoomCallData::Proceed()
{
  if(m_status == CREATE)
  {
    m_status = PROCESS;
    m_service->RequestBookRoom(&m_ctx, &m_request, &m_responder,m_cq, m_cq, this);
  }
  else if(m_status == PROCESS)
  {
    new BookRoomCallData(m_service, m_cq);
    ProcessRequest();
  }
  else
  {
    delete this;
  }
}

void AsyncRoomService::BookRoomCallData::ProcessRequest()
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

void AsyncRoomService::BookRoomCallData::CompleteRequest()
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

