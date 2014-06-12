/*
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.clients;


import com.google.common.collect.ConcurrentHashMultiset;
import static com.google.common.collect.Maps.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import static com.google.common.collect.Lists.*;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.mongodb.MongoException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.SAXException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import com.stratio.callbacks.RevisionCallback;
import com.stratio.data.Revision;
import com.stratio.parsers.XMLDumpParser;

/**
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class RedisApocalypse {

    int lastRevision;
    boolean ready;
    private RedisRevisionCallback callback;
    ExecutorService pool;
    Map<Long,Revision> revisions;
    Jedis jedis;
    Multiset<String> insertions;
    Multiset<String> deletions;
    long currentJob;
    Semaphore semaphore;

    public RedisApocalypse(int lastRevision) {
        this.lastRevision = lastRevision;
        ready = (lastRevision == 0);
        pool = Executors.newFixedThreadPool(30);
        revisions = newHashMap();
        revisions.put(0l, new Revision(0, ""));
        jedis = new Jedis("localhost");
        currentJob = 0;
        insertions = ConcurrentHashMultiset.create();
        deletions = ConcurrentHashMultiset.create();
        semaphore = new Semaphore(30);
    }

    public RevisionCallback getRevisionCallback() {
        if (callback == null) {
            callback = new RedisRevisionCallback();
        }
        return callback;
    }

    public synchronized void addJob(Revision revision) {
        try {
            semaphore.acquire();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        pool.execute(new RedisJob(currentJob, revision));
        currentJob++;
        if ((currentJob % 15) == 0) {
            for (String token: insertions.elementSet()) {
                int count = insertions.count(token);
                jedis.hincrBy(token, "i", count);
                insertions.remove(token, count);
            }
            for (String token: deletions.elementSet()) {
                int count = deletions.count(token);
                jedis.hincrBy(token, "d", count);
                deletions.remove(token, count);
            }
        }
        semaphore.release();
    }

    private class RedisJob implements Runnable {

        Long prevId;
        Revision revision;

        public RedisJob(Long prevId, Revision revision) {
            this.prevId = prevId;
            this.revision = revision;
        }

        @Override
        public void run() {
            try {
                semaphore.acquire();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            ImmutableMultiset<String> currentCount = revision.getLowerTokenCount();
            revisions.put(prevId + 1, revision);
            while (!revisions.containsKey(prevId)) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ex) {

                }
            }
            Revision previousRevision = revisions.get(prevId);
            revisions.remove(prevId);

            ImmutableMultiset<String> previousCount;
            if (revision.getPage() == previousRevision.getPage()) { //XXX: Reference! DANGER!
                previousCount = previousRevision.getLowerTokenCount();
            } else {
                previousCount = ImmutableMultiset.of();
            }

            for (String token : Sets.intersection(
                    previousCount.elementSet(), currentCount.elementSet())) {
                int count = currentCount.count(token) - previousCount.count(
                        token);
                if (count == 0) {
                    continue;
                }
                try {
                    if (count > 0) {
                        insertions.add(token, count);
                    } else {
                        deletions.add(token, 0 - count);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("|" + token + "|");
                    //System.exit(1);
                }
            }
            semaphore.release();
        }

    }

    private class RedisRevisionCallback implements RevisionCallback {

        @Override
        public void callback(Revision revision) {
            if (!ready) {
                if (lastRevision == revision.getId()) {
                    ready = true;
                }
                return;
            }

            System.out.println(revision.getId());

            addJob(revision);
            
        }
    }

    public static void main(String args[]) throws MongoException,
            UnknownHostException,
            SAXException, IOException {
        XMLDumpParser parser = new XMLDumpParser(
                System.in,
                ImmutableList.of(""));

        int lastRevision = 0;
        if (args.length > 0) {
            lastRevision = Integer.parseInt(args[0]);
        }

        RedisApocalypse redisApocalypse = new RedisApocalypse(lastRevision);

        parser.getContentHandler().setRevisionCallback(
                redisApocalypse.getRevisionCallback());

        try {
            parser.parse();
        } catch (Exception e) {
            e.printStackTrace();
            redisApocalypse.pool.shutdown();
            System.exit(1);

        }
        //dp.finish();

    }
}
