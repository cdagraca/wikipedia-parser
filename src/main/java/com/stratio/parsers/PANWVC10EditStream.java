/*
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
package com.stratio.parsers;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.base.Charsets;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.Maps.*;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import com.stratio.data.Contributor;
import com.stratio.data.Edit;
import com.stratio.data.EditStream;
import com.stratio.data.Revision;

/**
 *
 * @author coldwind
 */
public class PANWVC10EditStream implements EditStream {

    private final File corpusDir;
    private final CSVReader editReader;
    private Map<Integer, Boolean> vandalismAnnotations;
    private Map<Integer, File> revFiles;

    public PANWVC10EditStream(File corpusDir) throws
            IOException {
        this.corpusDir = checkNotNull(corpusDir);

        // This map will hold editid (PAN-specific) to vandalism or not.
        vandalismAnnotations = Files.readLines(
                new File(corpusDir, "gold-annotations.csv"), Charsets.UTF_8,
                new LineProcessor<Map<Integer, Boolean>>() {

                    boolean first = true;
                    Map<Integer, Boolean> ann = newHashMapWithExpectedSize(15000);

                    @Override
                    public boolean processLine(String line) throws
                            IOException {
                        if (first) {
                            first = false;
                            return true;
                        }
                        int commaIdx = line.indexOf(',');
                        Integer editid = Integer.parseInt(line.substring(0,
                                commaIdx));
                        boolean vandalism = (line.charAt(commaIdx + 2)
                                == 'v');
                        ann.put(editid, vandalism);
                        return true;
                    }

                    @Override
                    public Map<Integer, Boolean> getResult() {
                        return ann;
                    }
                });


        editReader = new CSVReader(
                new FileReader(new File(corpusDir, "edits.csv")),
                ',', '"', 1);

        File partDirs[] = new File(corpusDir, "article-revisions").listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return (pathname.isDirectory() && pathname.getName().startsWith(
                        "part"));
            }
        });

        revFiles = newHashMapWithExpectedSize(40000*2);
        for (File partDir : partDirs) {
            for (File revFile : partDir.listFiles()) {
                revFiles.put(Integer.parseInt(revFile.getName().replace(".txt",
                        "")), revFile);
            }
        }
    }

    private String unquote(String str) {
        return str.substring(1, str.length() - 1);
    }

    private String readRevision(int revid) throws IOException {
        return Files.toString(revFiles.get(revid), Charsets.UTF_8);
    }

    @Override
    public Edit nextEdit() throws IOException {
        String fields[] = editReader.readNext();
        if (fields == null) {
            return null;
        }

        Integer editid = Integer.parseInt(fields[0]);
        String user = fields[1];
        Integer oldRevisionId = Integer.parseInt(fields[2]);
        Integer newRevisionId = Integer.parseInt(fields[3]);
        // diffurl = fields[5];
        String time = fields[5];
        String comment = fields[6];
        // title = fields[7];

        Revision oldRevision = new Revision(oldRevisionId, readRevision(
                oldRevisionId));
        Revision newRevision = new Revision(newRevisionId, readRevision(
                newRevisionId), new Contributor(0, user));
        Edit edit = new Edit(oldRevision, newRevision);

        edit.addTag(
                (vandalismAnnotations.get(editid))
                ? "vandalism" : "regular");

        return edit;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            editReader.close();
        } finally {
            super.finalize();
        }
    }
}
