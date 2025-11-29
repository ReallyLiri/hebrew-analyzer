package com.hotstar.hebrew.plugin;

import com.hotstar.hebrew.analysis.PreprocessedJsonAnalyzer;
import com.hotstar.hebrew.analysis.SafePreprocessedJsonAnalyzer;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractIndexAnalyzerProvider;

public class PreprocessedJsonAnalyzerProvider extends AbstractIndexAnalyzerProvider<PreprocessedJsonAnalyzer> {

    public PreprocessedJsonAnalyzerProvider(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
        super(indexSettings, name, settings);
        try {
            // Use safe wrapper to prevent OpenSearch crashes
            analyzer = new SafePreprocessedJsonAnalyzer();
            System.err.println("PreprocessedJsonAnalyzerProvider: Successfully created SafePreprocessedJsonAnalyzer");
        } catch (Throwable e) {
            System.err.println("PreprocessedJsonAnalyzerProvider: CRITICAL ERROR creating analyzer: " + e.getMessage());
            e.printStackTrace();
            // Fallback to basic analyzer - don't let plugin registration fail
            analyzer = new PreprocessedJsonAnalyzer();
        }
    }

    public PreprocessedJsonAnalyzer get() {
        return this.analyzer;
    }

    protected PreprocessedJsonAnalyzer analyzer;

    public static final String NAME = "preprocessed_json";
}