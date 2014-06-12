/*
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
package com.stratio.probability;

import java.util.Set;

/**
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public interface DiscreteProbabilityDistribution<T> {

    public float prob(T event);
    public Set<T> events();
    public float epsilon();
    public float kld(DiscreteProbabilityDistribution<T> probDist);

}
