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

package org.geoserver.web.data.store.pgraster;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.data.store.panel.PasswordParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.CRSPanel;
import org.geoserver.web.wicket.SRSToCRSModel;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * a Panel with PGRaster automatic configuration options TODO: 1) Add numeric validator for PORT 2)
 * change text description on the GUI (right now there is the name of the params)
 */
public class PGRasterPanel extends Panel {

    private static final long serialVersionUID = -8845475833628642890L;

    /**
     * temporary parameter name used to hold the raster table selected by the drop down into the
     * store's connectionParameters
     */
    public static final String TABLE_NAME = "tableName";

    private static final String RESOURCE_KEY_PREFIX = PGRasterPanel.class.getSimpleName();

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(ImageMosaicReader.class);

    FormComponent server;

    FormComponent port;

    FormComponent user;

    FormComponent password;

    FormComponent database;

    FormComponent table;

    FormComponent schema;

    FormComponent fileext;

    FormComponent importopt;

    CRSPanel epsgcode;

    private static CoordinateReferenceSystem DEFAULT_CRS;

    static {
        try {
            DEFAULT_CRS = CRS.decode("EPSG:4326");
        } catch (NoSuchAuthorityCodeException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        } catch (FactoryException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }
    }

    public PGRasterPanel(final String id, final IModel paramsModel, final Form storeEditForm) {

        super(id);
        server = addTextPanel(paramsModel, "server", true);

        port = addTextPanel(paramsModel, "port", true);
        SRSToCRSModel srsModel = new SRSToCRSModel(new PropertyModel(paramsModel, "ESPG:4326"));
        epsgcode = new CRSPanel("epsgcode", srsModel);
        epsgcode.setModelObject(DEFAULT_CRS);
        add(epsgcode);

        user = addTextPanel(paramsModel, "user", "Postgis user", true);
        password = addPasswordPanel(paramsModel, "password");
        database = addTextPanel(paramsModel, "database", "Postgis Database", true);
        table = addTextPanel(paramsModel, "table", true);
        schema = addTextPanel(paramsModel, "schema", true);
        schema.setModelValue(new String[] {"public"});
        fileext = addTextPanel(paramsModel, "fileext", "tiles file extension filter", false);
        importopt =
                addTextPanel(paramsModel, "importopt", "raster2pgsql script import options", false);

        server.setOutputMarkupId(true);
        port.setOutputMarkupId(true);
        user.setOutputMarkupId(true);
        password.setOutputMarkupId(true);
        database.setOutputMarkupId(true);
        table.setOutputMarkupId(true);
        schema.setOutputMarkupId(true);

        fileext.setOutputMarkupId(true);
        importopt.setOutputMarkupId(true);
    }

    private FormComponent addPasswordPanel(final IModel paramsModel, final String paramName) {

        final String resourceKey = RESOURCE_KEY_PREFIX + "." + paramName;

        final PasswordParamPanel pwdPanel =
                new PasswordParamPanel(
                        paramName,
                        new MapModel(paramsModel, paramName),
                        new ResourceModel(resourceKey, paramName),
                        true);
        add(pwdPanel);

        String defaultTitle = paramName;

        ResourceModel titleModel = new ResourceModel(resourceKey + ".title", defaultTitle);
        String title = String.valueOf(titleModel.getObject());

        pwdPanel.add(AttributeModifier.replace("title", title));

        return pwdPanel.getFormComponent();
    }

    private FormComponent addTextPanel(
            final IModel paramsModel, final String paramName, final boolean required) {
        return addTextPanel(paramsModel, paramName, paramName, required);
    }

    private FormComponent addTextPanel(
            final IModel paramsModel,
            final String paramName,
            final String paramTitle,
            final boolean required) {
        final String resourceKey = getClass().getSimpleName() + "." + paramName;

        final TextParamPanel textParamPanel =
                new TextParamPanel(
                        paramName,
                        new MapModel(paramsModel, paramTitle),
                        new ResourceModel(resourceKey, paramName),
                        required);
        textParamPanel.getFormComponent().setType(String.class /*param.type*/);

        String defaultTitle = paramTitle;

        ResourceModel titleModel = new ResourceModel(resourceKey + ".title", defaultTitle);
        String title = String.valueOf(titleModel.getObject());

        textParamPanel.add(AttributeModifier.replace("title", title));

        add(textParamPanel);
        return textParamPanel.getFormComponent();
    }

    public FormComponent[] getDependentFormComponents() {
        return new FormComponent[] {
            server, port, user, password, database, schema, table, fileext, importopt
        };
    }

    /** Setup a URL String composing all the required configuration options */
    public String buildURL() {
        StringBuilder builder = new StringBuilder("pgraster://");
        //
        // pgraster://USER:PASS@HOST:PORT:DATABASE.SCHEMA.TABLE@EPSGCODE:*.FILE_EXTENSION?OPTIONS#/PATH/TO/RASTER_TILES/"
        builder.append(user.getValue())
                .append(":")
                .append(password.getValue())
                .append("@")
                .append(server.getValue())
                .append(":")
                .append(port.getValue())
                .append(":")
                .append(database.getValue())
                .append(".")
                .append(schema.getValue())
                .append(".")
                .append(table.getValue());
        final CoordinateReferenceSystem crs =
                (CoordinateReferenceSystem) epsgcode.getModel().getObject();
        if (crs != null) {
            Integer code;
            try {
                code = CRS.lookupEpsgCode(crs, false);
                if (code != null) {
                    builder.append("@").append(code);
                }
            } catch (FactoryException e) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning("Unable to parse the specified CRS due to " + e.getMessage());
                }
            }
        }
        builder.append(":");
        final String fileExt = fileext.getValue();
        if (fileExt != null && fileExt.trim().length() > 0) {
            builder.append(fileExt);
        }
        final String options = importopt.getValue();
        if (options != null && options.trim().length() > 0) {
            builder.append("?").append(options);
        }
        builder.append("#");
        return builder.toString();
    }
}
