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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import net.opengis.ows11.KeywordsType;
import net.opengis.ows11.OperationType;
import net.opengis.ows11.OperationsMetadataType;
import net.opengis.ows11.Ows11Factory;
import net.opengis.ows11.ResponsiblePartySubsetType;
import net.opengis.ows11.ServiceIdentificationType;
import net.opengis.ows11.ServiceProviderType;
import net.opengis.wps10.DefaultType2;
import net.opengis.wps10.GetCapabilitiesType;
import net.opengis.wps10.LanguagesType;
import net.opengis.wps10.LanguagesType1;
import net.opengis.wps10.ProcessBriefType;
import net.opengis.wps10.ProcessOfferingsType;
import net.opengis.wps10.WPSCapabilitiesType;
import net.opengis.wps10.Wps10Factory;
import org.eclipse.emf.common.util.ECollections;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.Ows11Util;
import org.geoserver.ows.util.RequestUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geotools.process.ProcessFactory;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.springframework.context.ApplicationContext;

/** @author Lucas Reed, Refractions Research Inc */
public class GetCapabilities {
    public WPSInfo wps;

    ApplicationContext context;

    static final Logger LOGGER = Logging.getLogger(GetCapabilities.class);

    public GetCapabilities(WPSInfo wps, ApplicationContext context) {
        this.wps = wps;
        this.context = context;
    }

    @SuppressWarnings("unchecked") // EMF model without generics
    public WPSCapabilitiesType run(GetCapabilitiesType request) throws WPSException {
        // do the version negotiation dance
        List<String> provided = Collections.singletonList("1.0.0");
        List<String> accepted = null;
        if (request.getAcceptVersions() != null)
            accepted = request.getAcceptVersions().getVersion();
        String version = RequestUtils.getVersionOws11(provided, accepted);

        if (!"1.0.0".equals(version)) {
            throw new WPSException("Could not understand version:" + version);
        }

        // TODO: add update sequence negotiation

        // encode the response
        Wps10Factory wpsf = Wps10Factory.eINSTANCE;
        Ows11Factory owsf = Ows11Factory.eINSTANCE;

        WPSCapabilitiesType caps = wpsf.createWPSCapabilitiesType();
        caps.setVersion("1.0.0");

        // TODO: make configurable
        caps.setLang("en");

        // ServiceIdentification
        ServiceIdentificationType si = owsf.createServiceIdentificationType();
        caps.setServiceIdentification(si);

        // Check if WPS config is loaded
        if (wps == null) {
            throw new ServiceException("WPS config not loaded. Check logs for details.");
        }

        si.getTitle().add(Ows11Util.languageString(wps.getTitle()));
        si.getAbstract().add(Ows11Util.languageString(wps.getAbstract()));

        KeywordsType kw = Ows11Util.keywords(wps.keywordValues());
        if (kw != null) {
            si.getKeywords().add(kw);
        }

        si.setServiceType(Ows11Util.code("WPS"));
        si.setServiceTypeVersion("1.0.0");
        si.setFees(wps.getFees());

        if (wps.getAccessConstraints() != null) {
            si.setAccessConstraints(wps.getAccessConstraints());
        }

        // ServiceProvider
        ServiceProviderType sp = owsf.createServiceProviderType();
        caps.setServiceProvider(sp);

        // TODO: set provder name from context
        SettingsInfo settings = wps.getGeoServer().getSettings();
        if (settings.getContact().getContactOrganization() != null) {
            sp.setProviderName(settings.getContact().getContactOrganization());
        } else {
            sp.setProviderName("GeoServer");
        }

        sp.setProviderSite(owsf.createOnlineResourceType());
        sp.getProviderSite().setHref(settings.getOnlineResource());
        sp.setServiceContact(responsibleParty(settings, owsf));

        // OperationsMetadata
        OperationsMetadataType om = owsf.createOperationsMetadataType();
        caps.setOperationsMetadata(om);

        OperationType gco = owsf.createOperationType();
        gco.setName("GetCapabilities");
        gco.getDCP().add(Ows11Util.dcp("wps", request));
        om.getOperation().add(gco);

        OperationType dpo = owsf.createOperationType();
        dpo.setName("DescribeProcess");
        dpo.getDCP().add(Ows11Util.dcp("wps", request));
        om.getOperation().add(dpo);

        OperationType eo = owsf.createOperationType();
        eo.setName("Execute");
        eo.getDCP().add(Ows11Util.dcp("wps", request));
        om.getOperation().add(eo);

        ProcessOfferingsType po = wpsf.createProcessOfferingsType();
        caps.setProcessOfferings(po);

        // gather the process list
        Set<ProcessFactory> pfs = GeoServerProcessors.getProcessFactories();
        for (ProcessFactory pf : pfs) {
            for (Name name : pf.getNames()) {
                ProcessBriefType p = wpsf.createProcessBriefType();
                p.setProcessVersion(pf.getVersion(name));
                po.getProcess().add(p);

                p.setIdentifier(Ows11Util.code(name));
                p.setTitle(Ows11Util.languageString(pf.getTitle(name)));
                p.setAbstract(Ows11Util.languageString(pf.getDescription(name)));
            }
        }
        // sort it
        ECollections.sort(
                po.getProcess(),
                new Comparator() {

                    public int compare(Object o1, Object o2) {
                        ProcessBriefType pb1 = (ProcessBriefType) o1;
                        ProcessBriefType pb2 = (ProcessBriefType) o2;

                        final String id1 = pb1.getIdentifier().getValue();
                        final String id2 = pb2.getIdentifier().getValue();
                        return id1.compareTo(id2);
                    }
                });

        LanguagesType1 languages = wpsf.createLanguagesType1();
        caps.setLanguages(languages);

        DefaultType2 defaultLanguage = wpsf.createDefaultType2();
        languages.setDefault(defaultLanguage);
        defaultLanguage.setLanguage("en-US");

        LanguagesType supportedLanguages = wpsf.createLanguagesType();
        languages.setSupported(supportedLanguages);
        supportedLanguages.getLanguage().add("en-US");

        return caps;
        // Version detection and alternative invocation if being implemented.
    }

    ResponsiblePartySubsetType responsibleParty(SettingsInfo settings, Ows11Factory f) {
        ResponsiblePartySubsetType rp = f.createResponsiblePartySubsetType();
        return rp;
    }
}
