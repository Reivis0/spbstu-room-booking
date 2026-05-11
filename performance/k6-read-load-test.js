import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const latencyTrend = new Trend('latency_ms');

// Test configuration for READ operations (440 RPS target)
export const options = {
  scenarios: {
    // Scenario 1: Baseline load (average 0.694 RPS)
    baseline: {
      executor: 'constant-arrival-rate',
      rate: 1,
      timeUnit: '1s',
      duration: '2m',
      preAllocatedVUs: 5,
      maxVUs: 20,
      startTime: '0s',
      tags: { test_type: 'baseline' },
    },
    // Scenario 2: Peak load (440 RPS)
    peak_load: {
      executor: 'constant-arrival-rate',
      rate: 440,
      timeUnit: '1s',
      duration: '3m',
      preAllocatedVUs: 100,
      maxVUs: 500,
      startTime: '2m',
      tags: { test_type: 'peak' },
    },
    // Scenario 3: Stress test (gradual increase to find breaking point)
    stress_test: {
      executor: 'ramping-arrival-rate',
      startRate: 440,
      timeUnit: '1s',
      preAllocatedVUs: 200,
      maxVUs: 1000,
      stages: [
        { duration: '2m', target: 600 },
        { duration: '2m', target: 800 },
        { duration: '2m', target: 1000 },
        { duration: '2m', target: 1200 },
        { duration: '2m', target: 1500 },
      ],
      startTime: '5m',
      tags: { test_type: 'stress' },
    },
  },
  thresholds: {
    'http_req_duration{test_type:peak}': ['p(95)<200'], // p95 < 200ms for peak load
    'http_req_duration{test_type:baseline}': ['p(95)<200'],
    'errors{test_type:peak}': ['rate<0.01'], // Error rate < 1%
    'http_req_failed{test_type:peak}': ['rate<0.01'],
  },
};

const BASE_URL = 'http://localhost:8081/api/v1';

// Test data - room IDs and building IDs (will be populated from actual data)
const testRoomIds = [
  '550e8400-e29b-41d4-a716-446655440000',
  '550e8400-e29b-41d4-a716-446655440001',
  '550e8400-e29b-41d4-a716-446655440002',
];

const testBuildingIds = [
  '650e8400-e29b-41d4-a716-446655440000',
  '650e8400-e29b-41d4-a716-446655440001',
];

// Simulate 50KB payload response (as per requirements)
export default function () {
  const scenarios = [
    // 1. Room search (most common read operation)
    () => {
      const params = {
        headers: {
          'Content-Type': 'application/json',
        },
        tags: { endpoint: 'room_search' },
      };
      
      const buildingId = testBuildingIds[Math.floor(Math.random() * testBuildingIds.length)];
      const response = http.get(
        `${BASE_URL}/rooms?building_id=${buildingId}&capacity_min=10&unpaged=true`,
        params
      );
      
      const success = check(response, {
        'status is 200': (r) => r.status === 200,
        'response time < 200ms': (r) => r.timings.duration < 200,
        'has rooms data': (r) => r.json() && Array.isArray(r.json()),
      });
      
      errorRate.add(!success);
      latencyTrend.add(response.timings.duration);
      
      return response;
    },
    
    // 2. Room schedule retrieval
    () => {
      const params = {
        headers: {
          'Content-Type': 'application/json',
        },
        tags: { endpoint: 'room_schedule' },
      };
      
      const roomId = testRoomIds[Math.floor(Math.random() * testRoomIds.length)];
      const date = '2026-05-04';
      const response = http.get(
        `${BASE_URL}/rooms/${roomId}/schedule?date=${date}`,
        params
      );
      
      const success = check(response, {
        'status is 200': (r) => r.status === 200,
        'response time < 200ms': (r) => r.timings.duration < 200,
      });
      
      errorRate.add(!success);
      latencyTrend.add(response.timings.duration);
      
      return response;
    },
    
    // 3. Room availability check
    () => {
      const params = {
        headers: {
          'Content-Type': 'application/json',
        },
        tags: { endpoint: 'room_availability' },
      };
      
      const roomId = testRoomIds[Math.floor(Math.random() * testRoomIds.length)];
      const date = '2026-05-04';
      const response = http.get(
        `${BASE_URL}/rooms/${roomId}/availability?date=${date}&min_duration=30`,
        params
      );
      
      const success = check(response, {
        'status is 200': (r) => r.status === 200,
        'response time < 300ms': (r) => r.timings.duration < 300,
      });
      
      errorRate.add(!success);
      latencyTrend.add(response.timings.duration);
      
      return response;
    },
    
    // 4. Room metadata with JSONB filtering
    () => {
      const params = {
        headers: {
          'Content-Type': 'application/json',
        },
        tags: { endpoint: 'room_metadata' },
      };
      
      const response = http.get(
        `${BASE_URL}/rooms?features=projector&features=whiteboard&capacity_min=20`,
        params
      );
      
      const success = check(response, {
        'status is 200': (r) => r.status === 200,
        'response time < 200ms (JSONB filter)': (r) => r.timings.duration < 200,
      });
      
      errorRate.add(!success);
      latencyTrend.add(response.timings.duration);
      
      return response;
    },
  ];
  
  // Randomly select a scenario (weighted distribution)
  const weights = [0.5, 0.25, 0.15, 0.1]; // Room search is most common
  const random = Math.random();
  let cumulative = 0;
  let selectedScenario = 0;
  
  for (let i = 0; i < weights.length; i++) {
    cumulative += weights[i];
    if (random < cumulative) {
      selectedScenario = i;
      break;
    }
  }
  
  scenarios[selectedScenario]();
  
  // Small sleep to prevent overwhelming the system
  sleep(0.1);
}

export function handleSummary(data) {
  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function textSummary(data, options) {
  const indent = options.indent || '';
  
  let summary = '\n' + indent + '=== READ LOAD TEST SUMMARY ===\n\n';
  
  // HTTP metrics
  if (data.metrics.http_req_duration) {
    const duration = data.metrics.http_req_duration.values;
    summary += indent + 'HTTP Request Duration:\n';
    summary += indent + `  avg: ${duration.avg.toFixed(2)}ms\n`;
    summary += indent + `  p95: ${duration['p(95)'].toFixed(2)}ms\n`;
    summary += indent + `  p99: ${duration['p(99)'].toFixed(2)}ms\n`;
  }
  
  // Request rate
  if (data.metrics.http_reqs) {
    summary += indent + `Total Requests: ${data.metrics.http_reqs.values.count}\n`;
    summary += indent + `Request Rate: ${data.metrics.http_reqs.values.rate.toFixed(2)} req/s\n\n`;
  }
  
  return summary;
}
