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

package org.geoserver.web.resources;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.TreeSet;
import org.apache.wicket.model.IModel;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.web.treeview.TreeNode;

/**
 * Implementation of TreeNode for Resource
 *
 * @author Niels Charlier
 */
public class ResourceNode implements TreeNode<Resource>, Comparable<ResourceNode> {

    private static final long serialVersionUID = 4941394483034830079L;

    private Resource resource;

    private ResourceExpandedStates expandedStates;

    private String uniqueId;

    public ResourceNode(Resource resource, ResourceExpandedStates expandedStates) {
        this.resource = Resources.serializable(resource);
        this.expandedStates = expandedStates;
        this.uniqueId = getUniqueId(this.resource.path());
    }

    public static String getUniqueId(String path) {
        if (path.isEmpty()) {
            return "/";
        } else if (!path.contains(":") && !path.contains("~")) {
            // Helps prevent duplicate Wicket IDs if there is a filename that
            // is the Base64 encoded path of a file that has to be encoded
            // without having to unnecessarily encode everything.
            return "/" + path;
        }
        // Base64 encode the file path to replace special characters that are
        // not allowed in Wicket component IDs.
        byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    @Override
    public Set<TreeNode<Resource>> getChildren() {
        Set<TreeNode<Resource>> children = new TreeSet<>();
        for (Resource res : resource.list()) {
            children.add(new ResourceNode(res, expandedStates));
        }
        return children;
    }

    @Override
    public TreeNode<Resource> getParent() {
        if (resource.name().isEmpty()) {
            return null;
        } else {
            return new ResourceNode(resource.parent(), expandedStates);
        }
    }

    @Override
    public Resource getObject() {
        return resource;
    }

    @Override
    public String getLabel() {
        String name = resource.name();
        if (name.isEmpty()) {
            return "/";
        } else {
            return name;
        }
    }

    @Override
    public boolean isLeaf() {
        return resource.getType() == Resource.Type.RESOURCE;
    }

    @Override
    public IModel<Boolean> getExpanded() {
        if (isLeaf()) {
            return null;
        } else {
            return expandedStates.getResourceExpandedState(resource);
        }
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public int compareTo(ResourceNode other) {
        int i = resource.name().compareTo(other.resource.name());
        if (i == 0 && resource.parent() != null) {
            i = resource.parent().name().compareTo(other.resource.parent().name());
        }
        return i;
    }

    @Override
    public boolean equals(Object node) {
        return node instanceof ResourceNode && isSameAs((ResourceNode) node);
    }

    @Override
    public int hashCode() {
        return getUniqueId().hashCode();
    }
}
