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

package org.geoserver.gwc.web;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.Component;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.web.CapabilitiesHomePageLinkProvider;
import org.geoserver.web.CapabilitiesHomePagePanel;
import org.geoserver.web.CapabilitiesHomePagePanel.CapsInfo;
import org.geoserver.web.GeoServerApplication;
import org.geotools.util.Version;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

/**
 * Implements the {@link CapabilitiesHomePageLinkProvider} extension point to contribute links to
 * GetCapabilities and other service description documents supported by GeoWebCache.
 *
 * @author Gabriel Roldan
 * @see CapabilitiesHomePagePanel
 */
public class GWCCapabilitiesHomePageProvider implements CapabilitiesHomePageLinkProvider {

    private final GWC gwcFacade;
    private final GeoServer geoServer;

    /**
     * @param gwc provides access to the {@link GWCConfig configuration} in order to show/hide
     *     getcaps links based on service enablement.
     */
    public GWCCapabilitiesHomePageProvider(GWC gwc, GeoServer geoServer) {
        this.gwcFacade = gwc;
        this.geoServer = geoServer;
    }

    /**
     * Adds capabilities links for WMS-C, WMTS and TMS, as long as they're available.
     *
     * @see org.geoserver.web.CapabilitiesHomePageLinkProvider#getCapabilitiesComponent
     * @see CapabilitiesHomePagePanel
     */
    public Component getCapabilitiesComponent(final String id) {

        List<CapsInfo> gwcCaps = new ArrayList<>();

        final GeoServerApplication app = GeoServerApplication.get();
        final GWCConfig gwcConfig = gwcFacade.getConfig();

        try {
            if (gwcConfig.isWMSCEnabled() && null != app.getBean("gwcServiceWMS")) {
                gwcCaps.add(
                        new CapsInfo(
                                "WMS-C",
                                new Version("1.1.1"),
                                "../gwc/service/wms?request=GetCapabilities&version=1.1.1&tiled=true"));
            }
        } catch (NoSuchBeanDefinitionException e) {
            // service not found, ignore exception
        }

        try {
            if (geoServer.getService(WMTSInfo.class).isEnabled()
                    && null != app.getBean("gwcServiceWMTS")) {
                gwcCaps.add(
                        new CapsInfo(
                                "WMTS",
                                new Version("1.0.0"),
                                "../gwc/service/wmts?REQUEST=GetCapabilities"));
            }
        } catch (NoSuchBeanDefinitionException e) {
            // service not found, ignore exception
        }
        try {
            if (gwcConfig.isTMSEnabled() && null != app.getBean("gwcServiceTMS")) {
                gwcCaps.add(new CapsInfo("TMS", new Version("1.0.0"), "../gwc/service/tms/1.0.0"));
            }
        } catch (NoSuchBeanDefinitionException e) {
            // service not found, ignore exception
        }

        return new CapabilitiesHomePagePanel(id, gwcCaps);
    }
}
