/*
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
package com.stratio.parsers;

import com.google.common.base.Charsets;
import static com.google.common.base.Preconditions.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.List;
import java.util.zip.GZIPInputStream;
import net.contrapunctus.lzma.LzmaInputStream;
import org.apache.commons.compress.bzip2.CBZip2InputStream;
import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLDumpParser {

    private String dumpFile;
    private InputStream inStream;
    private XMLDumpContentHandler ch;

    public XMLDumpParser(InputStream in, List<String> allowedNamespaces) {
        this.inStream = checkNotNull(in);
        ch = new XMLDumpContentHandler(allowedNamespaces);
    }

    public XMLDumpParser(InputStream in) {
        this(in, null);
    }

    public XMLDumpParser(String dumpFile, List<String> allowedNamespaces) {
        this.dumpFile = checkNotNull(dumpFile);
        ch = new XMLDumpContentHandler(allowedNamespaces);
    }

    public XMLDumpParser(String dumpFile) {
        this(dumpFile, null);
    }

    public void parse() throws SAXException, IOException {
        SAXParser parser = new SAXParser();
        parser.setContentHandler(ch); //TODO: Make sure that the ContentHandler is "reset".
        parser.parse(getInputSource());
    }

    public XMLDumpContentHandler getContentHandler() {
        return ch;
    }

    /**
     *
     * Based on wikixmlj by Delip Rao and Jason Smith.
     * Licensed under the Apache License 2.0.
     * Available at http://code.google.com/p/wikixmlj/
     *
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    protected InputSource getInputSource() throws FileNotFoundException,
            IOException {
        if (inStream != null) {
            return new InputSource(inStream);
        }

        FileInputStream fis = new FileInputStream(dumpFile);
        InputStream is;
        InputStreamReader isr;

        if (dumpFile.endsWith(".gz")) {
            is = new GZIPInputStream(fis);
        } else if (dumpFile.endsWith(".bz2")) {
            byte[] ignoreBytes = new byte[2];
            fis.read(ignoreBytes); //"B", "Z" bytes from commandline tools
            is = new CBZip2InputStream(fis);
        } else if (dumpFile.endsWith(".7z")) {
            //FIXME: Doesn't seem to work.
            byte[] ignoreBytes = new byte[2];
            fis.read(ignoreBytes); //"7", "z" bytes from commandline tools
            is = new LzmaInputStream(fis);
        } else {
            is = fis;
        }

        CharsetDecoder decoder = Charsets.UTF_8.newDecoder();
        decoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
        decoder.onMalformedInput(CodingErrorAction.IGNORE);
        decoder.replaceWith("?");
        isr = new InputStreamReader(is, decoder);

        return new InputSource(isr);
        //return new InputSource(new BufferedReader(isr));
    }
}