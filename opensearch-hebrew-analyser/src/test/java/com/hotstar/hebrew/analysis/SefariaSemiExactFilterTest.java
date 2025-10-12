package com.hotstar.hebrew.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.core.KeywordTokenizer;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class SefariaSemiExactFilterTest {

    public void testHebrewPrefixTokenization() throws IOException {
        String input = "הספר";
        TokenStream tokenStream = new KeywordTokenizer();
        ((KeywordTokenizer) tokenStream).setReader(new StringReader(input));

        SefariaSemiExactFilter filter = new SefariaSemiExactFilter(tokenStream);

        List<String> tokens = new ArrayList<>();
        CharTermAttribute charTermAttribute = filter.addAttribute(CharTermAttribute.class);

        filter.reset();
        while (filter.incrementToken()) {
            tokens.add(charTermAttribute.toString());
        }
        filter.close();

        assert tokens.contains("הספר$") : "Should contain original token with $";
        assert tokens.contains("ספר") : "Should contain token without prefix ה";
        assert tokens.size() > 2 : "Should contain prefixed variants";
    }

    public void testTokenWithoutPrefix() throws IOException {
        String input = "ספר";
        TokenStream tokenStream = new KeywordTokenizer();
        ((KeywordTokenizer) tokenStream).setReader(new StringReader(input));

        SefariaSemiExactFilter filter = new SefariaSemiExactFilter(tokenStream);

        List<String> tokens = new ArrayList<>();
        CharTermAttribute charTermAttribute = filter.addAttribute(CharTermAttribute.class);

        filter.reset();
        while (filter.incrementToken()) {
            tokens.add(charTermAttribute.toString());
        }
        filter.close();

        assert tokens.contains("ספר$") : "Should contain original token with $";
        assert tokens.contains("הספר") : "Should contain prefixed variants like הספר";
        assert tokens.contains("בספר") : "Should contain prefixed variants like בספר";
    }
}