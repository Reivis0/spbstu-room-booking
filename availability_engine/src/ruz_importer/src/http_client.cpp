#include "http_client.hpp"
#include <curl/curl.h>
#include <sstream>
#include <memory>

struct HttpClient::Impl
{
  CURL* curl;
  std::string response_data;
  
  Impl() : curl(curl_easy_init())
  {
    if (curl)
    {
      curl_easy_setopt(curl, CURLOPT_USERAGENT, "RUZ-Importer/1.0");
      curl_easy_setopt(curl, CURLOPT_TIMEOUT, 30L);
      curl_easy_setopt(curl, CURLOPT_CONNECTTIMEOUT, 10L);
      curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);
      curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, HttpClient::write_callback);
    }
  }
  
  ~Impl()
  {
    if (curl)
    {
      curl_easy_cleanup(curl);
    }
  }
};

size_t HttpClient::write_callback(char* ptr, size_t size, size_t nmemb, void* userdata)
{
  size_t total_size = size * nmemb;
  std::string* response = static_cast<std::string*>(userdata);
  response->append(ptr, total_size);
  return total_size;
}

HttpClient::HttpClient() : p_impl(std::make_unique<Impl>())
{
  if (!p_impl->curl)
  {
    LOG_ERROR("RUZ_IMPORTER:HTTP_CLIENT: Failed to initialize CURL");
  }
}

HttpClient::~HttpClient() = default;

std::string HttpClient::fetch_ruz_data(const std::string& api_url)
{
  if (!p_impl->curl)
  {
    LOG_ERROR("RUZ_IMPORTER:HTTP_CLIENT: CURL not initialized");
    return "";
  }
  
  p_impl->response_data.clear();
  curl_easy_setopt(p_impl->curl, CURLOPT_URL, api_url.c_str());
  curl_easy_setopt(p_impl->curl, CURLOPT_WRITEDATA, &p_impl->response_data);
  
  CURLcode res = curl_easy_perform(p_impl->curl);
  
  if (res != CURLE_OK)
  {
    LOG_ERROR("RUZ_IMPORTER:HTTP_CLIENT: CURL request failed: " + std::string(curl_easy_strerror(res)));
    return "";
  }
  
  long http_code = 0;
  curl_easy_getinfo(p_impl->curl, CURLINFO_RESPONSE_CODE, &http_code);
  
  if (http_code != 200)
  {
    LOG_ERROR("RUZ_IMPORTER:HTTP_CLIENT: HTTP error: " + std::to_string(http_code));
    return "";
  }
  
  LOG_INFO("RUZ_IMPORTER:HTTP_CLIENT: Successfully fetched " + std::to_string(p_impl->response_data.size()) + " bytes from RUZ API");
  return p_impl->response_data;
}