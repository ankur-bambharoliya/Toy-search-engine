package util;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;



public class ResultFormatter {
    /**
     * topDocs {TopDocs} Lucene TopDocs object returned from the search method.
     * Prints the search result in human readable format.
     */
    public static void saveSearchResults(IndexReader reader, TopDocs topDocs, int queryID, String modelName, String filePath, boolean append)  throws IOException {
        ScoreDoc[] hits = topDocs.scoreDocs;
        String[] results = new String[hits.length];
        // 4. display results
        System.out.println("Found " + hits.length + " hits.");
        for (int i = 0; i < hits.length; ++i) {

            String fileName = Paths.get(reader.document(hits[i].doc).get("path")).getFileName().toString();
            String doc = fileName.substring(0, fileName.indexOf('.'));
            results[i] = String.format("%d Q0 %s %d %f %s", queryID, doc, i+1, hits[i].score, modelName);
        }

        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, append));
        writer.write( String.join("\n", results));
        writer.write("\n");
        writer.close();
    }

}
