package util;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.*;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

public class IndexUtils {
    public static HashMap<Integer, Integer> docLen(IndexReader indexReader) throws IOException {

        LeafReaderContext ctx = indexReader.leaves().get(0);
        LeafReader leafReader = ctx.reader();
        HashMap<Integer, Integer> docLen = new HashMap();
        TermsEnum it = leafReader.terms("contents").iterator();
        BytesRef term = it.next();
        long sum = 0;
        while (term != null) {
            Term queryTerm = new Term("contents", term);
            PostingsEnum postingsList = leafReader.postings(queryTerm, PostingsEnum.ALL);
            while (postingsList.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                int docId = postingsList.docID();
                docLen.put(docId, docLen.getOrDefault(docId, 0)+postingsList.freq());
            }
            term = it.next();
        }
        System.out.println(sum);
        return docLen;
    }

    public static HashMap<String, Double> getIDFMap(IndexReader indexReader, Analyzer analyzer, String query, CharArraySet stopSet) throws IOException {
        TokenStream stream  = analyzer.tokenStream("contents", new StringReader(query));
        double totalDocs = indexReader.numDocs();
        HashMap<String, Double> idfMap = new HashMap();
        stream.reset();
        while(stream.incrementToken()) {
            Term queryTerm = new Term("contents", stream.getAttribute(CharTermAttribute.class).toString());
            if(stopSet.contains(queryTerm.text())){
                continue;
            }
            double idf = (double) Math.log(totalDocs/indexReader.docFreq(queryTerm));
            idfMap.put(queryTerm.text(), idf);
        }

        stream.close();
        return  idfMap;
    }
}
