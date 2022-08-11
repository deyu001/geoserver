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

package org.geoserver.nsg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServer;
import org.geoserver.nsg.timeout.TimeoutCallback;
import org.geoserver.wfs.DomainType;
import org.geoserver.wfs.GMLInfo;
import org.geoserver.wfs.OperationMetadata;
import org.geoserver.wfs.WFSExtendedCapabilitiesProvider;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.GetCapabilitiesRequest;
import org.geotools.util.Version;
import org.xml.sax.helpers.NamespaceSupport;

public class NSGWFSExtendedCapabilitiesProvider implements WFSExtendedCapabilitiesProvider {

    public static final String NSG_BASIC = "http://www.nga.mil/service/wfs/2.0/profile/basic";
    public static final String IMPLEMENTS_ENHANCED_PAGING = "ImplementsEnhancedPaging";
    public static final String IMPLEMENTS_FEATURE_VERSIONING = "ImplementsFeatureVersioning";
    static final Set<String> SRS_OPERATIONS =
            new HashSet<>(Arrays.asList("GetFeature", "GetFeatureWithLock", "Transaction"));
    static final Set<String> TIMEOUT_OPERATIONS =
            new HashSet<>(
                    Arrays.asList(
                            "GetFeature", "GetFeatureWithLock", "GetPropertyValue", "PageResults"));
    static final String GML32_FORMAT = "application/gml+xml; version=3.2";

    GeoServer gs;

    public NSGWFSExtendedCapabilitiesProvider(GeoServer gs) {
        this.gs = gs;
    }

    @Override
    public String[] getSchemaLocations(String schemaBaseURL) {
        return new String[0];
    }

    @Override
    public void registerNamespaces(NamespaceSupport namespaces) {
        // nothing to register
    }

    @Override
    public void encode(Translator tx, WFSInfo wfs, GetCapabilitiesRequest getCapabilitiesRequest)
            throws IOException {
        // no extensions to encode here
    }

    @Override
    public List<String> getProfiles(Version version) {
        if (isNSGProfileApplicable(version)) {
            return Arrays.asList(
                    NSG_BASIC
                    /*, "http://www.dgiwg.org/service/wfs/2.0/profile/locking" */ );
        } else {
            return Collections.emptyList();
        }
    }

    private boolean isNSGProfileApplicable(Version version) {
        return Integer.valueOf(2).equals(version.getMajor());
    }

    @Override
    public void updateRootOperationConstraints(Version version, List<DomainType> constraints) {
        if (isNSGProfileApplicable(version)) {
            for (DomainType constraint : constraints) {
                if (IMPLEMENTS_FEATURE_VERSIONING.equals(constraint.getName())) {
                    constraint.setDefaultValue("TRUE");
                }
            }
        }
        constraints.add(new DomainType(IMPLEMENTS_ENHANCED_PAGING, "TRUE"));
    }

    @Override
    public void updateOperationMetadata(Version version, List<OperationMetadata> operations) {
        if (isNSGProfileApplicable(version)) {
            // prepare SRS customization
            WFSInfo wfs = gs.getService(WFSInfo.class);
            DomainType srsParameter = getSrsParameter(wfs);
            DomainType timeoutParameter = getTimeoutParameter(wfs);
            DomainType versionParameter =
                    new DomainType("version", Arrays.asList("2.0.0", "1.1.0", "1.0.0"));

            // add the paged results operation
            OperationMetadata pageResults = new OperationMetadata("PageResults", true, true);
            pageResults.getParameters().add(new DomainType("outputFormat", GML32_FORMAT));
            operations.add(pageResults);

            for (OperationMetadata operation : operations) {
                // add the version if not GetCapabilities, could have been done in core, but this
                // seems like a niche
                // nitpick, it's a WFS 2.0 capabilities the version number should be obvious to the
                // client
                if (!"GetCapabilities".equals(operation.getName())) {
                    // add the version if missing
                    if (!containsParameter(operation, "version")) {
                        operation.getParameters().add(versionParameter);
                    }
                }
                // add the srs if configured in WFS
                if (SRS_OPERATIONS.contains(operation.getName())
                        && !containsParameter(operation, srsParameter.getName())) {
                    operation.getParameters().add(srsParameter);
                }
                // add the timeout if configured in WFS
                if (TIMEOUT_OPERATIONS.contains(operation.getName())
                        && !containsParameter(operation, timeoutParameter.getName())) {
                    operation.getParameters().add(timeoutParameter);
                }
            }
        }
    }

    private DomainType getTimeoutParameter(WFSInfo wfs) {
        Integer timeout = wfs.getMetadata().get(TimeoutCallback.TIMEOUT_CONFIG_KEY, Integer.class);
        if (timeout == null) {
            timeout = 300;
        }
        DomainType result = new DomainType("Timeout", String.valueOf(timeout));
        return result;
    }

    public DomainType getSrsParameter(WFSInfo wfs) {
        List<String> extraSRS = wfs.getSRS();
        Set<String> srsParameterValues;
        GMLInfo gml = wfs.getGML().get(WFSInfo.Version.V_20);
        String prefix = gml.getSrsNameStyle().getPrefix();
        Function<String, String> epsgMapper = srs -> qualifySRS(prefix, srs);
        if (extraSRS != null && !extraSRS.isEmpty()) {
            srsParameterValues =
                    extraSRS.stream()
                            .map(epsgMapper)
                            .collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            srsParameterValues = new LinkedHashSet<>();
        }
        // add values from feature types
        gs.getCatalog()
                .getFeatureTypes()
                .forEach(
                        ft -> {
                            String srs = epsgMapper.apply(ft.getSRS());
                            srsParameterValues.add(srs);
                        });

        // build the parameter
        DomainType srsParameter = new DomainType("srsName", new ArrayList<>(srsParameterValues));
        return srsParameter;
    }

    public String qualifySRS(String prefix, String srs) {
        if (srs.matches("(?ui)EPSG:[0-9]+")) {
            srs = prefix + srs.substring(5);
        } else {
            srs = prefix + srs;
        }
        return srs;
    }

    private boolean containsParameter(OperationMetadata operation, String parameterName) {
        return operation.getParameters().stream().anyMatch(p -> parameterName.equals(p.getName()));
    }
}
