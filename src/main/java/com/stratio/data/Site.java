/**
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
package com.stratio.data;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableMap;
import java.util.Map;


/**
 * A Mediawiki site.
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class Site {

    private String name;
    private ImmutableMap<Integer,String> namespacesMap;

    public Site(String siteName, Map<Integer,String> namespacesMap) {
        this.name = checkNotNull(siteName);
        this.namespacesMap = ImmutableMap.copyOf(checkNotNull(namespacesMap));
    }

    public String getName() {
        return name;
    }

    public ImmutableMap<Integer,String> getNamespacesMap() {
        return namespacesMap;
    }

    public String namespaceStringFromFullTitle(String fullTitle) {
        String namespace = "";
        int index = checkNotNull(fullTitle).indexOf(':');
        if (index != -1) {
            String potentialNamespace = fullTitle.substring(0, index);
            if (namespacesMap.values().contains(potentialNamespace)) {
                namespace = potentialNamespace;
            }
        }
        return namespace;
    }

    public String[] splitTitle(String fullTitle) {
        String namespace = namespaceStringFromFullTitle(checkNotNull(fullTitle));
        String title = fullTitle.substring(namespace.length());
        return new String[]{namespace, title};
    }

    public String titleFromFullTitle(String fullTitle) {
        return splitTitle(checkNotNull(fullTitle))[1];
    }

}
