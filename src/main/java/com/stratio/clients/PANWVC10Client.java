/**
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.clients;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import weka.core.Attribute;
import weka.core.Instance;
import weka.gui.streams.InstanceProducer;
import com.stratio.data.Edit;
import com.stratio.data.EditStream;
import com.stratio.features.CharacterTypeRatioFeature;
import com.stratio.features.EditInstanceProducer;
import com.stratio.features.FeatureExtractor;
import com.stratio.features.GroupFeature;
import com.stratio.features.IDFeature;
import com.stratio.features.LinkContextFeature;
import com.stratio.features.SizeDeltaFeature;
import com.stratio.features.SizeRatioFeature;
import com.stratio.features.TokenDeltaFeature;
import com.stratio.features.TokenRatioFeature;
import com.stratio.features.VandalismFeature;
import com.stratio.features.WordListFeature;
import com.stratio.parsers.PANWVC10Parser;

/**
 *
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class PANWVC10Client {

    public static void main(String args[]) throws IOException, Exception {
        //WordListFeature lists = new WordListFeature();
        /*/
        FeatureExtractor fe = new GroupFeature(ImmutableList.of(
                new IDFeature(),
                new SizeRatioFeature(),
                new SizeDeltaFeature(),
                new TokenRatioFeature(),
                new TokenDeltaFeature(),
                new CharacterTypeRatioFeature(),
                //lists,
                new VandalismFeature()
                ));
        /*/
        FeatureExtractor fe = new GroupFeature(ImmutableList.of(
                new LinkContextFeature(3, false),
                new VandalismFeature()
                ));

        System.out.println("@relation FOO");
        for (Attribute att: fe.attributes()) {
            System.out.println(att.toString());
        }
        System.out.println("@data");
        EditStream es = new PANWVC10Parser("/dev/shm/pan-wvc-10").getEditStream();

        /*/
        Edit edit;
        while ((edit = es.nextEdit()) != null) {
            System.out.println(fe.extract(edit).toString());
        }
        /*/
        
        InstanceProducer instanceProducer = new EditInstanceProducer(es, fe);
        Instance instance;
        while ((instance = instanceProducer.outputPeek()) != null) {
            System.out.println(instance.toString());
        }
    }

}
