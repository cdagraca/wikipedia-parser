package com.stratio.clients;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.Insert;
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
                    + "revision_redirection text, revision_text text, lucene text," +
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

    private static Insert addField(Insert insert, String field, Object value){
        if (value != null){
            return insert.value(field, value);
        }

        return insert;
    }

    public void write(Revision r) {

        Insert query = null;
        try {
            query = addField(QueryBuilder.insertInto("revision"),
                    "id", UUID.fromString(new com.eaio.uuid.UUID().toString()));

            query = addField(query, "revision_id", r.getId());
            query = addField(query, "revision_timestamp", r.getTimestamp());
            query = addField(query, "page_id", r.getPage().getId());
            query = addField(query, "page_ns", r.getPage().getNamespace());
            query = addField(query, "page_fulltitle",r.getPage().getFullTitle());
            query = addField(query, "page_title", r.getPage().getTitle());
            query = addField(query, "page_restrictions", r.getPage().getRestrictions());
            query = addField(query, "page_isredirect", r.getPage().isRedirect());
            query = addField(query, "contributor_id", r.getContributor().getId());
            query = addField(query, "contributor_username", r.getContributor().getUsername());
            query = addField(query, "contributor_isanonymous", r.getContributor().getIsAnonymous());
            query = addField(query, "revision_isminor", r.isMinor());
            query = addField(query, "revision_redirection", r.getRedirection());
            query = addField(query, "revision_text", r.getText());

        } catch (Exception e) {
            logger.error("Cannot parse revision with revision_id: " + r.getId(), e);

            return;
        }

        batchStatement.add(query.setConsistencyLevel(ConsistencyLevel.QUORUM));
        numStatement++;
        if (numStatement == NUM_BATCH_STATEMENT) {
            rateLimiter.acquire();
            try {
                session.execute(batchStatement);
                logger.info("Executed batch query CassandraWriter: " + numIteration++);

            } catch (Exception e) {
                logger.error("Error while executing batch",e);
            }
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

                    if (revCounter % 100 == 0){
                        logger.info("# parsed documents: "+ revCounter);
                    }

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
