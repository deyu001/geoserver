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

package org.geoserver.web.wicket.browser;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class FileProvider extends SortableDataProvider<File, String> {

    private static final long serialVersionUID = 2387540012977156321L;

    public static final String NAME = "name";

    public static final String LAST_MODIFIED = "lastModified";

    public static final String SIZE = "size";

    /** Compares the file names, makes sure directories are listed first */
    private static final Comparator<File> FILE_NAME_COMPARATOR =
            new AbstractFileComparator() {
                @Override
                public int compareProperty(File o1, File o2) {
                    // otherwise compare the name
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            };

    /** Compares last modified time */
    private static final Comparator<File> FILE_LM_COMPARATOR =
            new AbstractFileComparator() {
                @Override
                public int compareProperty(File o1, File o2) {
                    long lm1 = o1.lastModified();
                    long lm2 = o2.lastModified();
                    if (lm1 == lm2) {
                        return 0;
                    } else {
                        return lm1 < lm2 ? -1 : 1;
                    }
                }
            };

    /** Compares file size */
    private static final Comparator<File> FILE_SIZE_COMPARATOR =
            new AbstractFileComparator() {
                @Override
                public int compareProperty(File o1, File o2) {
                    long l1 = o1.length();
                    long l2 = o2.length();
                    if (l1 == l2) {
                        return 0;
                    } else {
                        return l1 < l2 ? -1 : 1;
                    }
                }
            };

    /** The current directory */
    IModel<File> directory;

    /** An eventual file filter */
    IModel<? extends FileFilter> fileFilter;

    public FileProvider(File directory) {
        this.directory = new Model<>(directory);
    }

    public FileProvider(IModel<File> directory) {
        this.directory = directory;
    }

    @Override
    public Iterator<File> iterator(long first, long count) {
        List<File> files = getFilteredFiles();

        // sorting
        Comparator<File> comparator = getComparator(getSort());
        if (comparator != null) Collections.sort(files, comparator);

        // paging
        long last = first + count;
        if (last > files.size()) {
            last = files.size();
        }
        return files.subList((int) first, (int) last).iterator();
    }

    List<File> getFilteredFiles() {
        // grab the current directory
        File d = directory.getObject();
        if (d.isFile()) d = d.getParentFile();

        // return a filtered view of the contents
        File[] files;
        if (fileFilter != null) files = d.listFiles(new HiddenFileFilter(fileFilter.getObject()));
        else files = d.listFiles(new HiddenFileFilter());

        if (files != null) return Arrays.asList(files);
        else return Collections.emptyList();
    }

    @Override
    public IModel<File> model(File object) {
        return new Model<>(object);
    }

    @Override
    public long size() {
        return getFilteredFiles().size();
    }

    private Comparator<File> getComparator(SortParam<String> sort) {
        if (sort == null) return FILE_NAME_COMPARATOR;

        // build base comparator
        Comparator<File> comparator = null;
        if (NAME.equals(sort.getProperty())) {
            comparator = FILE_NAME_COMPARATOR;
        } else if (LAST_MODIFIED.equals(sort.getProperty())) {
            comparator = FILE_LM_COMPARATOR;
        } else if (SIZE.equals(sort.getProperty())) {
            comparator = FILE_SIZE_COMPARATOR;
        } else {
            throw new IllegalArgumentException("Uknown sorting property " + sort.getProperty());
        }

        // reverse comparison direction if needed
        if (sort.isAscending()) return comparator;
        else return new ReverseComparator(comparator);
    }

    public IModel<File> getDirectory() {
        return directory;
    }

    public void setDirectory(IModel<File> directory) {
        this.directory = directory;
    }

    public IModel<? extends FileFilter> getFileFilter() {
        return fileFilter;
    }

    public void setFileFilter(IModel<? extends FileFilter> fileFilter) {
        this.fileFilter = fileFilter;
    }

    /**
     * A base file comparator: makes sure directories go first, files later. The subclass is used to
     * perform comparison when both are files, or both directories
     */
    private abstract static class AbstractFileComparator implements Comparator<File> {

        @Override
        public final int compare(File o1, File o2) {
            // directories first
            if (o1.isDirectory()) if (!o2.isDirectory()) return -1;
            if (o2.isDirectory()) if (!o1.isDirectory()) return 1;

            return compareProperty(o1, o2);
        }

        protected abstract int compareProperty(File f1, File f2);
    }

    /** A simple comparator inverter */
    private static class ReverseComparator implements Comparator<File> {
        Comparator<File> comparator;

        public ReverseComparator(Comparator<File> comparator) {
            this.comparator = comparator;
        }

        @Override
        public int compare(File o1, File o2) {
            return comparator.compare(o2, o1);
        }
    }

    private static class HiddenFileFilter implements FileFilter {
        FileFilter delegate;

        public HiddenFileFilter() {
            // no delegate, just skip the hidden ones
        }

        public HiddenFileFilter(FileFilter delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean accept(File pathname) {
            if (pathname.isHidden()) {
                return false;
            }

            if (delegate != null) {
                return delegate.accept(pathname);
            } else {
                return true;
            }
        }
    }
}
