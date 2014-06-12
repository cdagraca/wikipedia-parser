package com.stratio.clients;

import com.stratio.callbacks.PageCallback;
import com.stratio.callbacks.RevisionCallback;
import com.stratio.data.Page;
import com.stratio.data.Revision;
import com.stratio.parsers.XMLDumpParser;
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
        logger.info("Parsing file: "+args[0]);
        logger.info("Writing csv to: "+args[1]);
        XMLDumpParser parser = new XMLDumpParser(args[0]);

        final au.com.bytecode.opencsv.CSVWriter writer =
                new au.com.bytecode.opencsv.CSVWriter(
                        new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream( new FileOutputStream( new File( args[1] ) ), 10 * 1024), "UTF-8")));
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

            logger.info("Starting parser");
            parser.parse();
        } finally {
            logger.info("Closing parser");
            writer.flush();
            writer.close();
        }

    }
}

