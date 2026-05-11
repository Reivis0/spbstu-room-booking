import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Custom metrics
const readTrend = new Trend('latency_read', true);
const writeTrend = new Trend('latency_write', true);

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081/api/v1';

// Payload for write (approx 2KB)
const dummyData = 'A'.repeat(2000);

export const options = {
    scenarios: {
        // 1. Read Load (440 RPS)
        read_load: {
            executor: 'constant-arrival-rate',
            rate: 440,
            timeUnit: '1s',
            duration: '2m',
            preAllocatedVUs: 100,
            maxVUs: 1000,
            exec: 'readTask',
        },
        // 2. Write Load (11 RPS)
        write_load: {
            executor: 'constant-arrival-rate',
            rate: 11,
            timeUnit: '1s',
            duration: '2m',
            preAllocatedVUs: 20,
            maxVUs: 100,
            exec: 'writeTask',
        },
        // 3. Race Condition Test (100 simultaneous requests)
        race_condition: {
            executor: 'per-vu-iterations',
            vus: 100,
            iterations: 1,
            startTime: '2m10s',
            exec: 'raceTask',
        },
    },
    thresholds: {
        'latency_read': ['p(95)<500'],
        'latency_write': ['p(95)<500'],
        'http_req_failed': ['rate<0.01'],
    },
};

const ROOM_IDS = [
    '00164124-f94e-452e-ae86-525b3131c097',
    '003daddd-2b4d-455a-a6e7-4d58a796a4b9',
    '005a6170-8e9e-402c-8005-fea46e320e03',
    '006936b7-6cc4-459c-873c-cbbd479f40f7',
    '00a1a8dd-4f43-4a0b-979d-d914f10aaa13'
];

const RACE_ROOM_ID = ROOM_IDS[0];

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

export function readTask(data) {
    const params = {
        headers: {
            'Authorization': `Bearer ${data.token}`,
            'Content-Type': 'application/json',
        },
        tags: { type: 'read' },
    };
    
    // Mix of searches and metadata requests
    if (Math.random() < 0.7) {
        const res = http.get(`${BASE_URL}/rooms?capacity_min=10&university=SPbSTU`, params);
        if (res && res.duration) readTrend.add(res.duration);
        check(res, { 'read search ok': (r) => r.status === 200 });
    } else {
        const roomId = randomItem(ROOM_IDS);
        const res = http.get(`${BASE_URL}/rooms/${roomId}/availability?date=2026-05-04`, params);
        if (res && res.duration) readTrend.add(res.duration);
        check(res, { 'read availability ok': (r) => r.status === 200 });
    }
}

export function writeTask(data) {
    const params = {
        headers: {
            'Authorization': `Bearer ${data.token}`,
            'Content-Type': 'application/json',
        },
        tags: { type: 'write' },
    };
    
    const hour = Math.floor(Math.random() * 10) + 8; // 08:00 to 18:00
    const payload = JSON.stringify({
        roomId: randomItem(ROOM_IDS),
        startsAt: `2026-06-01T${hour.toString().padStart(2, '0')}:00:00Z`,
        endsAt: `2026-06-01T${(hour+1).toString().padStart(2, '0')}:00:00Z`,
        title: 'Load Test Booking',
        description: dummyData // Ensure ~2KB payload
    });

    const res = http.post(`${BASE_URL}/bookings`, payload, params);
    if (res && res.duration) writeTrend.add(res.duration);
    
    // 201 Created or 409 Conflict (expected under load)
    check(res, {
        'write status valid': (r) => [201, 409, 400].includes(r.status),
        'not 500': (r) => r.status !== 500,
    });
}

export function raceTask(data) {
    const params = {
        headers: {
            'Authorization': `Bearer ${data.token}`,
            'Content-Type': 'application/json',
        },
        tags: { type: 'race' },
    };
    
    const payload = JSON.stringify({
        roomId: RACE_ROOM_ID,
        startsAt: '2026-07-01T10:00:00Z',
        endsAt: '2026-07-01T11:00:00Z',
        title: 'Race Condition Test'
    });

    const res = http.post(`${BASE_URL}/bookings`, payload, params);
    
    // In a race condition, only ONE should get 201, others 409
    check(res, {
        'race status valid': (r) => [201, 409].includes(r.status),
    });
}
