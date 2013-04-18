package io.github.jwadamson.indexsearch;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
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
     * @throws org.apache.commons.cli.ParseException
     */
    public static void main(String[] args)
    throws IOException, ParseException, org.apache.commons.cli.ParseException {

        Options opts = new Options();
        Option indexDirOpt = new Option(null, "index", true, "the index directory");
        indexDirOpt.setRequired(true);
        opts.addOption(indexDirOpt);

        Option queryFile = new Option(null, "queryFile", true, "a file containing the query");
        opts.addOption(queryFile);

        Option maxResultsOption = new Option(null, "maxResults", true, "maximum number of results");
        opts.addOption(maxResultsOption);


        //
        // parse CLI
        //
        CommandLineParser parser = new GnuParser();
        CommandLine line = parser.parse(opts, args);

        String indexPath = line.getOptionValue("index");
        String queryFilePath = line.getOptionValue("queryFile");
        String maxResultsString = line.getOptionValue("maxResults", "100");

        File indexDir = new File(indexPath);  // Index directory create by Indexer
        String queryString;  // Query string
        if (queryFilePath != null) {
            queryString = FileUtils.readFileToString(new File(queryFilePath));
        }
        else {
            @SuppressWarnings("unchecked")
            List<String> argList = line.getArgList();
            if (argList.size() != 1) {
                throw new RuntimeException("Either a query file or string must be supplied");
            }
            queryString = argList.get(0);
        }

        assert indexDir.isDirectory() : indexDir+" is not a directory";

        int maxResults = Integer.valueOf(maxResultsString);

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
            TopDocs topDocs = indexSearcher.search(query, maxResults); // search index
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
