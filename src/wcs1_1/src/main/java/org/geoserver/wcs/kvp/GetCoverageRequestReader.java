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

package org.geoserver.wcs.kvp;

import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.MissingParameterValue;

import java.util.Map;
import net.opengis.ows11.BoundingBoxType;
import net.opengis.wcs11.DomainSubsetType;
import net.opengis.wcs11.GetCoverageType;
import net.opengis.wcs11.GridCrsType;
import net.opengis.wcs11.OutputType;
import net.opengis.wcs11.TimeSequenceType;
import net.opengis.wcs11.Wcs111Factory;
import org.geoserver.catalog.Catalog;
import org.geoserver.ows.kvp.EMFKvpRequestReader;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;

public class GetCoverageRequestReader extends EMFKvpRequestReader {

    Catalog catalog;

    public GetCoverageRequestReader(Catalog catalog) {
        super(GetCoverageType.class, Wcs111Factory.eINSTANCE);
        this.catalog = catalog;
    }

    @Override
    public Object read(Object request, Map<String, Object> kvp, Map<String, Object> rawKvp)
            throws Exception {
        GetCoverageType getCoverage = (GetCoverageType) super.read(request, kvp, rawKvp);

        // grab coverage info to perform further checks
        if (getCoverage.getIdentifier() == null)
            throw new WcsException(
                    "identifier parameter is mandatory", MissingParameterValue, "identifier");

        // build the domain subset
        getCoverage.setDomainSubset(parseDomainSubset(kvp));

        // build output element
        getCoverage.setOutput(parseOutputElement(kvp));

        return getCoverage;
    }

    private DomainSubsetType parseDomainSubset(Map kvp) {
        final DomainSubsetType domainSubset = Wcs111Factory.eINSTANCE.createDomainSubsetType();

        // either bbox or timesequence must be there
        BoundingBoxType bbox = (BoundingBoxType) kvp.get("BoundingBox");
        TimeSequenceType timeSequence = (TimeSequenceType) kvp.get("TimeSequence");
        if (timeSequence == null && bbox == null)
            throw new WcsException(
                    "Bounding box cannot be null, TimeSequence has not been specified",
                    WcsExceptionCode.MissingParameterValue,
                    "BoundingBox");

        domainSubset.setBoundingBox(bbox);
        domainSubset.setTemporalSubset(timeSequence);

        return domainSubset;
    }

    private OutputType parseOutputElement(Map kvp) throws Exception {
        final OutputType output = Wcs111Factory.eINSTANCE.createOutputType();
        output.setGridCRS(Wcs111Factory.eINSTANCE.createGridCrsType());

        // check and set store
        Boolean store = (Boolean) kvp.get("store");
        if (store != null) output.setStore(store.booleanValue());

        // check and set format
        String format = (String) kvp.get("format");
        if (format == null)
            throw new WcsException(
                    "format parameter is mandatory", MissingParameterValue, "format");
        output.setFormat(format);

        // set the other gridcrs properties
        final GridCrsType gridCRS = output.getGridCRS();
        gridCRS.setGridBaseCRS((String) kvp.get("gridBaseCrs"));

        String gridType = (String) kvp.get("gridType");
        if (gridType == null) {
            gridType = gridCRS.getGridType();
        }
        gridCRS.setGridType(gridType);

        String gridCS = (String) kvp.get("gridCS");
        if (gridCS == null) {
            gridCS = gridCRS.getGridCS();
        }
        gridCRS.setGridCS(gridCS);
        gridCRS.setGridOrigin(kvp.get("GridOrigin"));
        gridCRS.setGridOffsets(kvp.get("GridOffsets"));

        return output;
    }
}
