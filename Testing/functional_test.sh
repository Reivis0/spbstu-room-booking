#!/bin/bash

BASE_URL="http://localhost:8081/api/v1"
ROOM_ID="00164124-f94e-452e-ae86-525b3131c097"

echo "=== Starting Functional Tests ==="

# 1. Auth & RBAC Check
echo "Test 1: Admin Login"
ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@arch.local", "password":"adminpass"}' | jq -r .access_token)

if [ "$ADMIN_TOKEN" != "null" ]; then
  echo "[PASS] Admin authenticated"
else
  echo "[FAIL] Admin authentication failed"
  exit 1
fi

echo "Test 2: User Login"
USER_TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@arch.local", "password":"userpass"}' | jq -r .access_token)

if [ "$USER_TOKEN" != "null" ]; then
  echo "[PASS] User authenticated"
else
  echo "[FAIL] User authentication failed"
  # Try to create user if not exists or use existing if any
  echo "Attempting to find another user or proceed"
fi

# 2. Room Visibility
echo "Test 3: Search Rooms"
SEARCH_RES=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE_URL/rooms?capacity_min=10")
COUNT=$(echo "$SEARCH_RES" | jq '.content | length')
if [ "$COUNT" -gt 0 ]; then
  echo "[PASS] Found $COUNT rooms"
else
  echo "[FAIL] No rooms found"
fi

# 3. Booking Atomicity & Conflict
echo "Test 4: Double Booking Conflict"
BOOKING_PAYLOAD='{"roomId":"'$ROOM_ID'", "startsAt":"2026-08-01T12:00:00Z", "endsAt":"2026-08-01T13:00:00Z", "title":"Functional Test"}'

RES1=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/bookings" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$BOOKING_PAYLOAD")

echo "First booking attempt: $RES1"

RES2=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/bookings" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$BOOKING_PAYLOAD")

echo "Second booking attempt (conflict expected): $RES2"

if [ "$RES1" == "201" ] && [ "$RES2" == "409" ]; then
  echo "[PASS] Conflict detected correctly (Atomicity)"
else
  echo "[FAIL] Unexpected status codes: $RES1, $RES2"
fi

# 4. Trace ID Check
echo "Test 5: Trace ID in Logs"
TRACE_ID=$(curl -s -I -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE_URL/rooms/$ROOM_ID" | grep -i "X-Trace-Id")
echo "Trace ID from header: $TRACE_ID"

# 5. Cleanup
# (Optional: delete test bookings if API supports it)

echo "=== Functional Tests Completed ==="
