package models;

import org.apache.lucene.search.TopDocs;

import java.io.IOException;


public interface RankingModel {
    public TopDocs search(String query, int maxHits) throws IOException;
    public String getName();
}
