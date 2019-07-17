package models;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.*;

import org.apache.lucene.search.TopDocs;
import util.IndexUtils;
import util.ScoreUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

public class DirichletQL implements RankingModel {
    IndexReader indexReader;
    LeafReader leafReader;
    Analyzer analyzer;
    int mu;
    HashMap<Integer, Integer> docLen;

    public DirichletQL(IndexReader indexReader, Analyzer analyzer, int mu) throws IOException {
        this.indexReader = indexReader;
        this.analyzer = analyzer;
        this.docLen = IndexUtils.docLen(this.indexReader);
        this.mu = mu;
        LeafReaderContext ctx = this.indexReader.leaves().get(0);
        this.leafReader = ctx.reader();

    }

    public TopDocs search(String query, int maxHits) throws IOException {

        TokenStream stream  = this.analyzer.tokenStream("contents", new StringReader(query));
        HashMap<Integer, Float> documentScore = new HashMap();
        int freq = 0;
        long totalTokens = this.indexReader.getSumTotalTermFreq("contents");
        stream.reset();

        while(stream.incrementToken()) {
            Term queryTerm = new Term("contents", stream.getAttribute(CharTermAttribute.class).toString());
            if(this.indexReader.totalTermFreq(queryTerm)<=0){ // ignore the term if it is not in the collection.
                System.out.println(queryTerm.toString());
                continue;
            }

            float collectionProbability = this.mu*(((float) this.indexReader.totalTermFreq(queryTerm))/totalTokens);
            PostingsEnum postingsList = this.leafReader.postings(queryTerm, PostingsEnum.ALL);
            postingsList.nextDoc();
            for(int i=0;i<this.indexReader.maxDoc();i++){
                freq = 0;
                if(i==postingsList.docID()) {
                    freq = postingsList.freq();
                    postingsList.nextDoc();
                }

                Float score = (float)Math.log((freq + collectionProbability)/(this.docLen.get(i)+this.mu));
                documentScore.put(i, documentScore.getOrDefault(i,0f)+ score);
            }
        }
        stream.close();
        return ScoreUtils.createTopDocs(documentScore, maxHits);
    }

    @Override
    public String getName() {
        return "DIRICHLETQM";
    }

}
