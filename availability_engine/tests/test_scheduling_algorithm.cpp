#include <gtest/gtest.h>
#include <string>
#include <vector>
#include <algorithm>

// Mock structures for testing scheduling algorithm logic
struct TimeInterval {
    std::string start_time;
    std::string end_time;
    
    TimeInterval(const std::string& start, const std::string& end)
        : start_time(start), end_time(end) {}
};

struct Booking {
    std::string id;
    std::string start_time;
    std::string end_time;
    std::string user_id;
    std::string notes;
    std::string status;
};

// Core scheduling algorithm functions to test
class SchedulingAlgorithm {
public:
    // Compute available time slots given a list of bookings
    static std::vector<TimeInterval> computeAvailableSlots(
        const std::string& date,
        const std::string& window_start,
        const std::string& window_end,
        const std::vector<Booking>& bookings) {
        
        std::vector<TimeInterval> available_slots;
        std::vector<Booking> sorted_bookings = bookings;
        
        // Sort bookings by start time
        std::sort(sorted_bookings.begin(), sorted_bookings.end(), 
                  [](const Booking& a, const Booking& b) {
                      return a.start_time < b.start_time;
                  });
        
        std::string current_start = date + " " + window_start;
        
        for (const auto& booking : sorted_bookings) {
            // If there's a gap before this booking
            if (booking.start_time > current_start) {
                available_slots.emplace_back(
                    current_start.substr(11, 5),
                    booking.start_time.substr(11, 5)
                );
            }
            // Move current_start to the end of this booking
            if (booking.end_time > current_start) {
                current_start = booking.end_time.substr(0, 16);
            }
        }
        
        // Check if there's a gap after the last booking
        std::string window_end_full = date + " " + window_end;
        if (current_start < window_end_full) {
            available_slots.emplace_back(
                current_start.substr(11, 5),
                window_end_full.substr(11, 5)
            );
        }
        
        return available_slots;
    }
    
    // Validate if a time slot has conflicts
    static bool validateTimeSlot(
        const std::string& date,
        const std::string& start_time,
        const std::string& end_time,
        const std::vector<Booking>& bookings) {
        
        std::string start_full = date + " " + start_time;
        std::string end_full = date + " " + end_time;
        
        for (const auto& booking : bookings) {
            // Check for overlap: [start1, end1) overlaps [start2, end2) if start1 < end2 && start2 < end1
            if (booking.start_time < end_full && start_full < booking.end_time) {
                return false; // Conflict found
            }
        }
        
        return true; // No conflicts
    }
    
    // Merge overlapping intervals
    static std::vector<TimeInterval> mergeOverlappingIntervals(
        const std::vector<Booking>& bookings) {
        
        if (bookings.empty()) return {};
        
        std::vector<Booking> sorted_bookings = bookings;
        std::sort(sorted_bookings.begin(), sorted_bookings.end(),
                  [](const Booking& a, const Booking& b) {
                      return a.start_time < b.start_time;
                  });
        
        std::vector<TimeInterval> merged;
        std::string current_start = sorted_bookings[0].start_time;
        std::string current_end = sorted_bookings[0].end_time;
        
        for (size_t i = 1; i < sorted_bookings.size(); ++i) {
            if (sorted_bookings[i].start_time <= current_end) {
                // Overlapping, extend the current interval
                current_end = std::max(current_end, sorted_bookings[i].end_time);
            } else {
                // No overlap, save current interval and start a new one
                merged.emplace_back(current_start.substr(11, 5), current_end.substr(11, 5));
                current_start = sorted_bookings[i].start_time;
                current_end = sorted_bookings[i].end_time;
            }
        }
        
        // Add the last interval
        merged.emplace_back(current_start.substr(11, 5), current_end.substr(11, 5));
        
        return merged;
    }
};

// Test Suite for Scheduling Algorithm
class SchedulingAlgorithmTest : public ::testing::Test {
protected:
    std::string test_date = "2026-04-26";
};

// Test 1: Empty schedule - entire day should be available
TEST_F(SchedulingAlgorithmTest, EmptySchedule_EntireDayAvailable) {
    std::vector<Booking> bookings;
    
    auto slots = SchedulingAlgorithm::computeAvailableSlots(
        test_date, "08:00", "21:00", bookings);
    
    ASSERT_EQ(slots.size(), 1);
    EXPECT_EQ(slots[0].start_time, "08:00");
    EXPECT_EQ(slots[0].end_time, "21:00");
}

// Test 2: Single booking in the middle - two free slots
TEST_F(SchedulingAlgorithmTest, SingleBooking_TwoFreeSlots) {
    std::vector<Booking> bookings = {
        {"1", test_date + " 10:00", test_date + " 12:00", "user1", "Meeting", "confirmed"}
    };
    
    auto slots = SchedulingAlgorithm::computeAvailableSlots(
        test_date, "08:00", "21:00", bookings);
    
    ASSERT_EQ(slots.size(), 2);
    EXPECT_EQ(slots[0].start_time, "08:00");
    EXPECT_EQ(slots[0].end_time, "10:00");
    EXPECT_EQ(slots[1].start_time, "12:00");
    EXPECT_EQ(slots[1].end_time, "21:00");
}

