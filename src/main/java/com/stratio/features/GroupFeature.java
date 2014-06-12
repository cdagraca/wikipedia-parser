/*
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.features;

import static com.google.common.collect.Lists.*;
import com.google.common.collect.ImmutableList;
import java.util.List;

import com.stratio.data.Edit;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import com.stratio.data.Edit;

/**
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class GroupFeature extends FeatureExtractor {

    private ImmutableList<FeatureExtractor> features;
    private Instances _instances;

    public GroupFeature(List<FeatureExtractor> features) {
        this.features = ImmutableList.copyOf(features);

        ImmutableList.Builder<Attribute> result = ImmutableList.builder();
        for (FeatureExtractor fe: this.features) {
            for (Attribute att: fe.attributes()) {
                result.add((Attribute)att.copy());
            }
        }
        _attributes = result.build();

        _instances = new Instances("FOO", newArrayList(_attributes), 0);
        result = ImmutableList.builder();
        for (int i = 0; i < _instances.numAttributes(); i++) {
            result.add(_instances.attribute(i));
        }
        _attributes = result.build();
    }

    @Override
    public Instance extract(Edit edit) {
        Instance instance = newDenseInstance();
        int index = 0;
        for (FeatureExtractor fe: features) {
            for (Double value: fe.extract(edit).toDoubleArray()) {
                instance.setValue(index, value);
                index++;
            }
        }
        instance.setDataset(_instances);
        return instance;
    }

}
