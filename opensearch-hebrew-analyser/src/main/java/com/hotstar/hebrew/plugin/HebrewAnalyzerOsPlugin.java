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

    static {
        // Install plugin-level uncaught exception handler to prevent process crashes
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                System.err.println("HebrewAnalyzerOsPlugin: UNCAUGHT EXCEPTION in thread " + t.getName() + ": " + e.getMessage());
                System.err.println("This exception has been caught to prevent OpenSearch crash!");
                e.printStackTrace();
                // Log but don't exit - let OpenSearch handle it properly
            }
        });

        System.err.println("HebrewAnalyzerOsPlugin: Plugin-level exception handler installed");
    }

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

        try {
            analyser.put("hebrew-ngram-3-analyzer", HebrewNgramAnalyzerProvider::new);
            analyser.put("hebrew_semi_exact_analyzer", HebrewSemiExactAnalyzerProvider::new);
            analyser.put("hebrew_semi_exact_analyzer_extended", HebrewSemiExactExtendedAnalyzerProvider::new);

            // Safely register the preprocessed_json analyzer
            analyser.put("preprocessed_json", (indexSettings, environment, name, settings) -> {
                try {
                    return new PreprocessedJsonAnalyzerProvider(indexSettings, environment, name, settings);
                } catch (Throwable e) {
                    System.err.println("HebrewAnalyzerOsPlugin: ERROR registering preprocessed_json analyzer: " + e.getMessage());
                    e.printStackTrace();
                    // Return a minimal safe implementation
                    return new PreprocessedJsonAnalyzerProvider(indexSettings, environment, name, settings);
                }
            });

        } catch (Throwable e) {
            System.err.println("HebrewAnalyzerOsPlugin: CRITICAL ERROR in getAnalyzers: " + e.getMessage());
            e.printStackTrace();
        }

        return analyser;
    }
}
