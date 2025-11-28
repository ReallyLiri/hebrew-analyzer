package com.hotstar.hebrew.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        if (!tokensExtracted) {
            extractTokensFromJson();
            tokensExtracted = true;
        }

        if (currentTokenIndex < extractedTokens.size()) {
            TokenData tokenData = extractedTokens.get(currentTokenIndex);

            this.charTermAttribute.setEmpty();
            this.charTermAttribute.append(tokenData.token);

            this.offsetAttribute.setOffset(tokenData.startOffset, tokenData.endOffset);
            this.positionIncrementAttribute.setPositionIncrement(
                currentTokenIndex == 0 ? 1 :
                (tokenData.position - extractedTokens.get(currentTokenIndex - 1).position)
            );

            currentTokenIndex++;
            return true;
        }

        return false;
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
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonPart);
            JsonNode tokensNode = rootNode.get("tokens");

            if (tokensNode != null && tokensNode.isArray()) {
                int previousPosition = -1;
                for (JsonNode tokenNode : tokensNode) {
                    String token = tokenNode.get("token").asText();
                    int startOffset = tokenNode.get("start_offset").asInt();
                    int endOffset = tokenNode.get("end_offset").asInt();
                    int position = tokenNode.get("position").asInt();

                    if (token != null && !token.trim().isEmpty() && !token.equals("[BLANK]")) {
                        extractedTokens.add(new TokenData(token, startOffset, endOffset, position));
                    }
                }
            }
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
        super.reset();
        extractedTokens.clear();
        currentTokenIndex = 0;
        tokensExtracted = false;
    }
}