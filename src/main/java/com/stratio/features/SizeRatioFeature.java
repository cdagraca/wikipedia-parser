/*
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.features;

import com.google.common.collect.ImmutableList;
import weka.core.Attribute;
import weka.core.Instance;
import com.stratio.data.Edit;

/**
 * ( |E_{old}| + 1 ) / ( |E_{new}| + 1 )
 *
 * Based on (Potthast et al, 2008). Details unclear.
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class SizeRatioFeature extends FeatureExtractor {

    public SizeRatioFeature() {
        _attributes = attributesFromStrings(ImmutableList.of("size_ratio"));
    }

    @Override
    public Instance extract(Edit edit) {
        Instance instance = newDenseInstance();
        instance.setValue(attribute(0),
                (edit.getOldRevision().getText().length() + 1f)
                /
                (edit.getNewRevision().getText().length() + 1f)
                );
        return instance;
    }

}
