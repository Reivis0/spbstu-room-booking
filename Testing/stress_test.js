import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { randomItem } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

const readTrend = new Trend('latency_read', true);
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081/api/v1';

export const options = {
    scenarios: {
        stress_test: {
            executor: 'ramping-arrival-rate',
            startRate: 100,
            timeUnit: '1s',
            preAllocatedVUs: 200,
            maxVUs: 2000,
            stages: [
                { duration: '1m', target: 500 },  // Разгон до 500
                { duration: '2m', target: 800 },  // Разгон до 800
                { duration: '2m', target: 1200 }, // Разгон до 1200 (Breaking Point)
                { duration: '1m', target: 0 },
            ],
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<500'],
    },
};

const ROOM_IDS = [
    '00164124-f94e-452e-ae86-525b3131c097',
    '003daddd-2b4d-455a-a6e7-4d58a796a4b9',
    '005a6170-8e9e-402c-8005-fea46e320e03'
];

export function setup() {
    const res = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
        email: 'admin@arch.local',
        password: 'adminpass'
    }), { headers: { 'Content-Type': 'application/json' } });
    return { token: res.json().access_token };
}

export default function (data) {
    const params = {
        headers: {
            'Authorization': `Bearer ${data.token}`,
            'Content-Type': 'application/json',
        },
    };
    
    // Pure Read Load to find capacity limit
    const roomId = randomItem(ROOM_IDS);
    const res = http.get(`${BASE_URL}/rooms/${roomId}/availability?date=2026-05-04`, params);
    
    check(res, {
        'status is 200': (r) => r.status === 200,
    });
}
