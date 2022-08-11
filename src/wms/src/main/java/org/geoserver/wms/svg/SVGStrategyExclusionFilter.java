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

package org.geoserver.wms.svg;

import org.geoserver.platform.ExtensionFilter;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.WMS;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * An extension filter that tells {@link GeoServerExtensions#extensions(Class)} (where {@code Class
 * == GetMapOutputFormat.class}) whether the {@link SVGStreamingMapOutputFormat} or the {@link
 * SVGBatikMapOutputFormat} is to be excluded based on the {@link WMS#getSvgRenderer()} config
 * option.
 *
 * <p>Implementation note: it would be better if this bean received a reference to the {@link WMS}
 * facade instead of looking it up through {@link GeoServerExtensions} but if so, unit tests break
 * while setting up the mock application context when {@code SecureCatalogImpl}'s constructor
 * performs an extension lookup with the following error: <i> Error creating bean with name 'wms':
 * Requested bean is currently in creation: Is there an unresolvable circular reference?</i>
 *
 * @author Gabriel Roldan
 */
public class SVGStrategyExclusionFilter implements ExtensionFilter, ApplicationContextAware {

    private String wmsBeanName;

    private WMS wms;

    private ApplicationContext ctx;

    /**
     * @param wmsBeanName name of the bean of type {@link WMS} to be lazily looked up in the
     *     application context
     */
    public SVGStrategyExclusionFilter(final String wmsBeanName) {
        this.wmsBeanName = wmsBeanName;
    }

    /**
     * @param ctx context where to look for the {@link WMS} bean named as specified to this class'
     *     constructor
     * @see ApplicationContextAware#setApplicationContext(ApplicationContext)
     */
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.ctx = ctx;
    }

    /**
     * @return {@code true} if {@code bean} is one of the SVG rendering strategies but not the one
     *     that shall be used as per {@link WMS#getSvgRenderer()}
     * @see ExtensionFilter#exclude(String, Object)
     */
    public boolean exclude(String beanId, Object bean) {
        boolean exclude = false;
        /*
         * Lazy lookup of the WMS bean is performed here because this method is often called while
         * the application context is still being built
         */
        if (bean instanceof SVGStreamingMapOutputFormat) {
            exclude = !SVG.canHandle(getWMS(), WMS.SVG_SIMPLE);
        } else if (bean instanceof SVGBatikMapOutputFormat) {
            exclude = !SVG.canHandle(getWMS(), WMS.SVG_BATIK);
        }
        return exclude;
    }

    private WMS getWMS() {
        if (wms == null) {
            wms = (WMS) ctx.getBean(wmsBeanName);
        }
        return wms;
    }
}
