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

package org.geoserver.security.web.passwd;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.web.SecurityNamedServicePanel;

/**
 * Configuration panel for {@link PasswordPolicy}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class PasswordPolicyPanel extends SecurityNamedServicePanel<PasswordPolicyConfig> {

    MaxLengthPanel maxLengthPanel;

    public PasswordPolicyPanel(String id, IModel<PasswordPolicyConfig> model) {
        super(id, model);

        PasswordPolicyConfig pwPolicy = model.getObject();

        // add(new TextField("name").setRequired(true));
        add(new CheckBox("digitRequired"));
        add(new CheckBox("uppercaseRequired"));
        add(new CheckBox("lowercaseRequired"));
        add(new TextField<Integer>("minLength"));

        boolean unlimited = pwPolicy.getMaxLength() == -1;
        add(
                new AjaxCheckBox("unlimitedMaxLength", new Model<>(unlimited)) {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        Boolean value = getModelObject();
                        maxLengthPanel.setVisible(!value);
                        if (value) {
                            maxLengthPanel.setUnlimited();
                        }
                        target.add(maxLengthPanel.getParent());
                    }
                });
        add(
                maxLengthPanel =
                        (MaxLengthPanel) new MaxLengthPanel("maxLength").setVisible(!unlimited));
    }

    public void doSave(PasswordPolicyConfig config) throws Exception {
        getSecurityManager().savePasswordPolicy(config);
    }

    @Override
    public void doLoad(PasswordPolicyConfig config) throws Exception {
        getSecurityManager().loadPasswordPolicyConfig(config.getName());
    }

    class MaxLengthPanel extends FormComponentPanel<PasswordPolicyConfig> {

        public MaxLengthPanel(String id) {
            super(id, new Model<>());
            add(new TextField<Integer>("maxLength"));
            setOutputMarkupId(true);
        }

        public void setUnlimited() {
            get("maxLength").setDefaultModelObject(-1);
        }
    }
}
