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

package org.geoserver.ogcapi;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.template.DirectTemplateFeatureCollectionFactory;
import org.geoserver.template.FeatureWrapper;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geoserver.template.TemplateUtils;
import org.geotools.util.SoftValueHashMap;
import org.springframework.stereotype.Component;

/**
 * Support class that locates the templates based on the current response and eventual {@link
 * LocalWorkspace}.
 *
 * <p>Located in workspace using service landingPage prefix, or obtained from jar:
 *
 * <ul>
 *   <li>ogc/features/landingPage.ftl
 * </ul>
 */
@Component
public class FreemarkerTemplateSupport {

    private static final Map<Class, Configuration> configurationCache = new SoftValueHashMap<>(10);

    private final GeoServerResourceLoader resourceLoader;

    ClassTemplateLoader rootLoader = new ClassTemplateLoader(FreemarkerTemplateSupport.class, "");

    static DirectTemplateFeatureCollectionFactory FC_FACTORY =
            new DirectTemplateFeatureCollectionFactory();

    public FreemarkerTemplateSupport(GeoServerResourceLoader loader) {
        this.resourceLoader = loader;
    }

    /**
     * Returns the template for the specified feature type. Looking up templates is pretty
     * expensive, so we cache templates by feature type and template.
     */
    public Template getTemplate(ResourceInfo resource, String templateName, Class<?> clazz)
            throws IOException {
        GeoServerTemplateLoader templateLoader =
                new GeoServerTemplateLoader(clazz, resourceLoader) {
                    @Override
                    public Object findTemplateSource(String path) throws IOException {
                        Object source = null;

                        APIService service = clazz.getAnnotation(APIService.class);
                        if (service != null) {
                            source = super.findTemplateSource(service.landingPage() + "/" + path);
                        }

                        if (source == null) {
                            source = super.findTemplateSource(path);
                        }

                        if (source == null) {
                            source = rootLoader.findTemplateSource(path);

                            // wrap the source in a source that maintains the original path
                            if (source != null) {
                                return new ClassTemplateSource(path, source);
                            }
                        }

                        return source;
                    }
                };

        if (resource != null) {
            templateLoader.setResource(resource);
        } else {
            WorkspaceInfo ws = LocalWorkspace.get();
            if (ws != null) {
                templateLoader.setWorkspace(ws);
            }
        }

        // Configuration is not thread safe
        Configuration configuration = getTemplateConfiguration(clazz);
        synchronized (configuration) {
            configuration.setTemplateLoader(templateLoader);
            Template t = configuration.getTemplate(templateName);
            t.setEncoding("UTF-8");
            return t;
        }
    }

    Configuration getTemplateConfiguration(Class clazz) {
        return configurationCache.computeIfAbsent(
                clazz,
                k -> {
                    Configuration cfg = TemplateUtils.getSafeConfiguration();
                    cfg.setObjectWrapper(new FeatureWrapper(FC_FACTORY));
                    return cfg;
                });
    }

    /**
     * Processes a template and returns the result as a string
     *
     * @param resource The resource reference used to lookup templates in the data dir
     * @param templateName The template name
     * @param referenceClass The reference class for classpath template loading
     * @param model The model to be applied
     * @param writer The writer receiving the template output
     */
    public void processTemplate(
            ResourceInfo resource,
            String templateName,
            Class referenceClass,
            Map<String, Object> model,
            Writer writer)
            throws IOException {
        Template template = getTemplate(resource, templateName, referenceClass);

        try {
            template.process(model, writer);
        } catch (TemplateException e) {
            throw new IOException("Error occured processing template " + templateName, e);
        }
    }

    /**
     * Processes a template and returns the result as a string
     *
     * @param resource The resource reference used to lookup templates in the data dir
     * @param templateName The template name
     * @param referenceClass The reference class for classpath template loading
     * @param model The model to be applied
     */
    public String processTemplate(
            ResourceInfo resource,
            String templateName,
            Class referenceClass,
            Map<String, Object> model)
            throws IOException {
        StringWriter sw = new StringWriter();
        processTemplate(resource, templateName, referenceClass, model, sw);
        return sw.toString();
    }
}
