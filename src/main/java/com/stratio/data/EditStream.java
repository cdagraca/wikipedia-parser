/**
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
package com.stratio.data;

import com.stratio.data.Edit;
import java.io.IOException;

public interface EditStream {

    public Edit nextEdit() throws IOException;
}
