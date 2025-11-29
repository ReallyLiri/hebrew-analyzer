package com.hotstar.hebrew.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.common.xcontent.XContentFactory;

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
            XContentParser parser = XContentFactory.xContent(XContentType.JSON).createParser(
                org.opensearch.common.xcontent.NamedXContentRegistry.EMPTY,
                org.opensearch.common.xcontent.DeprecationHandler.THROW_UNSUPPORTED_OPERATION,
                jsonPart
            );

            // Parse the JSON structure
            while (parser.nextToken() != null) {
                if (parser.currentName() != null && parser.currentName().equals("tokens")) {
                    parser.nextToken(); // Move to array start
                    if (parser.currentToken() == XContentParser.Token.START_ARRAY) {
                        while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
                            if (parser.currentToken() == XContentParser.Token.START_OBJECT) {
                                String token = null;
                                Integer startOffset = null;
                                Integer endOffset = null;
                                Integer position = null;

                                while (parser.nextToken() != XContentParser.Token.END_OBJECT) {
                                    String fieldName = parser.currentName();
                                    parser.nextToken();

                                    if ("token".equals(fieldName)) {
                                        token = parser.text();
                                    } else if ("start_offset".equals(fieldName)) {
                                        startOffset = parser.intValue();
                                    } else if ("end_offset".equals(fieldName)) {
                                        endOffset = parser.intValue();
                                    } else if ("position".equals(fieldName)) {
                                        position = parser.intValue();
                                    }
                                }

                                if (token != null && !token.trim().isEmpty() && !token.equals("[BLANK]")
                                    && startOffset != null && endOffset != null && position != null) {
                                    extractedTokens.add(new TokenData(token, startOffset, endOffset, position));
                                }
                            }
                        }
                    }
                    break; // Found tokens array, we're done
                }
            }
            parser.close();
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