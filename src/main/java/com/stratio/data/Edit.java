/**
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
package com.stratio.data;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Maps.*;
import static com.google.common.collect.Sets.*;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Mediawiki edit, i.e., two consecutive revisions.
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class Edit {

    private final Revision oldRevision, newRevision;
    private Diff diff;
    private Set<String> tags;

    public Edit(Revision oldRevision, Revision newRevision) {
        this.tags = newHashSet();
        this.oldRevision = checkNotNull(oldRevision);
        this.newRevision = checkNotNull(newRevision);
    }

    public Revision getOldRevision() {
        return oldRevision;
    }

    public Revision getNewRevision() {
        return newRevision;
    }

    private ImmutableMultiset<String> _oldTokenCount;
    public ImmutableMultiset<String> getOldTokenCount() {
        if (_oldTokenCount == null) {
            _oldTokenCount = getOldRevision().getTokenCount();
        }
        return _oldTokenCount;
    }

    private ImmutableMultiset<String> _newTokenCount;
    public ImmutableMultiset<String> getNewTokenCount() {
        if (_newTokenCount == null) {
            _newTokenCount = getNewRevision().getTokenCount();
        }
        return _newTokenCount;
    }

    private ImmutableMultiset<String> _tokenCountMultiset;
    public ImmutableMultiset<String> getTokenCountMultiset() {


        if (_tokenCountMultiset == null) {
            Multiset<String> m = HashMultiset.create(newRevision.getTokens());
            m.removeAll(oldRevision.getTokens());
            _tokenCountMultiset = ImmutableMultiset.copyOf(m);
        }
        return _tokenCountMultiset;
    }

    public Diff getDiff() {
        return diff;
    }

    public Set<String> getTags() {
        return newHashSet(tags);
    }

    public void addTag(String tag) {
        tags.add(tag);
    }

    public void removeTag(String tag) {
        tags.remove(tag);
    }
}
