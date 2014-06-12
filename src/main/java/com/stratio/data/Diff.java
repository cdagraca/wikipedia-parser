/**
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.data;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class Diff<T> {

    public static class Unit {
        private Integer a, b;

        public Unit(Integer a, Integer b) {
            this.a = a;
            this.b = b;
        }

        public Integer getA() {
            return a;
        }

        public Integer getB() {
            return b;
        }
    }

    private List<Diff.Unit> indices;

    public Diff() {
        indices = new ArrayList<Diff.Unit>();
    }

    public void addUnit(int a, int b) {
        indices.add(new Unit(a, b));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Diff:\n");
        for (Unit unit: indices) {
            sb.append(unit.a).append(",").append(unit.b).append("\n");
        }
        return sb.toString();
    }
}
