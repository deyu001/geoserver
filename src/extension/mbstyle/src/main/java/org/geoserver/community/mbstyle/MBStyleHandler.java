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

package org.geoserver.community.mbstyle;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleType;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geotools.mbstyle.MapBoxStyle;
import org.geotools.styling.ResourceLocator;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Version;
import org.json.simple.parser.ParseException;
import org.xml.sax.EntityResolver;

/** Style handler for MBStyle */
public class MBStyleHandler extends StyleHandler {

    public static final String FORMAT = "mbstyle";

    public static final String MIME_TYPE = "application/vnd.geoserver.mbstyle+json";

    static final Map<StyleType, String> TEMPLATES = new HashMap<>();

    static {
        try {
            TEMPLATES.put(
                    StyleType.GENERIC,
                    IOUtils.toString(
                            MBStyleHandler.class.getResourceAsStream("template_generic.json"),
                            "UTF-8"));
            TEMPLATES.put(
                    StyleType.POINT,
                    IOUtils.toString(
                            MBStyleHandler.class.getResourceAsStream("template_point.json"),
                            "UTF-8"));
            TEMPLATES.put(
                    StyleType.POLYGON,
                    IOUtils.toString(
                            MBStyleHandler.class.getResourceAsStream("template_polygon.json"),
                            "UTF-8"));
            TEMPLATES.put(
                    StyleType.LINE,
                    IOUtils.toString(
                            MBStyleHandler.class.getResourceAsStream("template_line.json"),
                            "UTF-8"));
            TEMPLATES.put(
                    StyleType.RASTER,
                    IOUtils.toString(
                            MBStyleHandler.class.getResourceAsStream("template_raster.json"),
                            "UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException("Error loading up the style templates", e);
        }
    }

    private SLDHandler sldHandler;

    protected MBStyleHandler(SLDHandler sldHandler) {
        super("MBStyle", FORMAT);
        this.sldHandler = sldHandler;
    }

    @Override
    public StyledLayerDescriptor parse(
            Object input,
            Version version,
            ResourceLocator resourceLocator,
            EntityResolver entityResolver)
            throws IOException {
        // see if we can use the style cache, some conversions are expensive.
        if (input instanceof File) {
            // convert to resource, to avoid code duplication
            File jsonFile = (File) input;
            input = new FileSystemResourceStore(jsonFile.getParentFile()).get(jsonFile.getName());
        }

        if (input instanceof Resource) {
            Resource jsonResource = (Resource) input;
            Resource sldResource =
                    jsonResource
                            .parent()
                            .get(FilenameUtils.getBaseName(jsonResource.name()) + ".sld");
            if (sldResource.getType() != Resource.Type.UNDEFINED
                    && sldResource.lastmodified() > jsonResource.lastmodified()) {
                // if sld resource exists, use it
                return sldHandler.parse(
                        sldResource, SLDHandler.VERSION_10, resourceLocator, entityResolver);
            } else {
                // otherwise convert and write the cache
                try (Reader reader = toReader(input)) {
                    StyledLayerDescriptor sld = convertToSLD(reader);
                    try (OutputStream fos = sldResource.out()) {
                        sldHandler.encode(sld, SLDHandler.VERSION_10, true, fos);
                    }
                    return sldHandler.parse(
                            sldResource, SLDHandler.VERSION_10, resourceLocator, entityResolver);
                } catch (ParseException e) {
                    throw new IOException(e);
                }
            }
        }

        // in this case, just do a plain on the fly conversion
        try (Reader reader = toReader(input)) {
            return convertToSLD(toReader(input));
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }

    private StyledLayerDescriptor convertToSLD(Reader reader) throws IOException, ParseException {
        return MapBoxStyle.parse(reader);
    }

    @Override
    public void encode(
            StyledLayerDescriptor sld, Version version, boolean pretty, OutputStream output)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Exception> validate(Object input, Version version, EntityResolver entityResolver)
            throws IOException {
        return MapBoxStyle.validate(toReader(input));
    }

    @Override
    public String mimeType(Version version) {
        return MIME_TYPE;
    }

    @Override
    public String getFileExtension() {
        return "json";
    }

    @Override
    public String getCodeMirrorEditMode() {
        return "application/json";
    }

    @Override
    public String getStyle(StyleType type, Color color, String colorName, String layerName) {
        String template = TEMPLATES.get(type);
        String colorCode = Integer.toHexString(color.getRGB());
        colorCode = colorCode.substring(2, colorCode.length());
        return template.replace("${colorName}", colorName)
                .replace("${colorCode}", "#" + colorCode)
                .replace("${layerName}", layerName);
    }
}
