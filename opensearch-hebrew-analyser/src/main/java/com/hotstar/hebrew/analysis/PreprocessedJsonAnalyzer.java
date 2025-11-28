package com.hotstar.hebrew.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class PreprocessedJsonAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String field) {
        Tokenizer tokenizer = new StandardTokenizer();
        TokenStream filter = new PreprocessedJsonFilter(tokenizer);
        return new TokenStreamComponents(tokenizer, filter);
    }
}