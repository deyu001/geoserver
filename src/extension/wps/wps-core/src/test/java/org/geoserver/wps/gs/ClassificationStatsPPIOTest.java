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

package org.geoserver.wps.gs;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.custommonkey.xmlunit.XMLAssert;
import org.geotools.process.classify.ClassificationStats;
import org.geotools.process.vector.FeatureClassStats;
import org.jaitools.numeric.Range;
import org.jaitools.numeric.Statistic;
import org.jaitools.numeric.StreamingSampleStats;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;

public class ClassificationStatsPPIOTest {

    @Test
    public void testSanity() throws Exception {
        List<Range<Double>> ranges =
                Arrays.asList(
                        Range.create(0d, true, 10d, false), Range.create(10d, true, 20d, true));

        StreamingSampleStats s1 = new StreamingSampleStats();
        s1.setStatistic(Statistic.MEAN);
        s1.addRange(ranges.get(0));
        s1.offer(10d);

        StreamingSampleStats s2 = new StreamingSampleStats();
        s2.setStatistic(Statistic.MEAN);
        s2.addRange(ranges.get(0));
        s2.offer(10d);

        StreamingSampleStats[] stats = new StreamingSampleStats[] {s1, s2};

        ClassificationStats classStats = new FeatureClassStats.Results(ranges, stats);

        ClassificationStatsPPIO ppio = new ClassificationStatsPPIO();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ppio.encode(classStats, bout);

        Document doc =
                DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(new ByteArrayInputStream(bout.toByteArray()));

        assertEquals("Results", doc.getDocumentElement().getNodeName());
        XMLAssert.assertXpathExists("/Results/Class[@lowerBound='0.0']", doc);
        XMLAssert.assertXpathExists("/Results/Class[@lowerBound='10.0']", doc);
    }

    @Test
    public void testNamespacesNotNull() throws Exception {
        ContentHandler h = createNiceMock(ContentHandler.class);
        h.startElement(notNull(), notNull(), eq("Results"), anyObject());
        expectLastCall().times(1);

        replay(h);

        new ClassificationStatsPPIO().encode(newStats(), h);

        verify(h);
    }

    ClassificationStats newStats() {
        List<Range<Double>> ranges =
                Arrays.asList(
                        Range.create(0d, true, 10d, false), Range.create(10d, true, 20d, true));

        StreamingSampleStats s1 = new StreamingSampleStats();
        s1.setStatistic(Statistic.MEAN);
        s1.addRange(ranges.get(0));
        s1.offer(10d);

        StreamingSampleStats s2 = new StreamingSampleStats();
        s2.setStatistic(Statistic.MEAN);
        s2.addRange(ranges.get(0));
        s2.offer(10d);

        StreamingSampleStats[] stats = new StreamingSampleStats[] {s1, s2};

        return new FeatureClassStats.Results(ranges, stats);
    }
}
