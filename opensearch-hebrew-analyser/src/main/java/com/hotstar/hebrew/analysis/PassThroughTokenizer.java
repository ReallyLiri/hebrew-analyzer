package com.hotstar.hebrew.analysis;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.io.IOException;

/**
 * A tokenizer that passes through the entire input as a single token.
 * This preserves the original text structure for downstream processing.
 */
public class PassThroughTokenizer extends Tokenizer {

    private CharTermAttribute charTermAttribute = addAttribute(CharTermAttribute.class);
    private OffsetAttribute offsetAttribute = addAttribute(OffsetAttribute.class);
    private boolean hasEmitted = false;
    private String inputText;

    @Override
    public boolean incrementToken() throws IOException {
        if (hasEmitted) {
            return false;
        }

        if (inputText == null) {
            // Read the entire input
            StringBuilder sb = new StringBuilder();
            int ch;
            while ((ch = input.read()) != -1) {
                sb.append((char) ch);
            }
            inputText = sb.toString();
        }

        if (inputText.isEmpty()) {
            return false;
        }

        clearAttributes();
        charTermAttribute.setEmpty().append(inputText);
        offsetAttribute.setOffset(0, inputText.length());
        hasEmitted = true;

        return true;
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        hasEmitted = false;
        inputText = null;
    }
}