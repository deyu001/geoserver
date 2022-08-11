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

package org.geoserver.ows;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import net.opengis.ows11.CodeType;
import net.opengis.ows11.DCPType;
import net.opengis.ows11.DomainMetadataType;
import net.opengis.ows11.ExceptionReportType;
import net.opengis.ows11.ExceptionType;
import net.opengis.ows11.KeywordsType;
import net.opengis.ows11.LanguageStringType;
import net.opengis.ows11.Ows11Factory;
import net.opengis.ows11.RequestMethodType;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geotools.feature.NameImpl;
import org.geotools.xsd.EMFUtils;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;

public class Ows11Util {

    static Ows11Factory f = Ows11Factory.eINSTANCE;

    public static LanguageStringType languageString(InternationalString value) {
        if (value != null) {
            return languageString(value.toString(Locale.getDefault()));
        } else {
            return null;
        }
    }

    public static LanguageStringType languageString(String value) {
        LanguageStringType ls = f.createLanguageStringType();
        ls.setValue(value);
        return ls;
    }

    @SuppressWarnings("unchecked") // due to KeywordsType using raw collections
    public static KeywordsType keywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return null;
        }
        KeywordsType kw = f.createKeywordsType();
        for (String keyword : keywords) {
            kw.getKeyword().add(languageString(keyword));
        }
        return kw;
    }

    public static CodeType code(String value) {
        CodeType code = f.createCodeType();
        code.setValue(value);

        return code;
    }

    public static CodeType code(Name name) {
        CodeType code = f.createCodeType();
        //        code.setCodeSpace(name.getNamespaceURI());
        //        code.setValue(name.getLocalPart());
        code.setValue(name.getURI());

        return code;
    }

    public static Name name(CodeType code) {
        // mushy translation, code type seems to never have a code space in practice
        if (code.getCodeSpace() != null) {
            return new NameImpl(code.getCodeSpace(), code.getValue());
        } else {
            return name(code.getValue());
        }
    }

    /** Turns a prefix:localName into a Name */
    public static Name name(String URI) {
        String[] parsed = URI.trim().split(":");
        if (parsed.length == 1) {
            return new NameImpl(parsed[0]);
        } else {
            return new NameImpl(parsed[0], parsed[1]);
        }
    }

    public static CodeType code(CodeType value) {
        return code(value.getValue());
    }

    public static DomainMetadataType type(String name) {
        DomainMetadataType type = f.createDomainMetadataType();
        type.setValue(name);

        return type;
    }

    public static ExceptionReportType exceptionReport(
            ServiceException exception, boolean verboseExceptions) {
        return exceptionReport(exception, verboseExceptions, null);
    }

    @SuppressWarnings("unchecked") // due to ExceptionType using raw collections
    public static ExceptionReportType exceptionReport(
            ServiceException exception, boolean verboseExceptions, String version) {

        ExceptionType e = f.createExceptionType();

        if (exception.getCode() != null) {
            e.setExceptionCode(exception.getCode());
        } else {
            // set a default
            e.setExceptionCode("NoApplicableCode");
        }

        e.setLocator(exception.getLocator());

        // add the message
        StringBuffer sb = new StringBuffer();
        OwsUtils.dumpExceptionMessages(exception, sb, true);
        e.getExceptionText().add(sb.toString());
        e.getExceptionText().addAll(exception.getExceptionText());

        if (verboseExceptions) {
            // add the entire stack trace
            // exception.
            e.getExceptionText().add("Details:");
            ByteArrayOutputStream trace = new ByteArrayOutputStream();
            exception.printStackTrace(new PrintStream(trace));
            e.getExceptionText().add(new String(trace.toByteArray()));
        }

        ExceptionReportType report = f.createExceptionReportType();

        version = version != null ? version : "1.1.0";
        report.setVersion(version);
        report.getException().add(e);

        return report;
    }

    @SuppressWarnings("unchecked") // due to DCPType using raw collections
    public static DCPType dcp(String service, EObject request) {
        String baseUrl = (String) EMFUtils.get(request, "baseUrl");
        if (baseUrl == null) {
            throw new IllegalArgumentException(
                    "Request object" + request + " has no 'baseUrl' property.");
        }
        String href = ResponseUtils.buildURL(baseUrl, service, new HashMap<>(), URLType.SERVICE);

        DCPType dcp = f.createDCPType();
        dcp.setHTTP(f.createHTTPType());

        RequestMethodType get = f.createRequestMethodType();
        get.setHref(href);
        dcp.getHTTP().getGet().add(get);

        RequestMethodType post = f.createRequestMethodType();
        post.setHref(href);
        dcp.getHTTP().getPost().add(post);

        return dcp;
    }
}
