/*
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.features;

import com.google.common.collect.ImmutableList;
import com.stratio.data.Edit;
import weka.core.Attribute;
import weka.core.Instance;
import com.stratio.data.Edit;

/**
 * Wether the edit is vandalism or not.
 *
 * TODO: This does not handle missing vandalism tag yet.
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class VandalismFeature extends FeatureExtractor {

    public VandalismFeature() {
        _attributes =
            ImmutableList.of(
                new Attribute("vandalism", ImmutableList.of("yes", "no"), 0));
    }

    @Override
    public Instance extract(Edit edit) {
        Instance instance = newDenseInstance();
        instance.setValue(attribute(0),
                edit.getTags().contains("vandalism")? "yes" : "no");
        return instance;
    }

}