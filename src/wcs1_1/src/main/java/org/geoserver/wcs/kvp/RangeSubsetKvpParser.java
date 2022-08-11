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

package org.geoserver.wcs.kvp;

import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

import java.io.StringReader;
import net.opengis.ows11.CodeType;
import net.opengis.ows11.Ows11Factory;
import net.opengis.wcs11.AxisSubsetType;
import net.opengis.wcs11.FieldSubsetType;
import net.opengis.wcs11.RangeSubsetType;
import net.opengis.wcs11.Wcs111Factory;
import org.geoserver.ows.KvpParser;
import org.geoserver.wcs.kvp.rangesubset.ASTAxisId;
import org.geoserver.wcs.kvp.rangesubset.ASTAxisSubset;
import org.geoserver.wcs.kvp.rangesubset.ASTFieldId;
import org.geoserver.wcs.kvp.rangesubset.ASTFieldSubset;
import org.geoserver.wcs.kvp.rangesubset.ASTInterpolation;
import org.geoserver.wcs.kvp.rangesubset.ASTKey;
import org.geoserver.wcs.kvp.rangesubset.ASTRangeSubset;
import org.geoserver.wcs.kvp.rangesubset.Node;
import org.geoserver.wcs.kvp.rangesubset.RangeSubsetParser;
import org.geoserver.wcs.kvp.rangesubset.RangeSubsetParserVisitor;
import org.geoserver.wcs.kvp.rangesubset.SimpleNode;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Parses the RangeSubset parameter of a GetFeature KVP request
 *
 * @author Andrea Aime
 */
public class RangeSubsetKvpParser extends KvpParser {

    public RangeSubsetKvpParser() {
        super("RangeSubset", RangeSubsetType.class);
    }

    @Override
    public Object parse(String value) throws Exception {
        RangeSubsetParser parser = new RangeSubsetParser(new StringReader(value));
        SimpleNode root = parser.RangeSubset();
        RangeSubsetType result =
                (RangeSubsetType) root.jjtAccept(new RangeSubsetKvpParserVisitor(), null);

        for (Object o : result.getFieldSubset()) {
            FieldSubsetType type = (FieldSubsetType) o;
            String interpolationType = type.getInterpolationType();
            if (interpolationType != null) {
                try {
                    InterpolationMethod.valueOf(interpolationType);
                } catch (IllegalArgumentException e) {
                    throw new WcsException(
                            "Unknown interpolation method " + interpolationType,
                            InvalidParameterValue,
                            "RangeSubset");
                }
            }
        }

        return result;
    }

    private static class RangeSubsetKvpParserVisitor implements RangeSubsetParserVisitor {
        Wcs111Factory wcsf = Wcs111Factory.eINSTANCE;
        Ows11Factory owsf = Ows11Factory.eINSTANCE;

        public Object visit(SimpleNode node, Object data) {
            throw new UnsupportedOperationException("This method should never be reached");
        }

        @SuppressWarnings("unchecked") // EMF model without generics
        public Object visit(ASTRangeSubset node, Object data) {
            RangeSubsetType rs = wcsf.createRangeSubsetType();
            for (int i = 0; i < node.jjtGetNumChildren(); i++) {
                ASTFieldSubset fs = (ASTFieldSubset) node.jjtGetChild(i);
                FieldSubsetType fst = (FieldSubsetType) fs.jjtAccept(this, data);
                rs.getFieldSubset().add(fst);
            }
            return rs;
        }

        @SuppressWarnings("unchecked") // EMF model without generics
        public Object visit(ASTFieldSubset node, Object data) {
            FieldSubsetType fs = wcsf.createFieldSubsetType();

            for (int i = 0; i < node.jjtGetNumChildren(); i++) {
                Node child = node.jjtGetChild(i);
                if (child instanceof ASTFieldId) {
                    CodeType id = owsf.createCodeType();
                    id.setValue((String) child.jjtAccept(this, null));
                    fs.setIdentifier(id);
                } else if (child instanceof ASTInterpolation) {
                    fs.setInterpolationType((String) child.jjtAccept(this, null));
                } else if (child instanceof ASTAxisSubset) {
                    fs.getAxisSubset().add(child.jjtAccept(this, null));
                }
            }
            return fs;
        }

        @SuppressWarnings("unchecked") // EMF model without generics
        public Object visit(ASTAxisSubset node, Object data) {
            AxisSubsetType as = wcsf.createAxisSubsetType();
            as.setIdentifier(((SimpleNode) node.jjtGetChild(0)).getContent());
            for (int i = 1; i < node.jjtGetNumChildren(); i++) {
                as.getKey().add(node.jjtGetChild(i).jjtAccept(this, null));
            }
            return as;
        }

        public Object visit(ASTFieldId node, Object data) {
            return node.getContent();
        }

        public Object visit(ASTAxisId node, Object data) {
            return node.getContent();
        }

        public Object visit(ASTInterpolation node, Object data) {
            return node.getContent();
        }

        public Object visit(ASTKey node, Object data) {
            return node.getContent();
        }
    }
}
