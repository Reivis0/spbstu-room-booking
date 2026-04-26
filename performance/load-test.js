import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// --- Кастомные метрики для SLA ---
const readTrend = new Trend('latency_read', true);
const writeTrend = new Trend('latency_write', true);

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081/api/v1';

export const options = {
    scenarios: {
        // 1. Взрывное чтение (Burst Read) - до 440 RPS
        burst_read: {
            executor: 'ramping-arrival-rate',
            startRate: 44,
            timeUnit: '1s',
            preAllocatedVUs: 100,
            maxVUs: 1000,
            stages: [
                { duration: '30s', target: 440 }, // Взрывной рост
                { duration: '1m', target: 440 },  // Удержание пика
                { duration: '30s', target: 0 },
            ],
            exec: 'burstReadTask',
        },
        // 2. Конкурентная запись (Race Condition) - 15 VU
        race_write: {
            executor: 'constant-vus',
            vus: 15,
            duration: '30s',
            exec: 'raceWriteTask',
            startTime: '2m', 
        },
        // 3. Реалистичный микс (Mixed Load) - 60 RPS (85/15)
        mixed_load: {
            executor: 'ramping-arrival-rate',
            startRate: 10,
            timeUnit: '1s',
            preAllocatedVUs: 50,
            maxVUs: 200,
            stages: [
                { duration: '1m', target: 60 },  // Разгон
                { duration: '3m', target: 60 },  // Плато (сократил с 5 до 3 для экономии времени)
                { duration: '1m', target: 0 },   // Спад
            ],
            exec: 'mixedLoadTask',
            startTime: '2m30s',
        },
    },
    thresholds: {
        'latency_read': ['p(95)<200'],
        'http_req_failed{status:500}': ['rate<0.0005'],
    },
};

const ROOM_IDS = [
    'c9cf25f2-1e4f-44b2-a789-4875d562d12b',
    'f5b5c395-1d00-4ae7-894a-629fba0b3d1e',
    '30e37299-bef3-499d-a27f-fac5e3c4df56',
    'd0adb1d5-7706-4922-9902-652d4faa8d8b'
];

const RACE_ROOM_ID = ROOM_IDS[0];
const RACE_SLOT = {
    startsAt: '2026-10-24T10:00:00Z',
    endsAt: '2026-10-24T11:00:00Z'
};

function getAuthHeaders(token) {
    return {
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
        },
    };
}

export function setup() {
    const res = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
        email: 'admin@arch.local',
        password: 'adminpass'
    }), { headers: { 'Content-Type': 'application/json' } });
    
    if (res.status !== 200) {
        throw new Error(`Setup failed: Status ${res.status}`);
    }
    return { token: res.json().access_token };
}

export function burstReadTask(data) {
    const roomId = randomItem(ROOM_IDS);
    const tags = { type: 'read' };
    
    const resSearch = http.get(`${BASE_URL}/rooms?capacity=10`, getAuthHeaders(data.token), { tags });
    readTrend.add(resSearch.duration, tags);
    check(resSearch, { 'search ok': (r) => r.status === 200 });

    const resAvail = http.get(`${BASE_URL}/rooms/${roomId}/availability?date=2026-10-24`, getAuthHeaders(data.token), { tags });
    readTrend.add(resAvail.duration, tags);
    check(resAvail, { 'avail ok': (r) => r.status === 200 });
}

export function raceWriteTask(data) {
    const tags = { type: 'write', scenario: 'race' };
    const payload = JSON.stringify({
        roomId: RACE_ROOM_ID,
        ...RACE_SLOT
    });

    const res = http.post(`${BASE_URL}/bookings`, payload, getAuthHeaders(data.token), { tags });
    writeTrend.add(res.duration, tags);
    
    check(res, {
        'integrity ok': (r) => [201, 409].includes(r.status),
        'no 500': (r) => r.status !== 500,
    });
}

export function mixedLoadTask(data) {
    const isWrite = Math.random() < 0.15;
    
    if (isWrite) {
        const roomId = randomItem(ROOM_IDS);
        const hour = Math.floor(Math.random() * 10) + 12;
        const payload = JSON.stringify({
            roomId: roomId,
            startsAt: `2026-10-24T${hour}:00:00Z`,
            endsAt: `2026-10-24T${hour+1}:00:00Z`
        });
        const res = http.post(`${BASE_URL}/bookings`, payload, getAuthHeaders(data.token), { tags: { type: 'write' } });
        writeTrend.add(res.duration);
        check(res, { 'write ok': (r) => [201, 409].includes(r.status) });
    } else {
        burstReadTask(data);
    }
}
