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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;

/** @author Chris Berry http://opensource.atlassian.com/projects/spring/browse/SEC-531 */
public class RESTfulPathBasedFilterInvocationDefinitionMap
        implements FilterInvocationSecurityMetadataSource {

    private static Log log = LogFactory.getLog(RESTfulPathBasedFilterInvocationDefinitionMap.class);

    // ~ Instance fields
    // ================================================================================================

    private Collection<EntryHolder> requestMap = new ArrayList<>();
    private PathMatcher pathMatcher = new AntPathMatcher();
    private boolean convertUrlToLowercaseBeforeComparison = false;

    // ~ Methods
    // ========================================================================================================
    public boolean supports(Class clazz) {
        return FilterInvocation.class.isAssignableFrom(clazz);
    }

    public void addSecureUrl(
            String antPath, String[] httpMethods, Collection<ConfigAttribute> attrs) {
        requestMap.add(new EntryHolder(antPath, httpMethods, attrs));

        if (log.isDebugEnabled()) {
            log.debug(
                    "Added Ant path: "
                            + antPath
                            + "; attributes: "
                            + attrs
                            + ", httpMethods: "
                            + Arrays.toString(httpMethods));
        }
    }

    public void addSecureUrl(String antPath, Collection<ConfigAttribute> attrs) {
        throw new IllegalArgumentException(
                "addSecureUrl(String, Collection<ConfigAttribute> ) is INVALID for RESTfulDefinitionSource");
    }

    public Collection<ConfigAttribute> getAllConfigAttributes() {
        Set<ConfigAttribute> set = new HashSet<>();

        for (EntryHolder h : requestMap) {
            set.addAll(h.getConfigAttributes());
        }

        return set;
        // return set.iterator();
    }

    public int getMapSize() {
        return this.requestMap.size();
    }

    public boolean isConvertUrlToLowercaseBeforeComparison() {
        return convertUrlToLowercaseBeforeComparison;
    }

    public void setConvertUrlToLowercaseBeforeComparison(
            boolean convertUrlToLowercaseBeforeComparison) {
        this.convertUrlToLowercaseBeforeComparison = convertUrlToLowercaseBeforeComparison;
    }

    public Collection<ConfigAttribute> getAttributes(Object object)
            throws IllegalArgumentException {
        if ((object == null) || !this.supports(object.getClass())) {
            throw new IllegalArgumentException("Object must be a FilterInvocation");
        }

        String url = ((FilterInvocation) object).getRequestUrl();
        String method = ((FilterInvocation) object).getHttpRequest().getMethod();

        return this.lookupAttributes(url, method);
    }

    public Collection<ConfigAttribute> lookupAttributes(String url) {
        throw new IllegalArgumentException(
                "lookupAttributes(String url) is INVALID for RESTfulDefinitionSource");
    }

    public Collection<ConfigAttribute> lookupAttributes(String url, String httpMethod) {
        // Strip anything after a question mark symbol, as per SEC-161. See also SEC-321
        int firstQuestionMarkIndex = url.indexOf("?");

        if (firstQuestionMarkIndex != -1) {
            url = url.substring(0, firstQuestionMarkIndex);
        }

        if (isConvertUrlToLowercaseBeforeComparison()) {
            url = url.toLowerCase();

            if (log.isDebugEnabled()) {
                log.debug(
                        "Converted URL to lowercase, from: '"
                                + url
                                + "'; to: '"
                                + url
                                + "'  and httpMethod= "
                                + httpMethod);
            }
        }

        Iterator iter = requestMap.iterator();
        while (iter.hasNext()) {
            EntryHolder entryHolder = (EntryHolder) iter.next();

            String antPath = entryHolder.getAntPath();
            String[] methodList = entryHolder.getHttpMethodList();
            if (log.isDebugEnabled()) {
                log.debug(
                        "~~~~~~~~~~ antPath= "
                                + antPath
                                + " methodList= "
                                + Arrays.toString(methodList));
            }

            boolean matchedPath = pathMatcher.match(antPath, url);
            boolean matchedMethods = true;
            if (methodList != null) {
                matchedMethods = false;
                for (String s : methodList) {
                    if (s.equals(httpMethod)) {
                        matchedMethods = true;
                        break;
                    }
                }
            }
            if (log.isDebugEnabled())
                log.debug(
                        "Candidate is: '"
                                + url
                                + "'; antPath is "
                                + antPath
                                + "; matchedPath="
                                + matchedPath
                                + "; matchedMethods="
                                + matchedMethods);

            if (matchedPath && matchedMethods) {
                log.debug(
                        "returning "
                                + StringUtils.collectionToCommaDelimitedString(
                                        entryHolder.getConfigAttributes()));
                return entryHolder.getConfigAttributes();
            }
        }
        return null;
    }

    // ~ Inner Classes
    // ==================================================================================================

    protected class EntryHolder {
        private Collection<ConfigAttribute> configAttributes;
        private String antPath;
        private String[] httpMethodList;

        public EntryHolder(
                String antPath, String[] httpMethodList, Collection<ConfigAttribute> attrs) {
            this.antPath = antPath;
            this.configAttributes = attrs;
            this.httpMethodList = httpMethodList;
        }

        protected EntryHolder() {
            throw new IllegalArgumentException("Cannot use default constructor");
        }

        public String getAntPath() {
            return antPath;
        }

        public String[] getHttpMethodList() {
            return httpMethodList;
        }

        public Collection<ConfigAttribute> getConfigAttributes() {
            return configAttributes;
        }
    }
}
