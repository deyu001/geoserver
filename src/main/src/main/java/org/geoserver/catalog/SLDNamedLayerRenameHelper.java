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

package org.geoserver.catalog;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.platform.ServiceException;
import org.geotools.styling.NamedStyle;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.SuppressFBWarnings;

/** Utility for renaming named layers and styles in style groups */
public class SLDNamedLayerRenameHelper {

    protected static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.catalog");

    protected boolean applied = false;
    protected boolean skipErrors = false;

    Map<String, String> renamedLayers = new HashMap<>();
    Map<String, String> renamedLayerGroups = new HashMap<>();
    Map<String, String> renamedStyles = new HashMap<>();

    Catalog catalog;

    public SLDNamedLayerRenameHelper(Catalog catalog) {
        this.catalog = catalog;
    }

    /** @param skipErrors Skip styles that throw an exception when visited */
    public SLDNamedLayerRenameHelper(Catalog catalog, boolean skipErrors) {
        this.catalog = catalog;
        this.skipErrors = skipErrors;
    }

    /**
     * Given a {@link StyleInfo}, {@link LayerInfo}, or {@link LayerGroupInfo} that has been renamed
     * but not yet committed to the catalog, get the list of styles that would be affected by this
     * rename, and optionally apply this rename to those styles.
     *
     * @param updatedInfo The {@link StyleInfo}, {@link LayerInfo}, or {@link LayerGroupInfo} that
     *     has been renamed
     * @param doRename should the styles which have been found be renamed
     * @return List of styles to be renamed / which have been renamed
     * @throws IOException, UnsupportedOperationException
     */
    public static List<StyleInfo> getOrUpdateStylesToRename(
            Catalog catalog, CatalogInfo updatedInfo, boolean doRename) throws IOException {
        CatalogInfo oldInfo;
        String oldName = "";
        String newName = "";
        SLDNamedLayerRenameHelper helper = new SLDNamedLayerRenameHelper(catalog);
        if (updatedInfo instanceof StyleInfo) {
            oldInfo = catalog.getStyle(updatedInfo.getId());
            oldName = ((StyleInfo) oldInfo).getName();
            newName = ((StyleInfo) updatedInfo).getName();

            helper.registerStyleRename(oldName, newName);
        } else if (updatedInfo instanceof LayerInfo) {
            oldInfo = catalog.getLayer(updatedInfo.getId());
            oldName = ((LayerInfo) oldInfo).getName();
            newName = ((LayerInfo) updatedInfo).getName();

            helper.registerLayerRename(oldName, newName);
        } else if (updatedInfo instanceof LayerGroupInfo) {
            oldInfo = catalog.getLayer(updatedInfo.getId());
            oldName = ((LayerGroupInfo) oldInfo).getName();
            newName = ((LayerGroupInfo) updatedInfo).getName();

            helper.registerLayerGroupRename(oldName, newName);
        }
        if (newName.equals(oldName)) {
            return new ArrayList<>();
        }
        return helper.visitStyles(doRename);
    }

    public void registerLayerRename(String oldName, String newName) {
        renamedLayers.put(oldName, newName);
    }

    public void registerLayerGroupRename(String oldName, String newName) {
        renamedLayerGroups.put(oldName, newName);
    }

    public void registerStyleRename(String oldName, String newName) {
        renamedStyles.put(oldName, newName);
    }

    /**
     * Visit each style in the catalog and determine if any of them contain NamedLayers or
     * NamedStyles with the registered names that would need to be renamed. Optionally, apply these
     * renames.
     *
     * @param doRename Should the registered renames be applied
     * @return List of styles to be renamed / which have been renamed
     * @throws IOException, UnsupportedOperationException
     */
    public List<StyleInfo> visitStyles(boolean doRename) throws IOException {
        List<StyleInfo> stylesToUpdate = new ArrayList<>();
        for (StyleInfo style : catalog.getStyles()) {
            SLDNamedLayerRenameVisitor visitor = new SLDNamedLayerRenameVisitor(catalog, doRename);
            try {
                StyledLayerDescriptor sld = catalog.getResourcePool().getSld(style);
                sld.accept(visitor);
                if (visitor.needsRename) {
                    stylesToUpdate.add(style);
                    if (doRename) {
                        backupStyle(style);
                        catalog.getResourcePool().writeSLD(style, sld, true);
                    }
                }
            } catch (IOException | ServiceException e) {
                if (skipErrors) {
                    LOGGER.log(Level.INFO, "Skipping style '" + style.getName() + "'.", e);
                } else {
                    throw e;
                }
            }
        }
        return stylesToUpdate;
    }

    @SuppressFBWarnings({"NP_NONNULL_PARAM_VIOLATION", "NP_NULL_PARAM_DEREF"})
    StyleInfo backupStyle(StyleInfo s) throws IOException {
        StyleInfo backup = catalog.getFactory().createStyle();

        new CatalogBuilder(catalog).updateStyle(backup, s);
        backup.setWorkspace(s.getWorkspace());

        // find a unique name for the style
        String name =
                findUniqueStyleName(
                        s.getWorkspace() == null ? null : s.getWorkspace().getName(),
                        s.getName() + "_BACKUP");
        backup.setName(name);

        // update it's file name
        backup.setFilename(name + "." + FilenameUtils.getExtension(s.getFilename()));

        // copy over the style contents
        try (BufferedReader reader = catalog.getResourcePool().readStyle(s)) {
            catalog.getResourcePool()
                    .writeStyle(
                            backup, new ByteArrayInputStream(IOUtils.toByteArray(reader, "UTF-8")));
        }
        return backup;
    }

    String findUniqueStyleName(String workspace, String name) {
        StyleInfo style = catalog.getStyleByName(workspace, name);
        if (style == null) {
            return name;
        }

        String styleName = null;
        int i = 1;
        while (style != null) {
            styleName = name + i;
            style = catalog.getStyleByName(workspace, styleName);
            i++;
        }
        return styleName;
    }

    private class SLDNamedLayerRenameVisitor extends GeoServerSLDVisitorAdapter {

        // Should the named layers in the styles be renamed when they are visited
        boolean doRename = false;
        // Are there any named layers in this sld that will need to be renamed (set by visitor)
        boolean needsRename = false;

        public SLDNamedLayerRenameVisitor(Catalog catalog, boolean doRename) {
            super(catalog, null);
            this.doRename = doRename;
        }

        @Override
        public PublishedInfo visitNamedLayerInternal(StyledLayer namedLayer) {
            String layerName = namedLayer.getName();
            PublishedInfo p = catalog.getLayerGroupByName(layerName);
            if (p != null) {
                p = catalog.getLayerByName(layerName);
            }
            if (renamedLayerGroups.get(layerName) != null) {
                needsRename = true;
                if (doRename) {
                    namedLayer.setName(renamedLayerGroups.get(layerName));
                }
            } else if (renamedLayers.get(layerName) != null) {
                needsRename = true;
                if (doRename) {
                    namedLayer.setName(renamedLayers.get(layerName));
                }
            }
            return p;
        }

        @Override
        public StyleInfo visitNamedStyleInternal(NamedStyle namedStyle) {
            String styleName = namedStyle.getName();
            StyleInfo s = catalog.getStyleByName(styleName);
            if (renamedStyles.get(styleName) != null) {
                needsRename = true;
                if (doRename) {
                    namedStyle.setName(renamedStyles.get(styleName));
                }
            }
            return s;
        }
    }
}
