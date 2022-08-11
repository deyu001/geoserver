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

/**
 * The get executions list request
 *
 * @author Alessio Fabiani - GeoSolutions
 */
public class GetExecutionsType {

    String service;

    String version;

    String owner;

    String identifier;

    String status;

    String orderBy;

    Integer startIndex;

    Integer maxFeatures;

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    /** @return the startIndex */
    public Integer getStartIndex() {
        return startIndex;
    }

    /** @param startIndex the startIndex to set */
    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    /** @return the maxFeatures */
    public Integer getMaxFeatures() {
        return maxFeatures;
    }

    /** @param maxFeatures the maxFeatures to set */
    public void setMaxFeatures(Integer maxFeatures) {
        this.maxFeatures = maxFeatures;
    }

    @Override
    public String toString() {
        return "StatusType [service="
                + service
                + ", version="
                + version
                + ", owner="
                + owner
                + ", identifier="
                + identifier
                + ", status="
                + status
                + ", orderBy="
                + orderBy
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 7961;
        int result = 1;
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result + ((orderBy == null) ? 0 : orderBy.hashCode());
        result = prime * result + ((service == null) ? 0 : service.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        result = prime * result + ((startIndex == null) ? 0 : startIndex.hashCode());
        result = prime * result + ((maxFeatures == null) ? 0 : maxFeatures.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        GetExecutionsType other = (GetExecutionsType) obj;
        if (owner == null) {
            if (other.owner != null) return false;
        } else if (!owner.equals(other.owner)) return false;
        if (identifier == null) {
            if (other.identifier != null) return false;
        } else if (!identifier.equals(other.identifier)) return false;
        if (status == null) {
            if (other.status != null) return false;
        } else if (!status.equals(other.status)) return false;
        if (orderBy == null) {
            if (other.orderBy != null) return false;
        } else if (!orderBy.equals(other.orderBy)) return false;
        if (service == null) {
            if (other.service != null) return false;
        } else if (!service.equals(other.service)) return false;
        if (version == null) {
            if (other.version != null) return false;
        } else if (!version.equals(other.version)) return false;
        if (startIndex == null) {
            if (other.startIndex != null) return false;
        } else if (!startIndex.equals(other.startIndex)) return false;
        if (maxFeatures == null) {
            if (other.maxFeatures != null) return false;
        } else if (!maxFeatures.equals(other.maxFeatures)) return false;
        return true;
    }
}
