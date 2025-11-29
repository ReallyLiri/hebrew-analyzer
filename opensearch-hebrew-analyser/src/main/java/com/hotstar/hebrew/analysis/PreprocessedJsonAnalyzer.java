package com.hotstar.hebrew.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;

public class PreprocessedJsonAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String field) {
        try {
            // Use KeywordTokenizer which treats entire input as a single token
            Tokenizer tokenizer = new KeywordTokenizer();
            TokenStream filter = new PreprocessedJsonFilter(tokenizer);
            return new TokenStreamComponents(tokenizer, filter);
        } catch (Throwable e) {
            System.err.println("PreprocessedJsonAnalyzer: CRITICAL ERROR creating components: " + e.getMessage());
            e.printStackTrace();
            // Return safe fallback - just pass through the input
            Tokenizer fallbackTokenizer = new KeywordTokenizer();
            return new TokenStreamComponents(fallbackTokenizer);
        }
    }
}