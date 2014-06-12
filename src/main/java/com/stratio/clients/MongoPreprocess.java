/**
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
package com.stratio.clients;

import com.google.common.collect.ImmutableList;
import com.mongodb.MongoException;
import java.io.IOException;
import java.net.UnknownHostException;

import com.stratio.callbacks.PageCallback;
import org.xml.sax.SAXException;
import com.stratio.callbacks.PageCallback;
import com.stratio.callbacks.RevisionCallback;
import com.stratio.data.Page;
import com.stratio.data.Revision;
import com.stratio.db.MongoBackend;
import com.stratio.parsers.XMLDumpParser;

/**
 *
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class MongoPreprocess {

    private static class DumpProcessor {

        private Page lastPage;
        private Revision lastRevision;
        private MongoBackend mongo;
        private final int alreadyProcessed;
        private int nProcessed;

        public DumpProcessor(int alreadyProcessed) throws MongoException,
                UnknownHostException {
            this.alreadyProcessed = alreadyProcessed;
            this.mongo = MongoBackend.getInstance();
        }

        private void doRev() {
            nProcessed++;
            if (nProcessed < alreadyProcessed) {
                return;
            }

            try {
                mongo.addArticle(lastRevision);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                System.out.println("FAIL: " + lastRevision.getPage().getTitle()
                        + " " + lastRevision.getId());
                System.exit(1);
            }
        }

        public void finish() {
            doRev();
        }

        class _PageCallback implements PageCallback {

            @Override
            public void callback(Page page) {
                if (lastPage != null) {
                    doRev();
                }
                lastPage = page;
                lastRevision = null;
            }
        }

        public PageCallback getPageCallback() {
            return new _PageCallback();
        }

        class _RevisionCallback implements RevisionCallback {

            @Override
            public void callback(Revision revision) {
                if (lastRevision == null) {
                    lastRevision = revision;
                } else if (revision.getId().compareTo(lastRevision.getId())
                        > 0) {
                    lastRevision = revision;
                }
            }
        }

        public RevisionCallback getRevisionCallback() {
            return new _RevisionCallback();
        }
    }

    public static void main(String args[]) throws MongoException,
            UnknownHostException, SAXException, IOException {
        XMLDumpParser parser = new XMLDumpParser(
                "/corpora/inProduction/Wikipedia/en/enwiki-20100130-pages-articles.xml.bz2",
                ImmutableList.of(""));

        int alreadyProcessed = 0;
        if (args.length > 0) {
            alreadyProcessed = Integer.valueOf(args[0]);
        }

        DumpProcessor dp = new DumpProcessor(alreadyProcessed);
        parser.getContentHandler().setPageCallback(dp.getPageCallback());
        parser.getContentHandler().setRevisionCallback(dp.getRevisionCallback());

        parser.parse();

    }
}
