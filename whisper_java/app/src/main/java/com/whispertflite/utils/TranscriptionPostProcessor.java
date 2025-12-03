package com.whispertflite.utils;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * TranscriptionPostProcessor provides context-aware corrections for transcription,
 * specifically addressing homonym disambiguation issues.
 */
public class TranscriptionPostProcessor {
    private static final String TAG = "TranscriptionPostProcessor";
    
    // Common homonym correction rules with context patterns
    private static final Map<String, HomonymRule> HOMONYM_RULES = new HashMap<>();
    
    static {
        // their/there/they're
        HOMONYM_RULES.put("their", new HomonymRule(
            "their",
            new String[]{"there", "they're"},
            new String[]{".*\\b(house|car|dog|cat|book|phone|computer|family|friend|children|parents)\\b.*"},
            new String[]{".*\\b(is|are|was|were|will be|has been)\\b.*", ".*\\b(they are|they were)\\b.*"}
        ));
        
        HOMONYM_RULES.put("there", new HomonymRule(
            "there",
            new String[]{"their", "they're"},
            new String[]{".*\\b(is|are|was|were|will be|has been|over|here|go|went|live|located)\\b.*"},
            new String[]{".*\\b(house|car|dog|cat|book|phone)\\b.*", ".*\\b(they are|they were)\\b.*"}
        ));
        
        // your/you're
        HOMONYM_RULES.put("your", new HomonymRule(
            "your",
            new String[]{"you're"},
            new String[]{".*\\b(house|car|name|phone|book|idea|question|answer|problem|solution)\\b.*"},
            new String[]{".*\\b(you are|you were|you will be)\\b.*"}
        ));
        
        // to/too/two
        HOMONYM_RULES.put("to", new HomonymRule(
            "to",
            new String[]{"too", "two"},
            new String[]{".*\\b(go|went|going|send|give|talk|listen|want|need|have)\\b.*"},
            new String[]{".*\\b(much|many|also|as well)\\b.*", ".*\\b(people|things|items|persons|of them)\\b.*"}
        ));
        
        // its/it's
        HOMONYM_RULES.put("its", new HomonymRule(
            "its",
            new String[]{"it's"},
            new String[]{".*\\b(owns|has|possessive|belongs)\\b.*"},
            new String[]{".*\\b(it is|it was|it has been)\\b.*"}
        ));
        
        // hear/here
        HOMONYM_RULES.put("hear", new HomonymRule(
            "hear",
            new String[]{"here"},
            new String[]{".*\\b(listen|sound|noise|music|voice|audio|ear)\\b.*"},
            new String[]{".*\\b(place|location|present|come|go|stay|live)\\b.*"}
        ));
        
        // by/buy/bye
        HOMONYM_RULES.put("by", new HomonymRule(
            "by",
            new String[]{"buy", "bye"},
            new String[]{".*\\b(written|created|made|done|sent|located|near|beside)\\b.*"},
            new String[]{".*\\b(purchase|shop|store|price|cost|sell)\\b.*", ".*\\b(goodbye|farewell|leaving|see you)\\b.*"}
        ));
        
        // no/know
        HOMONYM_RULES.put("know", new HomonymRule(
            "know",
            new String[]{"no"},
            new String[]{".*\\b(I|you|we|they|he|she|understand|aware|familiar|recognize|information)\\b.*"},
            new String[]{".*\\b(not|never|none|nothing|nobody|denial|refuse|negative)\\b.*"}
        ));
        
        // new/knew
        HOMONYM_RULES.put("new", new HomonymRule(
            "new",
            new String[]{"knew"},
            new String[]{".*\\b(brand|fresh|recent|latest|modern|novel|car|phone|house|product)\\b.*"},
            new String[]{".*\\b(I|you|we|they|he|she|already|before|previously|past)\\b.*"}
        ));
        
        // sea/see
        HOMONYM_RULES.put("see", new HomonymRule(
            "see",
            new String[]{"sea"},
            new String[]{".*\\b(look|watch|view|notice|observe|eye|vision|understand|comprehend|I|you|we)\\b.*"},
            new String[]{".*\\b(ocean|water|beach|coast|marine|fish|wave|ship|boat)\\b.*"}
        ));
        
        // one/won
        HOMONYM_RULES.put("one", new HomonymRule(
            "one",
            new String[]{"won"},
            new String[]{".*\\b(number|single|only|first|another|more than|less than|at least)\\b.*"},
            new String[]{".*\\b(victory|game|race|competition|prize|award|battle|contest|defeated)\\b.*"}
        ));
    }
    
