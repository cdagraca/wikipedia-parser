/*
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.probability;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class MLEDiscreteProbabilityDistribution<T>
        extends AbstractDiscreteProbabilityDistribution<T>
        implements Serializable {

    private static final long serialVersionUID = 7620267394035471971L;

    private Multiset<T> counts;
    private int total;

    public static <T> MLEDiscreteProbabilityDistribution<T> create() {
        MLEDiscreteProbabilityDistribution<T> probDist = new MLEDiscreteProbabilityDistribution<T>();
        probDist.counts = HashMultiset.create();
        return probDist;
    }

    public static <T> MLEDiscreteProbabilityDistribution<T> create(Collection<T> events) {
        MLEDiscreteProbabilityDistribution<T> probDist = new MLEDiscreteProbabilityDistribution<T>();
        probDist.counts = HashMultiset.create(events.size());
        probDist.addAll(events);
        //probDist.counts = HashMultiset.create(events);
        probDist.total = probDist.counts.elementSet().size();
        return probDist;
    }

    public void add(T event) {
        counts.add(event);
        total = counts.elementSet().size();
    }

    public void add(T event, int i) {
        counts.add(event, i);
        total = counts.elementSet().size();
    }

    public void addAll(Collection<T> events) {
        counts.addAll(events);
        total = counts.elementSet().size();
    }

    public int count(T event) {
        return counts.count(event);
    }

    @Override
    public float prob(T event) {
        if (total == 0) {
            return epsilon();
        }
        return ((float)counts.count(event)) / total;
    }

    @Override
    public Set<T> events() {
        return counts.elementSet();
    }

    @Override
    public float epsilon() {
        return 0.1f / total;
    }

}
