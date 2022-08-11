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


package org.geoserver.wms.web.data;

import static org.geoserver.template.TemplateUtils.FM_VERSION;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.javascript.DefaultJavaScriptCompressor;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.resource.NoOpTextCompressor;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.template.TemplateUtils;
import org.geoserver.web.GeoServerApplication;
import org.geotools.util.logging.Logging;

/** */
public class GeoStyler extends Panel implements IHeaderContributor {

    private static final Logger LOGGER = Logging.getLogger(GeoStyler.class);

    private static final Configuration templates;

    private AbstractStylePage parent;

    static {
        templates = TemplateUtils.getSafeConfiguration();
        templates.setClassForTemplateLoading(GeoStyler.class, "");
        templates.setObjectWrapper(new DefaultObjectWrapper(FM_VERSION));
    }

    /**
     * Constructor
     *
     * @param id The id given to the component.
     * @param parent The parenting AbstractStylePage
     */
    public GeoStyler(String id, AbstractStylePage parent) {
        super(id);
        this.parent = parent;
        LOGGER.info("Created a new instance of GeoStyler");
    }

    /**
     * Add required CSS and Javascript resources
     *
     * @param header
     */
    public void renderHead(IHeaderResponse header) {
        super.renderHead(header);
        try {
            renderHeaderCss(header);
            renderHeaderScript(header);
        } catch (IOException | TemplateException e) {
            throw new WicketRuntimeException(e);
        }
    }

    /**
     * Renders header CSS
     *
     * @param header
     * @throws IOException
     * @throws TemplateException
     */
    private void renderHeaderCss(IHeaderResponse header) throws IOException, TemplateException {
        StringWriter css = new java.io.StringWriter();
        header.render(CssHeaderItem.forCSS(css.toString(), "geostyler-css"));
    }

    /**
     * Renders header scripts
     *
     * @param header
     * @throws IOException
     * @throws TemplateException
     */
    private void renderHeaderScript(IHeaderResponse header) throws IOException, TemplateException {
        Map<String, Object> context = new HashMap<>();
        LayerInfo layerInfo = parent.getLayerInfo();
        ResourceInfo resource = layerInfo.getResource();
        if (resource != null) {
            context.put("layer", layerInfo.prefixedName());
            context.put("layerType", layerInfo.getType());
        } else {
            context.put("layer", StringUtils.EMPTY);
            context.put("layerType", StringUtils.EMPTY);
        }

        Template template = templates.getTemplate("geostyler-init.ftl");
        StringWriter script = new java.io.StringWriter();
        template.process(context, script);

        // temporarily disable javascript compression since build resources are already compressed
        GeoServerApplication.get()
                .getResourceSettings()
                .setJavaScriptCompressor(new NoOpTextCompressor());
        header.render(
                CssHeaderItem.forReference(
                        new PackageResourceReference(
                                GeoStyler.class, "js/geostyler-integration.css")));
        header.render(
                CssHeaderItem.forReference(
                        new PackageResourceReference(GeoStyler.class, "js/geostyler.css")));
        header.render(
                CssHeaderItem.forReference(
                        new PackageResourceReference(GeoStyler.class, "js/antd.min.css")));
        header.render(
                JavaScriptHeaderItem.forReference(
                        new PackageResourceReference(
                                GeoStyler.class, "js/react.production.min.js")));
        header.render(
                JavaScriptHeaderItem.forReference(
                        new PackageResourceReference(
                                GeoStyler.class, "js/react-dom.production.min.js")));
        header.render(
                JavaScriptHeaderItem.forReference(
                        new PackageResourceReference(GeoStyler.class, "js/geostyler.js")));
        header.render(
                JavaScriptHeaderItem.forReference(
                        new PackageResourceReference(GeoStyler.class, "js/geoJsonDataParser.js")));
        header.render(
                JavaScriptHeaderItem.forReference(
                        new PackageResourceReference(GeoStyler.class, "js/sldStyleParser.js")));
        header.render(
                JavaScriptHeaderItem.forReference(
                        new PackageResourceReference(GeoStyler.class, "js/wfsDataParser.js")));
        header.render(OnLoadHeaderItem.forScript(script.toString()));
    }

    /**
     * As soon as the {@link GeoStyler} is removed the default Javascript compression needs to be
     * enabled
     */
    @Override
    protected void onRemove() {
        // (re-) enable javascript compression
        GeoServerApplication.get()
                .getResourceSettings()
                .setJavaScriptCompressor(new DefaultJavaScriptCompressor());
        super.onRemove();
    }
}
