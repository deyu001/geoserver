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

package org.geoserver.test;

import java.util.Optional;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.feature.NameImpl;
import org.junit.Test;
import org.opengis.feature.type.Name;
import org.w3c.dom.Document;

/** Testing for Coordinates formatting configurations on WFS GML 3.1 & 3.2 on complex features */
public class CoordinatesFormattingTest extends StationsAppSchemaTestSupport {

    @Test
    public void testCoordinateFormatWfs11() throws Exception {
        enableCoordinatesFormattingGml31();
        Document doc =
                getAsDOM(
                        "st_gml31/wfs?request=GetFeature&version=1.1.0&typename=st_gml31:Station_gml31");
        checkCount(
                WFS11_XPATH_ENGINE,
                doc,
                1,
                "//wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31[@gml:id=\"st.1\"]/st_gml31:location/gml:Point/gml:pos"
                        + "[text()=\"1.00000000 -1.00000000\"]");
        // check force decimal notation
        checkCount(
                WFS11_XPATH_ENGINE,
                doc,
                1,
                "//wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31[@gml:id=\"st.2\"]/st_gml31:location/gml:Point/gml:pos"
                        + "[text()=\"0.00000010 -0.00000010\"]");
    }

    @Test
    public void testCoordinateFormatDisabledWfs11() throws Exception {
        disableCoordinatesFormattingGml31();
        Document doc =
                getAsDOM(
                        "st_gml31/wfs?request=GetFeature&version=1.1.0&typename=st_gml31:Station_gml31");
        checkCount(
                WFS11_XPATH_ENGINE,
                doc,
                1,
                "//wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31[@gml:id=\"st.1\"]/st_gml31:location/gml:Point/gml:pos"
                        + "[text()=\"1 -1\"]");
        // check force decimal notation
        checkCount(
                WFS11_XPATH_ENGINE,
                doc,
                1,
                "//wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31[@gml:id=\"st.2\"]/st_gml31:location/gml:Point/gml:pos"
                        + "[text()=\"1.0E-7 -1.0E-7\"]");
    }

    @Test
    public void testCoordinateFormatWfs20() throws Exception {
        enableCoordinatesFormattingGml32();
        Document document =
                getAsDOM("wfs?request=GetFeature&version=2.0&typename=st_gml32:Station_gml32");
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "//wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32[@gml:id=\"st.1\"]/st_gml32:location/gml:Point/gml:pos"
                        + "[text()=\"1.00000000 -1.00000000\"]");
        // check force decimal notation
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "//wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32[@gml:id=\"st.2\"]/st_gml32:location/gml:Point/gml:pos"
                        + "[text()=\"0.00000010 -0.00000010\"]");
    }

    @Test
    public void testCoordinateFormatDisabledWfs20() throws Exception {
        disableCoordinatesFormattingGml32();
        Document document =
                getAsDOM("wfs?request=GetFeature&version=2.0&typename=st_gml32:Station_gml32");
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "//wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32[@gml:id=\"st.1\"]/st_gml32:location/gml:Point/gml:pos"
                        + "[text()=\"1 -1\"]");
        // check force decimal notation
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "//wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32[@gml:id=\"st.2\"]/st_gml32:location/gml:Point/gml:pos"
                        + "[text()=\"1.0E-7 -1.0E-7\"]");
    }

    private void enableCoordinatesFormattingGml31() {
        enableCoordinateFormatting(
                new NameImpl(StationsMockData.STATIONS_PREFIX_GML31, "Station_gml31"));
    }

    private void disableCoordinatesFormattingGml31() {
        disableCoordinateFormatting(
                new NameImpl(StationsMockData.STATIONS_PREFIX_GML31, "Station_gml31"));
    }

    private void enableCoordinatesFormattingGml32() {
        enableCoordinateFormatting(
                new NameImpl(StationsMockData.STATIONS_PREFIX_GML32, "Station_gml32"));
    }

    private void disableCoordinatesFormattingGml32() {
        disableCoordinateFormatting(
                new NameImpl(StationsMockData.STATIONS_PREFIX_GML32, "Station_gml32"));
    }

    private void enableCoordinateFormatting(Name qname) {
        FeatureTypeInfo info =
                getGeoServer().getCatalog().getResourceByName(qname, FeatureTypeInfo.class);
        info.setNumDecimals(8);
        info.setForcedDecimal(true);
        info.setPadWithZeros(true);
        getGeoServer().getCatalog().save(info);
    }

    private void disableCoordinateFormatting(Name qname) {
        FeatureTypeInfo info =
                getGeoServer().getCatalog().getResourceByName(qname, FeatureTypeInfo.class);
        info.setForcedDecimal(false);
        info.setPadWithZeros(false);
        getGeoServer().getCatalog().save(info);
    }

    @Override
    protected StationsMockData createTestData() {
        return new StationsMockData() {
            @Override
            protected Optional<String> extraStationFeatures() {
                String features =
                        "\nst.2=st.2|station2|32154895|station2@stations.org|POINT(-1.0E-7 1.0E-7)";
                return Optional.of(features);
            }

            @Override
            protected Optional<String> extraMeasurementFeatures() {
                String features = "\nms.3=ms.3|wind|km/h|st.2";
                return Optional.of(features);
            }
        };
    }
}
