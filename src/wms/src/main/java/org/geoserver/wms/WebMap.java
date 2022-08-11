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

package org.geoserver.wms;

import java.util.HashMap;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geotools.map.Layer;

public abstract class WebMap {

    private String mimeType;

    private java.util.Map<String, String> responseHeaders;

    protected final WMSMapContent mapContent;

    private String extension;

    private String disposition;

    /** @param context the map context, can be {@code null} is there's _really_ no context around */
    public WebMap(final WMSMapContent context) {
        this.mapContent = context;
    }

    /**
     * Disposes any resource held by this Map.
     *
     * <p>This method is meant to be called right after the map is no longer needed. That generally
     * happens at the end of a {@link Response#write} operation, and is meant to free any resource
     * the map implementation might be holding, specially if it contains a refrerence to {@link
     * WMSMapContent}, in which case it's mandatory that the map context's {@link
     * WMSMapContent#dispose()} method is called.
     */
    public final void dispose() {
        if (mapContent != null) {
            mapContent.dispose();
        }
        disposeInternal();
    }

    /**
     * Hook for Map concrete subclasses to dispose any other resource than the {@link WMSMapContent}
     */
    protected void disposeInternal() {
        // default implementation does nothing
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(final String mimeType) {
        this.mimeType = mimeType;
    }

    public void setResponseHeader(final String name, final String value) {
        if (responseHeaders == null) {
            responseHeaders = new HashMap<>();
        }
        responseHeaders.put(name, value);
    }

    public String[][] getResponseHeaders() {
        if (responseHeaders == null || responseHeaders.isEmpty()) {
            return null;
        }
        String[][] headers = new String[responseHeaders.size()][2];
        int index = 0;
        for (java.util.Map.Entry<String, String> entry : responseHeaders.entrySet()) {
            headers[index][0] = entry.getKey();
            headers[index][1] = entry.getValue();
            index++;
        }
        return headers;
    }

    /**
     * Utility method to build a standard content disposition header.
     *
     * <p>It will concatenate the titles of the various layers in the map context, or generate
     * "geoserver" instead (in the event no layer title is set).
     *
     * <p>The file name will be followed by the extension provided, for example, to generate
     * layer.pdf extension will be ".pdf"
     */
    public void setContentDispositionHeader(
            final WMSMapContent mapContent, final String extension) {
        setContentDispositionHeader(mapContent, extension, true);
    }

    /**
     * Utility method to build a standard content disposition header.
     *
     * <p>It will concatenate the titles of the various layers in the map context, or generate
     * "geoserver" instead (in the event no layer title is set).
     *
     * <p>The file name will be followed by the extension provided, for example, to generate
     * layer.pdf extension will be ".pdf"
     */
    public void setContentDispositionHeader(
            final WMSMapContent mapContent, final String extension, boolean attachment) {
        // ischneider - this is nasty, but backwards compatible
        this.extension = extension;
        this.disposition = attachment ? Response.DISPOSITION_ATTACH : Response.DISPOSITION_INLINE;
    }

    public String getDisposition() {
        return disposition;
    }

    public String getAttachmentFileName() {
        String filename = getSimpleAttachmentFileName();
        if (filename != null && extension != null) {
            return filename + extension;
        }
        return filename;
    }

    /** Returns the filename with no extension */
    public String getSimpleAttachmentFileName() {
        // see if we can get the original request, before the group expansion happened
        Request request = Dispatcher.REQUEST.get();
        String filename = null;
        if (request != null
                && request.getRawKvp() != null
                && request.getRawKvp().get("LAYERS") != null) {
            String layers = ((String) request.getRawKvp().get("LAYERS")).trim();
            if (layers.length() > 0) {
                filename = layers.replace(",", "_");
            }
        }
        if (filename == null && mapContent != null) {
            StringBuffer sb = new StringBuffer();
            for (Layer layer : mapContent.layers()) {
                String title = layer.getTitle();
                if (title != null && !title.equals("")) {
                    sb.append(title).append("_");
                }
            }
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
                filename = sb.toString();
            }
        }
        if (filename != null) {
            filename = filename.replace(":", "-");
        }
        return filename;
    }

    public WMSMapContent getMapContent() {
        return mapContent;
    }
}
