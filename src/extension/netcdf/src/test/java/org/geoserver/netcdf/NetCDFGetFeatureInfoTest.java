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


package org.geoserver.netcdf;

import java.util.HashMap;
import javax.xml.namespace.QName;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Assert;
import org.junit.Test;

/** Tests for WMS GetFeatureInfo on a layer sourced from NetCDF. */
public class NetCDFGetFeatureInfoTest extends WMSTestSupport {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        testData.addRasterLayer(
                new QName(MockData.SF_URI, "analyzed_sst", MockData.SF_PREFIX),
                "test-data/sst.nc",
                "nc",
                new HashMap(),
                getClass(),
                getCatalog());
        // workaround for SystemTestData assumption that rasters with a single coverage
        // should use the store name for the coverage name
        CoverageInfo ci = getCatalog().getCoverageByName("sf:analyzed_sst");
        ci.setNativeCoverageName("analyzed_sst");
        getCatalog().save(ci);
    }

    /**
     * Test that an XML GetFeatureInfo response contains a property whose name has been normalised
     * to a valid NCName.
     *
     * <p>The NetCDF source has <code>analyzed_sst:long_name ="Analyzed Sea Surface Temperature"
     * </code>, which must be converted to a valid NCName before it can be used in an XML response.
     * The implementation converts spaces to underscores to achieve this.
     */
    @Test
    public void testValidXmlNcName() throws Exception {
        String response =
                getAsString(
                        "wms?service=WMS&version=1.3.0&request=GetFeatureInfo"
                                + "&layers=sf%3Aanalyzed_sst&query_layers=sf%3Aanalyzed_sst"
                                + "&format=image/png&info_format=text/xml"
                                + "&srs=EPSG%3A4326&bbox=25,-86,27,-83&width=100&height=100&x=37&y=50");
        Assert.assertTrue(response.contains("<wfs:FeatureCollection"));
        Assert.assertTrue(
                response.contains(
                        "<sf:Analyzed_Sea_Surface_Temperature>23.0</sf:Analyzed_Sea_Surface_Temperature>"));
    }
}
