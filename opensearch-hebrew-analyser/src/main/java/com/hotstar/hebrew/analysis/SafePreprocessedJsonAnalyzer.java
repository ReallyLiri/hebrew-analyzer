package com.hotstar.hebrew.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;

/**
 * A safe wrapper around PreprocessedJsonAnalyzer that prevents any exception
 * from crashing the OpenSearch process.
 */
public class SafePreprocessedJsonAnalyzer extends Analyzer {

    private static volatile boolean isDisabled = false;
    private static int consecutiveFailures = 0;
    private static final int MAX_FAILURES = 5;
    private static long lastFailureTime = 0;
    private static final long RECOVERY_TIMEOUT = 60000; // 1 minute

    @Override
    protected TokenStreamComponents createComponents(String field) {
        try {
            // Circuit breaker: if too many failures, temporarily disable
            if (isDisabled) {
                long now = System.currentTimeMillis();
                if (now - lastFailureTime > RECOVERY_TIMEOUT) {
                    isDisabled = false;
                    consecutiveFailures = 0;
                    System.err.println("SafePreprocessedJsonAnalyzer: Recovery timeout reached, re-enabling analyzer");
                } else {
                    System.err.println("SafePreprocessedJsonAnalyzer: Analyzer disabled due to failures, using passthrough");
                    return createPassthroughComponents();
                }
            }

            // Try to create the normal components
            Tokenizer tokenizer = new KeywordTokenizer();
            TokenStream filter = new SafePreprocessedJsonFilter(tokenizer);
            return new TokenStreamComponents(tokenizer, filter);

        } catch (NoClassDefFoundError e) {
            handleCriticalError("NoClassDefFoundError", e);
            return createPassthroughComponents();
        } catch (LinkageError e) {
            handleCriticalError("LinkageError", e);
            return createPassthroughComponents();
        } catch (ExceptionInInitializerError e) {
            handleCriticalError("ExceptionInInitializerError", e);
            return createPassthroughComponents();
        } catch (OutOfMemoryError e) {
            handleCriticalError("OutOfMemoryError", e);
            return createPassthroughComponents();
        } catch (Throwable e) {
            handleCriticalError("UnexpectedError", e);
            return createPassthroughComponents();
        }
    }

    private void handleCriticalError(String errorType, Throwable e) {
        consecutiveFailures++;
        lastFailureTime = System.currentTimeMillis();

        System.err.println("SafePreprocessedJsonAnalyzer: CRITICAL ERROR - " + errorType);
        System.err.println("Error: " + e.getMessage());
        System.err.println("Consecutive failures: " + consecutiveFailures + "/" + MAX_FAILURES);

        // Log stack trace but don't let it propagate
        e.printStackTrace();

        if (consecutiveFailures >= MAX_FAILURES) {
            isDisabled = true;
            System.err.println("SafePreprocessedJsonAnalyzer: TOO MANY FAILURES - DISABLING ANALYZER FOR " +
                             (RECOVERY_TIMEOUT / 1000) + " seconds");
        }
    }

    /**
     * Creates a safe passthrough tokenizer that just returns the input as-is
     */
    private TokenStreamComponents createPassthroughComponents() {
        try {
            Tokenizer tokenizer = new KeywordTokenizer();
            return new TokenStreamComponents(tokenizer);
        } catch (Throwable e) {
            System.err.println("SafePreprocessedJsonAnalyzer: Even passthrough failed: " + e.getMessage());
            e.printStackTrace();
            // Return absolute minimal tokenizer
            return new TokenStreamComponents(new KeywordTokenizer());
        }
    }
}