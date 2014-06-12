package com.stratio.clients;

import com.google.common.collect.ImmutableList;
import com.stratio.callbacks.PageCallback;
import com.stratio.callbacks.RevisionCallback;
import com.stratio.data.Page;
import com.stratio.data.Revision;
import com.stratio.parsers.XMLDumpParser;
import org.apache.commons.compress.bzip2.CBZip2OutputStream;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.zip.GZIPOutputStream;

/**
 * Created by luca on 12/06/14.
 */
public class CSVWriter {
    private static Logger logger = Logger.getLogger(CSVWriter.class);

    public static void main(String[] args) throws IOException, SAXException {
        XMLDumpParser parser = new XMLDumpParser(
                "/Users/luca/Downloads/enwiki-20140502-pages-meta-current1.xml.bz2");

        final au.com.bytecode.opencsv.CSVWriter writer =
                new au.com.bytecode.opencsv.CSVWriter(
                        new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream( new FileOutputStream( new File( "/tmp/test.csv.gz" ) ), 10 * 1024), "UTF-8")));


        try {
            parser.getContentHandler().setPageCallback(new PageCallback() {
                @Override
                public void callback(Page page) {
                    //logger.info(page);
                }
            });
            parser.getContentHandler().setRevisionCallback(new RevisionCallback() {
                @Override
                public void callback(Revision revision) {
                    writer.writeNext(revision.csvEntry());
                }
            });

            parser.parse();
        } finally {
            writer.flush();
            writer.close();
        }

    }
}
