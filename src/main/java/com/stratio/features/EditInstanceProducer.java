/*
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.features;

import static com.google.common.collect.Lists.*;
import com.google.common.collect.ImmutableList;
import java.util.List;

import com.stratio.data.Edit;
import com.stratio.data.EditStream;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.gui.streams.InstanceListener;
import weka.gui.streams.InstanceProducer;
import com.stratio.data.Edit;
import com.stratio.data.EditStream;

/**
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class EditInstanceProducer implements InstanceProducer {

    private final Instances instances;
    private final FeatureExtractor featureExtractor;
    private final EditStream editStream;

    public EditInstanceProducer(EditStream editStream, FeatureExtractor featureExtractor) {
        this.featureExtractor = featureExtractor;
        this.editStream = editStream;
        instances = new Instances("FOO", newArrayList(featureExtractor.attributes()), 0); //TODO: Substitute by List
    }

    @Override
    public void addInstanceListener(InstanceListener il) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeInstanceListener(InstanceListener il) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Instances outputFormat() throws Exception {
        return new Instances(instances);
    }

    @Override
    public Instance outputPeek() throws Exception {
        final Edit edit = editStream.nextEdit();
        return (edit == null)? null : featureExtractor.extract(edit);
    }

}
