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
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

public class IndexSearch {

    //**************************************************************************
    // CLASS
    //**************************************************************************
    static private Options opts = new Options();
    static {

        Option indexDirOpt = new Option("i", "index", true, "the index directory");
        indexDirOpt.setRequired(true);
        opts.addOption(indexDirOpt);

        Option defaultField = new Option("d", "defaultField", true, "default field name (default keywords)");
        opts.addOption(defaultField);

        Option queryFile = new Option("q", "queryFile", true, "a file containing the query");
        opts.addOption(queryFile);

        Option maxResultsOption = new Option("m", "maxResults", true, "maximum number of results");
        opts.addOption(maxResultsOption);
    }

    /**
     * @param args
     * @throws IOException
     * @throws ParseException
     * @throws org.apache.commons.cli.ParseException
     */
    public static void main(String[] args)
    throws IOException, ParseException, org.apache.commons.cli.ParseException {

        //
        // parse CLI
        //
        CommandLineParser parser = new GnuParser();
        CommandLine line = parser.parse(opts, args);

        String indexPath = line.getOptionValue("index");
        String queryFilePath = line.getOptionValue("queryFile");
        String maxResultsString = line.getOptionValue("maxResults", "100");
        String defaultFieldName = line.getOptionValue('d', "keywords");

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
        try {
            Version ver = Version.LUCENE_30;
            QueryParser queryParser = new QueryParser(ver, defaultFieldName, new StandardAnalyzer(ver));
            queryParser.setAllowLeadingWildcard(true);

            Query query = queryParser.parse(queryString);  // Parse query

            IndexSearch search = new IndexSearch(fsDir);
            search.execute(query, maxResults);
        }
        finally {
            fsDir.close();
        }
    }

    //**************************************************************************
    // INSTANCE
    //**************************************************************************

    final FSDirectory fsDir;

    public IndexSearch(FSDirectory fsDir) {
        this.fsDir = fsDir;
    }

    public void execute(Query query, int maxResults)
    throws CorruptIndexException, IOException {

        IndexSearcher indexSearcher = new IndexSearcher(fsDir, true);  // Open index as read only
        boolean closeQuiety = true;
        try {

            long start = System.currentTimeMillis();
            TopDocs topDocs = indexSearcher.search(query, maxResults); // search index
            long end = System.currentTimeMillis();

            String msg = "Found %s documents in %s millisconds that matched query '%s'";
            System.out.println(String.format(msg, topDocs.totalHits, end - start, query));
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
}