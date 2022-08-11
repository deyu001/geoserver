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

package org.geoserver.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.springframework.util.StringUtils;

/**
 * List of filters applied to a pattern matching a set of requests.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class RequestFilterChain implements Serializable, Cloneable {

    /** */
    private static final long serialVersionUID = 1L;

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    /** The unique name of the chain */
    String name;

    /** The ANT patterns for this chain */
    List<String> patterns;

    /** The filter names */
    List<String> filterNames;

    /** Chain disabled ? */
    boolean disabled;

    /** Is this chain allowed to create an HTTP session ? */
    boolean allowSessionCreation;

    /** Does this chain accept SSL requests only */
    boolean requireSSL;

    /** Is this chain matching individual HTTP methods */
    boolean matchHTTPMethod;

    /** The set of HTTP methods to match against if {@link #matchHTTPMethod} is <code>true</code> */
    Set<HTTPMethod> httpMethods;

    String roleFilterName;

    public RequestFilterChain(String... patterns) {
        this.patterns = new ArrayList<>(Arrays.asList((patterns)));
        filterNames = new ArrayList<>();
        httpMethods = new TreeSet<>();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<String> getPatterns() {
        return patterns;
    }

    public List<String> getFilterNames() {
        return filterNames;
    }

    public abstract boolean isConstant();

    public void setFilterNames(String... filterNames) {
        setFilterNames(new ArrayList<>(Arrays.asList(filterNames)));
    }

    public void setFilterNames(List<String> filterNames) {
        this.filterNames = filterNames;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(patterns).append(":").append(filterNames);
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (isConstant() ? 1231 : 1237);
        result = prime * result + (isAllowSessionCreation() ? 17 : 19);
        result = prime * result + (isDisabled() ? 23 : 29);
        result = prime * result + (isRequireSSL() ? 31 : 37);
        result = prime * result + (isMatchHTTPMethod() ? 41 : 49);
        result = prime * ((roleFilterName == null) ? 1 : roleFilterName.hashCode());
        result = prime * result + ((httpMethods == null) ? 0 : httpMethods.hashCode());
        result = prime * result + ((filterNames == null) ? 0 : filterNames.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((patterns == null) ? 0 : patterns.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        RequestFilterChain other = (RequestFilterChain) obj;

        if (this.roleFilterName == null && other.roleFilterName != null) return false;
        if (this.roleFilterName != null
                && this.roleFilterName.equals(other.roleFilterName) == false) return false;

        if (this.isAllowSessionCreation() != other.isAllowSessionCreation()) return false;
        if (this.isDisabled() != other.isDisabled()) return false;
        if (this.isRequireSSL() != other.isRequireSSL()) return false;
        if (this.isMatchHTTPMethod() != other.isMatchHTTPMethod()) return false;

        if (filterNames == null) {
            if (other.filterNames != null) return false;
        } else if (!filterNames.equals(other.filterNames)) return false;

        if (httpMethods == null) {
            if (other.httpMethods != null) return false;
        } else if (!httpMethods.equals(other.httpMethods)) return false;

        if (name == null) {
            if (other.name != null) return false;
        } else if (!name.equals(other.name)) return false;
        if (patterns == null) {
            if (other.patterns != null) return false;
        } else if (!patterns.equals(other.patterns)) return false;
        return true;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        RequestFilterChain chain = (RequestFilterChain) super.clone();
        chain.setFilterNames(new ArrayList<>(filterNames));
        chain.patterns = new ArrayList<>(patterns);
        chain.httpMethods = new TreeSet<>(httpMethods);
        return chain;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public List<String> getCompiledFilterNames() {
        if (isDisabled() == true) return Collections.emptyList();

        List<String> result = new ArrayList<>();

        if (isRequireSSL()) result.add(GeoServerSecurityFilterChain.SSL_FILTER);

        if (isAllowSessionCreation())
            result.add(GeoServerSecurityFilterChain.SECURITY_CONTEXT_ASC_FILTER);
        else result.add(GeoServerSecurityFilterChain.SECURITY_CONTEXT_NO_ASC_FILTER);

        if (StringUtils.hasLength(getRoleFilterName())) result.add(getRoleFilterName());

        createCompiledFilterList(result);
        return result;
    }

    void createCompiledFilterList(List<String> list) {
        list.addAll(getFilterNames());
    }

    public boolean isAllowSessionCreation() {
        return allowSessionCreation;
    }

    public void setAllowSessionCreation(boolean allowSessionCreation) {
        this.allowSessionCreation = allowSessionCreation;
    }

    public boolean isRequireSSL() {
        return requireSSL;
    }

    public void setRequireSSL(boolean requireSSL) {
        this.requireSSL = requireSSL;
    }

    public boolean isMatchHTTPMethod() {
        return matchHTTPMethod;
    }

    public void setMatchHTTPMethod(boolean matchHTTPMethod) {
        this.matchHTTPMethod = matchHTTPMethod;
    }

    public Set<HTTPMethod> getHttpMethods() {
        return httpMethods;
    }

    public void setHttpMethods(Set<HTTPMethod> httpMethods) {
        this.httpMethods = httpMethods;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }

    public String getRoleFilterName() {
        return roleFilterName;
    }

    public void setRoleFilterName(String roleFilterName) {
        this.roleFilterName = roleFilterName;
    }

    public abstract boolean canBeRemoved();
}
