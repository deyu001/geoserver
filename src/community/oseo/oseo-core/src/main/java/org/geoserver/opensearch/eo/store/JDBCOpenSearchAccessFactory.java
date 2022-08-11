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

package org.geoserver.opensearch.eo.store;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.DataAccess;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.Repository;
import org.geotools.util.Converters;
import org.geotools.util.KVP;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;

/**
 * Builds {@link JDBCOpenSearchAccess} stores
 *
 * @author Andrea Aime - GeoSolutions
 */
public class JDBCOpenSearchAccessFactory implements DataAccessFactory {

    public static final Param REPOSITORY_PARAM =
            new Param(
                    "repository",
                    Repository.class,
                    "The repository that will provide the store instances",
                    false,
                    null,
                    new KVP(Param.LEVEL, "advanced"));

    public static final Param STORE_PARAM =
            new Param(
                    "store",
                    String.class,
                    "Delegate data store",
                    false,
                    null,
                    new KVP(Param.ELEMENT, String.class));

    /** parameter for database type */
    public static final Param DBTYPE =
            new Param("dbtype", String.class, "Type", true, "opensearch-eo-jdbc");

    /** parameter for namespace of the datastore */
    public static final Param NAMESPACE =
            new Param("namespace", String.class, "Namespace prefix", false);

    private static GeoServer geoServer;

    @Override
    public Map<Key, ?> getImplementationHints() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataAccess<? extends FeatureType, ? extends Feature> createDataStore(
            Map<String, ?> params) throws IOException {
        Repository repository = (Repository) REPOSITORY_PARAM.lookUp(params);
        String flatStoreName = (String) STORE_PARAM.lookUp(params);
        String ns = (String) NAMESPACE.lookUp(params);
        Name name = Converters.convert(flatStoreName, Name.class);
        return new JDBCOpenSearchAccess(
                repository, name, ns, GeoServerExtensions.bean(GeoServer.class));
    }

    @Override
    public String getDisplayName() {
        return "JDBC based OpenSearch store";
    }

    @Override
    public String getDescription() {
        return "Builds OpenSearch for EO information out of a suitable relational database source";
    }

    @Override
    public Param[] getParametersInfo() {
        return new Param[] {DBTYPE, REPOSITORY_PARAM, STORE_PARAM, NAMESPACE};
    }

    @Override
    public boolean canProcess(Map<String, ?> params) {
        // copied from AbstractDataStoreFactory... really, this code should be somewhere
        // where it can be reused...
        if (params == null) {
            return false;
        }
        Param arrayParameters[] = getParametersInfo();
        for (int i = 0; i < arrayParameters.length; i++) {
            Param param = arrayParameters[i];
            Object value;
            if (!params.containsKey(param.key)) {
                if (param.required) {
                    return false; // missing required key!
                } else {
                    continue;
                }
            }
            try {
                value = param.lookUp(params);
            } catch (IOException e) {
                return false;
            }
            if (value == null) {
                if (param.required) {
                    return (false);
                }
            } else {
                if (!param.type.isInstance(value)) {
                    return false; // value was not of the required type
                }
                if (param.metadata != null) {
                    // check metadata
                    if (param.metadata.containsKey(Param.OPTIONS)) {
                        List<Object> options = (List<Object>) param.metadata.get(Param.OPTIONS);
                        if (options != null && !options.contains(value)) {
                            return false; // invalid option
                        }
                    }
                }
            }
        }

        // dbtype specific check
        String type;
        try {
            type = (String) DBTYPE.lookUp(params);

            if (DBTYPE.sample.equals(type)) {
                return true;
            }

            return false;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
