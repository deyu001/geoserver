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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geotools.feature.type.ProfileImpl;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.Schema;

/**
 * A special profile of a pariticular {@link Schema} which maintains a unique mapping of java class
 * to xml schema type.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class TypeMappingProfile /*extends ProfileImpl*/ {

    /** Set of profiles to do mappings from. */
    Set /*<Profile>*/ profiles;

    //    public TypeMappingProfile(Schema schema, Set profile) {
    //        super(schema, profile);
    //    }

    public TypeMappingProfile(Set profiles) {
        this.profiles = profiles;
    }

    /**
     * Obtains the {@link AttributeDescriptor} mapped to a particular class.
     *
     * <p>If an exact match cannot be made, then those types which are supertypes of clazz are
     * examined.
     *
     * @param clazz The class.
     * @return The AttributeType, or <code>null</code> if no atttribute type mapped to <code>clazz
     *     </code>
     */
    public AttributeType type(Class clazz) {
        List<AttributeType> assignable = new ArrayList<>();

        for (Object o : profiles) {
            ProfileImpl profile = (ProfileImpl) o;

            for (AttributeType type : profile.values()) {
                if (type.getBinding().isAssignableFrom(clazz)) {
                    assignable.add(type);
                }

                if (clazz.equals(type.getBinding())) {
                    return type;
                }
            }
        }

        if (assignable.isEmpty()) {
            return null;
        }

        if (assignable.size() == 1) {
            return assignable.get(0);
        } else {
            // sort
            Comparator<AttributeType> comparator =
                    new Comparator<AttributeType>() {
                        public int compare(AttributeType a1, AttributeType a2) {
                            Class<?> c1 = a1.getBinding();
                            Class<?> c2 = a2.getBinding();

                            if (c1.equals(c2)) {
                                return 0;
                            }

                            if (c1.isAssignableFrom(c2)) {
                                return 1;
                            }

                            return -1;
                        }
                    };

            Collections.sort(assignable, comparator);

            if (!assignable.get(0).equals(assignable.get(1))) {
                return assignable.get(0);
            }
        }

        return null;
    }

    /**
     * Obtains the {@link Name} of the {@link AttributeDescriptor} mapped to a particular class.
     *
     * @param clazz The class.
     * @return The Name, or <code>null</code> if no atttribute type mapped to <code>clazz</code>
     */
    public Name name(Class clazz) {
        List<Map.Entry> assignable = new ArrayList<>();

        for (Object o : profiles) {
            ProfileImpl profile = (ProfileImpl) o;

            for (Map.Entry<Name, AttributeType> nameAttributeTypeEntry : profile.entrySet()) {
                Map.Entry entry = (Map.Entry) nameAttributeTypeEntry;
                AttributeType type = (AttributeType) entry.getValue();

                if (type.getBinding().isAssignableFrom(clazz)) {
                    assignable.add(entry);
                }

                if (clazz.equals(type.getBinding())) {
                    return (Name) entry.getKey();
                }
            }
        }

        if (assignable.isEmpty()) {
            return null;
        }

        if (assignable.size() == 1) {
            return (Name) assignable.get(0).getKey();
        } else {
            // sort
            Comparator<Map.Entry> comparator =
                    new Comparator<Map.Entry>() {
                        public int compare(Map.Entry e1, Map.Entry e2) {
                            AttributeType a1 = (AttributeType) e1.getValue();
                            AttributeType a2 = (AttributeType) e2.getValue();

                            Class<?> c1 = a1.getBinding();
                            Class<?> c2 = a2.getBinding();

                            if (c1.equals(c2)) {
                                return 0;
                            }

                            if (c1.isAssignableFrom(c2)) {
                                return 1;
                            }

                            return -1;
                        }
                    };

            Collections.sort(assignable, comparator);

            Map.Entry e1 = assignable.get(0);
            Map.Entry e2 = assignable.get(1);
            AttributeType a1 = (AttributeType) e1.getValue();
            AttributeType a2 = (AttributeType) e2.getValue();

            if (!a1.equals(a2)) {
                return (Name) e1.getKey();
            }
        }

        return null;
    }
}
