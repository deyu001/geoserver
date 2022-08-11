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

package org.geoserver.gwc.web.diskquota;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AbstractAutoCompleteTextRenderer;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geowebcache.diskquota.jdbc.JDBCConfiguration.ConnectionPoolConfiguration;

public class JDBCConnectionPoolPanel extends Panel {

    private static final long serialVersionUID = -1579697287836672528L;

    public JDBCConnectionPoolPanel(String id, IModel<ConnectionPoolConfiguration> model) {
        super(id, model);

        TextField<String> driver =
                new TextField<>("jdbcDriver", new PropertyModel<>(model, "driver"));
        driver.setRequired(true);
        AutoCompleteSettings as = new AutoCompleteSettings();
        as.setPreselect(true).setShowListOnEmptyInput(true).setShowCompleteListOnFocusGain(true);
        driver.add(
                new ContainsAutoCompleteBehavior(
                        "org.postgresql.Driver",
                        "oracle.jdbc.driver.OracleDriver",
                        "org.h2.Driver"));
        add(driver);

        TextField<String> url = new TextField<>("jdbcUrl", new PropertyModel<>(model, "url"));
        url.setRequired(true);
        url.add(
                new ContainsAutoCompleteBehavior(
                        "jdbc:h2://{server}:{9092}/{db-name}",
                        "jdbc:postgresql:[{//host}[:{5432}/]]{database}",
                        "jdbc:oracle:thin:@{server}[:{1521}]:{database_name}"));

        add(url);

        TextField<String> user =
                new TextField<>("jdbcUser", new PropertyModel<>(model, "username"));
        add(user);

        PasswordTextField password =
                new PasswordTextField("jdbcPassword", new PropertyModel<>(model, "password"));
        password.setResetPassword(false);
        add(password);

        TextField<Integer> minConnections =
                new TextField<>("jdbcMinConnections", new PropertyModel<>(model, "minConnections"));
        minConnections.setRequired(true);
        minConnections.add(RangeValidator.minimum(0));
        add(minConnections);

        TextField<Integer> maxConnections =
                new TextField<>("jdbcMaxConnections", new PropertyModel<>(model, "maxConnections"));
        maxConnections.setRequired(true);
        maxConnections.add(RangeValidator.minimum(1));
        add(maxConnections);

        TextField<Integer> connectionTimeout =
                new TextField<>(
                        "jdbcConnectionTimeout", new PropertyModel<>(model, "connectionTimeout"));
        connectionTimeout.setRequired(true);
        connectionTimeout.add(RangeValidator.minimum(1));
        add(connectionTimeout);

        TextField<String> validationQuery =
                new TextField<>(
                        "jdbcValidationQuery", new PropertyModel<>(model, "validationQuery"));
        add(validationQuery);

        TextField<Integer> maxOpenPreparedStatements =
                new TextField<>(
                        "jdbcMaxOpenPreparedStatements",
                        new PropertyModel<>(model, "maxOpenPreparedStatements"));
        maxOpenPreparedStatements.setRequired(true);
        maxOpenPreparedStatements.add(RangeValidator.minimum(0));
        add(maxOpenPreparedStatements);
    }

    /**
     * Matches any of the specified choices provided they contain the text typed by the user (in a
     * case insensitive way)
     *
     * @author Andrea Aime - GeoSolutions
     */
    private static class ContainsAutoCompleteBehavior extends AutoCompleteBehavior<String> {
        private static final long serialVersionUID = 993566054116148859L;
        private List<String> choices;

        public ContainsAutoCompleteBehavior(List<String> choices) {
            super(
                    new AbstractAutoCompleteTextRenderer<String>() {
                        private static final long serialVersionUID = 3192368880726583011L;

                        @Override
                        protected String getTextValue(String object) {
                            return object;
                        }
                    });
            settings.setPreselect(true)
                    .setShowListOnEmptyInput(true)
                    .setShowCompleteListOnFocusGain(true);
            this.choices = new ArrayList<>(choices);
        }

        public ContainsAutoCompleteBehavior(String... choices) {
            this(Arrays.asList(choices));
        }

        @Override
        protected Iterator<String> getChoices(String input) {
            String ucInput = input.toUpperCase();
            List<String> result = new ArrayList<>();
            for (String choice : choices) {
                if (choice.toUpperCase().contains(ucInput)) {
                    result.add(choice);
                }
            }

            return result.iterator();
        }
    }
}
