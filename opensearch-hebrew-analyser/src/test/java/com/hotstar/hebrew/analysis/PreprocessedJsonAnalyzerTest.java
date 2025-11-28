package com.hotstar.hebrew.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class PreprocessedJsonAnalyzerTest {

    @Test
    public void testSimpleTextWithoutLex() throws IOException {
        PreprocessedJsonAnalyzer analyzer = new PreprocessedJsonAnalyzer();
        TokenStream tokenStream = analyzer.tokenStream("test", new StringReader("simple text"));

        List<String> tokens = extractTokens(tokenStream);
        assertEquals("Should return no tokens when no LEX marker", 0, tokens.size());

        tokenStream.close();
        analyzer.close();
    }

    @Test
    public void testEmptyTokensArray() throws IOException {
        PreprocessedJsonAnalyzer analyzer = new PreprocessedJsonAnalyzer();
        String input = "hello ### LEX {\"tokens\": []}";
        TokenStream tokenStream = analyzer.tokenStream("test", new StringReader(input));

        List<String> tokens = extractTokens(tokenStream);
        assertEquals("Should return no tokens when tokens array is empty", 0, tokens.size());

        tokenStream.close();
        analyzer.close();
    }

    @Test
    public void testSingleToken() throws IOException {
        PreprocessedJsonAnalyzer analyzer = new PreprocessedJsonAnalyzer();
        String input = "hello ### LEX {\"tokens\": [{\"token\": \"test\", \"start_offset\": 0, \"end_offset\": 4, \"position\": 0, \"type\": \"word\"}]}";
        TokenStream tokenStream = analyzer.tokenStream("test", new StringReader(input));

        List<String> tokens = extractTokens(tokenStream);
        assertEquals("Should return one token", 1, tokens.size());
        assertEquals("Token should be 'test'", "test", tokens.get(0));

        tokenStream.close();
        analyzer.close();
    }

    @Test
    public void testMultipleTokens() throws IOException {
        PreprocessedJsonAnalyzer analyzer = new PreprocessedJsonAnalyzer();
        String input = "hello ### LEX {\"tokens\": [" +
            "{\"token\": \"hello\", \"start_offset\": 0, \"end_offset\": 5, \"position\": 0, \"type\": \"word\"}," +
            "{\"token\": \"world\", \"start_offset\": 6, \"end_offset\": 11, \"position\": 1, \"type\": \"word\"}" +
            "]}";
        TokenStream tokenStream = analyzer.tokenStream("test", new StringReader(input));

        List<String> tokens = extractTokens(tokenStream);
        assertEquals("Should return two tokens", 2, tokens.size());
        assertEquals("First token should be 'hello'", "hello", tokens.get(0));
        assertEquals("Second token should be 'world'", "world", tokens.get(1));

        tokenStream.close();
        analyzer.close();
    }

    @Test
    public void testBlankTokensFiltered() throws IOException {
        PreprocessedJsonAnalyzer analyzer = new PreprocessedJsonAnalyzer();
        String input = "hello ### LEX {\"tokens\": [" +
            "{\"token\": \"hello\", \"start_offset\": 0, \"end_offset\": 5, \"position\": 0, \"type\": \"word\"}," +
            "{\"token\": \"[BLANK]\", \"start_offset\": 6, \"end_offset\": 13, \"position\": 1, \"type\": \"word\"}," +
            "{\"token\": \"world\", \"start_offset\": 14, \"end_offset\": 19, \"position\": 2, \"type\": \"word\"}" +
            "]}";
        TokenStream tokenStream = analyzer.tokenStream("test", new StringReader(input));

        List<String> tokens = extractTokens(tokenStream);
        assertEquals("Should return two tokens (BLANK filtered)", 2, tokens.size());
        assertEquals("First token should be 'hello'", "hello", tokens.get(0));
        assertEquals("Second token should be 'world'", "world", tokens.get(1));

        tokenStream.close();
        analyzer.close();
    }

    @Test
    public void testLargeHebrewTextFromExampleCurl() throws IOException {
        PreprocessedJsonAnalyzer analyzer = new PreprocessedJsonAnalyzer();

        // This is a simplified version of the content from example_curl.sh
        String input = "0\nאז אהה--אני גדלתי בקיבוץ ניר עוז בן קיבוץ? בן\n\n### LEX\n" +
            "{\n" +
            "  \"identifier\": \"Edut710.Testimony.01042\",\n" +
            "  \"tokens\": [\n" +
            "    {\"token\": \"אז\", \"start_offset\": 2, \"end_offset\": 4, \"position\": 0, \"type\": \"word\"},\n" +
            "    {\"token\": \"אהה\", \"start_offset\": 5, \"end_offset\": 8, \"position\": 1, \"type\": \"word\"},\n" +
            "    {\"token\": \"אני\", \"start_offset\": 10, \"end_offset\": 13, \"position\": 4, \"type\": \"word\"},\n" +
            "    {\"token\": \"גדלתי\", \"start_offset\": 14, \"end_offset\": 19, \"position\": 5, \"type\": \"word\"},\n" +
            "    {\"token\": \"בקיבוץ\", \"start_offset\": 20, \"end_offset\": 26, \"position\": 6, \"type\": \"word\"},\n" +
            "    {\"token\": \"ניר\", \"start_offset\": 27, \"end_offset\": 30, \"position\": 7, \"type\": \"word\"},\n" +
            "    {\"token\": \"עוז\", \"start_offset\": 31, \"end_offset\": 34, \"position\": 8, \"type\": \"word\"},\n" +
            "    {\"token\": \"[BLANK]\", \"start_offset\": 66, \"end_offset\": 71, \"position\": 17, \"type\": \"word\"}\n" +
            "  ]\n" +
            "}";

        TokenStream tokenStream = analyzer.tokenStream("test", new StringReader(input));

        List<String> tokens = extractTokens(tokenStream);
        assertEquals("Should return 7 tokens (BLANK filtered)", 7, tokens.size());
        assertEquals("First token should be Hebrew", "אז", tokens.get(0));
        assertEquals("Second token should be Hebrew", "אהה", tokens.get(1));
        assertEquals("Third token should be Hebrew", "אני", tokens.get(2));
        assertEquals("Last token should be Hebrew", "עוז", tokens.get(6));

        // Verify [BLANK] token was filtered out
        assertFalse("Should not contain [BLANK] token", tokens.contains("[BLANK]"));

        tokenStream.close();
        analyzer.close();
    }

    @Test
    public void testInvalidJsonHandling() throws IOException {
        PreprocessedJsonAnalyzer analyzer = new PreprocessedJsonAnalyzer();
        String input = "hello ### LEX {invalid json}";
        TokenStream tokenStream = analyzer.tokenStream("test", new StringReader(input));

        // Should not throw exception, just return no tokens
        List<String> tokens = extractTokens(tokenStream);
        assertEquals("Should return no tokens when JSON is invalid", 0, tokens.size());

        tokenStream.close();
        analyzer.close();
    }

    private List<String> extractTokens(TokenStream tokenStream) throws IOException {
        List<String> tokens = new ArrayList<>();
        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);

        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            tokens.add(charTermAttribute.toString());
        }
        tokenStream.end();

        return tokens;
    }
}