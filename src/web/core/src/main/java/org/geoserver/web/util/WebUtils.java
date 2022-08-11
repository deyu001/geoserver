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

package org.geoserver.web.util;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.Localizer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.time.Time;
import org.geoserver.template.TemplateUtils;
import org.geoserver.web.GeoServerApplication;
import org.geotools.util.logging.Logging;

/**
 * Collection of utilities for GeoServer web application components.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class WebUtils {

    static final Logger LOGGER = Logging.getLogger(WebUtils.class);

    /**
     * Utility method for localizing strings using Wicket i18n subsystem. Useful if your model needs
     * to be localized and you don't have access to a Component instance. Use with care, in most
     * cases you should be able to localize your messages directly in pages or components.
     */
    public static String localize(String key, IModel<?> model, Object... params) {
        StringResourceModel rm =
                new StringResourceModel(key, (Component) null) {
                    private static final long serialVersionUID = 7276431319922312811L;

                    @Override
                    public Localizer getLocalizer() {
                        return GeoServerApplication.get().getResourceSettings().getLocalizer();
                    }
                }.setModel(model).setParameters(params);

        return rm.getString();
    }

    /**
     * Returns a resource stream based on a freemarker template.
     *
     * <p>
     *
     * @param c The component being marked up.
     * @param model The template model to pass to the freemarker template.
     * @return The resource stream.
     */
    public static IResourceStream getFreemakerMarkupStream(Component c, TemplateModel model) {
        return new FreemarkerResourceStream(c.getClass(), model);
    }

    static class FreemarkerResourceStream implements IResourceStream {

        private static final long serialVersionUID = -7129118945660086236L;

        Class<? extends Component> clazz;

        TemplateModel model;

        String templateName;

        Configuration cfg;

        FreemarkerResourceStream(Class<? extends Component> clazz, TemplateModel model) {
            this.clazz = clazz;
            this.model = model;

            templateName = clazz.getSimpleName() + ".ftl";

            cfg = TemplateUtils.getSafeConfiguration();
            cfg.setClassForTemplateLoading(clazz, "");
        }

        public String getContentType() {
            return "text/html";
        }

        public InputStream getInputStream() throws ResourceStreamNotFoundException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                Template t = cfg.getTemplate(templateName);
                t.process(model, new OutputStreamWriter(output));

                return new ByteArrayInputStream(output.toByteArray());
            } catch (IOException e) {
                throw (ResourceStreamNotFoundException)
                        new ResourceStreamNotFoundException("Could not find template for: " + clazz)
                                .initCause(e);
            } catch (TemplateException e) {
                throw (ResourceStreamNotFoundException)
                        new ResourceStreamNotFoundException("Error in tempalte for: " + clazz)
                                .initCause(e);
            }
        }

        public Locale getLocale() {
            return cfg.getLocale();
        }

        public void setLocale(Locale locale) {
            cfg.setLocale(locale);
        }

        public Bytes length() {
            return Bytes.bytes(-1);
        }

        public Time lastModifiedTime() {
            Object source;
            try {
                source = cfg.getTemplateLoader().findTemplateSource(templateName);
            } catch (IOException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Error getting last modified time from template \"" + templateName + "\"",
                        e);
                return null;
            }

            if (source != null) {
                long modified = cfg.getTemplateLoader().getLastModified(source);
                return Time.valueOf(new Date(modified));
            }

            return null;
        }

        public void close() throws IOException {}

        @Override
        public String getStyle() {
            return null;
        }

        @Override
        public void setStyle(String style) {}

        @Override
        public String getVariation() {
            return null;
        }

        @Override
        public void setVariation(String variation) {}
    }
}
