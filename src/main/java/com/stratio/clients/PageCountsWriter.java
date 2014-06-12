package com.stratio.clients;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.stratio.data.Revision;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

/**
 * Created by luca on 12/06/14.
 */
public class PageCountsWriter implements Closeable{
    private static Logger logger = Logger.getLogger(PageCountsWriter.class);

    private static final String CREATE_TABLE_IF_EXIST =
            "CREATE TABLE IF NOT EXISTS pagecounts (" +
                    "id uuid, title text, pagecounts int, ts timestamp, lucene text, primary key(id));";

    private Session session;
    private BatchStatement batchStatement = new BatchStatement();
    private int numStatement = 0;
    private int numIteration = 0;
    private static final int NUM_BATCH_STATEMENT = 1000;
    private Cluster cluster;

    public void close(){
        session.close();
        cluster.close();
    }

    static class PageCount{
        private String title;
        private Date ts;
        private Integer pagecounts;

        PageCount(String title, Date ts, Integer pagecounts) {
            this.title = title;
            this.ts = ts;
            this.pagecounts = pagecounts;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Date getTs() {
            return ts;
        }

        public void setTs(Date ts) {
            this.ts = ts;
        }

        public Integer getPagecounts() {
            return pagecounts;
        }

        public void setPagecounts(Integer pagecounts) {
            this.pagecounts = pagecounts;
        }
    }

    public PageCountsWriter(String host, Integer port, String keyspace) {
        cluster = Cluster.builder().addContactPoint(host).build();
        session = cluster.connect(keyspace);
        session.execute(CREATE_TABLE_IF_EXIST);
    }

    public void write(PageCount r) {

        Statement query =
                QueryBuilder.insertInto("pagecounts")
                        .values(
                                new String[]{"id", "title", "pagecounts",
                                        "ts"},
                                new Object[]{UUID.fromString(new com.eaio.uuid.UUID().toString()),
                                        r.getTitle(),
                                        r.getTs()})
                        .setConsistencyLevel(ConsistencyLevel.QUORUM);

        batchStatement.add(query);
        numStatement++;
        if (numStatement == NUM_BATCH_STATEMENT) {
            logger.info("Execute batch query CassandraWriter: " + numIteration++);
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

    public static void main(String[] args) throws IOException, ParseException {

        String filename = args[0];
        final String keyspace = args[1];
        final String host = args[2];
        final Integer port = Integer.parseInt(args[3]);

        SimpleDateFormat sdf = new SimpleDateFormat("'pagecounts-'yyyyMMdd'-'HHmmss");
        PageCountsWriter writer = new PageCountsWriter(host,port,keyspace);

        try(
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filename))))){
            String line = null;
            while ((line = reader.readLine()) != null){
                String[] splitted = line.split(" ");
                String title = splitted[1];
                Integer counter = Integer.parseInt(splitted[2]);
                Date ts = null;

                try {
                    ts = sdf.parse(filename);
                } catch (ParseException e) {
                    logger.error(e);
                }

                PageCount pc = new PageCount(title, ts, counter);
                writer.write(pc);
            }


        }
    }
}
