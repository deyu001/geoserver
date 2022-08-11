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

package org.geoserver.rest.converters;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import java.io.IOException;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.wrapper.RestHttpInputWrapper;
import org.geoserver.rest.wrapper.RestListWrapper;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/** Message converter implementation for JSON serialization via XStream */
public class XStreamJSONMessageConverter extends XStreamMessageConverter<Object> {

    static final MediaType TEXT_JSON = MediaType.valueOf("text/json");

    public XStreamJSONMessageConverter() {
        super(MediaType.APPLICATION_JSON, TEXT_JSON);
    }

    @Override
    public String getExtension() {
        return "json";
    }

    @Override
    public String getMediaType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        //        if( RestWrapper.class.isAssignableFrom(clazz) ){
        //            return !RestListWrapper.class.isAssignableFrom(clazz); // we can only write
        // RestWrapper, not RestListWrapper
        //        }
        return true; // reading objects is fine
    }
    //
    // reading
    //
    @Override
    public Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        XStreamPersister p = xpf.createJSONPersister();
        p.setCatalog(catalog);
        if (inputMessage instanceof RestHttpInputWrapper) {
            ((RestHttpInputWrapper) inputMessage).configurePersister(p, this);
        }
        return p.load(inputMessage.getBody(), clazz);
    }

    //
    // writing
    //
    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        // we can only write RestWrapper, not RestListWrapper
        return !RestListWrapper.class.isAssignableFrom(clazz)
                && RestWrapper.class.isAssignableFrom(clazz)
                && canWrite(mediaType);
    }

    @Override
    public void writeInternal(Object o, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        XStreamPersister xmlPersister = xpf.createJSONPersister();
        xmlPersister.setCatalog(catalog);
        xmlPersister.setReferenceByName(true);
        xmlPersister.setExcludeIds();
        if (o instanceof RestWrapper) {
            ((RestWrapper<?>) o).configurePersister(xmlPersister, this);
            o = ((RestWrapper<?>) o).getObject();
        }
        xmlPersister.save(o, outputMessage.getBody());
    }

    @Override
    public void encodeLink(String link, HierarchicalStreamWriter writer) {
        writer.startNode("href");
        writer.setValue(href(link));
        writer.endNode();
    }

    @Override
    public void encodeCollectionLink(String link, HierarchicalStreamWriter writer) {
        writer.setValue(href(link));
    }

    @Override
    protected XStream createXStreamInstance() {
        return new SecureXStream(new JettisonMappedXmlDriver());
    }
}
