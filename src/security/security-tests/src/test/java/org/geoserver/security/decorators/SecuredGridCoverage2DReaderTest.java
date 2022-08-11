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

package org.geoserver.security.decorators;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.geoserver.catalog.Predicates;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.security.impl.SecureObjectsTest;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.junit.Test;
import org.opengis.coverage.grid.Format;
import org.opengis.filter.Filter;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;

public class SecuredGridCoverage2DReaderTest extends SecureObjectsTest {

    @Test
    public void testFilter() throws Exception {
        final Filter securityFilter = ECQL.toFilter("A > 10");
        final Filter requestFilter = ECQL.toFilter("B < 10");

        // create the mocks we need
        Format format = setupFormat();
        GridCoverage2DReader reader = createNiceMock(GridCoverage2DReader.class);
        expect(reader.getFormat()).andReturn(format).anyTimes();

        setupReadAssertion(reader, requestFilter, securityFilter);

        CoverageAccessLimits accessLimits =
                new CoverageAccessLimits(CatalogMode.HIDE, securityFilter, null, null);
        SecuredGridCoverage2DReader secured =
                new SecuredGridCoverage2DReader(reader, WrapperPolicy.readOnlyHide(accessLimits));

        final ParameterValue pv = ImageMosaicFormat.FILTER.createValue();
        pv.setValue(requestFilter);
        secured.read(new GeneralParameterValue[] {pv});
    }

    @Test
    public void testFilterOnStructured() throws Exception {
        final Filter securityFilter = ECQL.toFilter("A > 10");
        final Filter requestFilter = ECQL.toFilter("B < 10");
        DefaultSecureDataFactory factory = new DefaultSecureDataFactory();

        // create the mocks we need
        Format format = setupFormat();
        StructuredGridCoverage2DReader reader =
                createNiceMock(StructuredGridCoverage2DReader.class);
        expect(reader.getFormat()).andReturn(format).anyTimes();

        setupReadAssertion(reader, requestFilter, securityFilter);

        CoverageAccessLimits accessLimits =
                new CoverageAccessLimits(CatalogMode.HIDE, securityFilter, null, null);
        Object securedObject = factory.secure(reader, WrapperPolicy.readOnlyHide(accessLimits));
        assertTrue(securedObject instanceof SecuredStructuredGridCoverage2DReader);
        SecuredStructuredGridCoverage2DReader secured =
                (SecuredStructuredGridCoverage2DReader) securedObject;

        final ParameterValue pv = ImageMosaicFormat.FILTER.createValue();
        pv.setValue(requestFilter);
        secured.read(new GeneralParameterValue[] {pv});
    }

    private static void setupReadAssertion(
            GridCoverage2DReader reader, final Filter requestFilter, final Filter securityFilter)
            throws IOException {
        // the assertion
        expect(reader.read(isA(GeneralParameterValue[].class)))
                .andAnswer(
                        new IAnswer<GridCoverage2D>() {

                            @Override
                            public GridCoverage2D answer() throws Throwable {
                                GeneralParameterValue[] params =
                                        (GeneralParameterValue[]) EasyMock.getCurrentArguments()[0];
                                ParameterValue param = (ParameterValue) params[0];
                                Filter filter = (Filter) param.getValue();
                                assertEquals(Predicates.and(requestFilter, securityFilter), filter);
                                return null;
                            }
                        });
        EasyMock.replay(reader);
    }

    private Format setupFormat() {
        Format format = createNiceMock(Format.class);
        expect(format.getReadParameters())
                .andReturn(new ImageMosaicFormat().getReadParameters())
                .anyTimes();
        EasyMock.replay(format);
        return format;
    }
}
