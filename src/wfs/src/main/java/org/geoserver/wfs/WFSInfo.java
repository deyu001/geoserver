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

package org.geoserver.wfs;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.geoserver.config.ServiceInfo;
import org.geotools.util.Version;

public interface WFSInfo extends ServiceInfo {

    static enum Version {
        V_10("1.0.0"),
        V_11("1.1.0"),
        V_20("2.0.0");

        org.geotools.util.Version version;

        Version(String ver) {
            this.version = new org.geotools.util.Version(ver);
        }

        public org.geotools.util.Version getVersion() {
            return version;
        }

        static Version get(String v) {
            if (v.startsWith("1.0")) {
                return V_10;
            }
            if (v.startsWith("1.1")) {
                return V_11;
            }
            if (v.startsWith("2.0")) {
                return V_20;
            }
            return null;
        }

        public static Version negotiate(String ver) {
            if (ver == null) {
                return null;
            }

            org.geotools.util.Version version = new org.geotools.util.Version(ver);
            if (version.compareTo(V_10.version) <= 0) {
                return V_10;
            }
            if (version.compareTo(V_11.version) <= 0) {
                return V_11;
            }
            return V_20;
        }

        /** Compares this value with a given version as string */
        public int compareTo(String version) {
            if (version == null) {
                return (this == latest()) ? 0 : -1;
            }
            return this.version.compareTo(new org.geotools.util.Version(version));
        }

        public static Version latest() {
            return V_20;
        }
    };

    static enum Operation {
        GETCAPABILITIES {
            public int getCode() {
                return 0;
            }
        },
        DESCRIBEFEATURETYPE {
            public int getCode() {
                return 0;
            }
        },
        GETFEATURE {
            public int getCode() {
                return 1;
            }
        },
        LOCKFEATURE {
            public int getCode() {
                return 2;
            }
        },
        TRANSACTION_INSERT {
            public int getCode() {
                return 4;
            }
        },
        TRANSACTION_UPDATE {
            public int getCode() {
                return 8;
            }
        },
        TRANSACTION_DELETE {
            public int getCode() {
                return 16;
            }
        },
        TRANSACTION_REPLACE {
            public int getCode() {
                return 32;
            }
        };

        public abstract int getCode();
    }

    static enum ServiceLevel {
        BASIC {
            public int getCode() {
                return 1;
            }

            public List<Operation> getOps() {
                return Arrays.asList(
                        Operation.GETCAPABILITIES,
                        Operation.DESCRIBEFEATURETYPE,
                        Operation.GETFEATURE);
            }
        },
        TRANSACTIONAL {
            public int getCode() {
                return 15;
            }

            public List<Operation> getOps() {
                return Arrays.asList(
                        Operation.GETCAPABILITIES,
                        Operation.DESCRIBEFEATURETYPE,
                        Operation.GETFEATURE,
                        Operation.TRANSACTION_INSERT,
                        Operation.TRANSACTION_UPDATE,
                        Operation.TRANSACTION_DELETE,
                        Operation.TRANSACTION_REPLACE);
            }
        },
        COMPLETE {
            public int getCode() {
                return 31;
            }

            public List<Operation> getOps() {
                return Arrays.asList(
                        Operation.GETCAPABILITIES, Operation.DESCRIBEFEATURETYPE,
                        Operation.GETFEATURE, Operation.TRANSACTION_INSERT,
                        Operation.TRANSACTION_UPDATE, Operation.TRANSACTION_DELETE,
                        Operation.TRANSACTION_REPLACE, Operation.LOCKFEATURE);
            }
        };

        public abstract int getCode();

        public abstract List<Operation> getOps();

        boolean contains(ServiceLevel other) {
            return getOps().containsAll(other.getOps());
        }

        public static ServiceLevel get(int code) {
            for (ServiceLevel s : values()) {
                if (s.getCode() == code) {
                    return s;
                }
            }

            return null;
        }
    };

    /** A map of wfs version to gml encoding configuration. */
    Map<Version, GMLInfo> getGML();

    /**
     * A global cap on the number of features to allow when processing a request.
     *
     * @uml.property name="maxFeatures"
     */
    int getMaxFeatures();

    /**
     * Sets the global cap on the number of features to allow when processing a request.
     *
     * @uml.property name="maxFeatures"
     */
    void setMaxFeatures(int maxFeatures);

    /** The level of service provided by the WFS. */
    ServiceLevel getServiceLevel();

    /** Sets the level of service provided by the WFS. */
    void setServiceLevel(ServiceLevel serviceLevel);

    /**
     * The flag which determines if gml:bounds elements should be encoded at the feature level in
     * gml output.
     */
    boolean isFeatureBounding();

    /**
     * Sets the flag which determines if gml:bounds elements should be encoded at the feature level
     * in gml output.
     */
    void setFeatureBounding(boolean featureBounding);

    /**
     * Get the flag that determines the encoding of the WFS schemaLocation. True if the WFS
     * schemaLocation should refer to the canonical location, false if the WFS schemaLocation should
     * refer to a copy served by GeoServer.
     */
    boolean isCanonicalSchemaLocation();

    /**
     * Set the flag that determines the encoding of the WFS schemaLocation. True if the WFS
     * schemaLocation should refer to the canonical location, false if the WFS schemaLocation should
     * refer to a copy served by GeoServer.
     */
    void setCanonicalSchemaLocation(boolean canonicalSchemaLocation);

    /**
     * Get the flag that determines encoding of featureMember or featureMembers True if the
     * featureMember should be encoded False if the featureMembers should be encoded
     *
     * @return encodingFeatureMember
     */
    boolean isEncodeFeatureMember();

    /** set the response encoding option, featureMembers or featureMember */
    void setEncodeFeatureMember(boolean encodeFeatureMember);

    /**
     * Get the flag that determines if WFS hit requests (counts) will ignore the maximum features
     * limit for this server
     *
     * @return hitsIgnoreMaxFeatures
     */
    boolean isHitsIgnoreMaxFeatures();

    /** Set the option to ignore the maximum feature limit for WFS hit counts */
    void setHitsIgnoreMaxFeatures(boolean hitsIgnoreMaxFeatures);

    /**
     * Get the maximum number of features to be displayed in a layer preview. Can be defined by the
     * user. By default, 50.
     *
     * @return maxNumberOfFeaturesForPreview
     */
    Integer getMaxNumberOfFeaturesForPreview();

    /** Set the maximum number of features to be displayed in a layer preview */
    void setMaxNumberOfFeaturesForPreview(Integer maxNumberOfFeaturesForPreview);

    /** The srs's that the WFS service will advertise in the capabilities document */
    List<String> getSRS();

    /** Flag that determines if global stored queries are allowed. Default true. */
    Boolean getAllowGlobalQueries();

    void setAllowGlobalQueries(Boolean allowGlobalQueries);

    /**
     * Flag that determines if complex features will be converted to simple feature for compatible
     * output formats.
     */
    boolean isSimpleConversionEnabled();

    /**
     * Sets the flag that determines if complex features will be converted to simple feature for
     * compatible output formats.
     */
    void setSimpleConversionEnabled(boolean simpleConversionEnabled);
    /**
     * Flag that determines if the wfsRequest.txt dump file should be included in shapefile/zip
     * output.
     */
    boolean getIncludeWFSRequestDumpFile();
    /**
     * Sets the flag that determines if the wfsRequest.txt dump file should be included in
     * shapefile/zip output
     */
    void setIncludeWFSRequestDumpFile(boolean includeWFSRequestDumpFile);
}
