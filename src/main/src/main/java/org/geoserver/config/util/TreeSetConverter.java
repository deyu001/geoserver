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

/*
 * Copyright (C) 2004, 2005 Joe Walnes.
 * Copyright (C) 2006, 2007, 2010, 2011, 2013, 2014, 2016 XStream Committers.
 * All rights reserved.
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 * Created on 08. May 2004 by Joe Walnes
 */
package org.geoserver.config.util;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.core.util.PresortedSet;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Converts a java.util.TreeSet to XML, and serializes the associated java.util.Comparator. The
 * converter assumes that the elements in the XML are already sorted according the comparator.
 *
 * <p>Cloned from XStream in order to avoid illegal reflective lookup warnings, might introduce
 * loading issues if there are circular references stored in the TreeSet. We don't have those right
 * now, the alternative would be open the java.util package for deep reflection from the command
 * line
 *
 * @author Joe Walnes
 * @author J&ouml;rg Schaible
 */
public class TreeSetConverter extends CollectionConverter {
    public TreeSetConverter(Mapper mapper) {
        super(mapper, TreeSet.class);
    }

    public void marshal(
            Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        SortedSet sortedSet = (SortedSet) source;
        TreeMapConverter.marshalComparator(mapper(), sortedSet.comparator(), writer, context);
        super.marshal(source, writer, context);
    }

    @SuppressWarnings("unchecked")
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        TreeSet result = null;
        Comparator unmarshalledComparator =
                TreeMapConverter.unmarshalComparator(mapper(), reader, context, null);
        boolean inFirstElement = unmarshalledComparator instanceof Mapper.Null;
        Comparator comparator = inFirstElement ? null : unmarshalledComparator;
        final PresortedSet set = new PresortedSet(comparator);
        result = comparator == null ? new TreeSet() : new TreeSet(comparator);
        if (inFirstElement) {
            // we are already within the first element
            addCurrentElementToCollection(reader, context, result, set);
            reader.moveUp();
        }
        populateCollection(reader, context, result, set);
        if (set.size() > 0) {
            result.addAll(set); // comparator will not be called if internally optimized
        }
        return result;
    }
}
