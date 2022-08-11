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

package org.geoserver.wfs;

import java.util.Collections;
import java.util.List;
import org.geoserver.ExtendedCapabilitiesProvider;
import org.geoserver.wfs.request.GetCapabilitiesRequest;
import org.geotools.util.Version;

/**
 * This interface is essentially an alias for the Generic ExtendedCapabilitiesProvider so that the
 * Type Parameters do not need to be declared everywhere and so that when loading extensions there
 * is a distinct class of beans to load
 *
 * @author Jesse Eichar, camptocamp
 */
public interface WFSExtendedCapabilitiesProvider
        extends ExtendedCapabilitiesProvider<WFSInfo, GetCapabilitiesRequest> {

    /**
     * Returns the extra profiles this plugin is adding to the base specification for the given
     * service version. Called only for version >= 2.0.
     *
     * @return A non null list of implemented profiles (eventually empty)
     */
    default List<String> getProfiles(Version version) {
        return Collections.emptyList();
    }

    /**
     * Allows extension points to examine and eventually alter top level operation metadata
     * constraints (the default implementation does nothing). Called only for version >= 2.0.
     */
    default void updateRootOperationConstraints(Version version, List<DomainType> constraints) {
        // does nothing
    }

    /**
     * Allows extension points to examine and eventually alter single operation metadata. Called
     * only for version >= 1.1.
     *
     * @param version The service version
     * @param operations The operations
     */
    default void updateOperationMetadata(Version version, List<OperationMetadata> operations) {
        // does nothing
    }
}
