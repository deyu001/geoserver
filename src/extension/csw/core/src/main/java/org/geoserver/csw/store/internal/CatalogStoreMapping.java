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

package org.geoserver.csw.store.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.csw.records.RecordDescriptor;
import org.geotools.data.complex.util.XPathUtil;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.util.logging.Logging;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

/**
 * Catalog Store Mapping An instance from this class provides a mapping from the data in the
 * Internal Geoserver Catalog to a particular CSW Record Type
 *
 * @author Niels Charlier
 */
public class CatalogStoreMapping {

    /**
     * A Catalog Store Mapping Element, provides mapping of particular attribute.
     *
     * @author Niels Charlier
     */
    public static class CatalogStoreMappingElement {
        protected String key;

        protected Expression content = null;

        protected boolean required = false;

        protected int[] splitIndex = {};

        /**
         * Create new Mapping Element
         *
         * @param key The Key to be mapped
         */
        protected CatalogStoreMappingElement(String key) {
            this.key = key;
        }

        /**
         * Getter for mapped key
         *
         * @return Mapped Key
         */
        public String getKey() {
            return key;
        }

        /**
         * Mapper for mapped content expression
         *
         * @return content
         */
        public Expression getContent() {
            return content;
        }

        /**
         * Getter for required property
         *
         * @return true if property is required
         */
        public boolean isRequired() {
            return required;
        }

        /**
         * Getter for splitIndex property
         *
         * @return splitIndex
         */
        public int[] getSplitIndex() {
            return splitIndex;
        }
    }

    protected static final Logger LOGGER = Logging.getLogger(CatalogStoreMapping.class);

    protected static final FilterFactory ff = CommonFactoryFinder.getFilterFactory();

    protected Map<String, CatalogStoreMappingElement> mappingElements = new HashMap<>();

    protected CatalogStoreMappingElement identifier = null;

    protected boolean includeEnvelope = true;

    /** Create new Catalog Store Mapping */
    protected CatalogStoreMapping() {}

    /**
     * Return Collection of all Elements
     *
     * @return a Collection with all Mapping Elements
     */
    public final Collection<CatalogStoreMappingElement> elements() {
        return mappingElements.values();
    }

    /**
     * Return a mapping element from a mapped key
     *
     * @param key the mapped key
     * @return the element, null if key doesn't exist
     */
    public CatalogStoreMappingElement getElement(String key) {
        return mappingElements.get(key);
    }

    /**
     * Getter for the identifier element, provides identifier expression for features
     *
     * @return the identifier element
     */
    public CatalogStoreMappingElement getIdentifierElement() {
        return identifier;
    }

    /**
     * Create a submapping from a list of property names Required properties will also be included.
     *
     * @param properties list of property names to be included in submapping
     * @param rd Record Descriptor
     */
    public CatalogStoreMapping subMapping(List<PropertyName> properties, RecordDescriptor rd) {
        Set<String> paths = new HashSet<>();
        for (PropertyName prop : properties) {
            paths.add(
                    toDotPath(
                            XPathUtil.steps(
                                    rd.getFeatureDescriptor(),
                                    prop.toString(),
                                    rd.getNamespaceSupport())));
        }

        CatalogStoreMapping mapping = new CatalogStoreMapping();

        for (Entry<String, CatalogStoreMappingElement> element : mappingElements.entrySet()) {
            if (element.getValue().isRequired() || paths.contains(element.getKey())) {
                mapping.mappingElements.put(element.getKey(), element.getValue());
            }
        }

        mapping.identifier = identifier;

        mapping.includeEnvelope =
                includeEnvelope && paths.contains(rd.getBoundingBoxPropertyName());

        return mapping;
    }

    public void setIncludeEnvelope(boolean value) {
        this.includeEnvelope = value;
    }

    public boolean isIncludeEnvelope() {
        return includeEnvelope;
    }

    /**
     * Parse a Textual representation of the mapping to create a CatalogStoreMapping
     *
     * <p>The textual representation is a set of key-value pairs, where the key represents the
     * mapped key and the value is an OGC expression representing the mapped content. Furthermore,
     * if the key starts with @ it also defines the ID element and if the key starts with $ it is a
     * required property.
     */
    public static CatalogStoreMapping parse(Map<String, String> mappingSource) {

        CatalogStoreMapping mapping = new CatalogStoreMapping();
        for (Map.Entry<String, String> mappingEntry : mappingSource.entrySet()) {

            String key = mappingEntry.getKey();
            boolean required = false;
            boolean id = false;
            if ("$".equals(key.substring(0, 1))) {
                key = key.substring(1);
                required = true;
            }
            if ("@".equals(key.substring(0, 1))) {
                key = key.substring(1);
                id = true;
            }
            if ("\\".equals(key.substring(0, 1))) {
                // escape character
                // can be used to avoid attribute @ being confused with id @
                key = key.substring(1);
            }
            List<Integer> splitIndexes = new ArrayList<>();
            while (key.contains("%.")) {
                splitIndexes.add(
                        StringUtils.countMatches(key.substring(0, key.indexOf("%.")), "."));
                key = key.replaceFirst(Pattern.quote("%."), ".");
            }

            CatalogStoreMappingElement element = mapping.mappingElements.get(key);
            if (element == null) {
                element = new CatalogStoreMappingElement(key);
                mapping.mappingElements.put(key, element);
            }

            element.content = parseOgcCqlExpression(mappingEntry.getValue());
            element.required = required;
            element.splitIndex = new int[splitIndexes.size()];
            for (int i = 0; i < splitIndexes.size(); i++) {
                element.splitIndex[i] = splitIndexes.get(i);
            }
            if (id) {
                mapping.identifier = element;
            }
        }

        return mapping;
    }

    /**
     * Helper method to parce cql expression
     *
     * @param sourceExpr The cql expression string
     * @return the expression
     */
    protected static Expression parseOgcCqlExpression(String sourceExpr) {
        Expression expression = Expression.NIL;
        if (sourceExpr != null && sourceExpr.trim().length() > 0) {
            try {
                expression = CQL.toExpression(sourceExpr, ff);
            } catch (CQLException e) {
                String formattedErrorMessage = e.getMessage();
                LOGGER.log(Level.SEVERE, formattedErrorMessage, e);
                throw new IllegalArgumentException(
                        "Error parsing CQL expression "
                                + sourceExpr
                                + ":\n"
                                + formattedErrorMessage);
            } catch (Exception e) {
                e.printStackTrace();
                String msg = "parsing expression " + sourceExpr;
                LOGGER.log(Level.SEVERE, msg, e);
                throw new IllegalArgumentException(msg + ": " + e.getMessage(), e);
            }
        }
        return expression;
    }

    /**
     * Helper method to convert StepList path to Dot path (separated by dots and no namespace
     * prefixes, used for mapping)
     *
     * @param steps XPath steplist
     * @return String with dot path
     */
    public static String toDotPath(XPathUtil.StepList steps) {

        StringBuilder sb = new StringBuilder();
        for (XPathUtil.Step step : steps) {
            sb.append(step.getName().getLocalPart());
            sb.append(".");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
