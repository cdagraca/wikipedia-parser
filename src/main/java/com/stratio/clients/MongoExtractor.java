/**
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.clients;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import java.net.UnknownHostException;
import java.util.List;
import com.stratio.db.MongoBackend;

/**
 *
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class MongoExtractor {

    public static void main(String args[]) throws MongoException, UnknownHostException {

        DBCollection articlesCollection = MongoBackend.getInstance().getArticlesCollection();
        for (DBObject dbo: articlesCollection.find(new BasicDBObject("redirect", Boolean.FALSE))) {
            List<String> tokens = (List<String>)dbo.get("tokens");
            System.out.println(Joiner.on(' ').join(tokens));
        }

    }

}
