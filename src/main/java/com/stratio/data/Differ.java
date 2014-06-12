/**
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.data;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import name.fraser.neil.plaintext.diff_match_patch;
import com.stratio.parsers.MediawikiTokenizer;

/**
 *
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class Differ<T> {

    public static <T> Diff<T> diffMyers(List<T> a, List<T> b) {
        StringBuilder aSb = new StringBuilder();
        StringBuilder bSb = new StringBuilder();
        Map<T, Character> charMap = new HashMap<T, Character>();
        for (T item : a) {
            Character c = charMap.get(item);
            if (c == null) {
                c = (char)(charMap.size() + 1);
                charMap.put(item, c);
            }
            aSb.append(c);
        }
        for (T item : b) {
            Character c = charMap.get(item);
            if (c == null) {
                c = (char)(charMap.size() + 1);
                charMap.put(item, c);
            }
            bSb.append(c); 
        }

        diff_match_patch dmp = new diff_match_patch();
        LinkedList<diff_match_patch.Diff> dmpDiff = dmp.diff_main(aSb.toString(), bSb.toString());

        Diff diff = new Diff();
        int aIdx = 0;
        int bIdx = 0;
        for (diff_match_patch.Diff d: dmpDiff) {
            switch (d.operation) {
                case EQUAL:
                    for (int i = 0; i < d.text.length(); i++) {
                        diff.addUnit(aIdx, bIdx);
                        aIdx++;
                        bIdx++;
                    }
                    break;
                case INSERT:
                    diff.addUnit(-1, bIdx);
                    bIdx++;
                    break;
                case DELETE:
                    diff.addUnit(aIdx, -1);
                    bIdx++;
                    break;
            }
        }

        System.out.println(diff.toString());
        return diff;

    }

    public static void main(String args[]) {
        List<String> a = MediawikiTokenizer.getTokens("A lazy dog. And foo bar baz.");
        List<String> b = MediawikiTokenizer.getTokens("A lazy fox. And xxx bar baz.");
        diffMyers(a, b);
    }

}
