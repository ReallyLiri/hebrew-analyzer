package com.hotstar.hebrew.plugin;
import org.apache.lucene.analysis.Analyzer;
import org.opensearch.index.analysis.AnalyzerProvider;
import org.opensearch.index.analysis.TokenFilterFactory;
import org.opensearch.index.analysis.TokenizerFactory;
import org.opensearch.indices.analysis.AnalysisModule;
import org.opensearch.plugins.AnalysisPlugin;
import org.opensearch.plugins.Plugin;

import java.util.HashMap;
import java.util.Map;

public class HebrewAnalyzerOsPlugin extends Plugin implements AnalysisPlugin {

    @Override
    public Map<String, AnalysisModule.AnalysisProvider<TokenFilterFactory>> getTokenFilters() {
        Map<String, AnalysisModule.AnalysisProvider<TokenFilterFactory>> tokenFilters = new HashMap<>();
        tokenFilters.put("hebrew_stop", HebrewNoOpTokenFilterFactory::new);
        tokenFilters.put("hebrew_word", HebrewNoOpTokenFilterFactory::new);
        return tokenFilters;
    }

    @Override
    public Map<String, AnalysisModule.AnalysisProvider<TokenizerFactory>> getTokenizers() {
        Map<String, AnalysisModule.AnalysisProvider<TokenizerFactory>> extra = new HashMap<>();
        extra.put("hebrew_tokenizer", HebrewTokenizerTokenizerFactory::new);
        extra.put("hebrew_sentence", HebrewTokenizerTokenizerFactory::new);
        return extra;
    }

    @Override
    public Map<String, AnalysisModule.AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> getAnalyzers() {
        Map<String, AnalysisModule.AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> analyser = new HashMap<>();
        analyser.put("hebrew-ngram-3-analyzer", HebrewNgramAnalyzerProvider::new);
        analyser.put("hebrew_semi_exact_analyzer", HebrewSemiExactAnalyzerProvider::new);
        return analyser;
    }
}
