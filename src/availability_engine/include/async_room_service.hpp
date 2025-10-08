#ifndef ASYNC_ROOM_SERVICE_HPP
#define ASYNC_ROOM_SERVICE_HPP

#include <include/genproto/message.grpc.pb.h>
#include <include/genproto/message.pb.h>
#include <grpcpp/grpcpp.h>

#include <src/common/cahce/redis_acync_client.hpp>
#include <src/common/database/postreSQL_async_clien.hpp>
#include <src/common/messaging/nats_async_client.hpp>

#include <string>
#include <memory>

class AsyncRoomService
{
public:
  AsyncRoomService(const std::string& adress) : m_adress(adress) {}
  ~AsyncRoomService();
  void start();
  void shutDown();

//CallData
private:
  class CallData
  {
  public:
    virtual ~CallData() = default;
    virtual void Proceed() = 0;


  };
  
  class AvailableSlotsCallData : public CallData
  {
  public:
    AvailableSlotsCallData(room_service::RoomService::AsyncService* service, grpc::ServerCompletionQueue* cq);
    void Proceed() override;
    void ProcessRequest();
    void ProcessWithCache();
    void ProcessWithDataBase();
    void CompleteRequest();

    private:
      room_service::RoomService::AsyncService* m_service;
      grpc::ServerCompletionQueue* m_cq;
      grpc::ServerContext m_ctx;
      room_service::AvailableSlotsRequest m_request;
      room_service::AvailableSlotsResponse m_response;
      grpc::ServerAsyncResponseWriter<room_service::AvailableSlotsResponse> m_responder;
      enum CallStatus {CREATE, PROCESS, FINISH};
      CallStatus m_status;
  };

  class BookRoomCallData : public CallData
  {
    public:
    BookRoomCallData(room_service::RoomService::AsyncService* service, grpc::ServerCompletionQueue* cq);
    void Proceed() override;
    void ProcessRequest();
    void CompleteRequest();

    private:
      room_service::RoomService::AsyncService* m_service;
      grpc::ServerCompletionQueue* m_cq;
      grpc::ServerContext m_ctx;
      room_service::BookRoomRequest m_request;
      room_service::BookRoomResponse m_response;
      grpc::ServerAsyncResponseWriter<room_service::BookRoomResponse> m_responder;
      enum CallStatus {CREATE, PROCESS, FINISH};
      CallStatus m_status;
  };
  
  class ValidateCallData : public CallData
  {
    public:
    ValidateCallData(room_service::RoomService::AsyncService* service, grpc::ServerCompletionQueue* cq);
    void Proceed() override;
    void ProcessRequest();
    void CompleteRequest();

    private:
      room_service::RoomService::AsyncService* m_service;
      grpc::ServerCompletionQueue* m_cq;
      grpc::ServerContext m_ctx;
      room_service::ValidateRequest m_request;
      room_service::ValidateResponse m_response;
      grpc::ServerAsyncResponseWriter<room_service::ValidateResponse> m_responder;
      enum CallStatus {CREATE, PROCESS, FINISH};
      CallStatus m_status;
  };

  class OcupiedIntervalsCallData : public CallData
  {
    public:
    OcupiedIntervalsCallData(room_service::RoomService::AsyncService* service, grpc::ServerCompletionQueue* cq);
    void Proceed() override;
    void ProcessRequest();
    void CompleteRequest();

    private:
      room_service::RoomService::AsyncService* m_service;
      grpc::ServerCompletionQueue* m_cq;
      grpc::ServerContext m_ctx;
      room_service::OcupiedIntervalsRequest m_request;
      room_service::OcupiedIntervalsResponce m_response;
      grpc::ServerAsyncResponseWriter<room_service::OcupiedIntervalsResponce> m_responder;
      enum CallStatus {CREATE, PROCESS, FINISH};
      CallStatus m_status;
  };


//методы
private:


//поля 
private:
  std::string  m_adress;
  room_service::RoomService::AsyncService m_service;
  std::unique_ptr<grpc::ServerCompletionQueue> m_cq;
  std::unique_ptr<grpc::Server> m_server;


};
#endif
