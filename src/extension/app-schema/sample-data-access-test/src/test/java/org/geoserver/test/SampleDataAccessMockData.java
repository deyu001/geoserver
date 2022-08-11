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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.data.CatalogWriter;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.util.IOUtils;
import org.geotools.appschema.resolver.data.SampleDataAccess;
import org.geotools.appschema.resolver.data.SampleDataAccessData;
import org.geotools.appschema.resolver.data.SampleDataAccessFactory;
import org.locationtech.jts.geom.Envelope;

/**
 * Mock data for testing integration of {@link SampleDataAccess} with GeoServer.
 *
 * <p>Inspired by {@link MockData}.
 */
public class SampleDataAccessMockData extends SystemTestData {

    public static final String DATASTORE_NAME = "datastore";

    /** Use FeatureTypeInfo constants for srs handling as values */
    public static final String KEY_SRS_HANDLINGS = "srsHandling";

    /** The feature type alias, a string */
    public static final String KEY_ALIAS = "alias";

    /** The style name */
    public static final String KEY_STYLE = "style";

    /** The srs code (a number) for this layer */
    public static final String KEY_SRS_NUMBER = "srs";

    /** The lon/lat envelope as a JTS Envelope */
    public static final String KEY_LL_ENVELOPE = "ll_envelope";

    /** The native envelope as a JTS Envelope */
    public static final String KEY_NATIVE_ENVELOPE = "native_envelope";

    static final Envelope DEFAULT_ENVELOPE = new Envelope(-180, 180, -90, 90);

    /** the 'featureTypes' directory, under 'data' */
    File featureTypes;

    /** Constructor. Creates empty mock data directory. */
    public SampleDataAccessMockData() throws IOException {
        data = IOUtils.createRandomDirectory("./target", "sample-data-access-mock", "data");
        data.delete();
        data.mkdir();

        // create a featureTypes directory
        featureTypes = new File(data, "featureTypes");
        featureTypes.mkdir();

        info(
                DATASTORE_NAME,
                SampleDataAccessData.NAMESPACE_PREFIX,
                SampleDataAccessData.MAPPEDFEATURE_TYPE_NAME.getLocalPart());
        // need to add nested type at top level so type definition is loaded into global schema and
        // can be found during encoding
        info(
                DATASTORE_NAME,
                SampleDataAccessData.NAMESPACE_PREFIX,
                SampleDataAccessData.GEOLOGICUNIT_TYPE_NAME.getLocalPart());
    }

    /**
     * Returns the root of the mock data directory,
     *
     * @see org.geoserver.data.test.TestData#getDataDirectoryRoot()
     */
    public File getDataDirectoryRoot() {
        return data;
    }

    /**
     * Returns true.
     *
     * @see org.geoserver.data.test.TestData#isTestDataAvailable()
     */
    public boolean isTestDataAvailable() {
        return true;
    }

    /**
     * Configures mock data directory.
     *
     * @see org.geoserver.data.test.TestData#setUp()
     */
    @SuppressWarnings("PMD.JUnit4TestShouldUseBeforeAnnotation")
    public void setUp() throws Exception {
        setUpCatalog();
        copyTo(MockData.class.getResourceAsStream("services.xml"), "services.xml");
    }

    @Override
    public void setUpDefault() throws Exception {
        // do nothing
    }

    /**
     * Removes the mock data directory.
     *
     * @see org.geoserver.data.test.TestData#tearDown()
     */
    @SuppressWarnings("PMD.JUnit4TestShouldUseAfterAnnotation")
    public void tearDown() throws Exception {
        IOUtils.delete(data);
        data = null;
    }

