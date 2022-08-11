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

package org.geogig.geoserver.web.security;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.file.File;
import org.geogig.geoserver.config.LogEvent;
import org.geogig.geoserver.config.LogEvent.Severity;
import org.geogig.geoserver.config.LogStore;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geotools.feature.type.DateUtil;

public class SecurityLogsPanel extends GeoServerTablePanel<LogEvent> {

    private static final long serialVersionUID = 5957961031378924960L;

    private static final EnumMap<Severity, PackageResourceReference> SEVERITY_ICONS =
            new EnumMap<>(Severity.class);

    static {
        final PackageResourceReference infoIcon =
                new PackageResourceReference(
                        GeoServerBasePage.class, "img/icons/silk/information.png");

        final PackageResourceReference successIcon =
                new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/accept.png");

        final PackageResourceReference errorIcon =
                new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/error.png");
        SEVERITY_ICONS.put(Severity.DEBUG, infoIcon);
        SEVERITY_ICONS.put(Severity.INFO, successIcon);
        SEVERITY_ICONS.put(Severity.ERROR, errorIcon);
    }

    private final ModalWindow popupWindow;

    /** Only to be used by {@link #logStore()} */
    private transient LogStore logStore;

    public SecurityLogsPanel(final String id) {
        super(id, new LogEventProvider(), false /* selectable */);
        super.setSelectable(false);
        super.setSortable(true);
        super.setFilterable(true);
        super.setFilterVisible(true);
        super.setPageable(true);
        popupWindow = new ModalWindow("popupWindow");
        add(popupWindow);
    }

    private LogStore logStore() {
        if (this.logStore == null) {
            this.logStore = GeoServerExtensions.bean(LogStore.class);
        }
        return this.logStore;
    }

    @Override
    protected Component getComponentForProperty(
            final String id,
            @SuppressWarnings("rawtypes") IModel<LogEvent> itemModel,
            Property<LogEvent> property) {

        LogEvent item = (LogEvent) itemModel.getObject();
        if (property == LogEventProvider.SEVERITY) {
            Severity severity = item.getSeverity();
            PackageResourceReference iconRef = SEVERITY_ICONS.get(severity);
            return new Icon(id, iconRef);
        }
        if (property == LogEventProvider.REPOSITORY) {
            return repositoryLink(id, item);
        }
        if (property == LogEventProvider.TIMESTAMP) {
            return new Label(id, DateUtil.serializeDateTime(item.getTimestamp()));
        }
        if (property == LogEventProvider.MESSAGE) {
            return messageLink(id, item);
        }
        return new Label(id, String.valueOf(property.getPropertyValue(item)));
    }

    private Component messageLink(String id, LogEvent item) {
        IModel<String> messageModel = new Model<>(item.getMessage());
        if (!item.getSeverity().equals(Severity.ERROR)) {
            return new Label(id, messageModel);
        }

        SimpleAjaxLink<LogEvent> link =
                new SimpleAjaxLink<LogEvent>(id, new Model<>(item), messageModel) {

                    private static final long serialVersionUID = 1242472443848716943L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        LogEvent event = getModelObject();
                        long eventId = event.getEventId();
                        LogStore logStore = logStore();
                        String stackTrace = logStore.getStackTrace(eventId);
                        popupWindow.setInitialHeight(525);
                        popupWindow.setInitialWidth(855);
                        popupWindow.setContent(
                                new StackTracePanel(popupWindow.getContentId(), stackTrace));
                        popupWindow.show(target);
                    }
                };
        return link;
    }

    public static class StackTracePanel extends Panel {
        private static final long serialVersionUID = 6428694990556424777L;

        public StackTracePanel(String id, String stackTrace) {
            super(id);

            MultiLineLabel stackTraceLabel = new MultiLineLabel("trace");

            add(stackTraceLabel);

            if (stackTrace != null) {
                stackTraceLabel.setDefaultModel(new Model<>(stackTrace));
            }
        }
    }

    private Component repositoryLink(String id, LogEvent item) {
        String repositoryURL = item.getRepositoryURL();
        String name = new File(repositoryURL).getName();
        Label label = new Label(id, name);
        label.add(AttributeModifier.replace("title", repositoryURL));
        return label;
    }

    static class LogEventProvider extends GeoServerDataProvider<LogEvent> {

        private static final long serialVersionUID = 4883560661021761394L;

        // static final Property<LogEvent> EVENT_ID = new BeanProperty<LogEvent>("eventId",
        // "eventId");

        static final Property<LogEvent> SEVERITY = new BeanProperty<>("severity", "severity");

        static final Property<LogEvent> TIMESTAMP = new BeanProperty<>("timestamp", "timestamp");

        static final Property<LogEvent> REPOSITORY =
                new BeanProperty<>("repository", "repositoryURL");

        static final Property<LogEvent> USER = new BeanProperty<>("user", "user");

        static final Property<LogEvent> MESSAGE = new BeanProperty<>("message", "message");

        final List<Property<LogEvent>> PROPERTIES =
                Arrays.asList(
                        /* EVENT_ID, */
                        SEVERITY, TIMESTAMP, REPOSITORY, USER, MESSAGE);

        // private transient List<LogEvent> items;

        private transient LogStore logStore;

        public LogEventProvider() {}

        private LogStore logStore() {
            if (logStore == null) {
                logStore = GeoServerExtensions.bean(LogStore.class);
            }
            return logStore;
        }

        @Override
        protected List<LogEvent> getItems() {
            // ensure logstore is set
            logStore();
            List<LogEvent> items = logStore.getLogEntries(0, Integer.MAX_VALUE);

            return items;
        }

        @Override
        protected List<Property<LogEvent>> getProperties() {
            return PROPERTIES;
        }

        @Override
        public IModel<LogEvent> newModel(LogEvent object) {
            return new Model<>(object);
        }

        @Override
        public int fullSize() {
            // ensure logstore is set
            logStore();
            return logStore.getFullSize();
        }
    }
}
