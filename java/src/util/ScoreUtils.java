package util;

import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHits;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class ScoreUtils {
    public static TopDocs createTopDocs(HashMap<Integer, Float> documentScore, int maxHits){
        int hits = maxHits < documentScore.size()? maxHits: documentScore.size();
        ScoreDoc[] scoreDocs = new ScoreDoc[documentScore.size()];
        int i =0;
        for(HashMap.Entry<Integer, Float> entry:documentScore.entrySet()){
            scoreDocs[i] = new ScoreDoc(entry.getKey(), entry.getValue());
            i++;
        }
        Arrays.sort(scoreDocs, new ScoreDocComparator());
        return new TopDocs(new TotalHits(scoreDocs.length, TotalHits.Relation.EQUAL_TO), Arrays.copyOfRange(scoreDocs, 0, hits));

    }
}


class  ScoreDocComparator implements Comparator<ScoreDoc> {

    @Override
    public int compare(ScoreDoc o1, ScoreDoc o2) {
        if(o2.score < o1.score) return -1;
        if(o2.score > o1.score) return 1;
        return 0;
    }
}