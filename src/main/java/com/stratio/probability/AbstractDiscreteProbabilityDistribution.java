/*
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.probability;

/**
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public abstract class AbstractDiscreteProbabilityDistribution<T> implements DiscreteProbabilityDistribution<T> {

    @Override
    public float kld(DiscreteProbabilityDistribution<T> probDist) {
        float kld = 0f;

        for (T event: events()) {
            float p = prob(event);

            if (p == 0) {
                continue;
            }

            float q = probDist.prob(event);

            if (q == 0) {
                q = probDist.epsilon();
            }

            kld += p * Math.log(p / q);
        }

        return kld;
    }

}
