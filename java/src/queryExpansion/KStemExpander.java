package queryExpansion;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

public class KStemExpander implements  QueryExpanderImpl {
    Analyzer analyzer;
    String fieldName;
    int n ;
    public KStemExpander(Analyzer analyzer, String fieldName, int n){
        this.analyzer = analyzer;
        this.fieldName = fieldName;
        this.n = n;
    }
    @Override
    /*
    * This method returns stemmed terms for given query.
    * */
    public String[] getExpansionTerms(String query) throws IOException {
        HashSet<String>stems = new HashSet();
        TokenStream queryTokenStream = this.analyzer.tokenStream(this.fieldName, query);
        KStemFilter filter = new KStemFilter(queryTokenStream);
        CharTermAttribute charTermAttribute = filter.addAttribute(CharTermAttribute.class);

        filter.reset();
        while (filter.incrementToken()) {
            String term = charTermAttribute.toString();
            stems.add(term);
        }

        filter.close();

        queryTokenStream = this.analyzer.tokenStream(this.fieldName, query);
        queryTokenStream.reset();
        charTermAttribute = queryTokenStream.addAttribute(CharTermAttribute.class);
        while (queryTokenStream.incrementToken()) {
            String term = charTermAttribute.toString();
            stems.remove(term);
        }
        String[] expansionTermsArray = new String[stems.size()];
        queryTokenStream.close();
        return stems.toArray(expansionTermsArray);
    }
}
