#ifndef ASYNC_ROOM_SERVICE_HPP
#define ASYNC_ROOM_SERVICE_HPP

#include <message.grpc.pb.h>
#include <message.pb.h>
#include <grpcpp/grpcpp.h>

#include <cache/redis_async_client.hpp>
#include <database/postgreSQL_async_client.hpp>
#include <messaging/nats_async_client.hpp>

#include <string>
#include <memory>
#include <atomic>
#include <thread>

class AsyncRoomService
{
public:
  AsyncRoomService(
    std::shared_ptr<RedisAsyncClient> redis_client,
    std::shared_ptr<PostgreSQLAsyncClient> pg_client
    //std::shared_ptr<NatsAsyncClient> nats_clien
  );
  ~AsyncRoomService();
  void start();
  void shutdown();

//CallData
private:
  class CallData
  {
  public:
    virtual ~CallData() = default;
    virtual void Proceed() = 0;


  };
  
  class ComputeIntervalsCallData : public CallData
  {
  public:
    ComputeIntervalsCallData(room_service::RoomService::AsyncService* service, grpc::ServerCompletionQueue* cq, AsyncRoomService* room_service);
    void Proceed() override;
    void ProcessRequest();
    void ProcessWithCache();
    void ProcessWithDataBase();
    void CompleteRequest();

    private:
      room_service::RoomService::AsyncService* m_service;
      grpc::ServerCompletionQueue* m_cq;
      grpc::ServerContext m_ctx;
      AsyncRoomService*  m_room_service;
      room_service::ComputeIntervalsRequest m_request;
      room_service::ComputeIntervalsResponse m_response;
      grpc::ServerAsyncResponseWriter<room_service::ComputeIntervalsResponse> m_responder;
      enum CallStatus {CREATE, PROCESS, FINISH};
      CallStatus m_status;

      bool m_done {false};
  };
  
  class ValidateCallData : public CallData
  {
    public:
    ValidateCallData(room_service::RoomService::AsyncService* service, grpc::ServerCompletionQueue* cq,  AsyncRoomService* room_service);
    void Proceed() override;
    void ProcessRequest();
    void CompleteRequest();

    private:
      room_service::RoomService::AsyncService* m_service;
      grpc::ServerCompletionQueue* m_cq;
      grpc::ServerContext m_ctx;
      AsyncRoomService*  m_room_service;
      room_service::ValidateRequest m_request;
      room_service::ValidateResponse m_response;
      grpc::ServerAsyncResponseWriter<room_service::ValidateResponse> m_responder;
      enum CallStatus {CREATE, PROCESS, FINISH};
      CallStatus m_status;
  };

  class OcupiedIntervalsCallData : public CallData
  {
    public:
    OcupiedIntervalsCallData(room_service::RoomService::AsyncService* service, grpc::ServerCompletionQueue* cq,  AsyncRoomService* room_service);
    void Proceed() override;
    void ProcessRequest();
    void CompleteRequest();
    void ProcessWithDataBase();

    private:
      room_service::RoomService::AsyncService* m_service;
      grpc::ServerCompletionQueue* m_cq;
      grpc::ServerContext m_ctx;
      AsyncRoomService*  m_room_service;
      room_service::OcupiedIntervalsRequest m_request;
      room_service::OcupiedIntervalsResponce m_response;
      grpc::ServerAsyncResponseWriter<room_service::OcupiedIntervalsResponce> m_responder;
      enum CallStatus {CREATE, PROCESS, FINISH};
      CallStatus m_status;
      
      bool m_done {false};
  };

//методы
private:


//поля 
private:
  room_service::RoomService::AsyncService m_service;
  std::unique_ptr<grpc::ServerCompletionQueue> m_cq;
  std::unique_ptr<grpc::Server> m_server;
  std::atomic<bool> m_shutdown {false};
  std::thread m_redis_thread;

  std::shared_ptr<RedisAsyncClient> m_redis_client;
  std::shared_ptr<PostgreSQLAsyncClient> m_pg_client;
  //std::shared_ptr<NatsAsyncClient> m_nats_client;

};
#endif
