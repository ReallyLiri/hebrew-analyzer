package com.hotstar.hebrew.analysis;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import java.io.IOException;

/**
 * A safe wrapper around PreprocessedJsonFilter that prevents any exception
 * from escaping and crashing OpenSearch.
 */
public class SafePreprocessedJsonFilter extends TokenFilter {

    private CharTermAttribute charTermAttribute = addAttribute(CharTermAttribute.class);
    private PositionIncrementAttribute positionIncrementAttribute =
            addAttribute(PositionIncrementAttribute.class);
    private OffsetAttribute offsetAttribute = addAttribute(OffsetAttribute.class);

    private PreprocessedJsonFilter actualFilter;
    private boolean filterFailed = false;
    private boolean inputProcessed = false;
    private String fallbackText = "";

    public SafePreprocessedJsonFilter(TokenStream tokenStream) {
        super(tokenStream);
        try {
            this.actualFilter = new PreprocessedJsonFilter(tokenStream);
        } catch (Throwable e) {
            System.err.println("SafePreprocessedJsonFilter: Failed to create actual filter: " + e.getMessage());
            e.printStackTrace();
            this.filterFailed = true;
        }
    }

    @Override
    public boolean incrementToken() throws IOException {
        try {
            // If the actual filter failed during construction, use fallback
            if (filterFailed) {
                return handleFallbackTokenization();
            }

            // Try to use the actual filter
            try {
                return actualFilter.incrementToken();
            } catch (NoClassDefFoundError e) {
                System.err.println("SafePreprocessedJsonFilter: NoClassDefFoundError in incrementToken: " + e.getMessage());
                filterFailed = true;
                return handleFallbackTokenization();
            } catch (LinkageError e) {
                System.err.println("SafePreprocessedJsonFilter: LinkageError in incrementToken: " + e.getMessage());
                filterFailed = true;
                return handleFallbackTokenization();
            } catch (OutOfMemoryError e) {
                System.err.println("SafePreprocessedJsonFilter: OutOfMemoryError in incrementToken: " + e.getMessage());
                filterFailed = true;
                return handleFallbackTokenization();
            } catch (Throwable e) {
                System.err.println("SafePreprocessedJsonFilter: Unexpected error in incrementToken: " + e.getMessage());
                e.printStackTrace();
                filterFailed = true;
                return handleFallbackTokenization();
            }

        } catch (Throwable e) {
            System.err.println("SafePreprocessedJsonFilter: CRITICAL - Even safe wrapper failed: " + e.getMessage());
            e.printStackTrace();
            return false; // Stop tokenization completely
        }
    }

    /**
     * Fallback tokenization that just passes through the original input
     */
    private boolean handleFallbackTokenization() throws IOException {
        try {
            if (inputProcessed) {
                return false; // Already processed the input
            }

            // Try to get the original input from the underlying tokenizer
            if (input.incrementToken()) {
                String inputText = input.getAttribute(CharTermAttribute.class).toString();

                clearAttributes();
                charTermAttribute.setEmpty().append(inputText);
                offsetAttribute.setOffset(0, inputText.length());
                positionIncrementAttribute.setPositionIncrement(1);

                inputProcessed = true;
                return true;
            }

            return false;

        } catch (Throwable e) {
            System.err.println("SafePreprocessedJsonFilter: Even fallback failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void reset() throws IOException {
        try {
            if (actualFilter != null && !filterFailed) {
                actualFilter.reset();
            }
        } catch (Throwable e) {
            System.err.println("SafePreprocessedJsonFilter: Error in reset: " + e.getMessage());
            e.printStackTrace();
            filterFailed = true;
        }

        try {
            super.reset();
            inputProcessed = false;
        } catch (Throwable e) {
            System.err.println("SafePreprocessedJsonFilter: Error in super.reset: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if (actualFilter != null) {
                actualFilter.close();
            }
        } catch (Throwable e) {
            System.err.println("SafePreprocessedJsonFilter: Error closing actual filter: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            super.close();
        } catch (Throwable e) {
            System.err.println("SafePreprocessedJsonFilter: Error in super.close: " + e.getMessage());
            e.printStackTrace();
        }
    }
}