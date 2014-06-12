/*
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.features;

import com.google.common.collect.ImmutableList;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import com.stratio.data.Edit;

/**
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public abstract class FeatureExtractor {

    protected ImmutableList<Attribute> _attributes;

    public ImmutableList<Attribute> attributes() {
        return _attributes;
    }

    public abstract Instance extract(Edit edit);

    protected DenseInstance newDenseInstance() {
        return new DenseInstance(attributes().size());
    }

    protected static ImmutableList<Attribute> attributesFromStrings(ImmutableList<String> attrs) {
        ImmutableList.Builder<Attribute> builder = ImmutableList.builder();
        int index = -1;
        for (String name: attrs) {
            index++;
            builder.add(new Attribute(name, index));
        }
        return builder.build();
    }

    public Attribute attribute(int i) {
        return attributes().get(i);
    }

}
