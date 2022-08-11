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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;

/**
 * Base test class for GeoServer unit tests.
 *
 * <p>Deriving from this test class provides the test case with preconfigured geoserver and catalog
 * objects.
 *
 * <p>This test case provides a spring application context which loads the application contexts from
 * all modules on the classpath.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public abstract class GeoServerTestSupport extends GeoServerAbstractTestSupport {

    @Override
    public MockData buildTestData() throws Exception {
        // create the data directory
        MockData dataDirectory = new MockData();
        populateDataDirectory(dataDirectory);
        return dataDirectory;
    }

    public MockData getTestData() {
        return (MockData) super.getTestData();
    }

    /**
     * Adds the desired type and coverages to the data directory. This method adds all well known
     * data types, subclasses may add their extra ones or decide to avoid the standar ones and build
     * a custom list calling {@link MockData#addPropertiesType(QName, java.net.URL, java.net.URL)}
     * and {@link MockData#addCoverage(QName, InputStream, String)}
     */
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        // set up the data directory
        dataDirectory.addWellKnownTypes(MockData.TYPENAMES);
    }

    /**
     * Sets up a template in a feature type directory.
     *
     * @param featureTypeName The name of the feature type.
     * @param template The name of the template.
     * @param body The content of the template.
     */
    protected void setupTemplate(QName featureTypeName, String template, String body)
            throws IOException {

        getTestData()
                .copyToFeatureTypeDirectory(
                        new ByteArrayInputStream(body.getBytes()), featureTypeName, template);
    }
}
