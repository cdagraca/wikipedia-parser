/*
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.features;

import static com.google.common.collect.Sets.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import java.util.Set;
import weka.core.Attribute;
import weka.core.Instance;
import com.stratio.data.Edit;

/**
 * Calculates various character ratios:
 *
 * uppercase_ratio:
 *  (1 + |upper|) / (1 + |letter|)
 *  Based on (Potthast et al, 2008).
 *
 * digit_ratio:
 *  (1 + |digit|) / (1 + |all|)
 *
 * nonalphanum_ratio:
 *  (1 + |nonalphanum|) / (1 + |all|)
 *
 * space_ratio: TODO: NOT IMPLEMENTED
 *  (1 + |space|) / (1 + |all|)
 *
 * character_diversity:
 *  |chars|^(1/|different chars|)
 *  this measures how many different characters are in the text.
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class CharacterTypeRatioFeature extends FeatureExtractor {

    public CharacterTypeRatioFeature() {
        _attributes =
            attributesFromStrings(ImmutableList.of(
                "uppercase_ratio",
                "digit_ratio",
                "nonalphanum_ratio",
                //"space_ratio",
                "character_diversity"
                ));
    }

    @Override
    public Instance extract(Edit edit) {
        int letter = 0;
        int upper = 0;
        int digit = 0;
        int nonalphanum = 0;
        int all = 0;
        Set<Character> charSet = newHashSet();

        for (Multiset.Entry<String> entry: edit.getTokenCountMultiset().entrySet()) {
            String token = entry.getElement();
            int co = entry.getCount();

            all += token.length() * co;

            for (int i = 0; i < token.length(); i++) {
                char ch = token.charAt(i);

                charSet.add(ch);

                if (Character.isUpperCase(ch)) {
                    upper += co;
                }

                if (Character.isLetter(ch)) {
                    letter += co;
                }

                if (Character.isDigit(ch)) {
                    digit += co;
                }

                if (!Character.isLetterOrDigit(ch)) {
                    nonalphanum += co;
                }

            }
        }

        Instance instance = newDenseInstance();
        instance.setValue(attribute(0), (upper + 1.0) / (letter + 1.0));
        instance.setValue(attribute(1), (digit + 1.0) / (all + 1.0));
        instance.setValue(attribute(2), (nonalphanum + 1.0) / (all + 1.0));
        instance.setValue(attribute(3), Math.exp(1.0 / (charSet.size() + 1.0)));
        return instance;
    }

}
