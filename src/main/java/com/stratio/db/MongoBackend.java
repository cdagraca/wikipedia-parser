/*
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
package com.stratio.db;

import static com.google.common.base.Preconditions.*;
import com.mongodb.Mongo;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import java.net.UnknownHostException;
import com.stratio.data.Revision;

/**
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class MongoBackend {

    private final Mongo m;
    private final DB db;
    private final DBCollection articlesCollection;
    private static MongoBackend _instance;

    protected MongoBackend() throws MongoException, UnknownHostException {
        m = new Mongo();
        db = m.getDB("wpv");
        articlesCollection = db.getCollection("articles");
    }

    public static MongoBackend getInstance() throws MongoException,
            UnknownHostException {
        if (_instance == null) {
            _instance = new MongoBackend();
        }
        return _instance;
    }

    public DBCollection getArticlesCollection() {
        return articlesCollection;
    }

    public DBObject getArticle(String title, boolean throughRedirects) {
        DBObject article = articlesCollection.findOne(new BasicDBObject("title", title));

        while (article != null && throughRedirects && Boolean.TRUE.equals((Boolean)article.get("redirect"))) {
            String redirect = (String)article.get("redirection");
            article = articlesCollection.findOne(new BasicDBObject("title", redirect));
        }

        return article;
    }

    /**
     * Adds an article from a Revision object.
     *
     * FIXME: This will throw IllegalArgumentException (object too big) for huge
     *        articles (MongoDB limitation). This is triggered by some articles
     *        in the Wikipedia namespace, such as "Wikipedia:Upload log archive/May 2004 (1)".
     *
     * @param revision
     */
    public void addArticle(Revision revision) {

        revision = checkNotNull(revision);

        BasicDBObject doc = new BasicDBObject();

        //doc.append("pageId", pageId);
        doc.append("_id", revision.getPage().getId());
        doc.append("title", revision.getPage().getTitle());
        doc.append("namespace", revision.getPage().getNamespace());
        doc.append("revId", revision.getId());
        doc.append("redirect", revision.getPage().isRedirect());
        if (revision.getPage().isRedirect()) {
            doc.append("redirection", revision.getRedirection());
        }
        doc.append("tokens", revision.getLowerTokens());

        try {
            articlesCollection.insert(doc);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            /*
             * For some pages in Wikipedia: namespace we trigger the object size limit.
             */
        }
    }
}
