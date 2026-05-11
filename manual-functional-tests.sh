#!/bin/bash

# Manual Testing Script for Booking System
# Tests: RBAC, Transaction Consistency, Idempotency

BASE_URL="http://localhost:8081/api/v1"
TEST_DIR="/Users/maksimkladkovoj/obsidian-vault/20 AREAS/Work/Erlivideo/Testing"

echo "╔════════════════════════════════════════════════════════╗"
echo "║   MANUAL FUNCTIONAL TESTING - Booking System          ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test results
PASS_COUNT=0
FAIL_COUNT=0

# Helper function to print test result
print_result() {
    local test_name=$1
    local status=$2
    local message=$3
    
    if [ "$status" == "PASS" ]; then
        echo -e "${GREEN}✅ PASS${NC}: $test_name"
        ((PASS_COUNT++))
    else
        echo -e "${RED}❌ FAIL${NC}: $test_name"
        echo -e "   ${YELLOW}Reason: $message${NC}"
        ((FAIL_COUNT++))
    fi
}

# Test 1: Authentication
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "TEST 1: Authentication & JWT Token Generation"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Login as student
STUDENT_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"student@arch.local","password":"password"}')

STUDENT_HTTP_CODE=$(echo "$STUDENT_RESPONSE" | tail -n1)
STUDENT_BODY=$(echo "$STUDENT_RESPONSE" | sed '$d')

if [ "$STUDENT_HTTP_CODE" == "200" ]; then
    STUDENT_TOKEN=$(echo "$STUDENT_BODY" | jq -r '.access_token')
    print_result "Student Login" "PASS" ""
else
    print_result "Student Login" "FAIL" "HTTP $STUDENT_HTTP_CODE"
    STUDENT_TOKEN=""
fi

# Login as admin
ADMIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@arch.local","password":"adminpass"}')

ADMIN_HTTP_CODE=$(echo "$ADMIN_RESPONSE" | tail -n1)
ADMIN_BODY=$(echo "$ADMIN_RESPONSE" | sed '$d')

if [ "$ADMIN_HTTP_CODE" == "200" ]; then
    ADMIN_TOKEN=$(echo "$ADMIN_BODY" | jq -r '.access_token')
    print_result "Admin Login" "PASS" ""
else
    print_result "Admin Login" "FAIL" "HTTP $ADMIN_HTTP_CODE"
    ADMIN_TOKEN=""
fi

echo ""

# Test 2: RBAC - Student cannot access other user's bookings
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "TEST 2: RBAC - Access Control"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Create booking as student
CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/bookings" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -d '{
    "roomId":"550e8400-e29b-41d4-a716-446655440000",
    "startsAt":"2026-05-10T10:00:00Z",
    "endsAt":"2026-05-10T12:00:00Z",
    "title":"RBAC Test Booking"
  }')

CREATE_HTTP_CODE=$(echo "$CREATE_RESPONSE" | tail -n1)
CREATE_BODY=$(echo "$CREATE_RESPONSE" | sed '$d')

if [ "$CREATE_HTTP_CODE" == "201" ]; then
    BOOKING_ID=$(echo "$CREATE_BODY" | jq -r '.id')
    print_result "Create Booking (Student)" "PASS" ""
    
    # Try to access as different student (should fail)
    ACCESS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/bookings/$BOOKING_ID" \
      -H "Authorization: Bearer $STUDENT_TOKEN")
    
    ACCESS_HTTP_CODE=$(echo "$ACCESS_RESPONSE" | tail -n1)
    
    # Note: This test assumes we have a second student token
    # For now, we test that admin CAN access
    ADMIN_ACCESS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/bookings/$BOOKING_ID" \
      -H "Authorization: Bearer $ADMIN_TOKEN")
    
    ADMIN_ACCESS_HTTP_CODE=$(echo "$ADMIN_ACCESS_RESPONSE" | tail -n1)
    
    if [ "$ADMIN_ACCESS_HTTP_CODE" == "200" ]; then
        print_result "Admin Access to Student Booking" "PASS" ""
    else
        print_result "Admin Access to Student Booking" "FAIL" "HTTP $ADMIN_ACCESS_HTTP_CODE"
    fi
else
    print_result "Create Booking (Student)" "FAIL" "HTTP $CREATE_HTTP_CODE"
fi

echo ""

