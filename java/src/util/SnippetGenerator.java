package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Scanner;
import java.util.Arrays;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import java.io.StringReader;


/***
 *  A class that generates html snippets of the following format...
 *  <html docId={doc_id}>...snippet...</html>
 *  ...with significant query terms highlighted within an html <b></b> tag
 */
public class SnippetGenerator {
//  public static void main(String[] args) {
//    File docStoreDir = new File(System.getProperty("user.dir") + "/src/cacm");
//    File outputFile = new File("snippets.html");
//    Map map = new HashMap<String, Double>();
//    map.put("the", 0.5);
//    map.put("ibm", 0.4);
//    map.put("for", 0.6);
//    map.put("system", 0.2);
//    map.put("world", 0.1);
//
//    try {
//      SnippetGenerator sg = new SnippetGenerator(docStoreDir, outputFile, map, 2, 10);
//    } catch (IOException ioe) {
//      System.out.println("Yikes; IOException encountered!");
//    }
//  }

  /***
   *
   * @param docStoreDir directory of the document store
   * @param outputFile desired filename for the output file
   * @param queryTerms a map of query terms to idf score
   * @param numSignificantQueryTerms number of query terms to be treated as significant
   * @param snippetLength the target length of the snippet to be generated in words
   * @throws IOException
   */
  public SnippetGenerator(File[] docs, File outputFile, Map<String, Double> queryTerms, int numSignificantQueryTerms, int snippetLength)  throws IOException {
//    File[] docs = docStoreDir.listFiles();
    Map<String, Double> query_terms = getTopQueryTerms(queryTerms, numSignificantQueryTerms);
    StringBuilder stringBuilder = new StringBuilder("<html>\n");

    // generate snippet for each document
    if (docs != null)
    {
      for (File doc : docs) {
        String filename = doc.getName();
        String docId = filename.substring(0, filename.indexOf('.'));
        stringBuilder.append(String.format("<p docId=%s>", docId));
        stringBuilder.append(getSnippet(doc, query_terms, snippetLength));
        stringBuilder.append("</p>\n");
      }
    }

    stringBuilder.append("</html>");

    BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
    writer.write(stringBuilder.toString());
    writer.close();
  }

  /***
   *
   * @param queryTerms a map of query terms to idf score
   * @param numSignificantQueryTerms number of query terms to be treated as significant
   * @return a map of sigificant query terms to idf scores
   */
  private Map<String, Double> getTopQueryTerms(Map<String, Double> queryTerms, int numSignificantQueryTerms) {
    Map<String, Double> topQueries = new HashMap<>();

    if (numSignificantQueryTerms < 1) {
      return topQueries;
    } else if (queryTerms.size() <= numSignificantQueryTerms) {
      return queryTerms;
    } else {
      Set<Entry<String, Double>> entrySet = queryTerms.entrySet();
      List<Entry<String, Double>> entryList = new LinkedList<>(entrySet);
      Collections.sort(entryList, Entry.comparingByValue());
      Collections.reverse(entryList);

      for (Entry<String, Double> e : entryList.subList(0, numSignificantQueryTerms)) {
        topQueries.put(e.getKey(), e.getValue());
      }

      return topQueries;
    }
  }

  /***
   *
   * @param doc the document from which the snippet is to be created
   * @param queryTerms a map of query terms to idf score
   * @param snippetLength the target length of the snippet to be generated in words
   * @return a string that represents the snippet
   * @throws IOException
   */
  private String getSnippet(File doc, Map<String, Double> queryTerms, int snippetLength) throws IOException {
    Scanner scanner = new Scanner(doc);
    StringBuilder stringBuilder = new StringBuilder();

    while (scanner.hasNext()) {
      stringBuilder.append(scanner.nextLine());
      stringBuilder.append(" ");
    }

    String docText = stringBuilder.toString();
    String[] tokens = docText.split("\\s+");

    return findSnippet(tokens, queryTerms, snippetLength);
  }

