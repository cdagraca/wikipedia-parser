/**
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.lm;

import com.google.common.collect.ImmutableList;
import java.util.Set;
import com.stratio.probability.AbstractDiscreteProbabilityDistribution;
import com.stratio.probability.DiscreteProbabilityDistribution;

/**
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class BigramBackoffModel extends AbstractDiscreteProbabilityDistribution<ImmutableList<String>> {

    DiscreteProbabilityDistribution<String> unigrams;
    DiscreteProbabilityDistribution<ImmutableList<String>> bigrams;
    float lambda;

    public BigramBackoffModel(DiscreteProbabilityDistribution<String> unigrams,
            DiscreteProbabilityDistribution<ImmutableList<String>> bigrams) {
        this.unigrams = unigrams;
        this.bigrams = bigrams;
        this.lambda = 1;
    }

    public void setLambda(float lambda) {
        this.lambda = lambda;
    }

    @Override
    public float prob(ImmutableList<String> event) {
        return lambda*bigrams.prob(event) +
                (1-lambda)*unigrams.prob(event.get(0))*unigrams.prob(event.get(1));
    }

    @Override
    public Set<ImmutableList<String>> events() {
        return bigrams.events();
    }

    @Override
    public float epsilon() {
        return bigrams.epsilon();
    }

}
