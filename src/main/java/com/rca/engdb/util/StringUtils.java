package com.rca.engdb.util;

import java.util.List;

public class StringUtils {

    /**
     * Find the best match for the input string from a list of candidates.
     * Returns the candidate if the edit distance is within the threshold.
     */
    public static String findBestMatch(String input, List<String> candidates, int maxDistance) {
        String bestMatch = null;
        int minDistance = Integer.MAX_VALUE;

        for (String candidate : candidates) {
            int distance = calculateLevenshteinDistance(input.toLowerCase(), candidate.toLowerCase());
            
            if (distance < minDistance) {
                minDistance = distance;
                bestMatch = candidate;
            }
        }

        if (minDistance <= maxDistance) {
            return bestMatch;
        }
        
        return null;
    }

    /**
     * Calculate Levenshtein Distance (Edit Distance) between two strings.
     */
    public static int calculateLevenshteinDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
            
        for (int i = 1; i <= s1.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= s2.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), 
                        s1.charAt(i - 1) == s2.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        
        return costs[s2.length()];
    }
}