  /***
   *
   * @param tokens a list of string tokens that represent a document
   * @param queryTerms a map of query terms to idf score
   * @param snippetLength the target length of the snippet to be generated in words
   * @return a string that represents the snippet
   */
  private String findSnippet(String[] tokens, Map<String, Double> queryTerms, int snippetLength) throws IOException {
    int tokenCount = tokens.length;
    String[] snippet = tokens;

    if (tokenCount > snippetLength) { // otherwise just return the tokens as is
      int bestStartIndex = 0;
      int start = 1;
      int stop = 1 + snippetLength;

      double[] factors = getFactors(tokens, queryTerms);
      double bestSignificance = getInitialSignificance(factors, snippetLength);
      double previousSignificance = bestSignificance;
      double currentSignificance;

      while (stop < tokens.length) {
        currentSignificance = getNextSignificance(factors, start, stop, previousSignificance);

        if (currentSignificance > bestSignificance) {
          bestSignificance = currentSignificance;
          bestStartIndex = start;
        }

        previousSignificance = currentSignificance;
        start++;
        stop++;
      }

      int toIndex = bestStartIndex + snippetLength;

      if (toIndex > tokens.length) {
        bestStartIndex = bestStartIndex - (toIndex - tokens.length);
        toIndex = tokens.length;
      }

      snippet = Arrays.copyOfRange(tokens, bestStartIndex, toIndex);
    }

    return highlightSnippet(snippet, queryTerms);
  }

  /**
   *
   * @param tokens a list of string tokens that represent a document
   * @param queryTerms a map of query terms to idf score
   * @return an array of scores: 0 if the token is not a query term, 1 * idf if it is
   */
  private double[] getFactors(String[] tokens, Map<String, Double> queryTerms) throws IOException {
    StandardAnalyzer analyzer = new StandardAnalyzer();
    String token;
    double[] factors = new double[tokens.length];

    for (int i = 0; i < tokens.length; i++) {
      token = tokenize(analyzer, tokens[i]);

      if (queryTerms.containsKey(token)) {
        factors[i] = 1.0 * queryTerms.get(token);
      }
    }

    return factors;
  }

  /**
   * strategy for tokenizing informed by https://stackoverflow.com/users/807231/gevorg
   * through comment on https://stackoverflow.com/questions/6334692/how-to-use-a-lucene-analyzer-to-tokenize-a-string
   * @param analyzer
   * @param token
   * @return
   * @throws IOException
   */
  private String tokenize(StandardAnalyzer analyzer, String token) throws IOException {
    TokenStream tokenStream  = analyzer.tokenStream(null, new StringReader(token));
    tokenStream.reset();
    tokenStream.incrementToken();
    String tokenized = tokenStream.getAttribute(CharTermAttribute.class).toString();
    tokenStream.close();

    return tokenized;
  }

  /***
   *
   * @param factors significance factors for each token in the document
   * @param snippetLength the target length of the snippet to be generated in words
   * @return the sum of the first snippetLength factors
   */
  private double getInitialSignificance(double[] factors, int snippetLength) {
    double initialSignificance = 0;

    for (int i = 0; i < snippetLength; i++) {
      initialSignificance += factors[i];
    }

    return initialSignificance;
  }

  /***
   *
   * @param factors significance factors for each token in the document
   * @param start the starting index for the next window
   * @param stop the stopping index for the next window
   * @param previous the significance score for the previous window
   * @return the significance score for the given window
   */
  private double getNextSignificance(double[] factors, int start, int stop, double previous) {
    return previous - factors[start - 1] + factors[stop - 1];
  }

  /***
   *
   * @param snippet the snippet to be highlighted
   * @param queryTerms a map of query terms to idf score
   * @return the snippet with query terms highlighted within an html <b></b> tag
   */
  private String highlightSnippet(String[] snippet, Map<String, Double> queryTerms) throws IOException {
    StringBuilder stringBuilder = new StringBuilder("...");
    StandardAnalyzer analyzer = new StandardAnalyzer();
    String token;

    for (String t : snippet) {
      if (t.startsWith("<") && t.endsWith(">")) {
        continue;
      }

      token = tokenize(analyzer, t);

      if (queryTerms.containsKey(token)) {
        stringBuilder.append(" <b>");
        stringBuilder.append(t);
        stringBuilder.append("</b>");
      } else {
        stringBuilder.append(" ");
        stringBuilder.append(t);
      }
    }

    stringBuilder.append(" ...");

    return stringBuilder.toString();
  }
}