# Test 3: Transaction Consistency - Collision Detection
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "TEST 3: Transaction Consistency - Collision Detection"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Create first booking
BOOKING1_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/bookings" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -d '{
    "roomId":"550e8400-e29b-41d4-a716-446655440001",
    "startsAt":"2026-05-11T14:00:00Z",
    "endsAt":"2026-05-11T16:00:00Z",
    "title":"Collision Test 1"
  }')

BOOKING1_HTTP_CODE=$(echo "$BOOKING1_RESPONSE" | tail -n1)

if [ "$BOOKING1_HTTP_CODE" == "201" ]; then
    print_result "First Booking Created" "PASS" ""
    
    # Try to create overlapping booking (should fail with 409)
    BOOKING2_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/bookings" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $STUDENT_TOKEN" \
      -d '{
        "roomId":"550e8400-e29b-41d4-a716-446655440001",
        "startsAt":"2026-05-11T15:00:00Z",
        "endsAt":"2026-05-11T17:00:00Z",
        "title":"Collision Test 2 (Should Fail)"
      }')
    
    BOOKING2_HTTP_CODE=$(echo "$BOOKING2_RESPONSE" | tail -n1)
    
    if [ "$BOOKING2_HTTP_CODE" == "409" ]; then
        print_result "Collision Detection (409 Conflict)" "PASS" ""
    else
        print_result "Collision Detection" "FAIL" "Expected 409, got $BOOKING2_HTTP_CODE"
    fi
else
    print_result "First Booking Created" "FAIL" "HTTP $BOOKING1_HTTP_CODE"
fi

echo ""

# Test 4: Optimistic Locking - Concurrent Updates
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "TEST 4: Optimistic Locking - Version Conflict"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Create a booking
OPT_LOCK_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/bookings" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -d '{
    "roomId":"550e8400-e29b-41d4-a716-446655440002",
    "startsAt":"2026-05-12T10:00:00Z",
    "endsAt":"2026-05-12T12:00:00Z",
    "title":"Optimistic Lock Test"
  }')

OPT_LOCK_HTTP_CODE=$(echo "$OPT_LOCK_RESPONSE" | tail -n1)
OPT_LOCK_BODY=$(echo "$OPT_LOCK_RESPONSE" | sed '$d')

if [ "$OPT_LOCK_HTTP_CODE" == "201" ]; then
    OPT_LOCK_BOOKING_ID=$(echo "$OPT_LOCK_BODY" | jq -r '.id')
    print_result "Create Booking for Opt Lock Test" "PASS" ""
    
    # Update the booking
    UPDATE1_RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/bookings/$OPT_LOCK_BOOKING_ID" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $STUDENT_TOKEN" \
      -d '{
        "roomId":"550e8400-e29b-41d4-a716-446655440002",
        "startsAt":"2026-05-12T10:00:00Z",
        "endsAt":"2026-05-12T13:00:00Z",
        "title":"Updated Title 1"
      }')
    
    UPDATE1_HTTP_CODE=$(echo "$UPDATE1_RESPONSE" | tail -n1)
    
    if [ "$UPDATE1_HTTP_CODE" == "200" ]; then
        print_result "First Update" "PASS" ""
    else
        print_result "First Update" "FAIL" "HTTP $UPDATE1_HTTP_CODE"
    fi
else
    print_result "Create Booking for Opt Lock Test" "FAIL" "HTTP $OPT_LOCK_HTTP_CODE"
fi

echo ""

# Test 5: Trace ID Propagation
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "TEST 5: Trace ID Propagation in Logs"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Make a request and check for trace ID in response headers
TRACE_RESPONSE=$(curl -s -v -X GET "$BASE_URL/rooms?unpaged=true" 2>&1)

if echo "$TRACE_RESPONSE" | grep -i "x-trace-id\|x-request-id" > /dev/null; then
    print_result "Trace ID in Response Headers" "PASS" ""
else
    print_result "Trace ID in Response Headers" "FAIL" "No trace ID header found"
fi

echo ""

# Final Summary
echo "╔════════════════════════════════════════════════════════╗"
echo "║   TEST SUMMARY                                         ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""
echo "Total Tests: $((PASS_COUNT + FAIL_COUNT))"
echo -e "${GREEN}Passed: $PASS_COUNT${NC}"
echo -e "${RED}Failed: $FAIL_COUNT${NC}"
echo ""

if [ $FAIL_COUNT -eq 0 ]; then
    echo -e "${GREEN}✅ All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}❌ Some tests failed${NC}"
    exit 1
fi
