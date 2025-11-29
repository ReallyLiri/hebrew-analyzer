package com.hotstar.hebrew.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PreprocessedJsonFilter extends TokenFilter {

    private CharTermAttribute charTermAttribute = addAttribute(CharTermAttribute.class);
    private PositionIncrementAttribute positionIncrementAttribute =
            addAttribute(PositionIncrementAttribute.class);
    private OffsetAttribute offsetAttribute = addAttribute(OffsetAttribute.class);

    private List<TokenData> extractedTokens = new ArrayList<>();
    private int currentTokenIndex = 0;
    private boolean tokensExtracted = false;

    private static class TokenData {
        String token;
        int startOffset;
        int endOffset;
        int position;

        TokenData(String token, int startOffset, int endOffset, int position) {
            this.token = token;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.position = position;
        }
    }

    public PreprocessedJsonFilter(TokenStream tokenStream) {
        super(tokenStream);
    }

    @Override
    public boolean incrementToken() throws IOException {
        try {
            if (!tokensExtracted) {
                try {
                    extractTokensFromJson();
                    tokensExtracted = true;
                } catch (Exception e) {
                    System.err.println("PreprocessedJsonFilter: Error extracting tokens: " + e.getMessage());
                    e.printStackTrace();
                    tokensExtracted = true; // Prevent retry
                    return false;
                }
            }

            if (currentTokenIndex < extractedTokens.size()) {
                try {
                    TokenData tokenData = extractedTokens.get(currentTokenIndex);

                    if (tokenData == null || tokenData.token == null) {
                        System.err.println("PreprocessedJsonFilter: Null token data at index " + currentTokenIndex);
                        currentTokenIndex++;
                        return incrementToken(); // Try next token
                    }

                    this.charTermAttribute.setEmpty();
                    this.charTermAttribute.append(tokenData.token);

                    this.offsetAttribute.setOffset(tokenData.startOffset, tokenData.endOffset);

                    int positionIncrement = currentTokenIndex == 0 ? 1 :
                        (tokenData.position - extractedTokens.get(currentTokenIndex - 1).position);

                    this.positionIncrementAttribute.setPositionIncrement(positionIncrement);

                    currentTokenIndex++;
                    return true;
                } catch (Exception e) {
                    System.err.println("PreprocessedJsonFilter: Error setting token attributes: " + e.getMessage());
                    e.printStackTrace();
                    currentTokenIndex++;
                    // Try to continue with next token
                    if (currentTokenIndex < extractedTokens.size()) {
                        return incrementToken();
                    }
                    return false;
                }
            }

            return false;
        } catch (Exception e) {
            System.err.println("PreprocessedJsonFilter: Unexpected error in incrementToken: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void extractTokensFromJson() throws IOException {
        String text = "";

        // Since we're using PassThroughTokenizer, we should get exactly one token with the full text
        if (this.input.incrementToken()) {
            text = this.input.getAttribute(CharTermAttribute.class).toString();
        }

        String lexMarker = "### LEX";
        int lexIndex = text.indexOf(lexMarker);

        if (lexIndex == -1) {
            System.err.println("PreprocessedJsonFilter: No '### LEX' marker found in text (length: " + text.length() + ")");
            if (text.length() > 0) {
                System.err.println("Text preview (first 100 chars): " +
                    text.substring(0, Math.min(100, text.length())));
            }
            return;
        }

        String jsonPart = text.substring(lexIndex + lexMarker.length()).trim();
        System.err.println("PreprocessedJsonFilter: Found LEX marker, JSON part length: " + jsonPart.length());

        try {
            // Use basic regex parsing to avoid any external dependencies
            parseTokensWithRegex(jsonPart);
        } catch (Exception e) {
            System.err.println("PreprocessedJsonFilter: Failed to parse JSON after ### LEX marker");
            System.err.println("Error: " + e.getMessage());
            if (jsonPart != null) {
                System.err.println("JSON part (first 200 chars): " +
                    jsonPart.substring(0, Math.min(200, jsonPart.length())));
            }
            e.printStackTrace();
        }
    }

    /**
     * Parse tokens using basic regex to avoid external JSON dependencies
     */
    private void parseTokensWithRegex(String jsonPart) {
        try {
            // Simple regex to find token objects in the JSON
            // Pattern matches: "token": "value", "start_offset": number, "end_offset": number, "position": number
            Pattern tokenPattern = Pattern.compile(
                "\\{[^}]*\"token\":\\s*\"([^\"]*?)\"[^}]*\"start_offset\":\\s*(\\d+)[^}]*\"end_offset\":\\s*(\\d+)[^}]*\"position\":\\s*(\\d+)[^}]*\\}",
                Pattern.DOTALL
            );

            Matcher matcher = tokenPattern.matcher(jsonPart);

            while (matcher.find()) {
                try {
                    String token = matcher.group(1);
                    int startOffset = Integer.parseInt(matcher.group(2));
                    int endOffset = Integer.parseInt(matcher.group(3));
                    int position = Integer.parseInt(matcher.group(4));

                    // Filter out blank tokens
                    if (token != null && !token.trim().isEmpty() && !token.equals("[BLANK]")) {
                        extractedTokens.add(new TokenData(token, startOffset, endOffset, position));
                    }
                } catch (NumberFormatException e) {
                    System.err.println("PreprocessedJsonFilter: Invalid number in token: " + e.getMessage());
                }
            }

            System.err.println("PreprocessedJsonFilter: Extracted " + extractedTokens.size() + " tokens using regex parsing");

        } catch (Exception e) {
            System.err.println("PreprocessedJsonFilter: Error in regex parsing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void reset() throws IOException {
        try {
            super.reset();
        } catch (Exception e) {
            System.err.println("PreprocessedJsonFilter: Error in super.reset(): " + e.getMessage());
            e.printStackTrace();
        }

        try {
            extractedTokens.clear();
            currentTokenIndex = 0;
            tokensExtracted = false;
        } catch (Exception e) {
            System.err.println("PreprocessedJsonFilter: Error resetting state: " + e.getMessage());
            e.printStackTrace();
            // Ensure we reset state even if clear fails
            extractedTokens = new ArrayList<>();
            currentTokenIndex = 0;
            tokensExtracted = false;
        }
    }
}