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

package org.vfny.geoserver.global.xml;

/**
 * NameSpaceElement purpose.
 *
 * <p>NameSpaceElement sub classes will represent a particular element found within a particular
 * namespace. Most of the methods below should return constants to improve performance.
 *
 * @author dzwiers, Refractions Research, Inc.
 * @author $Author: dmzwiers $ (last modification)
 * @version $Id$
 */
public abstract class NameSpaceElement {
    /** the namespace prefix to use for qualification */
    protected final String prefix;

    /**
     * NameSpaceElement constructor.
     *
     * <p>Creates an instance of this NameSpaceElement.
     *
     * <p>The prefix is to be used for the qualification routines. If the prefix passed is null,
     * then qualified names will be null unless they have a prefix specified.
     *
     * @param prefix The prefix to use for qualification.
     */
    public NameSpaceElement(String prefix) {
        this.prefix = prefix;
    }

    /**
     * NameSpaceElement constructor.
     *
     * <p>Creates an instance of this NameSpaceElement.
     *
     * <p>The prefix is to be used for the qualification routines is set to null. the qualified
     * names of the elements will be null
     */
    public NameSpaceElement() {
        this.prefix = null;
    }

    /**
     * getTypeDefName purpose.
     *
     * <p>This will return the name of the definition of the element. This method is useful when
     * defining a new type and wish to extend an existing defined type, such as <code>
     * gml:AbstractFeatureType</code>.
     *
     * <p><code>
     * <xs:complexType name="Lines_Type">
     *   <xs:complexContent>
     *     <xs:extension base="gml:AbstractFeatureType">
     *      <xs:sequence>
     *        <xs:element name="id" type="xs:string"/>
     *      <xs:element ref="gml:lineStringProperty" minOccurs="0"/>
     *     </xs:sequence>
     *    </xs:extension>
     *   </xs:complexContent>
     * </xs:complexType>
     * </code>
     *
     * @return The type def. name, for the above example AbstractFeatureType.
     */
    public abstract String getTypeDefName();

    /**
     * getTypeDefName purpose.
     *
     * <p>This will return the name of the element. This method is useful when defining a new
     * element and wish to extend an existing element, such as <code>xs:string</code>.
     *
     * <p><code>
     * <xs:element name="id" type="xs:string"/>
     * <xs:element ref="gml:lineStringProperty" minOccurs="0"/>
     * </code>
     *
     * @return The element name, for the above example string or lineStringProperty.
     */
    public abstract String getTypeRefName();

    /**
     * getQualifiedTypeDefName purpose.
     *
     * <p>Returns a qualified type definition name <code>prefix:definition name</code>.
     *
     * @return the name if the default prefix is non null, null otherwise
     * @see #getTypeDefName()
     */
    public abstract String getQualifiedTypeDefName();

    /**
     * getQualifiedTypeRefName purpose.
     *
     * <p>Returns a qualified type reference name <code>prefix:reference name</code>.
     *
     * @return the name if the default prefix is non null, null otherwise
     * @see #getTypeRefName()
     */
    public abstract String getQualifiedTypeRefName();

    /**
     * getQualifiedTypeDefName purpose.
     *
     * <p>Returns a qualified type definition name <code>prefix:definition name</code> with the
     * specified prefix.
     *
     * @param prefix The prefix to use for qualification.
     * @return the name if either the specified or default prefix is non null, null otherwise
     * @see #getTypeDefName()
     */
    public abstract String getQualifiedTypeDefName(String prefix);

    /**
     * getQualifiedTypeRefName purpose.
     *
     * <p>Returns a qualified type reference name <code>prefix:reference name</code> with the
     * specified prefix.
     *
     * @param prefix The prefix to use for qualification.
     * @return the name if either the specified or default prefix is non null, null otherwise
     * @see #getTypeRefName()
     */
    public abstract String getQualifiedTypeRefName(String prefix);

    /**
     * getJavaClass purpose.
     *
     * <p>Returns an instance of the Class object which would best represent this element.
     *
     * <p>for example an element of type xs:int would return <code>Integer.class</code>.
     *
     * @return Class instance of the Class object which would best represent this element.
     */
    public abstract Class getJavaClass();

    /**
     * This is a bit of a hack, so that GeoServer can generate with the best (default) xml mappings
     * for each Java class. This should be implemented the other way around, with a nice lookup
     * table to get the one and only default. But this is far easier to implement, as we just add
     * this method set to true for the namespace element classes we like best. If for some reason we
     * set two NSE's that map to the same java class to true then things will behave randomly, which
     * is why this is a bit of a hack. Apologies, it's late, and I need to finish my docs.
     */
    public boolean isDefault() {
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getQualifiedTypeDefName(prefix);
    }

    public abstract boolean isAbstract();
}
