package util;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;

import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;

public class SpellCorrector {
    public static void main(String[] args) throws IOException {
        IndexReader r = DirectoryReader.open(FSDirectory.open(Paths.get("../index/temp")));
        SpellChecker spellchecker = new SpellChecker(FSDirectory.open(Paths.get("../index/temp")));
        IndexWriterConfig conf = new IndexWriterConfig(new StandardAnalyzer());
        spellchecker.indexDictionary(new LuceneDictionary(r, "contents"),conf, true);
        System.out.println(spellchecker.suggestSimilar("computerer", 5)[0]);
    }

}