// Test 3: Multiple non-overlapping bookings
TEST_F(SchedulingAlgorithmTest, MultipleBookings_MultipleFreeSlots) {
    std::vector<Booking> bookings = {
        {"1", test_date + " 09:00", test_date + " 10:00", "user1", "Meeting 1", "confirmed"},
        {"2", test_date + " 11:00", test_date + " 12:00", "user2", "Meeting 2", "confirmed"},
        {"3", test_date + " 14:00", test_date + " 15:00", "user3", "Meeting 3", "confirmed"}
    };
    
    auto slots = SchedulingAlgorithm::computeAvailableSlots(
        test_date, "08:00", "21:00", bookings);
    
    ASSERT_EQ(slots.size(), 4);
    EXPECT_EQ(slots[0].start_time, "08:00");
    EXPECT_EQ(slots[0].end_time, "09:00");
    EXPECT_EQ(slots[1].start_time, "10:00");
    EXPECT_EQ(slots[1].end_time, "11:00");
    EXPECT_EQ(slots[2].start_time, "12:00");
    EXPECT_EQ(slots[2].end_time, "14:00");
    EXPECT_EQ(slots[3].start_time, "15:00");
    EXPECT_EQ(slots[3].end_time, "21:00");
}

// Test 4: Booking at the start of working hours
TEST_F(SchedulingAlgorithmTest, BookingAtStart_OneFreeSlotAtEnd) {
    std::vector<Booking> bookings = {
        {"1", test_date + " 08:00", test_date + " 10:00", "user1", "Early meeting", "confirmed"}
    };
    
    auto slots = SchedulingAlgorithm::computeAvailableSlots(
        test_date, "08:00", "21:00", bookings);
    
    ASSERT_EQ(slots.size(), 1);
    EXPECT_EQ(slots[0].start_time, "10:00");
    EXPECT_EQ(slots[0].end_time, "21:00");
}

// Test 5: Booking at the end of working hours
TEST_F(SchedulingAlgorithmTest, BookingAtEnd_OneFreeSlotAtStart) {
    std::vector<Booking> bookings = {
        {"1", test_date + " 19:00", test_date + " 21:00", "user1", "Late meeting", "confirmed"}
    };
    
    auto slots = SchedulingAlgorithm::computeAvailableSlots(
        test_date, "08:00", "21:00", bookings);
    
    ASSERT_EQ(slots.size(), 1);
    EXPECT_EQ(slots[0].start_time, "08:00");
    EXPECT_EQ(slots[0].end_time, "19:00");
}

// Test 6: Fully booked day - no free slots
TEST_F(SchedulingAlgorithmTest, FullyBooked_NoFreeSlots) {
    std::vector<Booking> bookings = {
        {"1", test_date + " 08:00", test_date + " 21:00", "user1", "All day event", "confirmed"}
    };
    
    auto slots = SchedulingAlgorithm::computeAvailableSlots(
        test_date, "08:00", "21:00", bookings);
    
    EXPECT_EQ(slots.size(), 0);
}

// Test 7: Overlapping bookings - should merge correctly
TEST_F(SchedulingAlgorithmTest, OverlappingBookings_MergedCorrectly) {
    std::vector<Booking> bookings = {
        {"1", test_date + " 09:00", test_date + " 11:00", "user1", "Meeting 1", "confirmed"},
        {"2", test_date + " 10:00", test_date + " 12:00", "user2", "Meeting 2", "confirmed"}
    };
    
    auto slots = SchedulingAlgorithm::computeAvailableSlots(
        test_date, "08:00", "21:00", bookings);
    
    ASSERT_EQ(slots.size(), 2);
    EXPECT_EQ(slots[0].start_time, "08:00");
    EXPECT_EQ(slots[0].end_time, "09:00");
    EXPECT_EQ(slots[1].start_time, "12:00");
    EXPECT_EQ(slots[1].end_time, "21:00");
}

// Test 8: Adjacent bookings (no gap between them)
TEST_F(SchedulingAlgorithmTest, AdjacentBookings_NoGapBetween) {
    std::vector<Booking> bookings = {
        {"1", test_date + " 09:00", test_date + " 10:00", "user1", "Meeting 1", "confirmed"},
        {"2", test_date + " 10:00", test_date + " 11:00", "user2", "Meeting 2", "confirmed"}
    };
    
    auto slots = SchedulingAlgorithm::computeAvailableSlots(
        test_date, "08:00", "21:00", bookings);
    
    ASSERT_EQ(slots.size(), 2);
    EXPECT_EQ(slots[0].start_time, "08:00");
    EXPECT_EQ(slots[0].end_time, "09:00");
    EXPECT_EQ(slots[1].start_time, "11:00");
    EXPECT_EQ(slots[1].end_time, "21:00");
}

