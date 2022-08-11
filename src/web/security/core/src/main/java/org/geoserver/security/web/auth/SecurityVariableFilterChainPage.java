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

package org.geoserver.security.web.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.VariableFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.filter.GeoServerExceptionTranslationFilter;
import org.geoserver.security.filter.GeoServerSecurityInterceptorFilter;
import org.geoserver.web.wicket.HelpLink;

/**
 * Class for configuration panels of {@link VariableFilterChain} objects
 *
 * @author christan
 */
public class SecurityVariableFilterChainPage extends SecurityFilterChainPage {

    private static final long serialVersionUID = 1L;

    /** logger */
    protected AuthFilterChainPalette palette;

    public SecurityVariableFilterChainPage(
            VariableFilterChain chain, SecurityManagerConfig secMgrConfig, boolean isNew) {

        VariableFilterChainWrapper wrapper = new VariableFilterChainWrapper(chain);

        Form<VariableFilterChainWrapper> theForm =
                new Form<>("form", new CompoundPropertyModel<>(wrapper));

        super.initialize(chain, secMgrConfig, isNew, theForm, wrapper);

        List<String> filterNames = new ArrayList<>();
        try {
            filterNames.addAll(
                    getSecurityManager().listFilters(GeoServerExceptionTranslationFilter.class));
            for (GeoServerExceptionTranslationFilter filter :
                    GeoServerExtensions.extensions(GeoServerExceptionTranslationFilter.class)) {
                filterNames.add(filter.getName());
            }
            form.add(
                    new DropDownChoice<>(
                            "exceptionTranslationName",
                            new PropertyModel<>(
                                    chainWrapper.getChain(), "exceptionTranslationName"),
                            filterNames));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        filterNames = new ArrayList<>();
        try {
            filterNames.addAll(
                    getSecurityManager().listFilters(GeoServerSecurityInterceptorFilter.class));
            for (GeoServerSecurityInterceptorFilter filter :
                    GeoServerExtensions.extensions(GeoServerSecurityInterceptorFilter.class)) {
                filterNames.add(filter.getName());
            }
            form.add(
                    new DropDownChoice<>(
                            "interceptorName",
                            new PropertyModel<>(chainWrapper.getChain(), "interceptorName"),
                            filterNames));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        form.add(
                palette =
                        new AuthFilterChainPalette(
                                "authFilterChain",
                                new AuthFilterNamesModel(getVariableFilterChainWrapper())));
        palette.setOutputMarkupId(true);
        palette.setChain(getVariableFilterChainWrapper().getVariableFilterChain());

        form.add(new HelpLink("chainConfigFilterHelp").setDialog(dialog));
    }

    VariableFilterChainWrapper getVariableFilterChainWrapper() {
        return (VariableFilterChainWrapper) chainWrapper;
    }

    class AuthFilterNamesModel implements IModel<List<String>> {

        private static final long serialVersionUID = 1L;
        VariableFilterChainWrapper chainModel;

        AuthFilterNamesModel(VariableFilterChainWrapper chainModel) {
            this.chainModel = chainModel;
        }

        @Override
        public List<String> getObject() {

            GeoServerSecurityManager secMgr = getSecurityManager();
            List<String> filters = new ArrayList<>(chainModel.getChain().getFilterNames());
            try {
                filters.retainAll(chainModel.getVariableFilterChain().listFilterCandidates(secMgr));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return filters;
        }

        @Override
        public void setObject(List<String> object) {
            chainModel.getChain().setFilterNames(object);
        }

        @Override
        public void detach() {
            // chainModel.detach();
        }
    }
}
