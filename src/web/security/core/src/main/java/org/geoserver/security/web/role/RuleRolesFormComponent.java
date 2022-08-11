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

package org.geoserver.security.web.role;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.web.GeoServerApplication;

/** A form component that can be used to edit user/rule role lists */
@SuppressWarnings("serial")
public class RuleRolesFormComponent extends RolePaletteFormComponent {

    static final Set<String> ANY_ROLE = Collections.singleton("*");

    public RuleRolesFormComponent(String id, IModel<Collection<String>> roleNamesModel) {
        super(id, new RolesModel(roleNamesModel), new RuleRolesModel());

        boolean anyRolesEnabled = ANY_ROLE.equals(roleNamesModel.getObject());
        add(
                new AjaxCheckBox("anyRole", new Model<>(anyRolesEnabled)) {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        palette.setEnabled(!getModelObject());
                        target.add(palette);
                    }
                });
        palette.setEnabled(!anyRolesEnabled);
    }

    public RuleRolesFormComponent setHasAnyRole(boolean hasAny) {
        get("anyRole").setDefaultModelObject(hasAny);
        palette.setEnabled(!hasAny);
        return this;
    }

    public boolean isHasAnyRole() {
        return (Boolean) get("anyRole").getDefaultModelObject();
    }

    @Override
    protected String getSelectedHeaderPropertyKey() {
        return "RuleRolesFormComponent.selectedHeader";
    }

    @Override
    protected String getAvaliableHeaderPropertyKey() {
        return "RuleRolesFormComponent.availableHeader";
    }

    //
    //        add(hasAnyBox);
    //        if (hasStoredAnyRole(rootObject)) {
    //            rolePalette.setEnabled(false);
    //            rolePalette.add(new AttributeAppender("disabled", true, new
    // Model<String>("disabled"), " "));
    //            hasAnyBox.setDefaultModelObject(Boolean.TRUE);
    //        }
    //        else {
    //            rolePalette.setEnabled(true);
    //            rolePalette.add(new AttributeAppender("enabled", true, new
    // Model<String>("enabled"), " "));
    //            hasAnyBox.setDefaultModelObject(Boolean.FALSE);
    //        }
    //
    //    }
    //
    //    public abstract boolean hasStoredAnyRole(T rootObject);
    //
    //    public boolean hasAnyRole() {
    //        return (Boolean) hasAnyBox.getDefaultModelObject();
    //    }
    //
    public Set<GeoServerRole> getRolesForStoring() {
        Set<GeoServerRole> result = new HashSet<>();
        if (isHasAnyRole()) {
            result.add(GeoServerRole.ANY_ROLE);
        } else {
            result.addAll(getSelectedRoles());
        }
        return result;
    }

    public Set<String> getRolesNamesForStoring() {
        Set<String> result = new HashSet<>();
        for (GeoServerRole role : getRolesForStoring()) {
            result.add(role.getAuthority());
        }
        return result;
    }

    static class RolesModel extends LoadableDetachableModel<List<GeoServerRole>> {

        IModel<Collection<String>> roleNamesModel;

        RolesModel(IModel<Collection<String>> roleNamesModel) {
            this.roleNamesModel = roleNamesModel;
        }

        @Override
        protected List<GeoServerRole> load() {

            Map<String, GeoServerRole> roleMap;
            roleMap = new HashMap<>();
            try {
                for (GeoServerRole role :
                        GeoServerApplication.get().getSecurityManager().getRolesForAccessControl())
                    roleMap.put(role.getAuthority(), role);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            List<GeoServerRole> roles = new ArrayList<>();
            for (String roleName : roleNamesModel.getObject()) {
                GeoServerRole role = roleMap.get(roleName);
                if (role != null) roles.add(role);
            }
            return roles;
        }

        @Override
        public void setObject(List<GeoServerRole> object) {
            super.setObject(object);

            // set back to the delegate model
            Collection<String> roleNames = roleNamesModel.getObject();
            roleNames.clear();

            for (GeoServerRole role : object) {
                roleNames.add(role.getAuthority());
            }
            // roleNamesModel.setObject(roleNames);
        }
    }

    //    public boolean isHasAny() {
    //        return hasAny;
    //    }
    //
    //    public void setHasAny(boolean hasAny) {
    //        this.hasAny = hasAny;
    //    }
    //
    //    @Override
    //    public void updateModel() {
    //        super.updateModel();
    //        hasAnyBox.updateModel();
    //    }

}
