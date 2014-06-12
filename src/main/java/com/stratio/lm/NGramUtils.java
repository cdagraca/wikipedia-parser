/**
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.lm;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import java.util.List;

/**
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class NGramUtils {

    public static <T> List<ImmutableList<T>> getNGramsList(List<T> text, int n) {
        checkArgument(n > 0, "n must be greater than 0");

        int size = text.size() - n + 1;
        if (size < 1) {
            return newArrayListWithCapacity(0);
        }

        List<ImmutableList<T>> ngrams = newArrayListWithCapacity(size);
        for (int i = 0; i < size; i++) {
            ngrams.add(ImmutableList.copyOf(text.subList(i, i + n)));
        }

        return ngrams;
    }


    public static <T> List<ImmutableList<T>> bigramSkipper(List<ImmutableList<T>> bigrams) {

        if (bigrams.size() < 2) {
            return ImmutableList.of();
        }

        List<ImmutableList<T>> newBigrams = newArrayListWithCapacity(bigrams.size() - 1);

        for (int i = 0; i < bigrams.size() - 1; i++) {
            newBigrams.add(ImmutableList.of(
                    bigrams.get(i).get(0), bigrams.get(i+1).get(1)));
        }

        return newBigrams;
    }

    /**
     * This class streams ngrams from a text. This implements the Iterable interface.
     *
     * WARNING: NGrams are computed on the fly by the iterator. Use this if you
     * need to iterate only once. Otherwise, use @link{NGramUtils.getNGramsList}.
     *
     * @param <T> Type for the elements of ngrams.
     */
    public static class NGramsStream<T> implements Iterable<ImmutableList<T>> {

        private final int n;
        private final List<T> text;

        protected NGramsStream(List<T> text, int n) {
            checkArgument(n > 0, "n must be greater than 0");

            this.text = text;
            this.n = n;
        }

        @Override
        public UnmodifiableIterator<ImmutableList<T>> iterator() {
            return new NGramsStreamIterator<T>(text, n);
        }

        private static class NGramsStreamIterator<T> extends UnmodifiableIterator<ImmutableList<T>> {

            private final int n;
            private final List<T> text;
            private int index;
            private int size;

            protected NGramsStreamIterator(List<T> text, int n) {
                checkArgument(n > 0, "n must be greater than 0");
                
                this.text = text;
                this.n = n;
                this.index = 0;
                this.size = text.size() - n + 1;
            }

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public ImmutableList<T> next() {
                ImmutableList result = ImmutableList.copyOf(text.subList(index, index + n));
                index++;
                return result;
            }
        }

    }

    public static <T> Iterable<ImmutableList<T>> getNGramsStream(List<T> text, int n) {
        return new NGramsStream<T>(text, n);
    }

}
