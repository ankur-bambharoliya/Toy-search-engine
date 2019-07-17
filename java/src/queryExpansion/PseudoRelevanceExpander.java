package queryExpansion;

import lucene.BaseModel;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.QueryBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class PseudoRelevanceExpander implements QueryExpanderImpl {
    Analyzer analyzer;
    IndexReader indexReader;
    BaseModel model;
    int k;
    int n;
    public PseudoRelevanceExpander(IndexReader indexReader, Analyzer analyzer, int k, int n) throws IOException {
        this.analyzer = analyzer;
        this.model = new BaseModel(indexReader);
        this.indexReader = indexReader;
        this.k = k;
        this.n = n;
    }

    @Override
    public String[] getExpansionTerms(String query) throws IOException, ParseException {
        ScoreDoc[] hits =  this.model.search(query, this.k).scoreDocs;
        // In memory index of the top k documents from the query search result.
        IndexReader relevantDocumentsReader = this.buildRAMIndex(hits);

        QueryBuilder queryBuilder= new QueryBuilder(this.analyzer);
        Query q = queryBuilder.createBooleanQuery("contents", query,  BooleanClause.Occur.MUST);
        List queryWords = Arrays.asList(this.analyzer.normalize("contents", query).utf8ToString().split(" "));

        int n_b = 0, n_ab =0;
        //number of documents that contains all the query words
        double n_a = (double)this.model.count(q);

        // Query that holds the terms sorted by there Dice coefficient score with the query
        PriorityQueue<Map.Entry<String, Double>> topWords = new PriorityQueue(n, Map.Entry.comparingByValue());
        LeafReader leafReader = relevantDocumentsReader.getContext().leaves().get(0).reader();
        final Terms terms = leafReader.terms("contents");
        final TermsEnum it = terms.iterator();
        BytesRef term = it.next();

        // iterate over the every terms of the top k documents and calculates the Dice score.
        while (term != null) {
            String termTxt = term.utf8ToString();
            // number of documents that contains term for which Dice score is being calculated.
            n_b = this.indexReader.docFreq(new Term("contents", termTxt));
            // number of the documents that contain both the query and the term.
            n_ab = this.model.count(queryBuilder.createBooleanQuery("contents", query + " "+termTxt,  BooleanClause.Occur.MUST));
            // score the term with its Dice score.
            topWords.add(new AbstractMap.SimpleEntry<String, Double>(termTxt, -n_ab/(n_a+n_b)));
            term = it.next();
        }

        Map.Entry<String, Double> topWord = null;
        String[] expansionWords = new String[n];
        // returns top K terms as expansion term.
        for (int i=0;i<n&&i<topWords.size();){
            topWord = topWords.poll();
            if(!queryWords.contains(topWord.getKey())){
                expansionWords[i] = topWord.getKey();
                System.out.println(topWord.getKey());
                i++;
            }
        }
        return expansionWords;
    }

    /**
     * hits {ScoreDoc[]} list of the docs sorted by its score
     *
     * Creates in-memory index of top k documents.
     */
    public IndexReader buildRAMIndex(ScoreDoc[] hits) throws IOException{

        Directory directory = new RAMDirectory();
        IndexWriterConfig iwc = new IndexWriterConfig(this.analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        final IndexWriter writer = new IndexWriter(directory, iwc);
        InputStream stream = null;
        for(int i=0; i<hits.length && i<this.k; i++){
            Document doc = new Document();
            stream = Files.newInputStream(Paths.get(this.indexReader.document(hits[i].doc).get("path")));
            doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));
            writer.addDocument(doc);
        }
        writer.close();
        return DirectoryReader.open(directory);
    }
}
