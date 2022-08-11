/*
 *==Description==
 *GeoServer is an open source software server written in Java that allows users
 *          to share and edit geospatial data.Designed for interoperability,
 *          it publishes data from any major spatial data source using open standards.
 *
 *Being a community-driven project, GeoServer is developed, tested, and supported by
 *      a diverse group of individuals and organizations from around the world.
 *
 *GeoServer is the reference implementation of the Open Geospatial Consortium (OGC)
 *          Web Feature Service (WFS) and Web Coverage Service (WCS) standards, as well as
 *          a high performance certified compliant Web Map Service (WMS), compliant
 *          Catalog Service for the Web (CSW) and implementing Web Processing Service (WPS).
 *          GeoServer forms a core component of the Geospatial Web.
 *
 *==License==
 *GeoServer is distributed under the GNU General Public License Version 2.0 license:
 *
 *    GeoServer, open geospatial information server
 *    Copyright (C) 2014-2020 Open Source Geospatial Foundation.
 *    Copyright (C) 2001-2014 OpenPlans
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version (collectively, "GPL").
 *
 *    As an exception to the terms of the GPL, you may copy, modify,
 *    propagate, and distribute a work formed by combining GeoServer with the
 *    EMF and XSD Libraries, or a work derivative of such a combination, even if
 *    such copying, modification, propagation, or distribution would otherwise
 *    violate the terms of the GPL. Nothing in this exception exempts you from
 *    complying with the GPL in all respects for all of the code used other
 *    than the EMF and XSD Libraries. You may include this exception and its grant
 *    of permissions when you distribute GeoServer.  Inclusion of this notice
 *    with such a distribution constitutes a grant of such permissions.  If
 *    you do not wish to grant these permissions, remove this paragraph from
 *    your distribution. "GeoServer" means the GeoServer software licensed
 *    under version 2 or any later version of the GPL, or a work based on such
 *    software and licensed under the GPL. "EMF and XSD Libraries" means
 *    Eclipse Modeling Framework Project and XML Schema Definition software
 *    distributed by the Eclipse Foundation, all licensed
 *    under the Eclipse Public License Version 1.0 ("EPL"), or a work based on
 *    such software and licensed under the EPL.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 51 Franklin Street, Suite 500, Boston, MA 02110-1335  USA
 *
 *==More Information==
 *Visit the website or read the docs.
 */

package org.vfny.geoserver.global.xml;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import org.vfny.geoserver.global.ConfigurationException;

/**
 * WriterUtils purpose.
 *
 * <p>Used to provide assitance writing xml to a Writer.
 *
 * <p>
 *
 * @author dzwiers, Refractions Research, Inc.
 * @version $Id$
 */
public class WriterHelper {
    /**
     * Will find out if a string contains chars that need to be turned into an xml entity, even if
     * the string has lineends inside of it (thus the DOTALL flag)
     */
    private static final Pattern XML_ENTITIES = Pattern.compile(".*[\"&'<>]+.*", Pattern.DOTALL);

    /** The output writer. */
    protected Writer writer;

    protected int indent;
    protected StringBuffer indentBuffer = new StringBuffer();

    /**
     * WriterUtils constructor.
     *
     * <p>Should never be called.
     */
    protected WriterHelper() {}

    /**
     * WriterUtils constructor.
     *
     * <p>Stores the specified writer to use for output.
     *
     * @param writer the writer which will be used for outputing the xml.
     */
    public WriterHelper(Writer writer) {
        this.writer = writer;
    }

    /**
     * writeln purpose.
     *
     * <p>Writes the String specified to the stored output writer.
     *
     * @param s The String to write.
     * @throws ConfigurationException When an IO exception occurs.
     */
    public void writeln(String s) throws ConfigurationException {
        try {
            writer.write(indentBuffer.subSequence(0, indent) + s + "\n");
            writer.flush();
        } catch (IOException e) {
            throw new ConfigurationException("Writeln" + writer, e);
        }
    }

    private void increaseIndent() {
        indent += 2;
        indentBuffer.append("  ");
    }

    private void decreaseIndent() {
        if (indent > 0) {
            indent -= 2;
            indentBuffer.setLength(indentBuffer.length() - 2);
        }
    }

    /**
     * openTag purpose.
     *
     * <p>Writes an open xml tag with the name specified to the stored output writer.
     *
     * @param tagName The tag name to write.
     * @throws ConfigurationException When an IO exception occurs.
     */
    public void openTag(String tagName) throws ConfigurationException {
        openTag(tagName, Collections.emptyMap());
    }

