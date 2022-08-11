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

package org.geoserver.importer.rest;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import java.beans.PropertyDescriptor;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.rest.converters.ImportJSONReader;
import org.geoserver.importer.rest.converters.ImportJSONWriter;
import org.geoserver.importer.rest.converters.ImportJSONWriter.FlushableJSONBuilder;
import org.geoserver.importer.transform.ImportTransform;
import org.geoserver.rest.RequestInfo;
import org.geotools.data.DataTestCase;
import org.springframework.beans.BeanUtils;
import org.springframework.web.context.request.AbstractRequestAttributes;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/** @author Ian Schneider <ischneider@opengeo.org> */
public abstract class TransformTestSupport extends DataTestCase {

    public void doJSONTest(ImportTransform transform) throws Exception {
        StringWriter buffer = new StringWriter();

        Importer im = createNiceMock(Importer.class);
        RequestInfo ri = createNiceMock(RequestInfo.class);

        replay(im, ri);

        RequestAttributes oldAttributes = RequestContextHolder.getRequestAttributes();
        RequestContextHolder.setRequestAttributes(new TransformTestSupport.MapRequestAttributes());

        RequestInfo.set(ri);

        ImportJSONWriter jsonio = new ImportJSONWriter(im);
        FlushableJSONBuilder builder = new FlushableJSONBuilder(buffer);

        ImportContext c = new ImportContext(0);
        c.addTask(new ImportTask());

        jsonio.transform(builder, transform, 0, c.task(0), true, 1);

        ImportJSONReader reader = new ImportJSONReader(im);
        ImportTransform transform2 = reader.transform(buffer.toString());
        PropertyDescriptor[] pd = BeanUtils.getPropertyDescriptors(transform.getClass());

        for (PropertyDescriptor propertyDescriptor : pd) {
            assertEquals(
                    "expected same value of " + propertyDescriptor.getName(),
                    propertyDescriptor.getReadMethod().invoke(transform),
                    propertyDescriptor.getReadMethod().invoke(transform2));
        }
        RequestContextHolder.setRequestAttributes(oldAttributes);
    }

    public static class MapRequestAttributes extends AbstractRequestAttributes {
        Map<String, Object> requestAttributes = new HashMap<>();

        @Override
        public Object getAttribute(String name, int scope) {
            return requestAttributes.get(name);
        }

        @Override
        public void setAttribute(String name, Object value, int scope) {
            requestAttributes.put(name, value);
        }

        @Override
        public void removeAttribute(String name, int scope) {
            requestAttributes.remove(name);
        }

        @Override
        public String[] getAttributeNames(int scope) {
            return requestAttributes.keySet().toArray(new String[requestAttributes.size()]);
        }

        @Override
        protected void updateAccessedSessionAttributes() {}

        @Override
        public void registerDestructionCallback(String name, Runnable callback, int scope) {}

        @Override
        public Object resolveReference(String key) {
            return null;
        }

        @Override
        public String getSessionId() {
            return null;
        }

        @Override
        public Object getSessionMutex() {
            return null;
        }
    }
}
