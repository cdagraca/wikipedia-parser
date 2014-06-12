/**
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.data;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;

import java.util.Date;
import java.util.List;
import java.util.Map;
import com.stratio.parsers.MediawikiTokenizer;

public class Revision {

    private Integer id;
    private Date timestamp;

    public Date getTimestamp() {
        return timestamp;
    }

    private Contributor contributor;
    private Boolean isMinor;
    private Page page;
    private String text;
    private ImmutableList<String> tokens;
    private ImmutableList<String> lowerTokens;
    private String redirection;

    public Revision(int id, String text) {
        this(id, null, text, null, false, null);
    }

    public Revision(int id, String text, Contributor contributor) {
        this(id, null, text, contributor, false, null);
    }

    public Revision(int id, Date timestamp,  String text, Contributor contributor, boolean isMinor, Page page) {
        this.id = id;
        this.timestamp = timestamp;
        this.text = checkNotNull(text);
        this.contributor = contributor;
        this.isMinor = isMinor;
        this.page = page;
    }

    public Integer getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public Contributor getContributor() {
        return contributor;
    }

    public Boolean isMinor() {
        return isMinor;
    }

    public Page getPage() {
        return page;
    }

    public ImmutableList<String> getTokens() {
        if (tokens == null) {
            tokens = ImmutableList.copyOf(MediawikiTokenizer.getTokens(text));
        }
        return tokens;
    }

    public ImmutableList<String> getLowerTokens() {
        if (lowerTokens == null) {
            ImmutableList.Builder<String> builder = ImmutableList.builder();
            for (String token: getTokens()) {
                builder.add(token.toLowerCase());
            }
            lowerTokens = builder.build();
        }
        return lowerTokens;
    }

    public ImmutableMultiset<String> getTokenCount() {
        return ImmutableMultiset.copyOf(getTokens());
    }

    public ImmutableMultiset<String> getLowerTokenCount() {
        return ImmutableMultiset.copyOf(getLowerTokens());
    }

    /**
     * Gets the title of the article this revision redirects to.
     *
     * @return Redirect title. If there is no redirection, returns null.
     */
    public String getRedirection() {
        if (redirection == null) {
            String redirectionCommand = null;
            
            if (getText().contains("#REDIRECT")) {
                redirectionCommand = "#REDIRECT";
            } else if (getText().contains(("#redirect"))) {
                redirectionCommand = "#redirect";
            }

            if (redirectionCommand != null) {
                redirection = getText()
                        .replace(redirectionCommand, "")
                        .trim()
                        .replace("[[", "");
                redirection = redirection.substring(0, redirection.indexOf("]]"));
            } else {
                redirection = "";
            }
        }

        return (redirection.equals(""))? null : redirection;
    }
    private String nlv(Object o){
        if (o == null)
            return "";
        else
            return o.toString();
    }
    public String[] csvEntry(){
        return new String[]{
                nlv(id),
                nlv(page.getId()),
                nlv(page.getNamespace()),
                nlv(page.getFullTitle()),
                nlv(page.getTitle()),
                nlv(page.getRestrictions()),
                nlv(page.isRedirect()),
                nlv(contributor.getId()),
                nlv(contributor.getUsername()),
                nlv(contributor.getIsAnonymous()),
                nlv(isMinor),
                nlv(tokens),
                nlv(lowerTokens),
                nlv(redirection),
                nlv(text).replaceAll("[\\n\\t]", ""),
        };
    }

    @Override
    public String toString() {
        return "Revision{" +
                "id=" + id +
                ", contributor=" + contributor +
                ", isMinor=" + isMinor +
                ", page=" + page +
                ", text='" + text.replaceAll("[\\n\\t]", "")+ '\'' +
                ", tokens=" + tokens +
                ", lowerTokens=" + lowerTokens +
                ", redirection='" + redirection + '\'' +
                '}';
    }
}
