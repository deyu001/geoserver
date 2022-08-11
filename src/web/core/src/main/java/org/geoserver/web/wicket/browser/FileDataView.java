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
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Locale;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxFallbackLink;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.OrderByBorder;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.util.convert.IConverter;

/**
 * A data view listing files in a certain directory, subject to a file filter
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public abstract class FileDataView extends Panel {
    private static final IConverter<File> FILE_NAME_CONVERTER =
            new StringConverter() {

                public String convertToString(File file, Locale locale) {
                    if (file.isDirectory()) {
                        return file.getName() + "/";
                    } else {
                        return file.getName();
                    }
                }
            };

    private static final IConverter<File> FILE_LASTMODIFIED_CONVERTER =
            new StringConverter() {

                public String convertToString(File file, Locale locale) {
                    long lastModified = file.lastModified();
                    if (lastModified == 0L) return null;
                    else {
                        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                .format(new Date(file.lastModified()));
                    }
                }
            };

    private static final IConverter<File> FILE_SIZE_CONVERTER =
            new StringConverter() {
                private static final double KBYTE = 1024;
                private static final double MBYTE = KBYTE * 1024;
                private static final double GBYTE = MBYTE * 1024;

                public String convertToString(File value, Locale locale) {
                    File file = value;

                    if (!file.isFile()) return "";

                    long size = file.length();
                    if (size == 0L) return null;

                    if (size < KBYTE) {
                        return size + "";
                    } else if (size < MBYTE) {
                        return new DecimalFormat("#.#").format(size / KBYTE) + "K";
                    } else if (size < GBYTE) {
                        return new DecimalFormat("#.#").format(size / MBYTE) + "M";
                    } else {
                        return new DecimalFormat("#.#").format(size / GBYTE) + "G";
                    }
                }
            };

    FileProvider provider;

    WebMarkupContainer fileContent;

    String tableHeight = "25em";

    public FileDataView(String id, FileProvider fileProvider) {
        super(id);

        this.provider = fileProvider;
        //        provider.setDirectory(currentPosition);
        //        provider.setSort(new SortParam(NAME, true));

        final WebMarkupContainer table = new WebMarkupContainer("fileTable");
        table.setOutputMarkupId(true);
        add(table);

        DataView<File> fileTable =
                new DataView<File>("files", fileProvider) {

                    @Override
                    protected void populateItem(final Item<File> item) {

                        // odd/even alternate style
                        item.add(
                                AttributeModifier.replace(
                                        "class", item.getIndex() % 2 == 0 ? "even" : "odd"));

                        // navigation/selection links
                        AjaxFallbackLink<?> link =
                                new IndicatingAjaxFallbackLink<Void>("nameLink") {

                                    @Override
                                    public void onClick(AjaxRequestTarget target) {
                                        linkNameClicked(item.getModelObject(), target);
                                    }
                                };
                        link.add(
                                new Label("name", item.getModel()) {
                                    @SuppressWarnings("unchecked")
                                    @Override
                                    public <C> IConverter<C> getConverter(Class<C> type) {
                                        return (IConverter<C>) FILE_NAME_CONVERTER;
                                    }
                                });
                        item.add(link);

                        // last modified and size labels
                        item.add(
                                new Label("lastModified", item.getModel()) {
                                    @SuppressWarnings("unchecked")
                                    @Override
                                    public <C> IConverter<C> getConverter(Class<C> type) {
                                        return (IConverter<C>) FILE_LASTMODIFIED_CONVERTER;
                                    }
                                });
                        item.add(
                                new Label("size", item.getModel()) {
                                    @SuppressWarnings("unchecked")
                                    @Override
                                    public <C> IConverter<C> getConverter(Class<C> type) {
                                        return (IConverter<C>) FILE_SIZE_CONVERTER;
                                    }
                                });
                    }
                };

        fileContent =
                new WebMarkupContainer("fileContent") {
                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        if (tableHeight != null) {
                            tag.getAttributes()
                                    .put("style", "overflow:auto; height:" + tableHeight);
                        }
                    }
                };

        fileContent.add(fileTable);

        table.add(fileContent);
        table.add(new OrderByBorder<>("nameHeader", FileProvider.NAME, fileProvider));
        table.add(
                new OrderByBorder<>(
                        "lastModifiedHeader", FileProvider.LAST_MODIFIED, fileProvider));
        table.add(new OrderByBorder<>("sizeHeader", FileProvider.SIZE, fileProvider));
    }

    protected abstract void linkNameClicked(File file, AjaxRequestTarget target);

    private abstract static class StringConverter implements IConverter<File> {

        public File convertToObject(String value, Locale locale) {
            throw new UnsupportedOperationException("This converter works only for strings");
        }
    }

    public FileProvider getProvider() {
        return provider;
    }

    public void setTableHeight(String tableHeight) {
        this.tableHeight = tableHeight;
    }
}
