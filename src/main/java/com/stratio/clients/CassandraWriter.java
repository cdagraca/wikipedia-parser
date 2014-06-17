package com.stratio.clients;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.common.util.concurrent.RateLimiter;
import com.stratio.callbacks.PageCallback;
import com.stratio.callbacks.RevisionCallback;
import com.stratio.data.Page;
import com.stratio.data.Revision;
import com.stratio.parsers.XMLDumpParser;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by luca on 12/06/14.
 */
public class CassandraWriter implements Closeable {
    private static Logger logger = Logger.getLogger(CassandraWriter.class);

    private static final String CREATE_TABLE_IF_EXIST =
            "CREATE TABLE IF NOT EXISTS revision (id uuid, revision_id int, revision_timestamp timestamp, "
                    + "page_id int, page_ns text, page_fulltitle text, page_title text, page_restrictions text, " +
                    "page_isredirect boolean, "
                    + "contributor_id int, contributor_username text, contributor_isanonymous boolean, " +
                    "revision_isminor boolean, "
                    + "revision_tokens list<text>, revision_lower_tokens list<text>, revision_redirection text, revision_text text, lucene text," +
                    "primary key (id));";

    private Session session;
    private BatchStatement batchStatement = new BatchStatement();
    private int numStatement = 0;
    private int numIteration = 0;
    private static final int NUM_BATCH_STATEMENT = 100;
    private Cluster cluster;

    private final double RATE = 5.0;

    private RateLimiter rateLimiter;

    public CassandraWriter(String host, Integer port, String keyspace) {
        cluster = Cluster.builder().addContactPoint(host).withPort(port).build();
        session = cluster.connect(keyspace);
        session.execute(CREATE_TABLE_IF_EXIST);
        rateLimiter = RateLimiter.create(RATE);
    }

    public void write(Revision r) {

        Statement query = null;
        try {
            query = QueryBuilder.insertInto("revision")
                    .values(
                            new String[]{"id", "revision_id", "revision_timestamp",
                                    "page_id", "page_ns", "page_fulltitle", "page_title",
                                    "page_restrictions",
                                    "page_isredirect", "contributor_id", "contributor_username",
                                    "contributor_isanonymous", "revision_isminor",
                                    "revision_tokens", "revision_lower_tokens", "revision_redirection",
                                    "revision_text"},
                            new Object[]{UUID.fromString(new com.eaio.uuid.UUID().toString()), r.getId(),
                                    r.getTimestamp(),
                                    r.getPage().getId(), r.getPage().getNamespace(),
                                    r.getPage().getFullTitle(), r.getPage().getTitle(),
                                    r.getPage().getRestrictions(),
                                    r.getPage().isRedirect(), r.getContributor().getId(),
                                    r.getContributor().getUsername(),
                                    r.getContributor().getIsAnonymous(), r.isMinor(), r.getTokens(),
                                    r.getLowerTokens(), r.getRedirection(), r.getText()})
                    .setConsistencyLevel(ConsistencyLevel.QUORUM);
        } catch (Exception e) {
            logger.error("Cannot parse revision with revision_id: " + r.getId(), e);

            return;
        }

        batchStatement.add(query);
        numStatement++;
        if (numStatement == NUM_BATCH_STATEMENT) {
            rateLimiter.acquire();
            logger.info("Execute batch query CassandraWriter: " + numIteration++);
            session.execute(batchStatement);
            batchStatement.clear();
            numStatement = 0;
        }

    }

    public void close(){
        session.close();
        cluster.close();
    }

    public static void main(String[] args) throws IOException, SAXException {
        if (args.length != 5) {
            logger.warn("Usage: <input_file> <keyspace> <cassandra_endpoint> <cassandra_port> <skip_first_n>");

            System.exit(1);
        }

        logger.info("Parsing file: " + args[0]);
        XMLDumpParser parser = new XMLDumpParser(args[0]);

        final String keyspace = args[1];
        final String host = args[2];
        final Integer port = Integer.parseInt(args[3]);
        final Integer skipFirstN = Integer.parseInt(args[4]);

        logger.info("Skipping first "+skipFirstN+" revisions");

        try (final CassandraWriter writer = new CassandraWriter(host, port, keyspace)){
            parser.getContentHandler().setPageCallback(new PageCallback() {
                @Override
                public void callback(Page page) {
                    //logger.info(page);
                }
            });
            parser.getContentHandler().setRevisionCallback(new RevisionCallback() {
                private int revCounter = 0;
                private boolean started = false;

                @Override
                public void callback(Revision revision) {
                    revCounter++;
                    if (revCounter > skipFirstN){

                        if (!started){
                            logger.info("Skip finished. Start writing");
                        }
                        started = true;
                        writer.write(revision);
                    }
                }
            });

            logger.info("Starting parser");
            parser.parse();
        }

    }
}
