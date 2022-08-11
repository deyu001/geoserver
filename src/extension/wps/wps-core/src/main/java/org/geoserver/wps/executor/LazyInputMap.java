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

package org.geoserver.wps.executor;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.geoserver.wps.ProcessDismissedException;
import org.geoserver.wps.WPSException;
import org.geotools.data.util.NullProgressListener;
import org.geotools.data.util.SubProgressListener;
import org.geotools.util.SimpleInternationalString;
import org.opengis.util.ProgressListener;

/**
 * A map using input providers internally, allows for deferred execution of the input parsing (it
 * happens in a single shot when the first input is fetched)
 *
 * @author Andrea Aime - GeoSolutions
 */
class LazyInputMap extends AbstractMap<String, Object> {

    private static ProgressListener DEFAULT_LISTENER = new NullProgressListener();

    Map<String, InputProvider> providers = new LinkedHashMap<>();

    Map<String, Object> values = new HashMap<>();

    boolean parsed = false;

    ProgressListener listener = DEFAULT_LISTENER;

    public LazyInputMap(Map<String, InputProvider> providers) {
        this.providers = providers;
    }

    public Object get(Object key) {
        // make sure we just kill the process is a dismiss happened
        if (listener.isCanceled()) {
            throw new ProcessDismissedException(listener);
        }
        // lazy parse inputs
        parseInputs();
        // return the value
        return values.get(key);
    }

    private void parseInputs() {
        // we want to (try to) actually parse stuff just once
        if (parsed) {
            return;
        }
        parsed = true;

        // count long parses
        int totalSteps = 0;
        for (InputProvider provider : providers.values()) {
            totalSteps += provider.longStepCount();
        }

        listener.started();
        float stepsSoFar = 0;
        for (InputProvider provider : providers.values()) {
            listener.setTask(
                    new SimpleInternationalString(
                            "Retrieving/parsing process input: " + provider.getInputId()));
            try {
                // force parsing
                float providerLongSteps = provider.longStepCount();
                ProgressListener subListener;
                if (providerLongSteps > 0) {
                    subListener =
                            new SubProgressListener(
                                    listener,
                                    (stepsSoFar / totalSteps) * 100,
                                    (providerLongSteps / totalSteps) * 100);
                } else {
                    subListener = new NullProgressListener();
                }
                stepsSoFar += providerLongSteps;
                subListener.started();
                subListener.progress(0);
                Object value = provider.getValue(subListener);
                values.put(provider.getInputId(), value);
            } catch (Exception e) {
                listener.exceptionOccurred(e);
                if (e instanceof WPSException) {
                    throw (WPSException) e;
                }
                throw new WPSException(
                        "Failed to retrieve value for input " + provider.getInputId(), e);
            }
        }
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        Set<Entry<String, Object>> result = new HashSet<>();
        for (String key : providers.keySet()) {
            result.add(new DeferredEntry(key));
        }
        return result;
    }

    public int longStepCount() {
        int count = 0;
        for (InputProvider provider : providers.values()) {
            count += provider.longStepCount();
        }
        return count;
    }

    public class DeferredEntry implements Entry<String, Object> {

        private String key;

        public DeferredEntry(String key) {
            this.key = key;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Object getValue() {
            parseInputs();
            return values.get(key);
        }

        @Override
        public Object setValue(Object value) {
            throw new UnsupportedOperationException();
        }
    }

    /** The listener will be informed of the parse progress, when it happens */
    public void setListener(ProgressListener listener) {
        this.listener = listener;
    }
}
