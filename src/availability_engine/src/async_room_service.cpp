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
    ProcessWithDataBase();
    //кешировать
  }
  catch(const std::exception& ex)
  {
    m_responder.FinishWithError(grpc::Status(grpc::StatusCode::INTERNAL, ex.what()), this);
    m_status = FINISH;
  }
}

void AsyncRoomService::ComputeIntervalsCallData::ProcessWithCache()
{
  //запись в редис Redis
}

void AsyncRoomService::ComputeIntervalsCallData::ProcessWithDataBase()
{
    const std::string room_code = m_request.room_id();
    const std::string day = m_request.date();

    const std::string req_start = m_request.start_time().empty() ? "9:00" : m_request.start_time();
    const std::string req_end = m_request.end_time().empty() ? "21:00" : m_request.end_time();

    struct PgCb final :IBookingsByRoomDateCb
    {
      ComputeIntervalsCallData* self;
      std::string dayISO, reqStart, reqEnd;
      explicit PgCb (ComputeIntervalsCallData * s, std::string d, std::string rs, std::string re) : self(s), dayISO(std::move(d)), reqStart(std::move(rs)), reqEnd(std::move(re)) {}
      static int toMinutes(const std::string& hhmm)
      {
        const char* p = hhmm.c_str();
        const char* t = strchr(p, 'T');
        if(t && strlen(t) >=6)
        {
          int H = (t[1] - '0') * 10 + (t[2] - '0');
          int M = (t[4] - '0') * 10 + (t[5] - '0');
          return H*60 + M;
        }
        int H = 0, M = 0;
        if(sscanf(p, "%d:%d", &H, &M) != 2)
        {
          return 0;
        }
        return H*60 + M;
      }
      static std::string mmToHHMM (int m)
      {
        int H = m / 60;
        int M = m % 60;
        char buf[6];
        snprintf(buf, sizeof(buf), "%02d:%02d", H, M);
        return std::string(buf);
      }
      void onBookings(Booking* rows, size_t n, bool ok, const char* err) override
      {
        if(!ok)
        {
          if(!self->m_done)
          {
            self->m_done = true;
            self->m_responder.FinishWithError(
              grpc::Status(grpc::StatusCode::INTERNAL, err ? err : "query failed"), self);
            self->m_status = FINISH;
          }
          delete this;
          return;
        }
        const int REQ_START = toMinutes(reqStart);
        const int REQ_END = toMinutes(reqEnd);
        std::vector<bool> busy(REQ_END - REQ_START, false);

        for(size_t i = 0; i < n; ++i)
        {
          const Booking& b = rows[i];
          int s = toMinutes(b.start_time);
          int e = toMinutes(b.end_time);

          if(s < e)
          {
            for(int m = s; m < e; ++m)
            {
              busy[m - REQ_START] = true;
            }
          }
        }
        auto ceilToHour = [](int m){return ((m + 59) / 60) *60;};
        auto floorToHour = [](int m){return ((m / 60) * 60);};
        int gridStart = ceilToHour(REQ_START);
        int gridEnd = floorToHour(REQ_END);

        auto* resp = self->m_response.mutable_available_slots();
        resp->Clear();

        for(int s = gridStart; s < gridEnd; s+=60)
        {
          int e =  s + 60;
          bool anyBusy = false;
          for(int m = s; m < e; ++m)
          {
            if(busy[m - REQ_START])
            {
              anyBusy = true;
              break;
            }
          }
          if(!anyBusy)
          {
            auto* ts = self->m_response.add_available_slots();
            ts->set_start_time(mmToHHMM(s));
            ts->set_end_time(mmToHHMM(e));
          }
        }
        if(!self->m_done)
        {
          self->CompleteRequest();
        }
        delete this;
      }
    };
    
    m_room_service->m_pg_client->getBookingsByRoomAndDate(room_code.c_str(), day.c_str(), new PgCb(this, day, req_start, req_end));

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
   //редис 
   //постгра
   //кешировать
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

void AsyncRoomService::OcupiedIntervalsCallData::ProcessWithDataBase()
{
  const std::string room_code = m_request.room_id();
    const std::string day = m_request.date();
    struct PgCb final : IBookingsByRoomDateCb
    {
      OcupiedIntervalsCallData* self;
      explicit PgCb(AsyncRoomService::OcupiedIntervalsCallData* s) : self(s) {}
      void onBookings(Booking* rows, size_t n, bool ok, const char* err) override
      {
        if(!ok)
        {
          if(self->m_done)
          {
            self->m_done = true;
            self->m_responder.FinishWithError(
              grpc::Status(grpc::StatusCode::INTERNAL, err ? err : "query failed"), self);
            self->m_status = FINISH;
          }
          delete this;
          return;
        }
        if(!self->m_done)
        {
          auto* resp = &self->m_response;
          resp->clear_intervals();
          resp->mutable_intervals()->Reserve(static_cast<int>(n));
          for(size_t i = 0; i < n; ++i)
          {
            const Booking& b = rows[i];
            auto it = resp ->add_intervals();
            it->set_booking_id(b.id);
            it->set_start_time(b.start_time);
            it->set_end_time(b.end_time);
            it->set_user_id(b.user_id);
            it->set_note(b.notes);
          }
          self->CompleteRequest();
        }
        delete this;
      }
    };

    m_room_service->m_pg_client->getBookingsByRoomAndDate(room_code.c_str(), day.c_str(), new PgCb(this));
}

void AsyncRoomService::OcupiedIntervalsCallData::ProcessRequest()
{
  try
  {
    ProcessWithDataBase();
    //кешировать
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
    m_pg_client->stop();
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
