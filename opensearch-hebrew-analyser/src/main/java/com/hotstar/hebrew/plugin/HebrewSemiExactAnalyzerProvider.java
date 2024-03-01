package com.hotstar.hebrew.plugin;


import com.hotstar.hebrew.analysis.HebrewSemiExactAnalyzer;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractIndexAnalyzerProvider;


public class HebrewSemiExactAnalyzerProvider extends AbstractIndexAnalyzerProvider<HebrewSemiExactAnalyzer> {

    /* Constructor. Nothing special here. */
    public HebrewSemiExactAnalyzerProvider(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
        super(indexSettings, name, settings);
        analyzer = new HebrewSemiExactAnalyzer();
    }

    /* This function needs to be overridden to return an instance of PlusSignAnalyzer. */
    public HebrewSemiExactAnalyzer get() {
        return this.analyzer;
    }

    /* Instance of PlusSignAnalyzer class that is returned by this class. */
    protected HebrewSemiExactAnalyzer analyzer;

    /* Name to associate with this class. We will use this in PlusSignBinderProcessor. */
    public static final String NAME = "hebrew_semi_exact_analyzer";
}
