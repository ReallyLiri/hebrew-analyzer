import com.hotstar.hebrew.analysis.SefariaSemiExactFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.core.KeywordTokenizer;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class test_analyzer {
    public static void main(String[] args) throws Exception {
        // Test with a Hebrew word that has a prefix
        String input = "הספר";
        System.out.println("Testing input: " + input);

        TokenStream tokenStream = new KeywordTokenizer();
        ((KeywordTokenizer) tokenStream).setReader(new StringReader(input));

        SefariaSemiExactFilter filter = new SefariaSemiExactFilter(tokenStream);

        List<String> tokens = new ArrayList<>();
        CharTermAttribute charTermAttribute = filter.addAttribute(CharTermAttribute.class);

        filter.reset();
        while (filter.incrementToken()) {
            tokens.add(charTermAttribute.toString());
        }
        filter.close();

        System.out.println("Generated tokens:");
        for (String token : tokens) {
            System.out.println("  " + token);
        }

        // Test with a word without prefix
        System.out.println("\nTesting input: ספר");

        tokenStream = new KeywordTokenizer();
        ((KeywordTokenizer) tokenStream).setReader(new StringReader("ספר"));

        filter = new SefariaSemiExactFilter(tokenStream);
        tokens.clear();
        charTermAttribute = filter.addAttribute(CharTermAttribute.class);

        filter.reset();
        while (filter.incrementToken()) {
            tokens.add(charTermAttribute.toString());
        }
        filter.close();

        System.out.println("Generated tokens:");
        for (String token : tokens) {
            System.out.println("  " + token);
        }
    }
}