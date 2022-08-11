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

package org.geoserver.wps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.geoserver.catalog.MetadataMap;
import org.geotools.process.ProcessFactory;

public class ProcessGroupInfoImpl implements ProcessGroupInfo {

    private static final long serialVersionUID = 4850653421657310854L;

    Class<? extends ProcessFactory> factoryClass;

    boolean enabled;

    List<String> roles = new ArrayList<>();

    List<ProcessInfo> filteredProcesses = new ArrayList<>();

    MetadataMap metadata = new MetadataMap();

    @Override
    public String getId() {
        return "wpsProcessFactory-" + factoryClass.getName();
    }

    public Class<? extends ProcessFactory> getFactoryClass() {
        return factoryClass;
    }

    public void setFactoryClass(Class<? extends ProcessFactory> factoryClass) {
        this.factoryClass = factoryClass;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<ProcessInfo> getFilteredProcesses() {
        return filteredProcesses;
    }

    public void setFilteredProcesses(List<ProcessInfo> filteredProcesses) {
        this.filteredProcesses = filteredProcesses;
    }

    @Override
    public MetadataMap getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataMap metadataMap) {
        this.metadata = metadataMap;
    }

    @Override
    public ProcessGroupInfo clone() {
        ProcessGroupInfoImpl clone = new ProcessGroupInfoImpl();
        clone.setEnabled(enabled);
        clone.setFactoryClass(factoryClass);
        clone.setRoles(roles);
        if (filteredProcesses != null) {
            clone.setFilteredProcesses(new ArrayList<>(filteredProcesses));
        }
        if (metadata != null) {
            clone.metadata = new MetadataMap(new HashMap<>(metadata));
        }

        return clone;
    }

    @Override
    public List<String> getRoles() {
        return roles;
    }

    @Override
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (enabled ? 1231 : 1237);
        result = prime * result + ((factoryClass == null) ? 0 : factoryClass.hashCode());
        result = prime * result + ((filteredProcesses == null) ? 0 : filteredProcesses.hashCode());
        result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
        result = prime * result + ((roles == null) ? 0 : roles.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ProcessGroupInfoImpl other = (ProcessGroupInfoImpl) obj;
        if (enabled != other.enabled) return false;
        if (factoryClass == null) {
            if (other.factoryClass != null) return false;
        } else if (!factoryClass.equals(other.factoryClass)) return false;
        if (filteredProcesses == null) {
            if (other.filteredProcesses != null) return false;
        } else if (!filteredProcesses.equals(other.filteredProcesses)) return false;
        if (metadata == null) {
            if (other.metadata != null) return false;
        } else if (!metadata.equals(other.metadata)) return false;
        if (roles == null) {
            if (other.roles != null) return false;
        } else if (!roles.equals(other.roles)) return false;
        return true;
    }
}
