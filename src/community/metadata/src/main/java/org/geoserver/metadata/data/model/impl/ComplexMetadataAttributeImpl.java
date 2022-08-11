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

package org.geoserver.metadata.data.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bouncycastle.util.Arrays;
import org.geoserver.metadata.data.model.ComplexMetadataAttribute;
import org.geotools.util.Converters;

public class ComplexMetadataAttributeImpl<T extends Serializable>
        implements ComplexMetadataAttribute<T> {

    private static final long serialVersionUID = 7309095204153589550L;

    private Map<String, Serializable> map;

    private String strPath;

    private ComplexMetadataIndexReference indexRef;

    private Class<T> clazz;

    public ComplexMetadataAttributeImpl(
            Map<String, Serializable> map,
            String strPath,
            ComplexMetadataIndexReference indexRef,
            Class<T> clazz) {
        this.map = map;
        this.strPath = strPath;
        this.indexRef = indexRef;
        this.clazz = clazz;
    }

    @Override
    public T getValue() {
        Object val = map.get(strPath);
        for (int i : indexRef.getIndex()) {
            if (val instanceof List<?>) {
                List<?> list = (List<?>) val;
                try {
                    val = list.get(i);
                } catch (IndexOutOfBoundsException e) {
                    val = null;
                }
            } else {
                break;
            }
        }
        if (val == null) {
            return null;
        } else if (clazz.isInstance(val)) {
            return clazz.cast(val);
        } else {
            return Converters.convert(val, clazz);
        }
    }

    @Override
    public void setValue(T value) {
        map.put(strPath, setValueInternal(map.get(strPath), indexRef.getIndex(), value));
    }

    protected Serializable setValueInternal(
            Serializable originalValue, int[] index, Serializable newValue) {
        if (index.length == 0) {
            return newValue;
        } else if (originalValue instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<Serializable> list = (List<Serializable>) originalValue;
            while (list.size() < index[0] + 1) {
                list.add(null);
            }
            if (index.length > 1) {
                int[] subIndex = Arrays.copyOfRange(index, 1, index.length);
                list.set(index[0], setValueInternal(list.get(index[0]), subIndex, newValue));
            } else {
                list.set(index[0], newValue);
            }
            return (Serializable) list;
        } else {
            ArrayList<Object> list = new ArrayList<Object>();
            if (index.length > 1) {
                int[] subIndex = Arrays.copyOfRange(index, 1, index.length);
                while (list.size() < index[0] + 1) {
                    list.add(null);
                }
                list.set(index[0], setValueInternal(originalValue, subIndex, newValue));
            } else {
                for (int i = 0; i < index[0] + 1; i++) {
                    list.add(originalValue);
                }
                list.set(index[0], newValue);
            }
            return list;
        }
    }

    @Override
    public Integer getIndex() {
        return indexRef.getIndex()[indexRef.getIndex().length - 1];
    }
}
