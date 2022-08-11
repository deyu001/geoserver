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

package org.geoserver.notification.common;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.mapper.Mapper;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.geoserver.platform.GeoServerExtensions;

public class EncoderXStreamInitializer implements NotificationXStreamInitializer {

    /** Alias for encoder tag of in xml configuration */
    public String name;

    /** Class to use for encoder with filed 'name' */
    public Class<? extends NotificationEncoder> clazz;

    /**
     * Define an alias for the {@link DefaultNotificationProcessor#encoder encoder}<br>
     * Define a class for the {@link NotificationEncoder}<br>
     * An example of encoder configuration section in notifier.xml is:
     *
     * <pre>{@code
     *  <genericProcessor>
     *           <geonodeEncoder>
     *           ...
     *           </geonodeEncoder>
     * </genericProcessor>
     *
     * }</pre>
     *
     * @param xs XStream object
     */
    public EncoderXStreamInitializer(String name, Class<? extends NotificationEncoder> clazz) {
        super();
        this.name = name;
        this.clazz = clazz;
    }

    @Override
    public void init(XStream xs) {
        xs.aliasAttribute(DefaultNotificationProcessor.class, "encoder", name);
        xs.registerLocalConverter(
                DefaultNotificationProcessor.class,
                "encoder",
                new EncoderConverter(xs.getMapper(), xs.getReflectionProvider(), this));
    }

    /** @author Alessio Fabiani, GeoSolutions S.A.S. */
    public static class EncoderConverter extends ReflectionConverter {

        private EncoderXStreamInitializer encoderXStreamInitializer;

        /** */
        public EncoderConverter(
                Mapper mapper,
                ReflectionProvider reflectionProvider,
                EncoderXStreamInitializer encoderXStreamInitializer) {
            super(mapper, reflectionProvider);
            this.encoderXStreamInitializer = encoderXStreamInitializer;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public boolean canConvert(Class clazz) {
            return clazz.isAssignableFrom(encoderXStreamInitializer.clazz);
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            NotificationEncoder encoder = null;
            String nodeName = reader.getNodeName();

            List<EncoderXStreamInitializer> serializers =
                    GeoServerExtensions.extensions(EncoderXStreamInitializer.class);

            for (EncoderXStreamInitializer serializer : serializers) {
                if (serializer.name.equals(nodeName)) {
                    try {
                        encoder = serializer.clazz.getDeclaredConstructor().newInstance();
                        encoder =
                                (NotificationEncoder)
                                        context.convertAnother(encoder, serializer.clazz);
                        break;
                    } catch (InstantiationException
                            | IllegalAccessException
                            | NoSuchMethodException
                            | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            return encoder;
        }
    }
}
