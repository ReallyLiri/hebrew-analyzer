package com.hotstar.hebrew.plugin;


import com.hotstar.hebrew.analysis.HebrewNgramAnalyzer;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractIndexAnalyzerProvider;


public class HebrewNgramAnalyzerProvider extends AbstractIndexAnalyzerProvider<HebrewNgramAnalyzer> {

    /* Constructor. Nothing special here. */
    public HebrewNgramAnalyzerProvider(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
        super(indexSettings, name, settings);
        analyzer = new HebrewNgramAnalyzer();
    }

    /* This function needs to be overridden to return an instance of PlusSignAnalyzer. */
    public HebrewNgramAnalyzer get() {
        return this.analyzer;
    }

    /* Instance of PlusSignAnalyzer class that is returned by this class. */
    protected HebrewNgramAnalyzer analyzer;

    /* Name to associate with this class. We will use this in PlusSignBinderProcessor. */
    public static final String NAME = "hebrew-ngram-3-analyzer";
}
