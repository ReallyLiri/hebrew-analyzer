package com.hotstar.hebrew.plugin;


import com.hotstar.hebrew.analysis.HebrewSemiExactExtendedAnalyzer;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractIndexAnalyzerProvider;


public class HebrewSemiExactExtendedAnalyzerProvider extends AbstractIndexAnalyzerProvider<HebrewSemiExactExtendedAnalyzer> {

    /* Constructor. Nothing special here. */
    public HebrewSemiExactExtendedAnalyzerProvider(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
        super(indexSettings, name, settings);
        analyzer = new HebrewSemiExactExtendedAnalyzer();
    }

    /* This function needs to be overridden to return an instance of PlusSignAnalyzer. */
    public HebrewSemiExactExtendedAnalyzer get() {
        return this.analyzer;
    }

    /* Instance of PlusSignAnalyzer class that is returned by this class. */
    protected HebrewSemiExactExtendedAnalyzer analyzer;

    /* Name to associate with this class. We will use this in PlusSignBinderProcessor. */
    public static final String NAME = "hebrew_semi_exact_analyzer_extended";
}