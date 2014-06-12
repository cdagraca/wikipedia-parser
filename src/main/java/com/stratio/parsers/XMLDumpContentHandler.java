/**
 * Copyright © 2010 Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
package com.stratio.parsers;

import static com.google.common.collect.Maps.*;
import com.google.common.collect.ImmutableList;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import com.stratio.callbacks.PageCallback;
import com.stratio.callbacks.RevisionCallback;
import com.stratio.callbacks.VoidPageCallback;
import com.stratio.callbacks.VoidRevisionCallback;
import com.stratio.data.Contributor;
import com.stratio.data.Page;
import com.stratio.data.Revision;
import com.stratio.data.Site;

/**
 * SAX content handler for Mediawiki XML dumps.
 *
 * Tested with <a href=http://www.mediawiki.org/xml/export-0.4/>export 0.4</a>.
 *
 * @author Santiago M. Mola Velasco <cooldwind@gmail.com>
 */
public class XMLDumpContentHandler extends DefaultHandler {

    private StringBuilder _sb;
    private Integer curNamespaceKey;
    private Integer curPageId;
    private Integer curRevisionId;
    private Integer curContributorId;
    private Boolean curIsAnonymous;
    private Boolean curIsMinor;
    private String curRestrictions;
    private String curUsername;
    private String curTitle;
    private Date curTimestamp;
    private String curComment;
    private String curText;
    private Boolean curRedirect;
    private Page curPage;
    private Map<Integer,String> namespaceMap;
    private Map<String,String> siteinfo;
    private PageCallback pageCallback;
    private RevisionCallback revisionCallback;
    private ImmutableList<String> allowedNamespaces;
    private Site site;

    //2014-04-27T06:00:47Z, ISO-8601 date
    private static final SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public PageCallback getPageCallback() {
        return pageCallback;
    }

    public void setPageCallback(PageCallback pageCallback) {
        this.pageCallback = pageCallback;
    }

    public RevisionCallback getRevisionCallback() {
        return revisionCallback;
    }

    public void setRevisionCallback(RevisionCallback revisionCallback) {
        this.revisionCallback = revisionCallback;
    }

    private static enum State {
        START, IN_MEDIAWIKI, IN_SITEINFO,
        IN_NAMESPACES, IN_PAGE, IN_REVISION, IN_CONTRIBUTOR, SKIP_PAGE
    };
    private State state;

    public XMLDumpContentHandler() {
        this(null);
    }

