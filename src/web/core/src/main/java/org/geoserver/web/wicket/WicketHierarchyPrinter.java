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

package org.geoserver.web.wicket;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;

/**
 * Dumps a wicket component/page hierarchy to text, eventually writing down the class and the model
 * value as a string.
 *
 * <p>Each line in the dump follow the <code>componentId(class) 'value'</code> format.
 *
 * <p>The class can be reused for multiple prints, but it's not thread safe
 */
public class WicketHierarchyPrinter {
    static final Pattern NEWLINE = Pattern.compile("\\n", Pattern.MULTILINE);

    PrintStream out;

    boolean valueDumpEnabled;

    boolean classDumpEnabled;

    boolean pathDumpEnabled;

    /** Utility method to dump a single component/page to standard output */
    public static void print(Component c, boolean dumpClass, boolean dumpValue, boolean dumpPath) {
        WicketHierarchyPrinter printer = new WicketHierarchyPrinter();
        printer.setPathDumpEnabled(dumpClass);
        printer.setClassDumpEnabled(dumpClass);
        printer.setValueDumpEnabled(dumpValue);
        if (c instanceof Page) {
            printer.print(c);
        } else {
            printer.print(c);
        }
    }

    /** Utility method to dump a single component/page to standard output */
    public static void print(Component c, boolean dumpClass, boolean dumpValue) {
        print(c, dumpClass, dumpValue, false);
    }

    /** Creates a printer that will dump to standard output */
    public WicketHierarchyPrinter() {
        out = System.out;
    }

    /** Creates a printer that will dump to the specified print stream */
    public WicketHierarchyPrinter(PrintStream out) {
        this.out = out;
    }

    /** Set to true if you want to see the model values in the dump */
    public void setValueDumpEnabled(boolean valueDumpEnabled) {
        this.valueDumpEnabled = valueDumpEnabled;
    }

    /** Set to true if you want to see the component classes in the dump */
    public void setClassDumpEnabled(boolean classDumpEnabled) {
        this.classDumpEnabled = classDumpEnabled;
    }

    /** Prints the component containment hierarchy */
    public void print(Component c) {
        walkHierarchy(c, 0);
    }

    /** Walks down the containment hierarchy depth first and prints each component found */
    private void walkHierarchy(Component c, int level) {
        printComponent(c, level);
        if (c instanceof MarkupContainer) {
            MarkupContainer mc = (MarkupContainer) c;
            for (Component component : mc) {
                walkHierarchy(component, level + 1);
            }
        }
    }

    /** Prints a single component */
    private void printComponent(Component c, int level) {
        if (c instanceof Page) out.print(tab(level) + "PAGE_ROOT");
        else out.print(tab(level) + c.getId());

        if (pathDumpEnabled) {
            out.print(" " + c.getPageRelativePath());
        }

        if (classDumpEnabled) {
            String className;
            if (c.getClass().isAnonymousClass()) {
                className = c.getClass().getSuperclass().getName();
            } else {
                className = c.getClass().getName();
            }

            out.print("(" + className + ")");
        }

        if (valueDumpEnabled) {
            try {
                String value =
                        NEWLINE.matcher(c.getDefaultModelObjectAsString()).replaceAll("\\\\n");
                out.print(" '" + value + "'");
            } catch (Exception e) {
                out.print(" 'ERROR_RETRIEVING_MODEL " + e.getMessage() + "'");
            }
        }

        out.println();
    }

    /** Generates three spaces per level */
    String tab(int level) {
        char[] spaces = new char[level * 3];
        Arrays.fill(spaces, ' ');
        return new String(spaces);
    }

    /** If the page relative path dumping is enabled */
    public boolean isPathDumpEnabled() {
        return pathDumpEnabled;
    }

    /** Sets/unsets the relative path dumping */
    public void setPathDumpEnabled(boolean pathDumpEnabled) {
        this.pathDumpEnabled = pathDumpEnabled;
    }
}
