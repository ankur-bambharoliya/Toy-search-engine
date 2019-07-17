import lucene.BaseModel;
import lucene.Indexer;
import models.DirichletQL;
import models.RankingModel;
import models.TfIdf;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import queryExpansion.KStemExpander;
import queryExpansion.PseudoRelevanceExpander;
import queryExpansion.QueryExpanderImpl;
import util.IndexUtils;
import util.ResultFormatter;
import util.SnippetGenerator;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    static ArrayList<String> stopWords;
    static {
        stopWords = new ArrayList();
        try {
            Scanner scanner = new Scanner(new File("./src/common_words"));
            while (scanner.hasNextLine()){
                stopWords.add(scanner.nextLine().trim());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, ParseException {
        final CharArraySet stopSet = new CharArraySet(
                stopWords, false);

        System.out.println("Enter the FULL path to the index: (e.g. /Usr/index or c:\\temp\\index)");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String indexLocation = br.readLine();
        Indexer indexer = new Indexer(indexLocation);

        System.out.println("Enter the FULL path to add into the index (q=quit): (e.g. /home/mydir/docs or c:\\Users\\mydir\\docs)");
        indexer.indexDocs( br.readLine());

        IndexReader r = DirectoryReader.open(FSDirectory.open(Paths.get(indexLocation)));


        System.out.println("Which analyzer do you want to use?");
        Analyzer analyzer = null;

        while (analyzer == null){
            System.out.println("Select one from: STANDARD and STOP");
            switch (br.readLine()){
                case "STANDARD":
                    analyzer = new StandardAnalyzer();
                    break;
                case "STOP":
                    analyzer = new StopAnalyzer(stopSet);
                    break;
            }
        }

        System.out.println("Which ranking model do you want to use?");
        RankingModel model = null;

        while (model == null){
            System.out.println("Select one from: LUCENE, TFIDF, DIRICHLETQM");
            switch (br.readLine()){
                case "LUCENE":
                    model = new BaseModel(r);
                break;
                case "TFIDF":
                    model = new TfIdf(r, analyzer);
                    break;
                case "DIRICHLETQM":
                    model = new DirichletQL(r,analyzer, 2000);
                    break;
            }
        }


        System.out.println("Which query expansion do you want to use?");
        QueryExpanderImpl expander = null;

        while (expander == null){
            System.out.println("Select one from: KSTEMMER, PSEUDO-RELEVANCE and NO");
            boolean match = false;
            switch (br.readLine()){
                case "KSTEMMER":
                    expander = new KStemExpander(new StandardAnalyzer(), "contents", 10);;
                    break;
                case "PSEUDO-RELEVANCE":
                    expander = new PseudoRelevanceExpander(r, new StandardAnalyzer(),10,10);
                    break;
                case "NO":
                    match = true;
                    break;
            }

            if (match){
                break;
            }
        }

        System.out.println("Path to query file");
        String queryFile = br.readLine();

        System.out.println("Path to query result file");
        String queryResultFile = br.readLine();
        boolean storeSnippets = false;
        File snippetsFile = null;

        System.out.println("Do you want to generate snippets?[Y/N]");
        if(br.readLine().matches("Y")){
            System.out.println("Snippet file path::");
            snippetsFile = new File(br.readLine());
            storeSnippets = true;
        }

        ArrayList<String> queryList = getQueryList(queryFile);
        boolean append = false;
        TopDocs results = null;
        for(int i=0;i<queryList.size();i++){
            String query = queryList.get(i);
            if(expander !=null){
                query = query+ " "+String.join(" ", expander.getExpansionTerms(query));
                System.out.println(query);
            }

            results = model.search(query,100);
            ResultFormatter.saveSearchResults(r, results,i+1,model.getName(), queryResultFile, append);

            if(storeSnippets){
                File[] files = new File[results.scoreDocs.length];
                for (int j=0;j<results.scoreDocs.length; j++){
                    files[j] = new File(r.document(results.scoreDocs[j].doc).get("path"));
                }
                new SnippetGenerator(files, snippetsFile,IndexUtils.getIDFMap(r, analyzer, query, stopSet), 30, 60);
            }
            append |= true;

        }

    }


    static ArrayList<String> getQueryList(String queryFile) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(queryFile));
        String line = scanner.nextLine().trim();
        ArrayList<String> queryList = new ArrayList<>();

        if(line.matches("<DOC>")){
            while(true){
                while (scanner.hasNextLine() && !scanner.nextLine().trim().matches("<DOCNO>.*</DOCNO>")){
                }

                if(!scanner.hasNextLine()){
                    break;
                }
                scanner.nextLine();
                scanner.nextLine();
                String query = "";
                line = scanner.nextLine().trim();
                while (!line.matches("</DOC>")){
                    query = query +" "+ line;
                    line = scanner.nextLine();
                }
                System.out.println(query);
                queryList.add(query);
            }
        }else{
            queryList.add(line);
            while (scanner.hasNextLine()){
                queryList.add(scanner.nextLine().trim());
            }
        }

        return queryList;
    }
}
