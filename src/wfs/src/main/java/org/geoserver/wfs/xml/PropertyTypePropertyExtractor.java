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

package org.geoserver.wfs.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.namespace.QName;
import net.opengis.wfs.PropertyType;
import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.XSDFactory;
import org.eclipse.xsd.XSDParticle;
import org.eclipse.xsd.XSDTypeDefinition;
import org.geotools.xsd.PropertyExtractor;
import org.geotools.xsd.SchemaIndex;
import org.geotools.xsd.Schemas;
import org.opengis.feature.type.Name;

/**
 * Extracts properties from an instance of {@link PropertyType}.
 *
 * <p>In a sense this class retypes {@link PropertyType#getValue()} to a new xml type so that the
 * encoder can encode it properly.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class PropertyTypePropertyExtractor implements PropertyExtractor {
    /** index for looking up xml types */
    SchemaIndex index;

    public PropertyTypePropertyExtractor(SchemaIndex index) {
        this.index = index;
    }

    public boolean canHandle(Object object) {
        return object instanceof PropertyType;
    }

    public List<Object[]> properties(Object object, XSDElementDeclaration element) {
        PropertyType property = (PropertyType) object;

        List<Object[]> properties = new ArrayList<>(2);

        // the Name particle we can use as is
        properties.add(
                new Object[] {
                    Schemas.getChildElementParticle(element.getType(), "Name", false),
                    property.getName()
                });

        // the Value particle we must retype

        // first guess its type
        QName newTypeName = guessValueType(property.getValue());
        XSDTypeDefinition type =
                (newTypeName != null) ? index.getTypeDefinition(newTypeName) : null;

        if (type != null) {
            // create a new particle based on the new type
            XSDElementDeclaration value = XSDFactory.eINSTANCE.createXSDElementDeclaration();
            value.setName("Value");
            value.setTypeDefinition(type);

            XSDParticle particle = XSDFactory.eINSTANCE.createXSDParticle();
            particle.setMinOccurs(1);
            particle.setMaxOccurs(1);
            particle.setContent(value);

            properties.add(new Object[] {particle, property.getValue()});
        } else {
            // coudl not determine new type, just fall back to xs:anyType
            Object[] p =
                    new Object[] {
                        Schemas.getChildElementParticle(element.getType(), "Value", false),
                        property.getValue()
                    };
            properties.add(p);
        }

        return properties;
    }

    private QName guessValueType(Object value) {
        Class clazz = value.getClass();
        List profiles = Arrays.asList(new Object[] {new XSProfile(), new GML3Profile()});

        for (Object o : profiles) {
            TypeMappingProfile profile = (TypeMappingProfile) o;
            Name name = profile.name(clazz);

            if (name != null) {
                return new QName(name.getNamespaceURI(), name.getLocalPart());
            }
        }

        return null;
    }
}
