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

package org.geoserver.wps.process;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.process.Process;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;
import org.opengis.feature.type.Name;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * GeoServer replacement for GeoTools's {@link Processors} class, it allow {@link ProcessFilter} to
 * be taken into account before creating factories and processes
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GeoServerProcessors implements ApplicationContextAware {

    private static List<ProcessFilter> filters;

    @Override
    public void setApplicationContext(ApplicationContext appContext) throws BeansException {
        filters = GeoServerExtensions.extensions(ProcessFilter.class, appContext);
    }

    /**
     * Set of available ProcessFactory, each eventually wrapped or filtered out by the registered
     * {@link ProcessFilter}
     *
     * @return Set of ProcessFactory
     */
    public static Set<ProcessFactory> getProcessFactories() {
        Set<ProcessFactory> factories = Processors.getProcessFactories();
        Set<ProcessFactory> result = new LinkedHashSet<>();

        // scan filters and let them wrap and exclude as necessary
        for (ProcessFactory pf : factories) {
            pf = applyFilters(pf);
            if (pf != null) {
                result.add(pf);
            }
        }

        return result;
    }

    private static ProcessFactory applyFilters(ProcessFactory pf) {
        if (pf == null) {
            return null;
        }
        if (filters != null) {
            for (ProcessFilter filter : filters) {
                pf = filter.filterFactory(pf);
                if (pf == null) {
                    break;
                }
            }
        }
        return pf;
    }

    /**
     * Look up a Factory by name of a process it supports.
     *
     * @param name Name of the Process you wish to work with
     * @param applyFilters Whether to apply the available {@link ProcessFilter} to the returned
     *     factory, or not (if the code needs to check the original process factory by class name
     *     for example, better not to apply the filters, which often wrap the factories to add extra
     *     functionality)
     * @return ProcessFactory capable of creating an instanceof the named process
     */
    public static ProcessFactory createProcessFactory(Name name, boolean applyFilters) {
        ProcessFactory pf = Processors.createProcessFactory(name);
        if (applyFilters) {
            pf = applyFilters(pf);
        }
        // JD: also check the names, this could be a filtered process factory with only a subset
        // disabled
        if (pf != null && !pf.getNames().contains(name)) {
            pf = null;
        }
        return pf;
    }

    /**
     * Look up an implementation of the named process.
     *
     * @param name Name of the Process to create
     * @return created process or null if not found
     */
    public static Process createProcess(Name name) {
        ProcessFactory factory = createProcessFactory(name, false);
        if (factory == null) return null;

        return factory.create(name);
    }

    /**
     * Returns the process factory instance corresponding to the specified class.
     *
     * @param factoryClass The factory to look for
     * @param applyFilters Whether to apply the registered {@link ProcessFilter} instances, or not
     */
    public static ProcessFactory getProcessFactory(Class factoryClass, boolean applyFilters) {
        Set<ProcessFactory> factories = Processors.getProcessFactories();
        for (ProcessFactory pf : factories) {
            if (factoryClass.equals(pf.getClass())) {
                if (!applyFilters) {
                    return pf;
                } else {
                    // scan filters and let them wrap as necessary
                    pf = applyFilters(pf);

                    return pf;
                }
            }
        }

        // not found
        return null;
    }
}
