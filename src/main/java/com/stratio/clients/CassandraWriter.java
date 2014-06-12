package com.stratio.clients;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.stratio.callbacks.PageCallback;
import com.stratio.callbacks.RevisionCallback;
import com.stratio.data.Page;
import com.stratio.data.Revision;
import com.stratio.parsers.XMLDumpParser;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created by luca on 12/06/14.
 */
public class CassandraWriter implements Closeable {
    private static Logger logger = Logger.getLogger(CassandraWriter.class);

    private static final String CREATE_TABLE_IF_EXIST =
            "CREATE TABLE IF NOT EXISTS revision (revision_id int, "
                    + "page_id int, page_ns text, page_fulltitle text, page_title text, page_restrictions text, " +
                    "page_isredirect boolean, "
                    + "contributor_id int, contributor_username text, contributor_isanonymous boolean, " +
                    "isminor boolean, "
                    + "tokens list<text>, lower_tokens list<text>, redirection text, revision_text text, " +
                    "primary key (page_id, revision_id));";

    private Session session;
    private BatchStatement batchStatement = new BatchStatement();
    private int numStatement = 0;
    private int numIteration = 0;
    private static final int NUM_BATCH_STATEMENT = 10;
    private Cluster cluster;

    public CassandraWriter(String host, Integer port, String keyspace) {
        cluster = Cluster.builder().addContactPoint(host).build();
        session = cluster.connect(keyspace);
        session.execute(CREATE_TABLE_IF_EXIST);
    }

    public void write(Revision r) {

        Statement query =
                QueryBuilder.insertInto("revision")
                        .values(
                                new String[]{"revision_id", "page_id", "page_ns", "page_fulltitle", "page_title",
                                        "page_restrictions",
                                        "page_isredirect", "contributor_id", "contributor_username",
                                        "contributor_isanonymous", "isminor",
                                        "tokens", "lower_tokens", "redirection", "revision_text"},
                                new Object[]{r.getId(), r.getPage().getId(), r.getPage().getNamespace(),
                                        r.getPage().getFullTitle(), r.getPage().getTitle(),
                                        r.getPage().getRestrictions(),
                                        r.getPage().isRedirect(), r.getContributor().getId(),
                                        r.getContributor().getUsername(),
                                        r.getContributor().getIsAnonymous(), r.isMinor(), r.getTokens(),
                                        r.getLowerTokens(), r.getRedirection(), r.getText()})
                        .setConsistencyLevel(ConsistencyLevel.QUORUM);

        batchStatement.add(query);
        numStatement++;
        if (numStatement == NUM_BATCH_STATEMENT) {
            logger.info("Execute batch query PersonRegister: " + numIteration++);
            session.execute(batchStatement);
            batchStatement.clear();
            numStatement = 0;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.error(e);
            }
        }

    }

    public void close(){
        session.close();
        cluster.close();
    }

    public static void main(String[] args) throws IOException, SAXException {
        if (args.length != 4) {
            logger.warn("Usage: <input_file> <keyspace> <cassandra_endpoint> <cassandra_port>");

            System.exit(1);
        }

        logger.info("Parsing file: " + args[0]);
        XMLDumpParser parser = new XMLDumpParser(args[0]);

        final String keyspace = args[1];
        final String host = args[2];
        final Integer port = Integer.parseInt(args[3]);

        try (final CassandraWriter writer = new CassandraWriter(host, port, keyspace)){
            parser.getContentHandler().setPageCallback(new PageCallback() {
                @Override
                public void callback(Page page) {
                    //logger.info(page);
                }
            });
            parser.getContentHandler().setRevisionCallback(new RevisionCallback() {
                @Override
                public void callback(Revision revision) {
                    writer.write(revision);
                }
            });

            logger.info("Starting parser");
            parser.parse();
        }

    }
}
