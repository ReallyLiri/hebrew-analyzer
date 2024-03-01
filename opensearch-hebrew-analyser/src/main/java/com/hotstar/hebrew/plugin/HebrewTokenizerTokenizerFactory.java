package com.hotstar.hebrew.plugin;

import org.apache.lucene.analysis.Tokenizer;
import com.hotstar.hebrew.analysis.SefariaTokenizer;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractTokenizerFactory;


public class HebrewTokenizerTokenizerFactory extends AbstractTokenizerFactory {
    public HebrewTokenizerTokenizerFactory(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
        super(indexSettings, settings, name);
    }

    @Override
    public Tokenizer create() {
        return new SefariaTokenizer();
    }
}
