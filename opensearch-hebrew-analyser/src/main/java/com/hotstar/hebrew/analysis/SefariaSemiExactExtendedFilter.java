package com.hotstar.hebrew.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SefariaSemiExactExtendedFilter extends TokenFilter {

    public static final char FINAL_CHAR = '$';

    private static final Set<String> HEBREW_PREFIXES = new HashSet<>(Arrays.asList(
        "ה", "ב", "כ", "ל", "מ", "ש", "כש", "כשה",
        "וה", "וב", "וכ", "ול", "ומ", "וש", "וכש", "וכשה", "ו"
    ));

    /**
     * This filter outputs WORD$ and generates Hebrew prefix variants for each token.
     * For Hebrew words with existing prefixes (ה,ב,כ,ל,מ,ש,כש,כשה,ו + combinations),
     * it outputs both the original word and the word without the prefix.
     * For words without prefixes, it generates versions with all possible Hebrew prefixes.
     * This allows comprehensive matching with Hebrew morphological variations.
     * @param tokenStream
     */

    public SefariaSemiExactExtendedFilter(TokenStream tokenStream) {
        super(tokenStream);
    }
    private CharTermAttribute charTermAttribute = addAttribute(CharTermAttribute.class);
    private PositionIncrementAttribute positionIncrementAttribute =
            addAttribute(PositionIncrementAttribute.class);

    private List<String> previousTokens = new ArrayList<String>();


    @Override
    public boolean incrementToken() throws IOException {

        // Loop over tokens in the token stream to find the next one that is not empty
        if (!previousTokens.isEmpty()) {
            this.charTermAttribute.setEmpty();
            this.charTermAttribute.append(previousTokens.remove(0));
            this.positionIncrementAttribute.setPositionIncrement(0);
            return true;
        }

        previousTokens.clear();
        String nextToken = null;

        while (nextToken == null) {

            // Reached the end of the token stream being processed
            if ( ! this.input.incrementToken()) {
                return false;
            }
            // Get text of the current token and remove any leading/trailing whitespace.
            String currentTokenInStream =
                    this.input.getAttribute(CharTermAttribute.class).toString().trim();

            // Save the token if it is not an empty string
            if (currentTokenInStream.length() > 0) {
                nextToken = currentTokenInStream;
            }
        }

        previousTokens.add(nextToken);

        generatePrefixVariants(nextToken, previousTokens);

        this.charTermAttribute.setEmpty();
        this.charTermAttribute.append(nextToken).append(FINAL_CHAR);
        this.positionIncrementAttribute.setPositionIncrement(1);

        return true;
    }

    private void generatePrefixVariants(String token, List<String> tokenList) {
        for (String prefix : HEBREW_PREFIXES) {
            if (token.startsWith(prefix) && token.length() > prefix.length()) {
                String withoutPrefix = token.substring(prefix.length());
                tokenList.add(prefix + withoutPrefix);
                tokenList.add(withoutPrefix);
            } else {
                tokenList.add(prefix + token);
            }
        }
    }
}