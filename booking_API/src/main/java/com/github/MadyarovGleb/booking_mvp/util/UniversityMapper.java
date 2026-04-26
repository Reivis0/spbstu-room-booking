package com.github.MadyarovGleb.booking_mvp.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for mapping frontend university codes to database university codes.
 * Frontend uses uppercase codes (SPBSTU, SPBU, LETI), database uses lowercase (spbptu, spbgu, leti).
 */
public class UniversityMapper {
    
    private static final Map<String, String> FRONTEND_TO_DB = new HashMap<>();
    private static final Map<String, String> DB_TO_FRONTEND = new HashMap<>();
    
    static {
        // Frontend -> Database mappings
        FRONTEND_TO_DB.put("SPBSTU", "spbptu");
        FRONTEND_TO_DB.put("SPBU", "spbgu");
        FRONTEND_TO_DB.put("LETI", "leti");
        
        // Database -> Frontend mappings (reverse)
        DB_TO_FRONTEND.put("spbptu", "SPBSTU");
        DB_TO_FRONTEND.put("spbgu", "SPBU");
        DB_TO_FRONTEND.put("leti", "LETI");
    }
    
    /**
     * Converts frontend university code to database code.
     * @param frontendCode Frontend code (e.g., "SPBSTU", "SPBU", "LETI")
     * @return Database code (e.g., "spbptu", "spbgu", "leti") or null if not found
     */
    public static String toDbCode(String frontendCode) {
        if (frontendCode == null) {
            return null;
        }
        return FRONTEND_TO_DB.get(frontendCode.toUpperCase());
    }
    
    /**
     * Converts database university code to frontend code.
     * @param dbCode Database code (e.g., "spbptu", "spbgu", "leti")
     * @return Frontend code (e.g., "SPBSTU", "SPBU", "LETI") or null if not found
     */
    public static String toFrontendCode(String dbCode) {
        if (dbCode == null) {
            return null;
        }
        return DB_TO_FRONTEND.get(dbCode.toLowerCase());
    }
}
