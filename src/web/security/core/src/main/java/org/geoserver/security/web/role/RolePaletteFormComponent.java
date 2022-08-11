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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.web.PaletteFormComponent;
import org.geoserver.web.GeoServerApplication;

/** A form component that can be used to edit user/rule role lists */
@SuppressWarnings("serial")
public class RolePaletteFormComponent extends PaletteFormComponent<GeoServerRole> {

    public RolePaletteFormComponent(String id, IModel<List<GeoServerRole>> model) {
        this(id, model, new RolesModel());
    }

    public RolePaletteFormComponent(
            String id,
            IModel<List<GeoServerRole>> model,
            IModel<List<GeoServerRole>> choicesModel) {
        super(id, model, choicesModel, new ChoiceRenderer<>("authority", "authority"));

        //        rolePalette = new Palette<GeoServerRole>(
        //                "roles", , choicesModel,
        //                , 10, false) {
        //            // trick to force the palette to have at least one selected elements
        //            // tried with a nicer validator but it's not used at all, the required thing
        //            // instead is working (don't know why...)
        //            protected Recorder<GeoServerRole> newRecorderComponent() {
        //                Recorder<GeoServerRole> rec = super.newRecorderComponent();
        //                //add any behaviours that need to be added
        //                rec.add(toAdd.toArray(new Behavior[toAdd.size()]));
        //                toAdd.clear();
        //                /*if (isRequired)
        //                    rec.setRequired(true);
        //                if (behavior!=null)
        //                    rec.add(behavior);*/
        //                return rec;
        //            }
        //        };

        GeoServerRoleService roleService = getSecurityManager().getActiveRoleService();
        final String roleServiceName = roleService.getName();

        if (choicesModel instanceof RuleRolesModel)
            add(new Label("roles", new StringResourceModel("roles", this)));
        else
            add(
                    new Label(
                            "roles",
                            new StringResourceModel("rolesFromActiveService", this)
                                    .setParameters(roleServiceName)));

        add(
                new SubmitLink("addRole") {
                    @Override
                    public void onSubmit() {
                        setResponsePage(
                                new NewRolePage(roleServiceName).setReturnPage(this.getPage()));
                    }
                }.setVisible(roleService.canCreateStore()));
    }

    public GeoServerSecurityManager getSecurityManager() {
        return GeoServerApplication.get().getSecurityManager();
    }

    public void diff(
            Collection<GeoServerRole> orig,
            Collection<GeoServerRole> add,
            Collection<GeoServerRole> remove) {

        remove.addAll(orig);
        for (GeoServerRole role : getSelectedRoles()) {
            if (!orig.contains(role)) {
                add.add(role);
            } else {
                remove.remove(role);
            }
        }
    }

    public List<GeoServerRole> getSelectedRoles() {
        return new ArrayList<>(palette.getModelCollection());
    }

    @Override
    protected String getSelectedHeaderPropertyKey() {
        return "RolePaletteFormComponent.selectedHeader";
    }

    @Override
    protected String getAvaliableHeaderPropertyKey() {
        // TODO Auto-generated method stub
        return "RolePaletteFormComponent.availableHeader";
    }
}
