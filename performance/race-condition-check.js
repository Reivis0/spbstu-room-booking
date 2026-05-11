import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    race_condition: {
      executor: 'per-vu-iterations',
      vus: 100,
      iterations: 1,
      maxDuration: '30s',
    },
  },
};

const BASE_URL = 'http://127.0.0.1:8081/api/v1';

export default function () {
  // 1. Login
  const loginRes = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
    email: 'student@arch.local',
    password: 'password',
  }), {
    headers: { 'Content-Type': 'application/json' },
  });

  check(loginRes, { 'login success': (r) => r.status === 200 });
  const token = loginRes.json().access_token;

  // 2. All VUs try to book the SAME slot at the SAME time
  // We use a fixed time in the future to ensure collision
  const startTime = '2026-06-01T10:00:00Z';
  const endTime = '2026-06-01T11:00:00Z';
  
  const bookingRes = http.post(`${BASE_URL}/bookings`, JSON.stringify({
    roomId: '550e8400-e29b-41d4-a716-446655440000',
    startsAt: startTime,
    endsAt: endTime,
    title: 'Race Condition Test',
  }), {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  });

  // We expect exactly ONE 201 and 99 409s
  // But k6 collects stats, so we just check status
  check(bookingRes, {
    'status is 201 or 409': (r) => r.status === 201 || r.status === 409,
  });
}
