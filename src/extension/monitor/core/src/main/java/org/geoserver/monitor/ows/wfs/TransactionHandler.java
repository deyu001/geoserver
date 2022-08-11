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

package org.geoserver.monitor.ows.wfs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.logging.Level;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.geoserver.catalog.Catalog;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.RequestData;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.xsd.EMFUtils;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class TransactionHandler extends WFSRequestObjectHandler {

    public TransactionHandler(MonitorConfig config, Catalog catalog) {
        super("net.opengis.wfs.TransactionType", config, catalog);
    }

    @Override
    public void handle(Object request, RequestData data) {
        super.handle(request, data);

        // also determine the sub operation
        FeatureMap elements = (FeatureMap) EMFUtils.get((EObject) request, "group");
        if (elements == null) {
            return;
        }

        ListIterator<Object> i = elements.valueListIterator();
        int flag = 0;
        while (i.hasNext()) {
            Object e = i.next();
            if (e.getClass().getSimpleName().startsWith("Insert")) {
                flag |= 1;
            } else if (e.getClass().getSimpleName().startsWith("Update")) {
                flag |= 2;
            } else if (e.getClass().getSimpleName().startsWith("Delete")) {
                flag |= 4;
            } else {
                flag |= 8;
            }
        }

        StringBuffer sb = new StringBuffer();
        if ((flag & 1) == 1) sb.append("I");
        if ((flag & 2) == 2) sb.append("U");
        if ((flag & 4) == 4) sb.append("D");
        if ((flag & 8) == 8) sb.append("O");
        data.setSubOperation(sb.toString());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getLayers(Object request) {
        FeatureMap elements = (FeatureMap) EMFUtils.get((EObject) request, "group");
        if (elements == null) {
            return null;
        }

        List<String> layers = new ArrayList<>();
        ListIterator<Object> i = elements.valueListIterator();
        while (i.hasNext()) {
            Object e = i.next();
            if (EMFUtils.has((EObject) e, "typeName")) {
                Object typeName = EMFUtils.get((EObject) e, "typeName");
                if (typeName != null) {
                    layers.add(toString(typeName));
                }
            } else {
                // this is most likely an insert, determine layers from feature collection
                if (isInsert(e)) {
                    List<Feature> features = (List<Feature>) EMFUtils.get((EObject) e, "feature");
                    Set<String> set = new LinkedHashSet<>();
                    for (Feature f : features) {
                        if (f instanceof SimpleFeature) {
                            set.add(((SimpleFeature) f).getType().getTypeName());
                        } else {
                            set.add(f.getType().getName().toString());
                        }
                    }

                    layers.addAll(set);
                }
            }
        }

        return layers;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<Object> getElements(Object request) {
        return (List<Object>) OwsUtils.get(request, "group");
    }

    @Override
    protected Object unwrapElement(Object element) {
        // For some reason it's wrapped inside an extra EMF object here but not in the other
        // request types
        return OwsUtils.get(element, "value");
    }

    boolean isInsert(Object element) {
        return element.getClass().getSimpleName().startsWith("InsertElementType");
    }

    @Override
    protected ReferencedEnvelope getBBoxFromElement(Object element) {
        if (isInsert(element)) {
            // check for srsName on insert element
            ReferencedEnvelope bbox = null;
            if (OwsUtils.has(element, "srsName")) {
                Object srs = OwsUtils.get(element, "srsName");
                CoordinateReferenceSystem crs = crs(srs);

                if (crs != null) {
                    bbox = new ReferencedEnvelope(crs);
                    bbox.setToNull();
                }
            }

            // go through all the features and aggregate the bounding boxes
            @SuppressWarnings("unchecked")
            List<Feature> feature = (List<Feature>) OwsUtils.get(element, "feature");
            for (Feature f : feature) {
                BoundingBox fbbox = f.getBounds();
                if (fbbox == null) {
                    continue;
                }

                if (bbox == null) {
                    bbox = new ReferencedEnvelope(fbbox);
                }
                bbox.include(fbbox);
            }

            return bbox;
        }
        return null;
    }

    @Override
    protected CoordinateReferenceSystem getCrsFromElement(Object element) {
        // special case for insert
        if (isInsert(element) && OwsUtils.has(element, "srsName")) {
            CoordinateReferenceSystem crs = crs(OwsUtils.get(element, "srsName"));
            if (crs != null) {
                return crs;
            }
        }

        return super.getCrsFromElement(element);
    }

    CoordinateReferenceSystem crs(Object srs) {
        try {
            return srs != null ? CRS.decode(srs.toString()) : null;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
        }
        return null;
    }
}
