#ifndef HTTP_CLIENT_HPP
#define HTTP_CLIENT_HPP

#include <logger.hpp>
#include <string>
#include <memory>

class HttpClient
{
public:
  HttpClient();
  ~HttpClient();
  
  std::string fetch_ruz_data(const std::string& api_url);
  
private:
  static size_t write_callback(char* ptr, size_t size, size_t nmemb, void* userdata);
  int get_jitter_ms(int base_delay_ms);
  
  struct Impl;
  std::unique_ptr<Impl> p_impl;
  int delay_ms_;
};

#endif