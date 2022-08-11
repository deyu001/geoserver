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

/*
 * Created on April 20, 2005
 *
 */
package org.vfny.geoserver.util;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.util.URLs;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class GETMAPValidator {

    public GETMAPValidator() {}

    /**
     * validates against the "normal" location of the schema (ie.
     * ".../capabilities/sld/StyleLayerDescriptor.xsd" uses the geoserver_home patch
     */
    public List<SAXException> validateGETMAP(InputStream xml) {
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);

        Resource schema = loader.get("data/capabilities/sld/GetMap.xsd");
        File schemaFile = schema.file();
        try {
            return validateGETMAP(xml, URLs.fileToUrl(schemaFile));
        } catch (Exception e) {
            List<SAXException> al = new ArrayList<>();
            al.add(new SAXException(e));

            return al;
        }
    }

    public static String getErrorMessage(InputStream xml, List<? extends Exception> errors) {
        return getErrorMessage(new InputStreamReader(xml), errors);
    }

    /**
     * returns a better formated error message - suitable for framing. There's a more complex
     * version in StylesEditorAction.
     *
     * <p>This will kick out a VERY LARGE errorMessage.
     */
    public static String getErrorMessage(Reader xml, List<? extends Exception> errors) {
        return SLDValidator.getErrorMessage(xml, errors);
    }

    public List<SAXException> validateGETMAP(InputStream xml, URL SchemaUrl) {
        return validateGETMAP(new InputSource(xml), SchemaUrl);
    }

    public List<SAXException> validateGETMAP(InputSource xml, ServletContext servContext) {

        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);

        Resource schema = loader.get("data/capabilities/sld/GetMap.xsd");
        File schemaFile = schema.file();

        //        File schemaFile = new File(GeoserverDataDirectory.getGeoserverDataDirectory(),
        //                "/data/capabilities/sld/GetMap.xsd");

        try {
            return validateGETMAP(xml, URLs.fileToUrl(schemaFile));
        } catch (Exception e) {
            List<SAXException> al = new ArrayList<>();
            al.add(new SAXException(e));

            return al;
        }
    }

    /**
     * validate a GETMAP against the schema
     *
     * @param xml input stream representing the GETMAP file
     * @param SchemaUrl location of the schemas. Normally use
     *     ".../capabilities/sld/StyleLayerDescriptor.xsd"
     * @return list of SAXExceptions (0 if the file's okay)
     */
    public List<SAXException> validateGETMAP(InputSource xml, URL SchemaUrl) {
        EntityResolverProvider provider = GeoServerExtensions.bean(EntityResolverProvider.class);
        return ResponseUtils.validate(xml, SchemaUrl, true, provider.getEntityResolver());
    }
}
