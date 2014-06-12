/**
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */

package com.stratio.callbacks;

import java.awt.print.Pageable;
import com.stratio.data.Page;

/**
 * Void callback for {@link Page}.
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class VoidPageCallback implements PageCallback {

    @Override
    public void callback(Page page) { }

}
