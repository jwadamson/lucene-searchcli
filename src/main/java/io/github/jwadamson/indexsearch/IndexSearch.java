package io.github.jwadamson.indexsearch;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.util.Version;

public class IndexSearch {

    //**************************************************************************
    // CLASS
    //**************************************************************************

    /**
     * @param args
     * @throws IOException
     * @throws ParseException
     */
    public static void main(String[] args)
    throws IOException, ParseException {

        //
        // parse CLI
        //
        assert args.length == 2 : "Usage: indexsearch.groovy <index dir> <query>";

        File indexDir = new File(args[0]);  // Index directory create by Indexer
        String queryString = args[1];  // Query string

        assert indexDir.isDirectory() : indexDir+" is not a directory";

        //
        // Perform Search
        //

        FSDirectory fsDir = FSDirectory.open(indexDir);
        IndexSearcher indexSearcher = new IndexSearcher(fsDir, true);  // Open index as read only
        boolean closeQuiety = true;
        try {

            QueryParser queryParser = new QueryParser(Version.LUCENE_30, "field", new SimpleAnalyzer());
            queryParser.setAllowLeadingWildcard(true);

            Query query = queryParser.parse(queryString);  // Parse query
            long start = System.currentTimeMillis();
            TopDocs topDocs = indexSearcher.search(query, 10);  // Search index, top ten results
            long end = System.currentTimeMillis();

            String msg = "Found %s documents in %s millisconds that matched query %s";
            System.out.println(String.format(msg, topDocs.totalHits, end - start, queryString));
            System.out.println();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                System.out.println(indexSearcher.doc(scoreDoc.doc));
            }
            closeQuiety = false;
        }
        finally {
            try {
                indexSearcher.close();
            }
            catch (IOException e) {
                if (!closeQuiety) {
                    throw e;
                }
            }
        }
    }

    //**************************************************************************
    // INSTANCE
    //**************************************************************************
}
