package com.hotstar.hebrew.plugin;

import com.hotstar.hebrew.analysis.PreprocessedJsonAnalyzer;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractIndexAnalyzerProvider;

public class PreprocessedJsonAnalyzerProvider extends AbstractIndexAnalyzerProvider<PreprocessedJsonAnalyzer> {

    public PreprocessedJsonAnalyzerProvider(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
        super(indexSettings, name, settings);
        try {
            analyzer = new PreprocessedJsonAnalyzer();
            System.err.println("PreprocessedJsonAnalyzerProvider: Successfully created PreprocessedJsonAnalyzer");
        } catch (Throwable e) {
            System.err.println("PreprocessedJsonAnalyzerProvider: CRITICAL ERROR creating analyzer: " + e.getMessage());
            e.printStackTrace();
            // This should never fail now, but just in case
            analyzer = new PreprocessedJsonAnalyzer();
        }
    }

    public PreprocessedJsonAnalyzer get() {
        return this.analyzer;
    }

    protected PreprocessedJsonAnalyzer analyzer;

    public static final String NAME = "preprocessed_json";
}