// Test 9: Validation - no conflicts
TEST_F(SchedulingAlgorithmTest, Validate_NoConflicts) {
    std::vector<Booking> bookings = {
        {"1", test_date + " 09:00", test_date + " 10:00", "user1", "Meeting", "confirmed"}
    };
    
    bool is_valid = SchedulingAlgorithm::validateTimeSlot(
        test_date, "10:00", "11:00", bookings);
    
    EXPECT_TRUE(is_valid);
}

// Test 10: Validation - conflict detected
TEST_F(SchedulingAlgorithmTest, Validate_ConflictDetected) {
    std::vector<Booking> bookings = {
        {"1", test_date + " 09:00", test_date + " 11:00", "user1", "Meeting", "confirmed"}
    };
    
    bool is_valid = SchedulingAlgorithm::validateTimeSlot(
        test_date, "10:00", "12:00", bookings);
    
    EXPECT_FALSE(is_valid);
}

// Test 11: Validation - exact overlap
TEST_F(SchedulingAlgorithmTest, Validate_ExactOverlap) {
    std::vector<Booking> bookings = {
        {"1", test_date + " 10:00", test_date + " 11:00", "user1", "Meeting", "confirmed"}
    };
    
    bool is_valid = SchedulingAlgorithm::validateTimeSlot(
        test_date, "10:00", "11:00", bookings);
    
    EXPECT_FALSE(is_valid);
}

// Test 12: Validation - booking inside existing booking
TEST_F(SchedulingAlgorithmTest, Validate_BookingInsideExisting) {
    std::vector<Booking> bookings = {
        {"1", test_date + " 09:00", test_date + " 12:00", "user1", "Long meeting", "confirmed"}
    };
    
    bool is_valid = SchedulingAlgorithm::validateTimeSlot(
        test_date, "10:00", "11:00", bookings);
    
    EXPECT_FALSE(is_valid);
}

// Test 13: Validation - existing booking inside new booking
TEST_F(SchedulingAlgorithmTest, Validate_ExistingInsideNew) {
    std::vector<Booking> bookings = {
        {"1", test_date + " 10:00", test_date + " 11:00", "user1", "Short meeting", "confirmed"}
    };
    
    bool is_valid = SchedulingAlgorithm::validateTimeSlot(
        test_date, "09:00", "12:00", bookings);
    
    EXPECT_FALSE(is_valid);
}

// Test 14: Merge overlapping intervals
TEST_F(SchedulingAlgorithmTest, MergeIntervals_TwoOverlapping) {
    std::vector<Booking> bookings = {
        {"1", test_date + " 09:00", test_date + " 11:00", "user1", "Meeting 1", "confirmed"},
        {"2", test_date + " 10:00", test_date + " 12:00", "user2", "Meeting 2", "confirmed"}
    };
    
    auto merged = SchedulingAlgorithm::mergeOverlappingIntervals(bookings);
    
    ASSERT_EQ(merged.size(), 1);
    EXPECT_EQ(merged[0].start_time, "09:00");
    EXPECT_EQ(merged[0].end_time, "12:00");
}

// Test 15: Merge intervals - no overlap
TEST_F(SchedulingAlgorithmTest, MergeIntervals_NoOverlap) {
    std::vector<Booking> bookings = {
        {"1", test_date + " 09:00", test_date + " 10:00", "user1", "Meeting 1", "confirmed"},
        {"2", test_date + " 11:00", test_date + " 12:00", "user2", "Meeting 2", "confirmed"}
    };
    
    auto merged = SchedulingAlgorithm::mergeOverlappingIntervals(bookings);
    
    ASSERT_EQ(merged.size(), 2);
    EXPECT_EQ(merged[0].start_time, "09:00");
    EXPECT_EQ(merged[0].end_time, "10:00");
    EXPECT_EQ(merged[1].start_time, "11:00");
    EXPECT_EQ(merged[1].end_time, "12:00");
}

// Test 16: Edge case - booking exactly at boundary
TEST_F(SchedulingAlgorithmTest, EdgeCase_BookingAtBoundary) {
    std::vector<Booking> bookings = {
        {"1", test_date + " 08:00", test_date + " 09:00", "user1", "Early", "confirmed"}
    };
    
    bool is_valid_before = SchedulingAlgorithm::validateTimeSlot(
        test_date, "07:00", "08:00", bookings);
    bool is_valid_after = SchedulingAlgorithm::validateTimeSlot(
        test_date, "09:00", "10:00", bookings);
    
    EXPECT_TRUE(is_valid_before);
    EXPECT_TRUE(is_valid_after);
}

int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}
