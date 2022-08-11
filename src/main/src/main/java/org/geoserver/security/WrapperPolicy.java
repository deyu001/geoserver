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

package org.geoserver.security;

import java.io.Serializable;

/** The combination of access level granted and response policy (lists only possible cases) */
public class WrapperPolicy implements Serializable, Comparable<WrapperPolicy> {
    private static final long serialVersionUID = -7490634837165130290L;

    // TODO: turn these into private fields
    public final AccessLevel level; // needed, depends on catalog mode and request type
    public final Response response; // needed, by catalog mode
    public final AccessLimits limits;

    public static final WrapperPolicy hide(AccessLimits limits) {
        return new WrapperPolicy(AccessLevel.HIDDEN, Response.HIDE, limits);
    }

    public static final WrapperPolicy metadata(AccessLimits limits) {
        return new WrapperPolicy(AccessLevel.METADATA, Response.CHALLENGE, limits);
    }

    public static final WrapperPolicy readOnlyChallenge(AccessLimits limits) {
        return new WrapperPolicy(AccessLevel.READ_ONLY, Response.CHALLENGE, limits);
    }

    public static final WrapperPolicy readOnlyHide(AccessLimits limits) {
        return new WrapperPolicy(AccessLevel.READ_ONLY, Response.HIDE, limits);
    }

    public static final WrapperPolicy readWrite(AccessLimits limits) {
        Response respone =
                limits == null || limits.getMode() == CatalogMode.HIDE
                        ? Response.HIDE
                        : Response.CHALLENGE;
        return new WrapperPolicy(AccessLevel.READ_WRITE, respone, limits);
    }

    WrapperPolicy(AccessLevel level, Response response, AccessLimits limits) {
        this.level = level;
        this.response = response;
        this.limits = limits;
    }

    public Response getResponse() {
        return response;
    }

    public AccessLimits getLimits() {
        return limits;
    }

    public AccessLevel getAccessLevel() {
        return level;
    }

    public boolean isHide() {
        return level == AccessLevel.HIDDEN && response == Response.HIDE;
    }

    public boolean isMetadata() {
        return level == AccessLevel.METADATA && response == Response.CHALLENGE;
    }

    public boolean isReadOnlyChallenge() {
        return level == AccessLevel.READ_ONLY && response == Response.CHALLENGE;
    }

    public boolean isReadOnlyHide() {
        return level == AccessLevel.READ_ONLY && response == Response.HIDE;
    }

    public boolean isReadWrite() {
        return level == AccessLevel.READ_ONLY && response == Response.CHALLENGE;
    }

    /** Builds a new WrapperPolicy copying this one, but with a different access limits object */
    public WrapperPolicy derive(AccessLimits limits) {
        return new WrapperPolicy(this.level, this.response, limits);
    }

    /**
     * Sorts wrapper policies from more to less restrictive limits.
     *
     * <p>That is, first comparison order is {@link #getAccessLevel() getAccessLevel() ==} {@link
     * AccessLevel#HIDDEN HIDDEN}/{@link AccessLevel#METADATA METADATA}/ {@link
     * AccessLevel#READ_ONLY READ_ONLY}/{@link AccessLevel#READ_WRITE READ_WRITE}.
     *
     * <p>Second comparison criteria is {@link AccessLimits#getMode() getLimits().getMode()} {@code
     * == } {@link CatalogMode#HIDE HIDE}/ {@link CatalogMode#CHALLENGE CHALLENGE}/ {@link
     * CatalogMode#MIXED MIXED}/{@link #getLimits() getLimits() == null} (i.e. no limits)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(WrapperPolicy w) {
        int levelComparison = getAccessLevel().compareTo(w.getAccessLevel());
        if (levelComparison != 0) {
            return levelComparison;
        }
        CatalogMode myLimits = getLimits() == null ? null : getLimits().getMode();
        CatalogMode theirLimits = w.getLimits() == null ? null : w.getLimits().getMode();

        return myLimits == null
                ? (theirLimits == null ? 0 : 1)
                : (theirLimits == null ? -1 : myLimits.compareTo(theirLimits));
    }

    @Override
    public String toString() {
        return "WrapperPolicy [level="
                + level
                + ", response="
                + response
                + ", limits="
                + limits
                + "]";
    }
}
