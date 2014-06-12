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
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class IDFeature extends FeatureExtractor {
    
    public IDFeature() {
        _attributes =
            attributesFromStrings(ImmutableList.of("oldid", "newid"));
    }

    @Override
    public Instance extract(Edit edit) {
        Instance instance = newDenseInstance();
        instance.setValue(attribute(0), edit.getOldRevision().getId());
        instance.setValue(attribute(1), edit.getNewRevision().getId());
        return instance;
    }

}
