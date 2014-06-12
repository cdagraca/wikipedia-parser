/*
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.features;

import com.google.common.base.Charsets;
import static com.google.common.collect.Lists.*;
import static com.google.common.collect.Maps.*;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import weka.core.Attribute;
import weka.core.Instance;
import com.stratio.data.Edit;

/**
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class WordListFeature extends FeatureExtractor {

    private List<String> categories;
    private Map<String,List<String>> lists;
    private int attIndex;

    public WordListFeature() {
        categories = newArrayList();
        lists = newHashMap();
        _attributes = ImmutableList.of();
        attIndex = 0;
    }

    public void addList(String category, File file) throws IOException {
        addList(category, Files.readLines(file, Charsets.UTF_8));
    }

    public void addList(String category, List<String> list) {
        categories.add(category);
        lists.put(category, list);
        _attributes = ImmutableList.copyOf(
                Iterables.concat(attributes(),
                ImmutableList.of(
                new Attribute(category + "_increment", attIndex),
                new Attribute(category + "_impact", attIndex + 1)
                )));
        attIndex += 2;
    }

    @Override
    public Instance extract(Edit edit) {
        Multiset<String> oldCounts = HashMultiset.create();
        Multiset<String> newCounts = HashMultiset.create();

        for (Multiset.Entry<String> entry: edit.getOldTokenCount().entrySet()) {
            for (String category: categories) {
                if (lists.get(category).contains(entry.getElement())) {
                    oldCounts.add(category, entry.getCount());
                }
            }
        }
        
        for (Multiset.Entry<String> entry: edit.getNewTokenCount().entrySet()) {
            for (String category: categories) {
                if (lists.get(category).contains(entry.getElement())) {
                    newCounts.add(category, entry.getCount());
                }
            }
        }

        Instance instance = newDenseInstance();
        for (int i = 0; i < categories.size(); i++) {
            instance.setValue(attribute(i*2),
                    oldCounts.count(categories.get(i))
                    -
                    newCounts.count(categories.get(i))
                    ); // _increment
            instance.setValue(attribute(i*2+1),
                    ((float)oldCounts.count(categories.get(i)) - newCounts.count(categories.get(i)))
                    /
                    oldCounts.count(categories.get(i))
                    ); // _impact

        }

        return instance;
    }



}
