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

package org.geoserver.security.web.auth;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.geoserver.security.HTTPMethod;
import org.geoserver.security.RequestFilterChain;
import org.springframework.util.StringUtils;

/**
 * Model for {@link RequestFilterChain}
 *
 * @author christian
 */
public class RequestFilterChainWrapper implements Serializable {

    private static final long serialVersionUID = 1L;
    RequestFilterChain chain;

    public RequestFilterChainWrapper(RequestFilterChain chain) {
        this.chain = chain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestFilterChainWrapper that = (RequestFilterChainWrapper) o;
        return Objects.equals(chain, that.chain);
    }

    public void setName(String name) {
        chain.setName(name);
    }

    public String getName() {
        return chain.getName();
    }

    public List<String> getPatterns() {
        return chain.getPatterns();
    }

    public void setPatterns(List<String> patterns) {
        chain.setPatterns(patterns);
    }

    public List<String> getFilterNames() {
        return chain.getFilterNames();
    }

    public void setFilterNames(String... filterNames) {
        chain.setFilterNames(filterNames);
    }

    public void setFilterNames(List<String> filterNames) {
        chain.setFilterNames(filterNames);
    }

    public int hashCode() {
        return chain.hashCode();
    }

    public boolean isDisabled() {
        return chain.isDisabled();
    }

    public void setDisabled(boolean disabled) {
        chain.setDisabled(disabled);
    }

    public boolean isAllowSessionCreation() {
        return chain.isAllowSessionCreation();
    }

    public void setAllowSessionCreation(boolean allowSessionCreation) {
        chain.setAllowSessionCreation(allowSessionCreation);
    }

    public boolean isRequireSSL() {
        return chain.isRequireSSL();
    }

    public void setRequireSSL(boolean requireSSL) {
        chain.setRequireSSL(requireSSL);
    }

    public boolean isMatchHTTPMethod() {
        return chain.isMatchHTTPMethod();
    }

    public void setMatchHTTPMethod(boolean matchHTTPMethod) {
        chain.setMatchHTTPMethod(matchHTTPMethod);
    }

    public Set<HTTPMethod> getHttpMethods() {
        return chain.getHttpMethods();
    }

    public void setHttpMethods(Set<HTTPMethod> httpMethods) {
        chain.setHttpMethods(httpMethods);
    }

    public String getPatternString() {
        if (chain.getPatterns() != null)
            return StringUtils.collectionToCommaDelimitedString(chain.getPatterns());
        else return "";
    }

    public void setPatternString(String patternString) {
        if (StringUtils.hasLength(patternString))
            chain.setPatterns(
                    Arrays.asList(StringUtils.commaDelimitedListToStringArray(patternString)));
        else chain.getPatterns().clear();
    }

    public boolean isGET() {
        return chain.getHttpMethods().contains(HTTPMethod.GET);
    }

    public void setGET(boolean gET) {
        if (gET) chain.getHttpMethods().add(HTTPMethod.GET);
        else chain.getHttpMethods().remove(HTTPMethod.GET);
    }

    public boolean isPUT() {
        return chain.getHttpMethods().contains(HTTPMethod.PUT);
    }

    public void setPUT(boolean pUT) {
        if (pUT) chain.getHttpMethods().add(HTTPMethod.PUT);
        else chain.getHttpMethods().remove(HTTPMethod.PUT);
    }

    public boolean isDELETE() {
        return chain.getHttpMethods().contains(HTTPMethod.DELETE);
    }

    public void setDELETE(boolean dELETE) {
        if (dELETE) chain.getHttpMethods().add(HTTPMethod.DELETE);
        else chain.getHttpMethods().remove(HTTPMethod.DELETE);
    }

    public boolean isPOST() {
        return chain.getHttpMethods().contains(HTTPMethod.POST);
    }

    public void setPOST(boolean pOST) {
        if (pOST) chain.getHttpMethods().add(HTTPMethod.POST);
        else chain.getHttpMethods().remove(HTTPMethod.POST);
    }

    public boolean isOPTIONS() {
        return chain.getHttpMethods().contains(HTTPMethod.OPTIONS);
    }

    public void setOPTIONS(boolean oPTIONS) {
        if (oPTIONS) chain.getHttpMethods().add(HTTPMethod.OPTIONS);
        else chain.getHttpMethods().remove(HTTPMethod.OPTIONS);
    }

    public boolean isTRACE() {
        return chain.getHttpMethods().contains(HTTPMethod.TRACE);
    }

    public void setTRACE(boolean tRACE) {
        if (tRACE) chain.getHttpMethods().add(HTTPMethod.TRACE);
        else chain.getHttpMethods().remove(HTTPMethod.TRACE);
    }

    public boolean isHEAD() {
        return chain.getHttpMethods().contains(HTTPMethod.HEAD);
    }

    public void setHEAD(boolean hEAD) {
        if (hEAD) chain.getHttpMethods().add(HTTPMethod.HEAD);
        else chain.getHttpMethods().remove(HTTPMethod.HEAD);
    }

    public RequestFilterChain getChain() {
        return chain;
    }

    public String getHttpMethodString() {
        if (chain.isMatchHTTPMethod())
            return StringUtils.collectionToCommaDelimitedString(chain.getHttpMethods());
        else return "*";
    }

    public String getRoleFilterName() {
        return chain.getRoleFilterName();
    }

    public void setRoleFilterName(String roleFilterName) {
        chain.setRoleFilterName(roleFilterName);
    }
}
