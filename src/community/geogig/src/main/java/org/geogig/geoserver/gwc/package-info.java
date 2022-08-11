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
 * Package {@code org.geogig.geoserver.gwc} integrates GeoServer configured geogig data stores with GeoWebCache.
 * <p>
 * <H2>Truncate GWC tiles<H2>
 * <p>
 * The {@link org.geogig.geoserver.gwc.TruncateTilesOnUpdateRefHook} command hook is declared
 * in {@code src/main/resources/META-INF/services/org.geogig.api.hooks.CommandHook}, following GeoGIG's
 * SPI mechanism to declare "classpath" command hooks.
 * <p>
 * This command hook captures calls to {@link org.locationtech.geogig.api.plumbing.UpdateRef} commands in any
 * configured {@link org.locationtech.geogig.geotools.data.GeoGigDataStore geogig datastore}, and figures out
 * which {@link org.geoserver.gwc.layer.GeoServerTileLayer tile layers} would be affected by the change.
 * <p>
 * For the affected tile layers, the {@link org.geogig.geoserver.gwc.MinimalDiffBounds} command is used to compute
 * the so called "minimal bounds" of the diff between the old and new trees pointed by the ref update and
 * the layer's tree path, which is a geometry that's big enough to cover the changes but much smaller than
 * the whole bounds, so that the number of tiles truncated is minimized.
 * <p>
 * That geometry is then used as a mask to issue GWC truncate tasks for each of the layer's configured gridset and styles.
 */

package org.geogig.geoserver.gwc;
