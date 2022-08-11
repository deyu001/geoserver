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

package org.geoserver.security.impl;

import java.util.Properties;
import org.geotools.util.SuppressFBWarnings;
import org.springframework.security.core.GrantedAuthority;

/**
 * Extends {@link GrantedAuthority} and represents an anonymous role
 *
 * <p>If a user name is set, the role is personalized
 *
 * <p>Example: the role ROLE_EMPLOYEE could have a role parameter EPLOYEE_NUMBER
 *
 * @author christian
 */
public class GeoServerRole implements GrantedAuthority, Comparable<GeoServerRole> {

    /** Pre-defined role assigned to adminstrator. */
    public static final GeoServerRole ADMIN_ROLE = new GeoServerRole("ROLE_ADMINISTRATOR");

    /** Pre-defined role assigned to group adminstrators. */
    public static final GeoServerRole GROUP_ADMIN_ROLE = new GeoServerRole("ROLE_GROUP_ADMIN");

    /** Pre-defined role assigned to any authenticated user. */
    public static final GeoServerRole AUTHENTICATED_ROLE = new GeoServerRole("ROLE_AUTHENTICATED");

    /** Pre-defined wildcard role. */
    public static final GeoServerRole ANY_ROLE = new GeoServerRole("*");

    /** Predefined anonymous role */
    public static final GeoServerRole ANONYMOUS_ROLE = new GeoServerRole("ROLE_ANONYMOUS");

    /** Geoserver system roles */
    public static final GeoServerRole[] SystemRoles =
            new GeoServerRole[] {ADMIN_ROLE, GROUP_ADMIN_ROLE, AUTHENTICATED_ROLE, ANONYMOUS_ROLE};

    /** Mappable system roles */
    public static final GeoServerRole[] MappedRoles =
            new GeoServerRole[] {ADMIN_ROLE, GROUP_ADMIN_ROLE};

    /** Roles which cannot be assigned to a user or a group */
    public static final GeoServerRole[] UnAssignableRoles =
            new GeoServerRole[] {AUTHENTICATED_ROLE, ANONYMOUS_ROLE};

    private static final long serialVersionUID = 1L;

    protected String userName;
    protected Properties properties;
    protected String role;

    public GeoServerRole(String role) {
        this.role = role;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public boolean isAnonymous() {
        return getUserName() == null;
    }

    /**
     * Generic mechanism to store additional information (role paramaters)
     *
     * <p>examples: a user with the role ROLE_EMPLOYEE could have a role parameter EMPLOYEE_NUMBER
     * To be filled by the backend store
     */
    public Properties getProperties() {
        if (properties == null) properties = new Properties();
        return properties;
    }

    public int compareTo(GeoServerRole o) {
        if (o == null) return 1;
        if (getAuthority().equals(o.getAuthority())) {
            if (getUserName() == null && o.getUserName() == null) return 0;
            if (getUserName() == null) return -1;
            if (o.getUserName() == null) return 1;
            return getUserName().compareTo(o.getUserName());
        }
        return getAuthority().compareTo(o.getAuthority());
    }

    // not sure why the equals would compare against types that are not a GeoServerRole
    // suppressing for the moment...
    @SuppressFBWarnings("EQ_CHECK_FOR_OPERAND_NOT_COMPATIBLE_WITH_THIS")
    public boolean equals(Object obj) {
        if (obj == null) return false;

        if (obj instanceof String && getUserName() == null) {
            return equalsWithoutUserName(obj);
        }

        if (obj instanceof GrantedAuthority && getUserName() == null) {
            if (obj instanceof GeoServerRole == false) return equalsWithoutUserName(obj);
        }

        if (obj instanceof GeoServerRole) {
            return compareTo((GeoServerRole) obj) == 0;
        }
        return false;
    }

    public boolean equalsWithoutUserName(Object obj) {
        if (obj instanceof String) {
            return obj.equals(this.role);
        }
        return this.role.equals(((GrantedAuthority) obj).getAuthority());
    }

    public int hashCode() {
        int hash = getAuthority().hashCode();
        if (getUserName() != null) hash += getUserName().hashCode();
        return hash;
    }

    public String toString() {
        if (getUserName() != null) {
            StringBuffer buff = new StringBuffer(role);
            buff.append(" for user ").append(getUserName());
            return buff.toString();
        } else return role;
    }

    @Override
    public String getAuthority() {
        return role;
    }
}
