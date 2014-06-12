/*
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.features;

import com.google.common.collect.ImmutableList;
import com.stratio.data.Edit;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import com.stratio.data.Edit;

/**
 * |E_{old}| - |E_{new}|
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class SizeDeltaFeature extends FeatureExtractor {
    
    public SizeDeltaFeature() {
        _attributes = attributesFromStrings(ImmutableList.of("size_delta"));
    }

    @Override
    public Instance extract(Edit edit) {
        Instance instance = newDenseInstance();
        instance.setValue(attribute(0),
                edit.getOldRevision().getText().length()
                -
                edit.getNewRevision().getText().length()
                );
        return instance;
    }

}
