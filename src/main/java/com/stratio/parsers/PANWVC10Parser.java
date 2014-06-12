/*
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
package com.stratio.parsers;

import static com.google.common.base.Preconditions.*;
import java.io.File;
import java.io.IOException;

import com.stratio.data.EditStream;
import com.stratio.data.EditStream;

public class PANWVC10Parser {

	private final File corpusDir;

	public PANWVC10Parser(String path) {
		this.corpusDir = new File(checkNotNull(path));
	}

	public EditStream getEditStream() throws IOException {
		return new PANWVC10EditStream(corpusDir);
	}
	
}
