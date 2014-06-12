/*
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.parsers;

import com.google.common.base.CharMatcher;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Lists.*;
import static com.google.common.collect.Sets.*;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tokenizes the source of a MediaWiki revision preserving punctuation marks
 * and wiki markup.
 *
 * A token is constituted by a word, punctuation or markup elements.
 *
 * <ref name="NAME"/> and similar are normalized to <ref name=>.
 *
 * Replaces formulas insice <math> with __MATH__.
 *
 * TODO: Properly document tokenizing behaviour.
 * TODO: &nbsp;
 * TODO: doi=10.1080/07393140701510160
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class MediawikiTokenizer {

    private static final CharMatcher _bracket_chars = CharMatcher.anyOf("{}[]").precomputed();
    private static final CharMatcher _single_chars = CharMatcher.anyOf("/⁄–—:;\"«»|()*,&%#…").precomputed(); // Plus CURRENCY_SYMBOL and MATH_SYMBOL
    private static final CharMatcher _multi_chars = CharMatcher.anyOf("<>'.?!¿¡=‘’“”").precomputed();
    private static final CharMatcher _valid_word_inner_chars = CharMatcher.anyOf("_-&.@+").precomputed();
    private static final Pattern _url_pattern =
            Pattern.compile("(https?|ftp)://[\\w]+[-\\w.]*(?:/[^ \t\r\n\\]}]*)?");
    private static final Pattern _link_pattern =
            Pattern.compile("\\[{2}([^\\]|]*+)(|[^\\]]*+)?\\]{2}");
    private static final Pattern _interwiki_candidate_pattern =
            Pattern.compile("^[a-z]{2}:.*");

    private static boolean _isLetterOrDigit(char ch) {
        return _isLetterOrDigit(Character.getType(ch));
    }

    private static boolean _isLetterOrDigit(int charType) {
        return _isLetter(charType) ||
                charType == Character.DECIMAL_DIGIT_NUMBER
                ;
    }

    private static boolean _isLetter(int charType) {
        return charType == Character.UPPERCASE_LETTER || charType
                == Character.LOWERCASE_LETTER || charType
                == Character.TITLECASE_LETTER || charType
                == Character.MODIFIER_LETTER || charType
                == Character.OTHER_LETTER || charType
                == Character.NON_SPACING_MARK || // Needed, at least, for Devanagari chars
                charType == Character.COMBINING_SPACING_MARK // Needed for Devanagri, Malayalam chars and others
                ;
    }

    /**
     * Tokenizes the input text and returns a list of tokens.
     *
     * Converts URLs to a special token <code>__URL__</code>.
     *
     * @param input Input text to be tokenized.
     * @return A List<String> containing the tokens.
     */
    public static List<String> getTokens(String input) {

        input = checkNotNull(input);

        /**
         * ArrayList holding tokens. We will set initialCapacity
         * to prevent, whenever possible, resizing and array copying.
         * We estimate an upper bound of number of tokens based on
         * the revision length. A quick and dirty estimation showed
         * that there is an average of chars-per-token around 5. We
         * will use 4 to be on the safe side.
         */
        List<String> tokens = newArrayListWithExpectedSize((input.length() + 4) / 4);

        for (int i = 0; i < input.length(); i++) {

            char iChar = input.charAt(i);

            /*
             * 1. Discard whitespaces.
             */
            if (Character.isWhitespace(iChar)) {
                continue;
            }

            int iCharType = Character.getType(iChar);

            /*
             * 2. A valid letter or digit, this looks like the beggining of a word.
             */
            if (_isLetterOrDigit(iCharType)) {

                /*
                 * 2.1. If this starts with 'h' or 'f', check if it is an URL.
                 *
                 * FIXME: Add more protocols? (gopher:// has a marginal use).
                 */
                if (iChar == 'h' || iChar == 'f') {
                    if (input.startsWith("http://", i) || input.startsWith(
                            "https://", i) || input.startsWith("ftp://", i)) {
                        int k = i + 5;
                        while (k < input.length() && !Character.isWhitespace(input.
                                charAt(k)) && input.charAt(k) != ']' && input.
                                charAt(k) != '}') {
                            i = k;
                            k++;
                        }

                        tokens.add("__URL__");
                        continue;
                    }
                }

                /*
                 * 2.2. Or we have a regular word (not URL)...
                 */
                int k = i + 1;
                while (k < input.length() && (_isLetterOrDigit(input.charAt(k)) || _valid_word_inner_chars.
                        matches(input.charAt(k)))) {
                    k++;
                }
                k--;
                if (Character.getType(input.charAt(k))
                        == Character.OTHER_PUNCTUATION) {
                    k--;
                }

                String token = input.substring(i, k + 1);
                if (CharMatcher.DIGIT.matchesAllOf(token)) {
                    token = "__NUM" + token.length() + "__";
                }
                tokens.add(token);
                i = k;
                continue;
            }

            /*
             * 3. This is punctuation or wikimarkup
             */

            /*
             * 3.1. This is a single or pair of brackets ([]{}).
             */
            if (_bracket_chars.matches(iChar)) {
                if (i < input.length() - 1 && input.charAt(i + 1) == iChar) {
                    tokens.add(input.substring(i, i + 2));
                    i++;
                } else {
                    tokens.add(input.substring(i, i + 1));
                }
                continue;
            }

            /*
             * 3.2. This is an XML-like tag or a comment.
             *
             * We should expect <!--, -->, <ref>, <nowiki>, <blockquote>, etc.
             */
            if (iChar == '<') {
                if (input.startsWith("!--", i + 1)) {
                    tokens.add("<!--");
                    i += 3;
                    continue;
                }

                if (input.startsWith("ref name=", i + 1)) {
                    int close = input.indexOf('>', i + 10);
                    //XXX: If close is -1, revision is broken!
                    if (close == -1) {
                        tokens.add("<");
                        continue;
                    }

                    tokens.add("<ref name=>");
                    i = close;
                    continue;
                }

                if (input.startsWith("math>", i + 1)) {
                    int close = input.indexOf("</math>", i + 4);
                    //XXX: If close is -1, then this revision is broken! In that case,
                    //     we try to tokenize the rest as usual.
                    if (close != -1) {
                        tokens.add("<math>");
                        tokens.add("__MATH__");
                        tokens.add("</math>");
                        i = close + 6;
                        continue;
                    }
                }

                int k = i + 1;
                int close = 0;
                while (k < i + 10 && k < input.length()) {
                    if (Character.isWhitespace(input.charAt(k))) { //BROKEN
                        break;
                    }
                    if (input.charAt(k) == '>') {
                        close = k;
                        break;
                    }
                    k++;
                }
                if (close > 0) {
                    tokens.add(input.substring(i, k + 1));
                    i = k;
                    continue;
                }
            }

            /*
             * This is a closing comment.
             */
            if (input.startsWith("-->", i)) {
                tokens.add("-->");
                i += 2;
                continue;
            }

            /*
             * 3.3. This is a single special char.
             */
            if (_single_chars.matches(iChar)) {
                tokens.add(input.substring(i, i + 1));
                continue;
            }

            if (iCharType == Character.CURRENCY_SYMBOL || iCharType
                    == Character.MATH_SYMBOL) {
                tokens.add(input.substring(i, i + 1));
                continue;
            }

            /*
             * 3.4. This is a multi special char.
             */
            if (_multi_chars.matches(iChar)) {
                int k = i + 1;
                while (k < input.length() - 1 && input.charAt(k) == iChar) {
                    k++;
                }
                tokens.add(input.substring(i, k));
                i = k - 1;
                continue;
            }
        }

        return tokens;
    }

    /**
     * Extracts URLs in the source of a Mediawiki page.
     *
     * This uses a fast regular expression rather than a correct one.
     *
     * @param Source text.
     * @return A set of all the URLs in the page.
     */
    public static Set<String> extractURLs(String text) {
        Set<String> urls = newHashSet();
        Matcher m = _url_pattern.matcher(text);
        while (m.find()) {
            urls.add(m.group());
        }
        return urls;
    }
    
    /**
     * Extracts wikilinks in the source of a Mediawiki page.
     *
     * @param Source text.
     * @return A set of all the URLs in the page.
     */
    public static Set<String> extractLinks(String text) {
        Set<String> links = newHashSet();
        Matcher m = _link_pattern.matcher(text);
        while (m.find()) {
            String link = m.group(1);

            // Discard interwiki candidates.
            if (_interwiki_candidate_pattern.matcher(link).matches()) {
                continue;
            }

            links.add(link);
        }
        return links;
    }


    public static void main(String args[]) {
        final String txt = "{{Infobox Archbishop  \n| honorific-prefix = \n| name = Thomas I of York\n| honorific-suffix = \n| archbishop_of = [[Archbishop of York]]\n| image = Acrdwnch.jpg\n| imagesize = 220px\n| alt = Bottom of a manuscript with several signatures below a block of handwritten text.\n| caption = The [[Accord of Winchester]], 1072. Thomas' signature is on the right, next to Lanfranc's.\n| province = \n| diocese = [[Diocese of York]]\nn| enthroned = unknown\n| ended = 18 November 1100\n| predecessor = [[Ealdred (archbishop)|Ealdred]]\n| successor = [[Gerard (Archbishop of York)|Gerard]]\n| ordination = \n| consecration = 23 May 1070\n| other_post = \n| birth_name = Thomas\n| birth_date = \n| birth_place = \n| death_date=18 November 1100\n| death_place=[[York]]\n| buried = [[York Minster]]\n}}\n\n'''Thomas of Bayeux''' (died {{Nowrap|18 November}} 1100) was [[Archbishop of York]] from 1070 until 1100. A native of [[Bayeux]], he was educated at [[Liège (city)|Liège]] and became a royal chaplain to Duke William of Normandy, later King [[William&nbsp;I of England]]. After the [[Norman conquest of England|Norman Conquest]], the King nominated Thomas to succeed [[Ealdred (archbishop)|Ealdred]] as Archbishop of York. After Thomas' election, [[Lanfranc]], [[Archbishop of Canterbury]], demanded an oath from Thomas to obey him and any future Archbishops of Canterbury; this was part of Lanfranc's claim that Canterbury was the primary bishopric, and its holder the head of the English Church. Thomas countered that York had never made such an oath.  As a result, Lanfranc refused to consecrate him. The King eventually persuaded Thomas to submit, but Thomas and Lanfranc continued to clash over ecclesiastical issues, including the primacy of Canterbury, which dioceses belonged to the province of York, and the question of how York's obedience to Canterbury would be expressed. \n\nAfter King William I's death Thomas served his successor, [[William II of England|William&nbsp;II]], and helped to put down a rebellion led by Thomas' old mentor [[Odo of Bayeux]]. Thomas also attended the trial for rebellion of the [[Bishop of Durham]], [[William de St-Calais]], Thomas' sole [[Suffragan bishop|suffragan]], or bishop subordinate to York. During William II's reign Thomas once more became involved in the dispute with Canterbury over the primacy when he refused to consecrate the new Archbishop of Canterbury, [[Anselm of Canterbury|Anselm]], if Anselm was named the [[Primate of England]] in the consecration service. After William II's sudden death in 1100, Thomas arrived too late to crown King [[Henry I of England|Henry&nbsp;I]], and died soon after the coronation.\n\n== Early life==\nThomas is sometimes referred to as Thomas&nbsp;I to distinguish the elder Thomas from his nephew [[Thomas II of York|Thomas]], who was also an Archbishop of York. The elder Thomas' father was a priest<ref name=Rufus198>[[Frank Barlow (historian)|Barlow]], ''William Rufus'' pp. 198–199</ref> named Osbert; his mother was named Muriel, but little else of them is known.<ref name=Douglas129>Douglas ''William the Conqueror'' p. 129</ref> He had a brother named [[Samson (Bishop of Worcester)|Sampson]], who was [[Bishop of Worcester]] from 1086 until 1112.<ref name=Douglas129/> and [http://google.com] {{Foo url=ftp://ftp.heya.com/~foo?&bar=%20do.html}} [[foo]] and [[bar|jarl]] and [damn]... 1wor.d9 soli⁄dus$";

        for (int i = 0; i < 1000; i++) {
            MediawikiTokenizer.getTokens(txt);
        }

        long newtime = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++) {
            MediawikiTokenizer.getTokens(txt);
        }
        newtime = System.currentTimeMillis() - newtime;

        System.out.println("newtime: " + newtime);

        int ntokens = MediawikiTokenizer.getTokens(txt).size();
        long basetime = System.currentTimeMillis();
        for (int i = 0; i < 5000; i++) {
            List<String> arr = newArrayListWithCapacity(ntokens);
            for (int j = 0; j < ntokens; j++) {
                arr.add("test");
            }
        }
        basetime = System.currentTimeMillis() - basetime;

        System.out.println("basetime: " + basetime);

        List<String> tokens = MediawikiTokenizer.getTokens(txt);
        for (String token : tokens) {
            System.out.println(token);
        }

        System.out.println("Tokens: " + tokens.size() + " Length str: " + txt.
                length());

    }
}
