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

package org.geoserver.test;

import java.io.File;
import java.io.IOException;
import javax.servlet.ServletContext;
import org.apache.commons.io.FileUtils;
import org.geoserver.util.IOUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.ui.context.Theme;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.ServletContextAwareProcessor;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.w3c.dom.Element;

/**
 * A spring application context used for GeoServer testing.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class GeoServerTestApplicationContext extends ClassPathXmlApplicationContext
        implements WebApplicationContext {
    ServletContext servletContext;

    boolean useLegacyGeoServerLoader = true;

    final File contextTmp;

    public GeoServerTestApplicationContext(String[] configLocation, ServletContext servletContext)
            throws BeansException {
        super(configLocation, false);
        try {
            contextTmp = IOUtils.createRandomDirectory("./target", "mock", "tmp");
            servletContext.setAttribute("javax.servlet.context.tempdir", contextTmp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.servletContext = servletContext;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public Theme getTheme(String themeName) {
        return null;
    }

    public void setUseLegacyGeoServerLoader(boolean useLegacyGeoServerLoader) {
        this.useLegacyGeoServerLoader = useLegacyGeoServerLoader;
    }

    /*
     * JD: Overriding manually and playing with bean definitions. We do this
     * because we have not ported all the mock test data to a 2.x style configuration
     * directory, so we need to force the legacy data directory loader to engage.
     */
    protected void loadBeanDefinitions(XmlBeanDefinitionReader reader)
            throws BeansException, IOException {
        super.loadBeanDefinitions(reader);

        if (useLegacyGeoServerLoader) {
            BeanDefinition def = reader.getBeanFactory().getBeanDefinition("geoServerLoader");
            def.setBeanClassName("org.geoserver.test.TestGeoServerLoaderProxy");
        }
    }

    @Override
    protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext));
        beanFactory.ignoreDependencyInterface(ServletContextAware.class);
        beanFactory.ignoreDependencyInterface(ServletConfigAware.class);

        WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.servletContext);
        WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.servletContext);
    }

    @Override
    protected void initPropertySources() {
        super.initPropertySources();
        WebApplicationContextUtils.initServletPropertySources(
                this.getEnvironment().getPropertySources(), this.servletContext);
    }

    @Override
    protected void initBeanDefinitionReader(XmlBeanDefinitionReader reader) {
        super.initBeanDefinitionReader(reader);
        reader.setDocumentReaderClass(LazyBeanDefinitionDocumentReader.class);
    }

    static class LazyBeanDefinitionDocumentReader extends DefaultBeanDefinitionDocumentReader {

        @Override
        protected BeanDefinitionParserDelegate createDelegate(
                XmlReaderContext readerContext,
                Element root,
                BeanDefinitionParserDelegate parentDelegate) {
            root.setAttribute("default-lazy-init", "true");
            BeanDefinitionParserDelegate delegate =
                    super.createDelegate(readerContext, root, parentDelegate);
            return delegate;
        }
    }

    @Override
    protected void onClose() {
        super.onClose();
        FileUtils.deleteQuietly(contextTmp);
    }
}
