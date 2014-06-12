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
 * (|E_{old} tokens| + 1) / (|E_{new} tokens| + 1)
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class TokenRatioFeature extends FeatureExtractor {
    
    public TokenRatioFeature() {
        _attributes = attributesFromStrings(ImmutableList.of("token_ratio"));
    }

    @Override
    public Instance extract(Edit edit) {
        Instance instance = newDenseInstance();
        instance.setValue(attribute(0),
                (edit.getOldRevision().getTokens().size() + 1f)
                /
                (edit.getNewRevision().getTokens().size() + 1f)
                );
        return instance;
    }

}
