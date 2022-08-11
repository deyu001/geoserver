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

package org.geoserver.ows.xml.v1_0;

import net.opengis.ows10.Ows10Factory;
import org.geotools.xlink.XLINKConfiguration;
import org.geotools.xsd.Configuration;
import org.picocontainer.MutablePicoContainer;

/**
 * Parser configuration for ows schema.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class OWSConfiguration extends Configuration {
    /** Creates a new configuration, adding the dependency on {@link OWSConfiguration}. */
    public OWSConfiguration() {
        super(OWS.getInstance());

        addDependency(new XLINKConfiguration());
    }

    protected void registerBindings(MutablePicoContainer container) {
        // Types
        container.registerComponentImplementation(
                OWS.ACCEPTFORMATSTYPE, AcceptFormatsTypeBinding.class);
        container.registerComponentImplementation(
                OWS.ACCEPTVERSIONSTYPE, AcceptVersionsTypeBinding.class);
        container.registerComponentImplementation(OWS.ADDRESSTYPE, AddressTypeBinding.class);
        container.registerComponentImplementation(
                OWS.BOUNDINGBOXTYPE, BoundingBoxTypeBinding.class);
        container.registerComponentImplementation(
                OWS.CAPABILITIESBASETYPE, CapabilitiesBaseTypeBinding.class);
        container.registerComponentImplementation(OWS.CODETYPE, CodeTypeBinding.class);
        container.registerComponentImplementation(OWS.CONTACTTYPE, ContactTypeBinding.class);
        container.registerComponentImplementation(
                OWS.DESCRIPTIONTYPE, DescriptionTypeBinding.class);
        container.registerComponentImplementation(OWS.DOMAINTYPE, DomainTypeBinding.class);
        container.registerComponentImplementation(OWS.EXCEPTIONTYPE, ExceptionTypeBinding.class);
        container.registerComponentImplementation(
                OWS.GETCAPABILITIESTYPE, GetCapabilitiesTypeBinding.class);
        container.registerComponentImplementation(
                OWS.IDENTIFICATIONTYPE, IdentificationTypeBinding.class);
        container.registerComponentImplementation(OWS.KEYWORDSTYPE, KeywordsTypeBinding.class);
        container.registerComponentImplementation(OWS.METADATATYPE, MetadataTypeBinding.class);
        // container.registerComponentImplementation(OWS.MIMETYPE,MimeTypeBinding.class);
        container.registerComponentImplementation(
                OWS.ONLINERESOURCETYPE, OnlineResourceTypeBinding.class);
        container.registerComponentImplementation(OWS.POSITIONTYPE, PositionTypeBinding.class);
        container.registerComponentImplementation(OWS.POSITIONTYPE2D, PositionType2DBinding.class);
        container.registerComponentImplementation(
                OWS.REQUESTMETHODTYPE, RequestMethodTypeBinding.class);
        container.registerComponentImplementation(
                OWS.RESPONSIBLEPARTYSUBSETTYPE, ResponsiblePartySubsetTypeBinding.class);
        container.registerComponentImplementation(
                OWS.RESPONSIBLEPARTYTYPE, ResponsiblePartyTypeBinding.class);
        container.registerComponentImplementation(OWS.SECTIONSTYPE, SectionsTypeBinding.class);
        // container.registerComponentImplementation(OWS.SERVICETYPE,ServiceTypeBinding.class);
        container.registerComponentImplementation(OWS.TELEPHONETYPE, TelephoneTypeBinding.class);
        // container.registerComponentImplementation(OWS.UPDATESEQUENCETYPE,UpdateSequenceTypeBinding.class);
        // container.registerComponentImplementation(OWS.VERSIONTYPE,VersionTypeBinding.class);
        container.registerComponentImplementation(
                OWS.WGS84BOUNDINGBOXTYPE, WGS84BoundingBoxTypeBinding.class);

        // elements
        container.registerComponentImplementation(
                OWS.EXCEPTIONREPORT, ExceptionReportBinding.class);
    }

    /**
     * Configures the ows context.
     *
     * <p>The following factories are registered:
     *
     * <ul>
     *   <li>{@link Ows10Factory}
     * </ul>
     */
    protected void configureContext(MutablePicoContainer container) {
        super.configureContext(container);

        container.registerComponentInstance(Ows10Factory.eINSTANCE);
    }
}
