/**
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
package com.stratio.data;

import static com.google.common.base.Preconditions.*;

/**
 * A Mediawiki page.
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class Page {

    private String namespace;
    private String title;
    private String fullTitle;
    private Integer id;
    private Boolean isRedirect;
    private String restrictions;
    private Site site;

    public Page(int id, String fullTitle, boolean isRedirect,
            String restrictions, Site site) {
        this(id, checkNotNull(site).namespaceStringFromFullTitle(checkNotNull(fullTitle)),
                site.titleFromFullTitle(fullTitle),
                isRedirect, restrictions, site);
    }

    public Page(int id, String namespace, String title, boolean isRedirect,
            String restrictions, Site site) {
        this.id = id;
        this.namespace = checkNotNull(namespace);
        this.title = checkNotNull(title);
        this.isRedirect = isRedirect;
        this.restrictions = checkNotNull(restrictions);
        this.site = checkNotNull(site);
    }

    public String getNamespace() {
        return namespace;
    }

    public String getTitle() {
        return title;
    }

    public String getFullTitle() {
        if (fullTitle == null) {
            fullTitle = namespace + ":" + title;
        }
        return fullTitle;
    }

    public Integer getId() {
        return id;
    }

    public Boolean isRedirect() {
        return isRedirect;
    }

    public String getRestrictions() {
        return restrictions;
    }

    public Site getSite() {
        return site;
    }

    @Override
    public String toString() {
        return "Page{" +
                "namespace='" + namespace + '\'' +
                ", title='" + title + '\'' +
                ", fullTitle='" + fullTitle + '\'' +
                ", id=" + id +
                ", isRedirect=" + isRedirect +
                ", restrictions='" + restrictions + '\'' +
                ", site=" + site +
                '}';
    }
}
