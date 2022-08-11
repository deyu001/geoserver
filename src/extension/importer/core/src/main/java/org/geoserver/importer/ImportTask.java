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

package org.geoserver.importer;

import static org.geoserver.importer.ImporterUtils.resolve;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.importer.job.ProgressMonitor;
import org.geoserver.importer.transform.ImportTransform;
import org.geoserver.importer.transform.TransformChain;
import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;

/**
 * A unit of work during an import.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class ImportTask implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public static final String TYPE_NAME = "typeName";
    public static final String TYPE_SPEC = "typeSpec";

    public static enum State {
        PENDING,
        READY,
        RUNNING,
        NO_CRS,
        NO_BOUNDS,
        NO_FORMAT,
        BAD_FORMAT,
        ERROR,
        CANCELED,
        COMPLETE
    }

    /** task id */
    long id;

    /** the context this task is part of */
    ImportContext context;

    /** source of data for the import */
    ImportData data;

    /** The target store for the import */
    StoreInfo store;

    /** state */
    State state = State.PENDING;

    /** id generator for items */
    int itemid = 0;

    /** flag signalling direct/indirect import */
    boolean direct;

    /** how data should be applied to the target, during ingest/indirect import */
    UpdateMode updateMode;

    /** The original layer name assigned to the task */
    String originalLayerName;

    /** the layer/resource */
    LayerInfo layer;

    /** Any error associated with the resource */
    Exception error;

    /** transform to apply to this import item */
    TransformChain<? extends ImportTransform> transform;

    /** messages logged during proessing */
    List<LogRecord> messages = new ArrayList<>();

    /** various metadata */
    transient Map<Object, Object> metadata;

    /** used to track progress */
    int totalToProcess;

    int numberProcessed;

    String typeName;

    String typeSpec;

    public ImportTask() {
        updateMode = UpdateMode.CREATE;
    }

    public ImportTask(ImportData data) {
        this();
        this.data = data;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ImportContext getContext() {
        return context;
    }

    public void setContext(ImportContext context) {
        this.context = context;
    }

    public ImportData getData() {
        return data;
    }

    public void setData(ImportData data) {
        this.data = data;
    }

    public void setStore(StoreInfo store) {
        this.store = store;
    }

    public StoreInfo getStore() {
        return store;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean isDirect() {
        return direct;
    }

    public void setDirect(boolean direct) {
        this.direct = direct;
    }

    public LayerInfo getLayer() {
        return layer;
    }

    public void setLayer(LayerInfo layer) {
        this.layer = layer;
    }

    public Exception getError() {
        return error;
    }

    public void setError(Exception error) {
        this.error = error;
    }

    public TransformChain<? extends ImportTransform> getTransform() {
        return transform;
    }

    @SuppressWarnings("unchecked")
    public void addTransform(ImportTransform tx) {
        ((TransformChain) this.transform).add(tx);
    }

    @SuppressWarnings("unchecked")
    public void removeTransform(ImportTransform tx) {
        ((TransformChain) this.transform).remove(tx);
    }

    public void setTransform(TransformChain<? extends ImportTransform> transform) {
        this.transform = transform;
    }

    /**
     * Returns a transient metadata map, useful for caching information that's expensive to compute.
     * The map won't be stored in the {@link ImportStore} so don't use it for anything that needs to
     * be persisted.
     */
    public Map<Object, Object> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        return metadata;
    }

    public void clearMessages() {
        if (messages != null) {
            messages.clear();
        }
    }

    public void addMessage(Level level, String msg) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(new LogRecord(level, msg));
    }

    public List<LogRecord> getMessages() {
        List<LogRecord> retval;
        if (messages == null) {
            retval = Collections.emptyList();
        } else {
            retval = Collections.unmodifiableList(messages);
        }
        return retval;
    }

    public String getOriginalLayerName() {
        return originalLayerName == null ? layer.getResource().getNativeName() : originalLayerName;
    }

    public void setOriginalLayerName(String originalLayerName) {
        this.originalLayerName = originalLayerName;
    }

    public int getNumberProcessed() {
        return numberProcessed;
    }

    public void setNumberProcessed(int numberProcessed) {
        this.numberProcessed = numberProcessed;
    }

    public int getTotalToProcess() {
        return totalToProcess;
    }

    public void setTotalToProcess(int totalToProcess) {
        this.totalToProcess = totalToProcess;
    }

    public UpdateMode getUpdateMode() {
        return updateMode;
    }

    public void setUpdateMode(UpdateMode updateMode) {
        this.updateMode = updateMode;
    }

    public void reattach(Catalog catalog) {
        reattach(catalog, false);
    }

    public void reattach(Catalog catalog, boolean lookupByName) {
        store = resolve(store, catalog, lookupByName);
        layer = resolve(layer, catalog, lookupByName);
    }

    public boolean readyForImport() {
        return state == State.READY || state == State.CANCELED;
    }

    public ProgressMonitor progress() {
        return context.progress();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((context == null) ? 0 : context.hashCode());
        result = prime * result + (int) (id ^ (id >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ImportTask other = (ImportTask) obj;
        if (context == null) {
            if (other.context != null) return false;
        } else if (!context.equals(other.context)) return false;
        if (id != other.id) return false;
        return true;
    }

    public SimpleFeatureType getFeatureType() {
        SimpleFeatureType schema = (SimpleFeatureType) getMetadata().get(FeatureType.class);
        if (schema == null) {
            if (typeName != null && typeSpec != null) {
                try {
                    schema = DataUtilities.createType(typeName, typeSpec);
                    getMetadata().put(FeatureType.class, schema);
                } catch (SchemaException e) {
                    // ignore
                }
            }
        }

        return schema;
    }

    public void setFeatureType(SimpleFeatureType featureType) {
        getMetadata().put(FeatureType.class, featureType);
        if (featureType != null) {
            typeName = featureType.getTypeName();
            typeSpec = DataUtilities.encodeType(featureType);
        }
    }
}
