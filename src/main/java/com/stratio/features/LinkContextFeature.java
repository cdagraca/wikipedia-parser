/*
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.features;
import static com.google.common.collect.Lists.*;
import static com.google.common.collect.Maps.*;
import static com.google.common.collect.Sets.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.stratio.data.Edit;
import com.stratio.db.MongoBackend;
import weka.core.Instance;
import com.stratio.data.Edit;
import com.stratio.db.MongoBackend;
import com.stratio.lm.BigramBackoffModel;
import com.stratio.lm.NGramUtils;
import com.stratio.parsers.MediawikiTokenizer;
import com.stratio.probability.DiscreteProbabilityDistribution;
import com.stratio.probability.MLEDiscreteProbabilityDistribution;

/**
 *
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class LinkContextFeature extends FeatureExtractor {

    private int maxGap;
    private boolean sort;

    public LinkContextFeature(int maxGap, boolean sort) {
        this.maxGap = maxGap;

        ImmutableList.Builder<String> builder = ImmutableList.builder();
        builder.add("unique_linked_articles",
                "old_unigrams",
                "new_unigrams",
                "context_unigrams",
                "link_context_uni_delta",
                "link_context_uni_ratio");

        for (int g = 0; g <= maxGap; g++) {
            for (String set : ImmutableList.of("old", "new", "context")) {
                builder.add(set + "_big" + g);
            }
            builder.add("link_context_big" + g + "_delta");
            builder.add("link_context_big" + g + "_ratio");
            for (float lambda = 0.1f; lambda < 1 ; lambda += 0.1f) {
                builder.add("link_context_big" + g + "_l"+lambda+"_delta");
                builder.add("link_context_big" + g + "_l"+lambda+"_ratio");
            }
        }

        _attributes = attributesFromStrings(builder.build());
    }

    private List<String> getContextTokens(Edit edit, Instance instance) {
        Set<String> links = newHashSet();
        links.addAll(MediawikiTokenizer.extractLinks(edit.getOldRevision().
                getText()));
        links.addAll(MediawikiTokenizer.extractLinks(edit.getNewRevision().
                getText()));

        // Let's GC do his work.
        edit = null;

        if (links.isEmpty()) {
            return ImmutableList.of();
        }

        MongoBackend mb;
        try {
            mb = MongoBackend.getInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return ImmutableList.of();
        }

        int tokenEstimation = (int) 0.9f
                * ((links.size() < 600) ? links.size() : 600)
                * 9000;

        List<String> contextTokens = newArrayListWithExpectedSize(
                tokenEstimation);
        Set<Integer> pageIds = newHashSetWithExpectedSize(links.size());
        int pageIdsEffectiveCount = 0;
        int tokenRealCount = 0;
        for (String link : links) {
            DBObject article = mb.getArticle(link, true);
            if (article == null) {
                continue;
            }
            Integer pageId = (Integer) article.get("_id");
            if (pageIds.contains(pageId)) {
                continue;
            }

            pageIds.add(pageId);
            List<String> tokens = (List<String>) article.get("tokens");

            tokenRealCount += tokens.size();

            if (contextTokens.size() + tokens.size() > 2000000) {
                continue;
            }

            pageIdsEffectiveCount++;
            contextTokens.addAll(tokens);
        }

        //FIXME: this shouldn't be here, but hey!
        instance.setValue(attribute(0), pageIds.size());

        return contextTokens;
    }

    private final static ImmutableList<String> KEYS = ImmutableList.of("old", "new", "context");

    private static <T> void filterProbabilityDistribution(List<ImmutableList<String>> grams,
            Set<ImmutableList<String>> vocab,
            MLEDiscreteProbabilityDistribution<ImmutableList<String>> contextDist) {

        ImmutableList<String> emptyList = ImmutableList.of();
        for (ImmutableList<String> gram: grams) {
            if (vocab.contains(gram)) {
                contextDist.add(gram);
            } else {
                contextDist.add(emptyList);
            }
        }
    }

    @Override
    public Instance extract(Edit edit) {
        Instance instance = newDenseInstance();
        int a = 1;

        List<String> contextTokens = getContextTokens(edit, instance);

        if (contextTokens.isEmpty()) {
            return instance;
        }

        ImmutableMap<String, List<String>> tokensLists = ImmutableMap.of(
                "old", edit.getOldRevision().getLowerTokens(),
                "new", edit.getNewRevision().getLowerTokens(),
                "context", contextTokens);

        edit = null;
        contextTokens = null;

        Map<String,MLEDiscreteProbabilityDistribution<String>> unigramDistsMap = newHashMap();
        Map<String,List<ImmutableList<String>>> bigramsMap = newHashMap();

        for (String key: KEYS) {
            
            List<String> tokens = tokensLists.get(key);
            MLEDiscreteProbabilityDistribution<String> dist = MLEDiscreteProbabilityDistribution.create(tokens);
            unigramDistsMap.put(key, dist);
            bigramsMap.put(key, NGramUtils.getNGramsList(tokens, 2));

            instance.setValue(attribute(a), dist.events().size());
            a++;
        }
        tokensLists = null;

        // link_context_unigram_{delta,ratio}
        float oldKld = unigramDistsMap.get("old").kld(unigramDistsMap.get("context"));
        float newKld = unigramDistsMap.get("new").kld(unigramDistsMap.get("context"));
        instance.setValue(attribute(a), oldKld - newKld);
        a++;
        instance.setValue(attribute(a), oldKld / newKld);
        a++;

        Map<String,MLEDiscreteProbabilityDistribution<ImmutableList<String>>> probs = newHashMap();
        for (String key: ImmutableList.of("old", "new")) {
            probs.put(key, MLEDiscreteProbabilityDistribution.create(bigramsMap.get(key)));
        }

        Set<ImmutableList<String>> vocab = union(probs.get("old").events(),
                probs.get("new").events());
        MLEDiscreteProbabilityDistribution<ImmutableList<String>> bigramContextDist = MLEDiscreteProbabilityDistribution.create();
        filterProbabilityDistribution(bigramsMap.get("context"), vocab,
                bigramContextDist);
        probs.put("context", bigramContextDist);

        BigramBackoffModel lm = new BigramBackoffModel(unigramDistsMap.get("context"), probs.get("context"));

        for (int g = 0; g <= maxGap; g++) {

            // link_context_big*_{delta,ratio}
            instance.setValue(attribute(a), probs.get("old").events().size());
            a++;
            instance.setValue(attribute(a), probs.get("new").events().size());
            a++;
            instance.setValue(attribute(a), bigramContextDist.events().size());
            a++;

            oldKld = probs.get("old").kld(bigramContextDist);
            newKld = probs.get("new").kld(bigramContextDist);
            instance.setValue(attribute(a), oldKld - newKld);
            a++;
            instance.setValue(attribute(a), oldKld / newKld);
            a++;

            for (float lambda = 0.1f; lambda < 1; lambda += 0.1f) {
                lm.setLambda(lambda);
                oldKld = probs.get("old").kld(lm);
                newKld = probs.get("new").kld(lm);
                instance.setValue(attribute(a), oldKld - newKld);
                a++;
                instance.setValue(attribute(a), oldKld / newKld);
                a++;
            }

            if (g < maxGap) {
                for (String key: KEYS) {
                    bigramsMap.put(key, NGramUtils.bigramSkipper(bigramsMap.get(key)));
                }
                for (String key: ImmutableList.of("old", "new")) {
                    probs.get(key).addAll(bigramsMap.get(key));
                }
                vocab = union(probs.get("old").events(), probs.get("new").events());
                filterProbabilityDistribution(bigramsMap.get("context"), vocab,
                bigramContextDist);

            }
        }

        return instance;
    }



}
