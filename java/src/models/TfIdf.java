package models;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.*;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.TopDocs;
import util.IndexUtils;
import util.ScoreUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

public class TfIdf implements RankingModel{
    IndexReader indexReader;
    Analyzer analyzer;
    HashMap<Integer, Integer> docLen;
    public TfIdf(IndexReader indexReader, Analyzer analyzer) throws IOException {
        this.indexReader = indexReader;
        this.analyzer = analyzer;
        this.docLen = IndexUtils.docLen(this.indexReader);
    }


    public TopDocs search(String query, int maxHits) throws IOException {

        TokenStream stream  = this.analyzer.tokenStream("contents", new StringReader(query));
        LeafReaderContext ctx = this.indexReader.leaves().get(0);
        LeafReader leafReader = ctx.reader();
        HashMap<Integer, Float> documentScore = new HashMap();
        stream.reset();
        float totalDocs = indexReader.numDocs();
        while(stream.incrementToken()) {
            Term queryTerm = new Term("contents", stream.getAttribute(CharTermAttribute.class).toString());
            float idf = (float) Math.log(totalDocs/this.indexReader.docFreq(queryTerm));
            PostingsEnum postingsList = leafReader.postings(queryTerm, PostingsEnum.ALL);
            while (postingsList != null && postingsList.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                int docID = postingsList.docID();
                float tf = ((float)  postingsList.freq())/this.docLen.get(docID);
                documentScore.put(docID, documentScore.getOrDefault(docID,0f)+ idf*tf);
            }
        }

        stream.close();

        return ScoreUtils.createTopDocs(documentScore, maxHits);
    }

    @Override
    public String getName() {
        return "TFIDF";
    }
}

