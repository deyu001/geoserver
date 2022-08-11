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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.Version;

/** Style edit page */
public class StyleEditPage extends AbstractStylePage {

    private static final long serialVersionUID = 5478083954402101191L;

    public static final String NAME = "name";
    public static final String WORKSPACE = "workspace";

    public StyleEditPage(PageParameters parameters) {
        String name = parameters.get(NAME).toString();
        String workspace = parameters.get(WORKSPACE).toOptionalString();

        StyleInfo si =
                workspace != null
                        ? getCatalog().getStyleByName(workspace, name)
                        : getCatalog().getStyleByName(name);

        if (si == null) {
            error(new ParamResourceModel("StyleEditPage.notFound", this, name).getString());
            doReturn(StylePage.class);
            return;
        }

        recoverCssStyle(si);
        initPreviewLayer(si);
        initUI(si);

        if (!isAuthenticatedAsAdmin()) {
            // global styles only editable by full admin
            if (si.getWorkspace() == null) {
                styleForm.setEnabled(false);

                editor.add(new AttributeAppender("class", new Model<>("disabled"), " "));
                get("validate")
                        .add(new AttributeAppender("style", new Model<>("display:none;"), " "));
                add(
                        new Behavior() {

                            private static final long serialVersionUID = -4336130086161028141L;

                            @Override
                            public void renderHead(Component component, IHeaderResponse response) {
                                super.renderHead(component, response);
                                response.render(
                                        OnLoadHeaderItem.forScript(
                                                "document.getElementById('mainFormSubmit').style.display = 'none';"));
                                response.render(
                                        OnLoadHeaderItem.forScript(
                                                "document.getElementById('uploadFormSubmit').style.display = 'none';"));
                            }
                        });
                info(new StringResourceModel("globalStyleReadOnly", this, null).getString());
            }
        }
    }

    public StyleEditPage(StyleInfo style) {
        super(style);
    }

    @Override
    protected String getTitle() {
        StyleInfo style = styleModel.getObject();
        String styleName = "";
        if (style != null) {
            styleName =
                    (style.getWorkspace() == null ? "" : style.getWorkspace().getName() + ":")
                            + style.getName();
        }

        return new ParamResourceModel("title", this, styleName).getString();
    }

    @Override
    protected void onStyleFormSubmit() {
        // write out the file and save name modifications
        try {
            StyleInfo style = getStyleInfo();
            String format = style.getFormat();
            style.setFormat(format);
            Version version = Styles.handler(format).version(rawStyle);
            style.setFormatVersion(version);
            // make sure the legend is null if there is no URL
            if (null == style.getLegend()
                    || null == style.getLegend().getOnlineResource()
                    || style.getLegend().getOnlineResource().isEmpty()) {
                style.setLegend(null);
            }
            // write out the SLD, we try to use the old style so the same path is used
            StyleInfo stylePath = getCatalog().getStyle(style.getId());
            if (stylePath == null) {
                // the old style is no available anymore, so use the new path
                stylePath = style;
            }
            // ask the catalog to write the style
            try {
                getCatalog()
                        .getResourcePool()
                        .writeStyle(stylePath, new ByteArrayInputStream(rawStyle.getBytes()));
            } catch (IOException e) {
                throw new WicketRuntimeException(e);
            }
            // update the catalog
            getCatalog().save(style);
            // provide feedback to the user
            styleForm.info("Style saved");
            // retrieve sld style for non-sld formatted styles on update
            if ((!SLDHandler.FORMAT.equals(format))) {
                getCatalog().getResourcePool().getStyle(stylePath);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred saving the style", e);
            styleForm.error(e);
        }
    }
}
