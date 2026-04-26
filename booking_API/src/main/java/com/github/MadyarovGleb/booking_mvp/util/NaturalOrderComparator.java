package com.github.MadyarovGleb.booking_mvp.util;

import java.util.Comparator;

/**
 * Natural order comparator for room names.
 * Sorts rooms naturally: "1", "2", "10", "11" instead of "1", "10", "11", "2"
 * Also handles prefixes: "Г-1", "Г-2", "Г-10"
 */
public class NaturalOrderComparator implements Comparator<String> {

    @Override
    public int compare(String s1, String s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return -1;
        if (s2 == null) return 1;

        int i1 = 0, i2 = 0;
        int len1 = s1.length(), len2 = s2.length();

        while (i1 < len1 && i2 < len2) {
            char c1 = s1.charAt(i1);
            char c2 = s2.charAt(i2);

            // Both are digits - compare numerically
            if (Character.isDigit(c1) && Character.isDigit(c2)) {
                // Extract full numbers
                int num1Start = i1;
                while (i1 < len1 && Character.isDigit(s1.charAt(i1))) {
                    i1++;
                }
                int num2Start = i2;
                while (i2 < len2 && Character.isDigit(s2.charAt(i2))) {
                    i2++;
                }

                String numStr1 = s1.substring(num1Start, i1);
                String numStr2 = s2.substring(num2Start, i2);

                try {
                    int num1 = Integer.parseInt(numStr1);
                    int num2 = Integer.parseInt(numStr2);
                    if (num1 != num2) {
                        return Integer.compare(num1, num2);
                    }
                } catch (NumberFormatException e) {
                    // Fallback to string comparison
                    int cmp = numStr1.compareTo(numStr2);
                    if (cmp != 0) return cmp;
                }
            } else {
                // Lexicographic comparison
                if (c1 != c2) {
                    return Character.compare(c1, c2);
                }
                i1++;
                i2++;
            }
        }

        // If one string is prefix of another
        return Integer.compare(len1, len2);
    }
}