    public XMLDumpContentHandler(List<String> allowedNamespaces) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        sdf.setTimeZone(tz);
        this.pageCallback = new VoidPageCallback();
        this.revisionCallback = new VoidRevisionCallback();
        if (allowedNamespaces == null) {
            this.allowedNamespaces = ImmutableList.of();
        } else {
            this.allowedNamespaces = ImmutableList.copyOf(allowedNamespaces);
        }
    }

    private void unexpectedStartTagException(String tag, String expected) throws
            SAXException {
        throw new SAXNotRecognizedException("Found <" + tag + ">, expected <"
                + expected + ">");
    }

    private void unexpectedStartTagException(String tag, String expected[])
            throws SAXException {
        StringBuilder sb = new StringBuilder("Found <");
        sb.append(tag);
        sb.append(">, expected ");
        for (String exp : expected) {
            sb.append("<");
            sb.append(exp);
            sb.append(">");
            sb.append(" ");
        }
        throw new SAXNotRecognizedException(sb.toString());
    }

    private void unexpectedEndTagException(String tag, String expected) throws
            SAXException {
        throw new SAXNotRecognizedException("Found </" + tag + ">, expected <"
                + expected + ">");
    }

    private void unexpectedEndTagException(String tag, String expected[]) throws
            SAXException {
        StringBuilder sb = new StringBuilder("Found </");
        sb.append(tag);
        sb.append(">, expected ");
        for (String exp : expected) {
            sb.append("</");
            sb.append(exp);
            sb.append(">");
            sb.append(" ");
        }
        throw new SAXNotRecognizedException(sb.toString());
    }

    @Override
    public void characters(char[] arg0, int arg1, int arg2) throws SAXException {
        if (state != State.SKIP_PAGE) {
            _sb.append(arg0, arg1, arg2);
        }
    }

    @Override
    public void startDocument() throws SAXException {
        _sb = new StringBuilder();
        namespaceMap = newHashMap();
        siteinfo = newHashMap();
        state = State.START;
    }

    @Override
    public void endDocument() throws SAXException {
    }

    @Override
    public void startElement(String namespaceURI, String localname,
            String rawname, Attributes atts) throws SAXException {
        //System.out.println("Start: " + localname + ", State: " + state.toString());

        _sb.setLength(0);

        switch (state) {
            case IN_REVISION:
                if ("contributor".equals(localname)) {
                    state = State.IN_CONTRIBUTOR;
                } else if ("id".equals(localname) || "timestamp".equals(
                        localname) || "comment".equals(localname) || "text".
                        equals(localname) || "minor".equals(localname)) {
                }
                break;
            case IN_PAGE:
                if ("revision".equals(localname)) {
                    state = State.IN_REVISION;
                    curRevisionId = 0;
                    curContributorId = 0;
                    curUsername = "";
                    curIsMinor = false;

                    // Create Page object
                    if (curPage == null) {
                        curPage = new Page(curPageId, curTitle, curRedirect,
                                curRestrictions, site);
                        pageCallback.callback(curPage);
                    }
                } else if ("title".equals(localname) || "id".equals(
                        localname) || "restrictions".equals(localname)) {
                } else if ("redirect".equals(localname)) {
                } else if ("ns".equals(localname)) {
                } else {
                    unexpectedStartTagException(localname, new String[]{"title",
                                "id", "redirect", "restrictions", "revision"});
                }
                break;
            case IN_CONTRIBUTOR:
                if ("id".equals(localname) || "username".equals(localname) || "ip".
                        equals(localname)) {
                } else {
                    unexpectedStartTagException(localname, new String[]{"id",
                                "username"});
                }
                break;
            case IN_MEDIAWIKI:
                if ("page".equals(localname)) {
                    state = State.IN_PAGE;
                    curPageId = 0;
                    curTitle = "";
                    curRedirect = false;
                    curRestrictions = "";
                    curPage = null;
                } else if ("siteinfo".equals(localname)) {
                    state = State.IN_SITEINFO;
                } else {
                    unexpectedStartTagException(localname, new String[]{
                                "siteinfo", "page"});
                }
                break;
            case START:
                if ("mediawiki".equals(localname)) {
                    state = State.IN_MEDIAWIKI;
                } else {
                    unexpectedStartTagException(localname, new String[]{
                                "mediawiki", "page"});
                }
                break;
            case IN_SITEINFO:
                if ("namespaces".equals(localname)) {
                    state = State.START.IN_NAMESPACES;
                }
                break;
            case IN_NAMESPACES:
                if ("namespace".equals(localname)) {
                    curNamespaceKey = Integer.parseInt(atts.getValue("key"));
                } else {
                    unexpectedStartTagException(localname, "namespace");
                }
                break;
        }
    }

    @Override
    public void endElement(String namespaceURI, String localname,
            String rawname) throws SAXException {
        switch (state) {
            case IN_CONTRIBUTOR:
                if ("contributor".equals(localname)) {
                    state = State.IN_REVISION;
                } else if ("id".equals(localname)) {
                    curContributorId = Integer.parseInt(_sb.toString().trim());
                } else if ("ip".equals(localname)) {
                    curIsAnonymous = true;
                    curUsername = _sb.toString();
                } else if ("username".equals(localname)) {
                    curIsAnonymous = false;
                    curUsername = _sb.toString();
                } else {
                    unexpectedEndTagException(localname, new String[]{"id", "ip",
                                "username"});
                }
                break;
            case IN_REVISION:
                if ("revision".equals(localname)) {
                    state = State.IN_PAGE;
                    revisionCallback.callback(new Revision(curRevisionId,curTimestamp,
                            curText, new Contributor(curContributorId,
                            curUsername, curIsAnonymous), curIsMinor, curPage));
                } else if ("id".equals(localname)) {
                    curRevisionId = Integer.parseInt(_sb.toString());
                } else if ("timestamp".equals(localname)) {
                    if (!"".equals(_sb.toString()))
                        try {
                            curTimestamp = sdf.parse(_sb.toString());
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                } else if ("comment".equals(localname)) {
                    curComment = _sb.toString();
                } else if ("text".equals(localname)) {
                    curText = _sb.toString();
                } else if ("minor".equals(localname)) {
                    curIsMinor = true;
                }
                break;
            case IN_PAGE:
                if ("page".equals(localname)) {
                    state = State.IN_MEDIAWIKI;
                } else if ("title".equals(localname)) {
                    curTitle = _sb.toString();
                    if (!allowedNamespaces.isEmpty() &&
                            !allowedNamespaces.contains(
                            site.namespaceStringFromFullTitle(curTitle))) {
                        state = State.START.SKIP_PAGE;
                    }
                } else if ("id".equals(localname)) {
                    curPageId = Integer.parseInt(_sb.toString().trim());
                } else if ("redirect".equals(localname)) {
                    curRedirect = true;
                } else if ("restrictions".equals(localname)) {
                    curRestrictions = _sb.toString();
                }
                break;
            case SKIP_PAGE:
                if ("page".equals(localname)) {
                    state = State.IN_MEDIAWIKI;
                }
                break;
            case IN_SITEINFO:
                if ("siteinfo".equals(localname)) {
                    site = new Site(siteinfo.get("sitename"), namespaceMap);
                    state = State.IN_MEDIAWIKI;
                } else {
                    siteinfo.put(localname, _sb.toString());
                }
                break;
            case IN_NAMESPACES:
                if ("namespace".equals(localname)) {
                    namespaceMap.put(curNamespaceKey, _sb.toString());
                } else if ("namespaces".equals(localname)) {
                    state = State.IN_SITEINFO;
                }
                break;
            case IN_MEDIAWIKI:
                if ("mediawiki".equals(localname)) {
                    break;
                }
            case START:
                unexpectedEndTagException(localname, "");
        }

        _sb.setLength(0);
    }
    
}