    /** Writes catalog.xml to the data directory. */
    @SuppressWarnings("serial")
    protected void setUpCatalog() throws IOException {
        CatalogWriter writer = new CatalogWriter();
        writer.dataStores(
                new HashMap<String, Map<String, Serializable>>() {
                    {
                        put(DATASTORE_NAME, SampleDataAccessFactory.PARAMS);
                    }
                },
                new HashMap<String, String>() {
                    {
                        put(DATASTORE_NAME, SampleDataAccessData.NAMESPACE_PREFIX);
                    }
                },
                Collections.<String>emptySet());
        writer.coverageStores(
                new HashMap<String, Map<String, String>>(),
                new HashMap<String, String>(),
                Collections.<String>emptySet());
        writer.namespaces(
                new HashMap<String, String>() {
                    {
                        put(
                                SampleDataAccessData.NAMESPACE_PREFIX,
                                SampleDataAccessData.NAMESPACE_URI);
                    }
                });
        writer.styles(Collections.<String, String>emptyMap());
        writer.write(new File(data, "catalog.xml"));
    }

    /**
     * Copies from an {@link InputStream} to path under the mock data directory.
     *
     * @param input source from which file content is copied
     * @param location path relative to mock data directory
     */
    public void copyTo(InputStream input, String location) throws IOException {
        IOUtils.copy(input, new File(getDataDirectoryRoot(), location));
    }

    /** Stolen from {@link MockData}. */
    public void info(String datastore, String prefix, String type) throws IOException {

        // prepare extra params default
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_STYLE, "Default");
        params.put(KEY_SRS_HANDLINGS, 2);
        params.put(KEY_ALIAS, null);

        Integer srs = 4326;

        params.put(KEY_SRS_NUMBER, srs);

        File featureTypeDir = new File(featureTypes, prefix + "_" + type);
        featureTypeDir.mkdir();

        File info = new File(featureTypeDir, "info.xml");
        info.delete();
        info.createNewFile();

        try (FileWriter writer = new FileWriter(info)) {
            writer.write("<featureType datastore=\"" + datastore + "\">");
            writer.write("<name>" + type + "</name>");
            if (params.get(KEY_ALIAS) != null)
                writer.write("<alias>" + params.get(KEY_ALIAS) + "</alias>");
            writer.write("<SRS>" + params.get(KEY_SRS_NUMBER) + "</SRS>");
            // this mock type may have wrong SRS compared to the actual one in the property files...
            // let's configure SRS handling not to alter the original one, and have 4326 used only
            // for capabilities
            writer.write("<SRSHandling>" + params.get(KEY_SRS_HANDLINGS) + "</SRSHandling>");
            writer.write("<title>" + type + "</title>");
            writer.write("<abstract>abstract about " + type + "</abstract>");
            writer.write("<numDecimals value=\"8\"/>");
            writer.write("<keywords>" + type + "</keywords>");
            Envelope llEnvelope = (Envelope) params.get(KEY_LL_ENVELOPE);
            if (llEnvelope == null) llEnvelope = DEFAULT_ENVELOPE;
            writer.write(
                    "<latLonBoundingBox dynamic=\"false\" minx=\""
                            + llEnvelope.getMinX()
                            + "\" miny=\""
                            + llEnvelope.getMinY()
                            + "\" maxx=\""
                            + llEnvelope.getMaxX()
                            + "\" maxy=\""
                            + llEnvelope.getMaxY()
                            + "\"/>");

            Envelope nativeEnvelope = (Envelope) params.get(KEY_NATIVE_ENVELOPE);
            if (nativeEnvelope != null)
                writer.write(
                        "<nativeBBox dynamic=\"false\" minx=\""
                                + nativeEnvelope.getMinX()
                                + "\" miny=\""
                                + nativeEnvelope.getMinY()
                                + "\" maxx=\""
                                + nativeEnvelope.getMaxX()
                                + "\" maxy=\""
                                + nativeEnvelope.getMaxY()
                                + "\"/>");

            String style = (String) params.get(KEY_STYLE);
            if (style == null) style = "Default";
            writer.write("<styles default=\"" + style + "\"/>");

            writer.write("</featureType>");

            writer.flush();
        }
    }
}
