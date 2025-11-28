package com.hotstar.hebrew.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;

public class PreprocessedJsonAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String field) {
        Tokenizer tokenizer = new PassThroughTokenizer();
        TokenStream filter = new PreprocessedJsonFilter(tokenizer);
        return new TokenStreamComponents(tokenizer, filter);
    }
}