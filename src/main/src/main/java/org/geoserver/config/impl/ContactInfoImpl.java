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

package org.geoserver.config.impl;

import org.geoserver.config.ContactInfo;

public class ContactInfoImpl implements ContactInfo {

    String id = "contact";

    String address;

    String addressCity;

    String addressCountry;

    String addressDeliveryPoint;

    String addressPostalCode;

    String addressState;

    String addressType;

    String contactEmail;

    String contactFacsimile;

    String contactOrganization;

    String contactPerson;

    String contactPosition;

    String contactVoice;

    String onlineResource;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public String getAddressCountry() {
        return addressCountry;
    }

    public void setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
    }

    @Override
    public String getAddressDeliveryPoint() {
        return addressDeliveryPoint;
    }

    @Override
    public void setAddressDeliveryPoint(String addressDeliveryPoint) {
        this.addressDeliveryPoint = addressDeliveryPoint;
    }

    public String getAddressPostalCode() {
        return addressPostalCode;
    }

    public void setAddressPostalCode(String addressPostalCode) {
        this.addressPostalCode = addressPostalCode;
    }

    public String getAddressState() {
        return addressState;
    }

    public void setAddressState(String addressState) {
        this.addressState = addressState;
    }

    public String getAddressType() {
        return addressType;
    }

    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactFacsimile() {
        return contactFacsimile;
    }

    public void setContactFacsimile(String contactFacsimile) {
        this.contactFacsimile = contactFacsimile;
    }

    public String getContactOrganization() {
        return contactOrganization;
    }

    public void setContactOrganization(String contactOrganization) {
        this.contactOrganization = contactOrganization;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPosition() {
        return contactPosition;
    }

    public void setContactPosition(String contactPosition) {
        this.contactPosition = contactPosition;
    }

    public String getContactVoice() {
        return contactVoice;
    }

    public void setContactVoice(String contactVoice) {
        this.contactVoice = contactVoice;
    }

    public String getOnlineResource() {
        return onlineResource;
    }

    public void setOnlineResource(String onlineResource) {
        this.onlineResource = onlineResource;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((address == null) ? 0 : address.hashCode());
        result = PRIME * result + ((addressCity == null) ? 0 : addressCity.hashCode());
        result = PRIME * result + ((addressCountry == null) ? 0 : addressCountry.hashCode());
        result = PRIME * result + ((addressPostalCode == null) ? 0 : addressPostalCode.hashCode());
        result = PRIME * result + ((addressState == null) ? 0 : addressState.hashCode());
        result = PRIME * result + ((addressType == null) ? 0 : addressType.hashCode());
        result = PRIME * result + ((contactEmail == null) ? 0 : contactEmail.hashCode());
        result = PRIME * result + ((contactFacsimile == null) ? 0 : contactFacsimile.hashCode());
        result =
                PRIME * result
                        + ((contactOrganization == null) ? 0 : contactOrganization.hashCode());
        result = PRIME * result + ((contactPerson == null) ? 0 : contactPerson.hashCode());
        result = PRIME * result + ((contactPosition == null) ? 0 : contactPosition.hashCode());
        result = PRIME * result + ((contactVoice == null) ? 0 : contactVoice.hashCode());
        result = PRIME * result + ((onlineResource == null) ? 0 : onlineResource.hashCode());
        result =
                PRIME * result
                        + ((addressDeliveryPoint == null) ? 0 : addressDeliveryPoint.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof ContactInfo)) return false;
        final ContactInfo other = (ContactInfo) obj;
        if (address == null) {
            if (other.getAddress() != null) return false;
        } else if (!address.equals(other.getAddress())) return false;
        if (addressCity == null) {
            if (other.getAddressCity() != null) return false;
        } else if (!addressCity.equals(other.getAddressCity())) return false;
        if (addressCountry == null) {
            if (other.getAddressCountry() != null) return false;
        } else if (!addressCountry.equals(other.getAddressCountry())) return false;
        if (addressPostalCode == null) {
            if (other.getAddressPostalCode() != null) return false;
        } else if (!addressPostalCode.equals(other.getAddressPostalCode())) return false;
        if (addressState == null) {
            if (other.getAddressState() != null) return false;
        } else if (!addressState.equals(other.getAddressState())) return false;
        if (addressType == null) {
            if (other.getAddressType() != null) return false;
        } else if (!addressType.equals(other.getAddressType())) return false;
        if (contactEmail == null) {
            if (other.getContactEmail() != null) return false;
        } else if (!contactEmail.equals(other.getContactEmail())) return false;
        if (contactFacsimile == null) {
            if (other.getContactFacsimile() != null) return false;
        } else if (!contactFacsimile.equals(other.getContactFacsimile())) return false;
        if (contactOrganization == null) {
            if (other.getContactOrganization() != null) return false;
        } else if (!contactOrganization.equals(other.getContactOrganization())) return false;
        if (contactPerson == null) {
            if (other.getContactPerson() != null) return false;
        } else if (!contactPerson.equals(other.getContactPerson())) return false;
        if (contactPosition == null) {
            if (other.getContactPosition() != null) return false;
        } else if (!contactPosition.equals(other.getContactPosition())) return false;
        if (contactVoice == null) {
            if (other.getContactVoice() != null) return false;
        } else if (!contactVoice.equals(other.getContactVoice())) return false;
        if (onlineResource == null) {
            if (other.getOnlineResource() != null) return false;
        } else if (!onlineResource.equals(other.getOnlineResource())) return false;
        if (addressDeliveryPoint == null) {
            if (other.getAddressDeliveryPoint() != null) return false;
        } else if (!addressDeliveryPoint.equals(other.getAddressDeliveryPoint())) return false;
        return true;
    }
}