    /**
     * openTag purpose.
     *
     * <p>Writes an open xml tag with the name and attributes specified to the stored output writer.
     *
     * @param tagName The tag name to write.
     * @param attributes The tag attributes to write.
     * @throws ConfigurationException When an IO exception occurs.
     */
    public void openTag(String tagName, Map attributes) throws ConfigurationException {
        StringBuffer sb = new StringBuffer();
        sb.append("<" + tagName + " ");

        Iterator i = attributes.keySet().iterator();

        while (i.hasNext()) {
            String s = (String) i.next();

            if (attributes.get(s) != null) {
                sb.append(s + " = " + "\"" + escape((attributes.get(s)).toString()) + "\" ");
            }
        }

        sb.append(">");
        writeln(sb.toString());
        increaseIndent();
    }

    /**
     * closeTag purpose.
     *
     * <p>Writes an close xml tag with the name specified to the stored output writer.
     *
     * @param tagName The tag name to write.
     * @throws ConfigurationException When an IO exception occurs.
     */
    public void closeTag(String tagName) throws ConfigurationException {
        decreaseIndent();
        writeln("</" + tagName + ">");
    }

    /**
     * valueTag purpose.
     *
     * <p>Writes an xml tag with the name and value specified to the stored output writer.
     *
     * @param tagName The tag name to write.
     * @param value The text data to write.
     * @throws ConfigurationException When an IO exception occurs.
     */
    public void valueTag(String tagName, String value) throws ConfigurationException {
        writeln("<" + tagName + " value = \"" + escape(value) + "\" />");
    }

    /**
     * attrTag purpose.
     *
     * <p>Writes an xml tag with the name and attributes specified to the stored output writer.
     *
     * @param tagName The tag name to write.
     * @param attributes The tag attributes to write.
     * @throws ConfigurationException When an IO exception occurs.
     */
    public void attrTag(String tagName, Map attributes) throws ConfigurationException {
        StringBuffer sb = new StringBuffer();
        sb.append("<" + tagName + " ");

        Iterator i = attributes.keySet().iterator();

        while (i.hasNext()) {
            String s = (String) i.next();

            if (attributes.get(s) != null) {
                sb.append(s + " = " + "\"" + escape((attributes.get(s)).toString()) + "\" ");
            }
        }

        sb.append("/>");
        writeln(sb.toString());
    }

    /**
     * textTag purpose.
     *
     * <p>Writes a text xml tag with the name and text specified to the stored output writer.
     *
     * @param tagName The tag name to write.
     * @param data The text data to write.
     * @throws ConfigurationException When an IO exception occurs.
     */
    public void textTag(String tagName, String data) throws ConfigurationException {
        textTag(tagName, Collections.emptyMap(), data);
    }

    /**
     * textTag purpose.
     *
     * <p>Writes an xml tag with the name, text and attributes specified to the stored output
     * writer.
     *
     * @param tagName The tag name to write.
     * @param attributes The tag attributes to write.
     * @param data The tag text to write.
     * @throws ConfigurationException When an IO exception occurs.
     */
    public void textTag(String tagName, Map attributes, String data) throws ConfigurationException {
        StringBuffer sb = new StringBuffer();
        sb.append("<" + tagName + ((attributes.isEmpty()) ? "" : " "));

        Iterator i = attributes.keySet().iterator();

        while (i.hasNext()) {
            String s = (String) i.next();

            if (attributes.get(s) != null) {
                sb.append(s + " = " + "\"" + escape((attributes.get(s)).toString()) + "\" ");
            }
        }

        String escapedData = "";
        if (data != null) escapedData = escape(data);
        sb.append(">" + escapedData + "</" + tagName + ">");
        writeln(sb.toString());
    }

    /**
     * comment purpose.
     *
     * <p>Writes an xml comment with the text specified to the stored output writer.
     *
     * @param comment The comment text to write.
     * @throws ConfigurationException When an IO exception occurs.
     */
    public void comment(String comment) throws ConfigurationException {
        writeln("<!--");
        increaseIndent();

        String ib = indentBuffer.substring(0, indent);
        comment = comment.trim();
        comment = comment.replaceAll("\n", "\n" + ib);
        writeln(comment);
        decreaseIndent();
        writeln("-->");
    }

    /**
     * Escapes the provided text with XML entities, see
     * (http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references#Character_entities_in_XML)
     */
    private String escape(String text) {
        // All redundant carriage returns should already have been stripped.
        String s = text.replaceAll("\r\n", "\n");

        if (XML_ENTITIES.matcher(s).matches()) {
            s = s.replaceAll("&", "&amp;");
            s = s.replaceAll("\"", "&quot;");
            s = s.replaceAll("'", "&apos;");
            s = s.replaceAll("<", "&lt;");
            s = s.replaceAll(">", "&gt;");
        }
        return s;
    }
}
