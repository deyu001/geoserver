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

package org.geoserver.opensearch.eo;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.geoserver.config.GeoServer;

public class ProductClass implements Serializable, Cloneable {

    /** The generic product class */
    public static final ProductClass GENERIC =
            new ProductClass("eop_generic", "eop", "http://www.opengis.net/eop/2.1");

    /** Optical products */
    public static final ProductClass OPTICAL =
            new ProductClass("optical", "opt", "http://www.opengis.net/opt/2.1");

    /** Radar products */
    public static final ProductClass RADAR =
            new ProductClass("radar", "sar", "http://www.opengis.net/sar/2.1");

    /** Altimetric products */
    public static final ProductClass ALTIMETRIC =
            new ProductClass("altimetric", "alt", "http://www.opengis.net/alt/2.1");

    /** Atmospheric products */
    public static final ProductClass ATMOSPHERIC =
            new ProductClass("atmospheric", "atm", "http://www.opengis.net/atm/2.1");

    /** Limb products */
    public static final ProductClass LIMB =
            new ProductClass("limb", "lmb", "http://www.opengis.net/lmb/2.1");

    /** SSP products */
    public static final ProductClass SSP =
            new ProductClass("ssp", "ssp", "http://www.opengis.net/ssp/2.1");

    /** The default, built-in product classes */
    public static final List<ProductClass> DEFAULT_PRODUCT_CLASSES =
            Collections.unmodifiableList(
                    Arrays.asList(GENERIC, OPTICAL, RADAR, ALTIMETRIC, ATMOSPHERIC, LIMB, SSP));

    public static ProductClass getProductClassFromName(GeoServer geoServer, String name) {
        for (ProductClass pc : getProductClasses(geoServer)) {
            if (name.equalsIgnoreCase(pc.getName())) {
                return pc;
            }
        }
        throw new IllegalArgumentException("Could not locate a product class named " + name);
    }

    /**
     * Searches a product by name (search is case insensitive)
     *
     * @param oseo reference to the service configuration
     * @param name the product class name
     */
    public static ProductClass getProductClassFromName(OSEOInfo oseo, String name) {
        for (ProductClass pc : getProductClasses(oseo)) {
            if (name.equalsIgnoreCase(pc.getName())) {
                return pc;
            }
        }
        throw new IllegalArgumentException("Could not locate a product class named " + name);
    }

    /**
     * Searches a product by prefix (search is case insensitive)
     *
     * @param oseo reference to the service configuration
     * @param name the product class prefix
     */
    public static ProductClass getProductClassFromPrefix(OSEOInfo oseo, String prefix) {
        for (ProductClass pc : getProductClasses(oseo)) {
            if (prefix.equalsIgnoreCase(pc.getPrefix())) {
                return pc;
            }
        }
        throw new IllegalArgumentException(
                "Could not locate a product class with prefix " + prefix);
    }

    /**
     * Checks if the given prefix matches a product class
     *
     * @param oseo reference to the service configuration
     * @param name the product class name
     */
    public static boolean isProductClass(OSEOInfo oseo, String prefix) {
        for (ProductClass pc : getProductClasses(oseo)) {
            if (pc.getPrefix().equals(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String name;

    private String namespace;

    private String prefix;

    /**
     * Builds a new product class
     *
     * @param name The name of the class, used as sensor type in the collection model
     * @param prefix The prefix, used in database mappings and XML/JSON outputs
     * @param namespace The namespace, used in XML outputs
     */
    public ProductClass(String name, String prefix, String namespace) {
        this.name = name;
        this.prefix = prefix;
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductClass that = (ProductClass) o;
        return Objects.equals(name, that.name)
                && Objects.equals(namespace, that.namespace)
                && Objects.equals(prefix, that.prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, namespace, prefix);
    }

    @Override
    public String toString() {
        return "ProductClass{"
                + "name='"
                + name
                + '\''
                + ", namespace='"
                + namespace
                + '\''
                + ", prefix='"
                + prefix
                + '\''
                + '}';
    }

    /**
     * Returns the configured product classes
     *
     * @param geoServer A GeoServer reference used to retrieve the OpenSearch configuration
     */
    public static List<ProductClass> getProductClasses(GeoServer geoServer) {
        if (geoServer != null) {
            OSEOInfo oseo = geoServer.getService(OSEOInfo.class);
            return getProductClasses(oseo);
        } else {
            return DEFAULT_PRODUCT_CLASSES;
        }
    }

    /**
     * Returns the configured product classes
     *
     * @param oseo The OpenSearch configuration
     */
    public static List<ProductClass> getProductClasses(OSEOInfo oseo) {
        if (oseo == null) {
            return DEFAULT_PRODUCT_CLASSES;
        } else {
            return oseo.getProductClasses();
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new ProductClass(this.name, this.prefix, this.namespace);
    }
}
