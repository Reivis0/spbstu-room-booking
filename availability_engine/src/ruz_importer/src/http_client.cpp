#include "http_client.hpp"
#include <curl/curl.h>
#include <sstream>
#include <memory>
#include <thread>
#include <logger.hpp>
#include <algorithm>
#include <random>
#include <cstdlib>

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
      curl_easy_setopt(curl, CURLOPT_TCP_KEEPALIVE, 1L);
      curl_easy_setopt(curl, CURLOPT_TCP_KEEPIDLE, 60L);
      curl_easy_setopt(curl, CURLOPT_TCP_KEEPINTVL, 10L);
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

HttpClient::HttpClient() : p_impl(std::make_unique<Impl>()), delay_ms_(300)
{
  if (!p_impl->curl)
  {
    LOG_ERROR("RUZ_IMPORTER:HTTP_CLIENT: Failed to initialize CURL");
  }
  
  // Load proactive delay from environment
  if (const char* env_delay = std::getenv("RUZ_DELAY_MS")) {
    delay_ms_ = std::stoi(env_delay);
  }
  
  LOG_INFO("RUZ_IMPORTER:HTTP_CLIENT: Initialized with proactive delay=" + std::to_string(delay_ms_) + "ms");
}

HttpClient::~HttpClient() = default;

int HttpClient::get_jitter_ms(int base_delay_ms) {
  static thread_local std::mt19937 gen(std::random_device{}());
  std::uniform_int_distribution<> dist(-50, 100); // Jitter: -50ms to +100ms
  return base_delay_ms + dist(gen);
}

std::string HttpClient::fetch_ruz_data(const std::string& api_url)
{
  if (!p_impl->curl)
  {
    LOG_ERROR("RUZ_IMPORTER:HTTP_CLIENT: CURL not initialized");
    return "";
  }

  LOG_INFO("RUZ_IMPORTER:HTTP_CLIENT: Starting fetch for URL: " + api_url);

  // PROACTIVE THROTTLING: Sleep BEFORE making request to avoid 429
  int jittered_delay = get_jitter_ms(delay_ms_);
  LOG_INFO("RUZ_IMPORTER:HTTP_CLIENT: Sleeping for " + std::to_string(jittered_delay) + "ms before request");
  std::this_thread::sleep_for(std::chrono::milliseconds(jittered_delay));
  LOG_INFO("RUZ_IMPORTER:HTTP_CLIENT: Sleep completed, starting HTTP request");

  int retry_count = 5; // Reduced retries since we're being proactive
  int delay_ms = 1000;

  for (int attempt = 1; attempt <= retry_count; ++attempt) {
    LOG_INFO("RUZ_IMPORTER:HTTP_CLIENT: Attempt " + std::to_string(attempt) + " for URL: " + api_url);
    p_impl->response_data.clear();
    curl_easy_setopt(p_impl->curl, CURLOPT_URL, api_url.c_str());
    curl_easy_setopt(p_impl->curl, CURLOPT_WRITEDATA, &p_impl->response_data);
    LOG_INFO("RUZ_IMPORTER:HTTP_CLIENT: Calling curl_easy_perform for URL: " + api_url);
    CURLcode res = curl_easy_perform(p_impl->curl);
    LOG_INFO("RUZ_IMPORTER:HTTP_CLIENT: curl_easy_perform completed with code: " + std::to_string(res));
    
    if (res != CURLE_OK) {
        LOG_WARN("RUZ_IMPORTER:HTTP_CLIENT: Attempt " + std::to_string(attempt) + " failed: " + std::string(curl_easy_strerror(res)));
    } else {
        long http_code = 0;
        curl_easy_getinfo(p_impl->curl, CURLINFO_RESPONSE_CODE, &http_code);
        
        if (http_code == 200) {
            return p_impl->response_data;
        } else if (http_code == 429) {
            LOG_WARN("RUZ_IMPORTER:HTTP_CLIENT: Rate limited (429) despite throttling. Attempt " + std::to_string(attempt));
            // Shorter exponential backoff since we're already being careful
            int sleep_time = std::min(2 << attempt, 16); // 4, 8, 16 sec max
            std::this_thread::sleep_for(std::chrono::seconds(sleep_time));
            continue;
        } else if (http_code >= 500) {
            LOG_WARN("RUZ_IMPORTER:HTTP_CLIENT: Attempt " + std::to_string(attempt) + " server error: " + std::to_string(http_code));
        } else {
            LOG_ERROR("RUZ_IMPORTER:HTTP_CLIENT: HTTP error " + std::to_string(http_code) + " for URL: " + api_url);
            return ""; // Non-retryable error
        }
    }

    if (attempt < retry_count) {
        std::this_thread::sleep_for(std::chrono::milliseconds(delay_ms * attempt));
    }
  }
  
  LOG_ERROR("RUZ_IMPORTER:HTTP_CLIENT: Failed to fetch data after " + std::to_string(retry_count) + " attempts: " + api_url);
  return "";
}