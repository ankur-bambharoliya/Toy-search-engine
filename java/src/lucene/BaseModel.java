package lucene;
import models.RankingModel;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;

import java.io.IOException;
import java.nio.file.Paths;

public class BaseModel implements RankingModel {

    Analyzer analyzer = null;
    IndexReader reader = null;
    IndexSearcher searcher = null;

    /**
     * indexLocation {String} location of the index directory on which query should be performed.
     */
    public BaseModel(IndexReader indexReader)  throws IOException {
        this.analyzer = new StandardAnalyzer();
        this.reader = indexReader;
        this.searcher = new IndexSearcher(reader);
    }

    /**
     * query {String} query string
     * execute the given query on the index and return 100 top documents.
     */
    public TopDocs search(String query, int maxHits) throws IOException {
        //Creates a boolean query from query string.
        Query q = new QueryBuilder(this.analyzer).createBooleanQuery("contents", query);
        return this.search(q, maxHits);
    }

    /**
     * query {Query} Lucene query object
     * execute the given query on the index and return 100 top documents;
     */
    public TopDocs search(Query query, int maxHits)  throws IOException{
        return this.searcher.search(query,maxHits);
    }

    /**
     * query {Query} Lucene query object
     * returns the number of documents that satisfies query;
     */
    public int count(Query query)  throws IOException{
        return this.searcher.count(query);
    }

    @Override
    public String getName() {
        return "LUCENE";
    }
}
