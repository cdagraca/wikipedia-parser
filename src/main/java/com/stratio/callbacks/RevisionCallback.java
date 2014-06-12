/**
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.callbacks;

import com.stratio.data.Revision;

/**
 * Callback for {@link Revision}.
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public interface RevisionCallback {
    public void callback(Revision revision);
}
