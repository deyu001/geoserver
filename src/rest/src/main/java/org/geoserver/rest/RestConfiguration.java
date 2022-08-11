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

package org.geoserver.rest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Comparator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.Styles;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.geoserver.rest.converters.FreemarkerHTMLMessageConverter;
import org.geoserver.rest.converters.InputStreamConverter;
import org.geoserver.rest.converters.StyleReaderConverter;
import org.geoserver.rest.converters.StyleWriterConverter;
import org.geoserver.rest.converters.XStreamCatalogListConverter;
import org.geoserver.rest.converters.XStreamJSONMessageConverter;
import org.geoserver.rest.converters.XStreamXMLMessageConverter;
import org.geotools.util.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.util.UrlPathHelper;
import org.xml.sax.EntityResolver;

/** Configure various aspects of Spring MVC, in particular message converters */
@Configuration
public class RestConfiguration extends WebMvcConfigurationSupport {

    private ContentNegotiationManager contentNegotiationManager;

    @Autowired private ApplicationContext applicationContext;

    /**
     * Return a {@link ContentNegotiationManager} instance to use to determine requested {@linkplain
     * MediaType media types} in a given request.
     */
    @Override
    @Bean
    public ContentNegotiationManager mvcContentNegotiationManager() {
        if (this.contentNegotiationManager == null) {
            this.contentNegotiationManager = super.mvcContentNegotiationManager();
            this.contentNegotiationManager
                    .getStrategies()
                    .add(0, new DelegatingContentNegotiationStrategy());
        }
        return this.contentNegotiationManager;
    }

    /** Allows extension point configuration of {@link ContentNegotiationStrategy}s */
    private static class DelegatingContentNegotiationStrategy
            implements ContentNegotiationStrategy {
        @Override
        public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest)
                throws HttpMediaTypeNotAcceptableException {
            List<ContentNegotiationStrategy> strategies =
                    GeoServerExtensions.extensions(ContentNegotiationStrategy.class);
            List<MediaType> mediaTypes;
            for (ContentNegotiationStrategy strategy : strategies) {
                if (!(strategy instanceof ContentNegotiationManager
                        || strategy instanceof DelegatingContentNegotiationStrategy)) {
                    mediaTypes = strategy.resolveMediaTypes(webRequest);
                    if (!mediaTypes.isEmpty()) {
                        return mediaTypes;
                    }
                }
            }
            return MEDIA_TYPE_ALL_LIST;
        }
    }

    @Override
    protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        Catalog catalog = (Catalog) applicationContext.getBean("catalog");

        List<BaseMessageConverter> gsConverters =
                GeoServerExtensions.extensions(BaseMessageConverter.class);

        // Add default converters
        gsConverters.add(new FreemarkerHTMLMessageConverter("UTF-8"));
        gsConverters.add(new XStreamXMLMessageConverter());
        gsConverters.add(new XStreamJSONMessageConverter());
        gsConverters.add(new XStreamCatalogListConverter.XMLXStreamListConverter());
        gsConverters.add(new XStreamCatalogListConverter.JSONXStreamListConverter());
        gsConverters.add(new InputStreamConverter());

        // Deal with the various Style handler
        EntityResolver entityResolver = catalog.getResourcePool().getEntityResolver();
        for (StyleHandler sh : Styles.handlers()) {
            for (Version ver : sh.getVersions()) {
                gsConverters.add(
                        new StyleReaderConverter(sh.mimeType(ver), ver, sh, entityResolver));
                gsConverters.add(new StyleWriterConverter(sh.mimeType(ver), ver, sh));
            }
        }
        // Add GWC REST converter (add it first, since it has stricter constraints than the defalt
        // GS XML converters)
        if (applicationContext.containsBean("gwcConverter")) {
            converters.add((HttpMessageConverter<?>) applicationContext.getBean("gwcConverter"));
        }

        // Sort the converters based on ExtensionPriority
        gsConverters.sort(Comparator.comparingInt(BaseMessageConverter::getPriority));
        for (BaseMessageConverter converter : gsConverters) {
            converters.add(converter);
        }

        // use the default ones as lowest priority
        super.addDefaultHttpMessageConverters(converters);
    }

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RestInterceptor());
        registry.addInterceptor(new CallbackInterceptor());
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // scan and register media types for style handlers
        List<StyleHandler> styleHandlers = GeoServerExtensions.extensions(StyleHandler.class);
        for (StyleHandler handler : styleHandlers) {
            if (handler.getVersions() != null && handler.getVersions().size() > 0) {
                // Spring configuration allows associating a single mime to extensions, pick the
                // latest
                List<Version> versions = handler.getVersions();
                final Version firstVersion = versions.get(versions.size() - 1);
                configurer.mediaType(
                        handler.getFormat(), MediaType.valueOf(handler.mimeType(firstVersion)));
            }
        }
        // manually force SLD to v10 for backwards compatibility
        configurer.mediaType("sld", MediaType.valueOf(SLDHandler.MIMETYPE_10));

        // other common media types
        configurer.mediaType("html", MediaType.TEXT_HTML);
        configurer.mediaType("xml", MediaType.APPLICATION_XML);
        configurer.mediaType("json", MediaType.APPLICATION_JSON);
        configurer.mediaType("xslt", MediaType.valueOf("application/xslt+xml"));
        configurer.mediaType("ftl", MediaType.TEXT_PLAIN);
        configurer.mediaType("xml", MediaType.APPLICATION_XML);
        configurer.favorParameter(true);

        // allow extension point configuration of media types
        List<MediaTypeCallback> callbacks = GeoServerExtensions.extensions(MediaTypeCallback.class);
        for (MediaTypeCallback callback : callbacks) {
            callback.configure(configurer);
        }
        //        configurer.favorPathExtension(true);
        // todo properties files are only supported for test cases. should try to find a way to
        // support them without polluting prod code with handling
        //        configurer.mediaType("properties", MediaType.valueOf("application/prs.gs.psl"));
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Force MVC to use /restng endpoint. If we need something more advanced, we should make a
        // custom PathHelper
        GeoServerUrlPathHelper helper = new GeoServerUrlPathHelper();
        helper.setAlwaysUseFullPath(true);
        configurer.setUrlPathHelper(helper);
    }

    @Override
    protected void addFormatters(FormatterRegistry registry) {
        // add all configured Spring Converter classes to allow extension/pluggability
        for (Converter converter : GeoServerExtensions.extensions(Converter.class)) {
            registry.addConverter(converter);
        }
    }

    static class GeoServerUrlPathHelper extends UrlPathHelper {

        public GeoServerUrlPathHelper() {
            setAlwaysUseFullPath(true);
            setDefaultEncoding("UTF-8");
        }

        @Override
        public String decodeRequestString(HttpServletRequest request, String source) {
            // compatibility with old Restlet based config, it also decodes "+" into space
            try {
                return URLDecoder.decode(source, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
