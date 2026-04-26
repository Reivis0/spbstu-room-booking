package com.github.MadyarovGleb.booking_mvp.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for extracting floor number from room name using heuristic analysis.
 * 
 * Examples:
 * - "314" -> 3 (first digit)
 * - "1002" -> 10 (first two digits)
 * - "Г-305" -> 3 (digit after prefix)
 * - "каф.ТТС" -> null (no numeric pattern)
 */
public class FloorExtractor {
    
    // Pattern 1: Pure numeric room names (e.g., "314", "1002", "11")
    private static final Pattern PURE_NUMERIC = Pattern.compile("^(\\d+)$");
    
    // Pattern 2: Prefix + numeric (e.g., "Г-305", "А-201", "Л-1427")
    private static final Pattern PREFIX_NUMERIC = Pattern.compile("^[А-Яа-яA-Za-z]+-?(\\d+)$");
    
    // Pattern 3: Numeric + suffix (e.g., "314А", "201Б")
    private static final Pattern NUMERIC_SUFFIX = Pattern.compile("^(\\d+)[А-Яа-яA-Za-z]+$");
    
    /**
     * Extract floor number from room name.
     * 
     * @param roomName Room name (e.g., "314", "Г-305", "каф.ТТС")
     * @return Floor number or null if cannot be determined
     */
    public static Integer extractFloor(String roomName) {
        if (roomName == null || roomName.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = roomName.trim();
        
        // Try pure numeric pattern
        Matcher pureNumeric = PURE_NUMERIC.matcher(trimmed);
        if (pureNumeric.matches()) {
            String digits = pureNumeric.group(1);
            return extractFloorFromDigits(digits);
        }
        
        // Try prefix + numeric pattern (e.g., "Г-305")
        Matcher prefixNumeric = PREFIX_NUMERIC.matcher(trimmed);
        if (prefixNumeric.matches()) {
            String digits = prefixNumeric.group(1);
            return extractFloorFromDigits(digits);
        }
        
        // Try numeric + suffix pattern (e.g., "314А")
        Matcher numericSuffix = NUMERIC_SUFFIX.matcher(trimmed);
        if (numericSuffix.matches()) {
            String digits = numericSuffix.group(1);
            return extractFloorFromDigits(digits);
        }
        
        // No recognizable pattern
        return null;
    }
    
    /**
     * Extract floor from digit string.
     * 
     * Logic:
     * - 1-2 digits (e.g., "11", "9"): floor = first digit (1, 9)
     * - 3 digits (e.g., "314"): floor = first digit (3)
     * - 4+ digits (e.g., "1002", "1427"): floor = first 2 digits (10, 14)
     * 
     * @param digits Digit string
     * @return Floor number
     */
    private static Integer extractFloorFromDigits(String digits) {
        int length = digits.length();
        
        if (length <= 2) {
            // 1-2 digits: floor = first digit
            return Character.getNumericValue(digits.charAt(0));
        } else if (length == 3) {
            // 3 digits: floor = first digit
            return Character.getNumericValue(digits.charAt(0));
        } else {
            // 4+ digits: floor = first 2 digits
            String firstTwo = digits.substring(0, 2);
            try {
                return Integer.parseInt(firstTwo);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
