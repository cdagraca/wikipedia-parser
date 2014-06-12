/*
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.features;

import com.google.common.collect.ImmutableList;
import weka.core.Attribute;
import weka.core.Instance;
import com.stratio.data.Edit;

/**
 * |E_{old}| - |E_{new}|
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class TokenDeltaFeature extends FeatureExtractor {

    public TokenDeltaFeature() {
        _attributes = attributesFromStrings(ImmutableList.of("token_delta"));
    }

    @Override
    public Instance extract(Edit edit) {
        Instance instance = newDenseInstance();
        instance.setValue(attribute(0),
                edit.getOldRevision().getTokens().size()
                -
                edit.getNewRevision().getTokens().size()
                );
        return instance;
    }

}
