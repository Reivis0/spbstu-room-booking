package com.github.MadyarovGleb.booking_mvp.util;

import java.util.regex.Pattern;

/**
 * Utility class for estimating room capacity based on room name patterns.
 * 
 * Heuristic rules:
 * - Large lecture halls (contains "актовый", "зал", "аудитория", or 4+ digit room): 100
 * - Computer labs (contains "комп", "лаб", "lab", "пк"): 20
 * - Small rooms (1-2 digit room number): 15
 * - Default: 30
 */
public class CapacityEstimator {
    
    // Patterns for large lecture halls
    private static final Pattern LARGE_HALL = Pattern.compile(
        ".*(актовый|зал|аудитория|конференц).*", 
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    
    // Patterns for computer labs
    private static final Pattern COMPUTER_LAB = Pattern.compile(
        ".*(комп|лаб|lab|пк|компьютер).*", 
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    
    // Pattern for extracting numeric part
    private static final Pattern NUMERIC_PART = Pattern.compile("(\\d+)");
    
    /**
     * Estimate room capacity based on room name.
     * 
     * @param roomName Room name (e.g., "314", "актовый зал", "комп. класс")
     * @return Estimated capacity
     */
    public static int estimateCapacity(String roomName) {
        if (roomName == null || roomName.trim().isEmpty()) {
            return 30; // Default
        }
        
        String trimmed = roomName.trim().toLowerCase();
        
        // Check for large lecture halls
        if (LARGE_HALL.matcher(trimmed).matches()) {
            return 100;
        }
        
        // Check for computer labs
        if (COMPUTER_LAB.matcher(trimmed).matches()) {
            return 20;
        }
        
        // Check room number length
        java.util.regex.Matcher numericMatcher = NUMERIC_PART.matcher(trimmed);
        if (numericMatcher.find()) {
            String digits = numericMatcher.group(1);
            int length = digits.length();
            
            if (length <= 2) {
                // Small room (1-2 digits): 15 people
                return 15;
            } else if (length >= 4) {
                // Large room (4+ digits): 100 people
                return 100;
            }
        }
        
        // Default capacity
        return 30;
    }
}
