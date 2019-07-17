package queryExpansion;

import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;

public interface QueryExpanderImpl {
    public String[] getExpansionTerms(String query) throws IOException, ParseException;
}
