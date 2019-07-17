package lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.io.InputStream;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class Indexer {

    Analyzer analyzer = null;
    String indexLocation = null;
    /**
     * indexLocation {String} the name of the folder in which the index should be created
     */
    public Indexer(String indexLocation)  throws IOException {
        this.analyzer = new StandardAnalyzer();
        this.indexLocation =  indexLocation;
    }

    /**
     * indexLocation {String} the name of the folder in which the index should be created
     */
    public Indexer(String indexLocation, Analyzer analyzer)  throws IOException {
        this.analyzer = analyzer;
        this.indexLocation =  indexLocation;
    }

    /**
     * docDir {String} corpus directory path
     * Index the documents in the given directory
     */
    public void indexDocs(String docDir)  throws IOException{
        IndexWriterConfig iwc = new IndexWriterConfig(this.analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        final IndexWriter writer = new IndexWriter(FSDirectory.open(Paths.get(this.indexLocation)), iwc);

        Files.walkFileTree(Paths.get(docDir), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    indexDoc(writer, file);
                } catch (IOException ignore) {}

                return FileVisitResult.CONTINUE;
            }
        });
        writer.close();
    }

    /**
     * Index the given file. The indexer creates the two field for the file.
     * Path field stores the path of the file and content field which hold the terms from the file.
     */
    protected void indexDoc(IndexWriter writer, Path file) throws IOException {
        InputStream stream = Files.newInputStream(file);
        Document doc = new Document();
        Field pathField = new StringField("path", file.toString(), Field.Store.YES);
        doc.add(pathField);
//        doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));
        doc.add(new TextField("contents", readFile(file, StandardCharsets.UTF_8), Field.Store.YES));
        writer.addDocument(doc);
    }

    static String readFile(Path path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(path);
        return new String(encoded, encoding);
    }
}
