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

package org.geoserver.config.util;

import java.util.List;
import java.util.Map;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.ServiceLoader;

/**
 * Base class for service loaders loading from the legacy service.xml file.
 *
 * <p>
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public abstract class LegacyServiceLoader<T extends ServiceInfo> implements ServiceLoader<T> {

    /** reader pointing to services.xml */
    LegacyServicesReader reader;

    /**
     * Sets the legacy services.xml reader.
     *
     * <p>This method is called by the GeoServer startup, it should not be called by client code.
     */
    public void setReader(LegacyServicesReader reader) {
        this.reader = reader;
    }

    /**
     * Loads the service.
     *
     * <p>This method calls through to {@link #load(LegacyServicesReader, GeoServer)}
     */
    public final T load(GeoServer gs) throws Exception {
        return load(reader, gs);
    }

    /**
     * Creates the service configuration object.
     *
     * <p>Subclasses implementing this method can use the {@link #readCommon(ServiceInfo, Map,
     * GeoServer)} method to read those attributes common to all services.
     *
     * @param reader The services.xml reader.
     */
    public abstract T load(LegacyServicesReader reader, GeoServer geoServer) throws Exception;

    /**
     * Reads all the common attributes from the service info class.
     *
     * <p>This method is intended to be called by subclasses after creating an instance of
     * ServiceInfo. Example:
     *
     * <pre>
     *   // read properties
     *   Map<String,Object> props = reader.wfs();
     *
     *   // create config object
     *   WFSInfo wfs = new WFSInfoImpl();
     *
     *   //load common properties
     *   load( wfs, reader );
     *
     *   //load wfs specific properties
     *   wfs.setServiceLevel( map.get( "serviceLevel") );
     *   ...
     * </pre>
     */
    protected void readCommon(ServiceInfo service, Map<String, Object> properties, GeoServer gs)
            throws Exception {

        service.setEnabled((Boolean) properties.get("enabled"));
        service.setName((String) properties.get("name"));
        service.setTitle((String) properties.get("title"));
        service.setAbstract((String) properties.get("abstract"));

        Map metadataLink = (Map) properties.get("metadataLink");
        if (metadataLink != null) {
            MetadataLinkInfo ml = gs.getCatalog().getFactory().createMetadataLink();
            ml.setAbout((String) metadataLink.get("about"));
            ml.setMetadataType((String) metadataLink.get("metadataType"));
            ml.setType((String) metadataLink.get("type"));
            service.setMetadataLink(ml);
        }

        @SuppressWarnings("unchecked")
        List<String> keywords = (List<String>) properties.get("keywords");
        if (keywords != null) {
            for (String kw : keywords) {
                service.getKeywords().add(new Keyword(kw));
            }
        }

        service.setOnlineResource((String) properties.get("onlineResource"));
        service.setFees((String) properties.get("fees"));
        service.setAccessConstraints((String) properties.get("accessConstraints"));
        service.setCiteCompliant((Boolean) properties.get("citeConformanceHacks"));
        service.setMaintainer((String) properties.get("maintainer"));
        service.setSchemaBaseURL((String) properties.get("SchemaBaseUrl"));
    }

    public void save(T service, GeoServer gs) throws Exception {
        // do nothing, saving implemented elsewhere
    }

    @Override
    public T create(GeoServer gs) throws Exception {
        throw new UnsupportedOperationException("Use xstream loader equivalent instead");
    }
}