    /**
     * Process transcription to correct homonym errors based on context
     * @param transcription The raw transcription text
     * @return Corrected transcription
     */
    public static String processTranscription(String transcription) {
        if (transcription == null || transcription.trim().isEmpty()) {
            return transcription;
        }
        
        String processed = transcription;
        
        // Split into sentences for better context analysis
        String[] sentences = processed.split("(?<=[.!?])\\s+");
        StringBuilder result = new StringBuilder();
        
        for (String sentence : sentences) {
            String correctedSentence = correctHomonyms(sentence);
            result.append(correctedSentence);
            if (!sentence.endsWith(" ")) {
                result.append(" ");
            }
        }
        
        String finalResult = result.toString().trim();
        
        // Log if corrections were made
        if (!finalResult.equals(transcription)) {
            Log.d(TAG, "Applied homonym corrections");
        }
        
        return finalResult;
    }
    
    /**
     * Correct homonyms in a sentence based on context
     */
    private static String correctHomonyms(String sentence) {
        String lowerSentence = sentence.toLowerCase();
        String corrected = sentence;
        
        // Check each homonym rule
        for (Map.Entry<String, HomonymRule> entry : HOMONYM_RULES.entrySet()) {
            HomonymRule rule = entry.getValue();
            
            // Check if any alternative form exists in the sentence
            for (String alternative : rule.alternatives) {
                Pattern pattern = Pattern.compile("\\b" + Pattern.quote(alternative) + "\\b", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(corrected);
                
                if (matcher.find()) {
                    // Check if context matches the correct word's pattern
                    boolean shouldCorrect = false;
                    
                    for (String correctPattern : rule.correctContextPatterns) {
                        if (Pattern.matches(correctPattern, lowerSentence)) {
                            shouldCorrect = true;
                            break;
                        }
                    }
                    
                    // Also check if context matches the alternative's pattern (don't correct)
                    if (shouldCorrect && rule.incorrectContextPatterns != null) {
                        for (String incorrectPattern : rule.incorrectContextPatterns) {
                            if (Pattern.matches(incorrectPattern, lowerSentence)) {
                                shouldCorrect = false;
                                break;
                            }
                        }
                    }
                    
                    // Apply correction if appropriate
                    if (shouldCorrect) {
                        // Preserve the case of the original word
                        String replacement = rule.correctWord;
                        String found = matcher.group();
                        
                        if (Character.isUpperCase(found.charAt(0))) {
                            replacement = Character.toUpperCase(replacement.charAt(0)) + replacement.substring(1);
                        }
                        
                        corrected = matcher.replaceFirst(replacement);
                        Log.d(TAG, String.format("Corrected '%s' to '%s' in context", alternative, replacement));
                    }
                }
            }
        }
        
        return corrected;
    }
    
    /**
     * Rule definition for homonym correction
     */
    private static class HomonymRule {
        String correctWord;
        String[] alternatives;
        String[] correctContextPatterns;
        String[] incorrectContextPatterns;
        
        HomonymRule(String correctWord, String[] alternatives, String[] correctContextPatterns, String[] incorrectContextPatterns) {
            this.correctWord = correctWord;
            this.alternatives = alternatives;
            this.correctContextPatterns = correctContextPatterns;
            this.incorrectContextPatterns = incorrectContextPatterns;
        }
    }
    
    /**
     * Additional post-processing for common transcription issues
     */
    public static String improveReadability(String transcription) {
        if (transcription == null || transcription.trim().isEmpty()) {
            return transcription;
        }
        
        String improved = transcription;
        
        // Capitalize first letter after sentence endings
        improved = improved.replaceAll("([.!?]\\s+)([a-z])", "$1" + "$2".toUpperCase());
        
        // Capitalize 'I' when it's a standalone word
        improved = improved.replaceAll("\\bi\\b", "I");
        
        // Fix spacing around punctuation
        improved = improved.replaceAll("\\s+([,.:;!?])", "$1");
        improved = improved.replaceAll("([,.:;!?])([a-zA-Z])", "$1 $2");
        
        // Remove duplicate spaces
        improved = improved.replaceAll("\\s+", " ");
        
        // Ensure first character is capitalized
        if (improved.length() > 0) {
            improved = Character.toUpperCase(improved.charAt(0)) + improved.substring(1);
        }
        
        return improved.trim();
    }
}
