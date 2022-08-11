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

package org.geogig.geoserver.web.repository;

import static org.geogig.geoserver.config.RepositoryManager.isGeogigDirectory;

import java.io.File;
import java.net.URI;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geogig.geoserver.util.PostgresConnectionErrorHandler;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryEditPanel extends FormComponentPanel<RepositoryInfo> {

    private static final long serialVersionUID = -870873448379832051L;
    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryEditPanel.class);

    private final GeoGigRepositoryInfoFormComponent config;

    public RepositoryEditPanel(
            final String wicketId, IModel<RepositoryInfo> model, final boolean isNew) {
        super(wicketId, model);

        config = new GeoGigRepositoryInfoFormComponent("repositoryConfig", model, isNew);
        config.setVisible(true);
        add(config);

        add(
                new IValidator<RepositoryInfo>() {

                    private static final long serialVersionUID = 224160688160723504L;

                    @Override
                    public void validate(IValidatable<RepositoryInfo> validatable) {
                        if (isNew) {
                            config.processInput();
                        }
                        ValidationError error = new ValidationError();
                        RepositoryInfo repo = validatable.getValue();
                        final URI location = repo.getLocation();
                        final RepositoryResolver resolver = RepositoryResolver.lookup(location);
                        final String scheme = location.getScheme();
                        final boolean nameExists =
                                RepositoryManager.get().repoExistsByName(repo.getRepoName());
                        if (isNew && nameExists) {
                            error.addKey("errRepositoryNameExists");
                        } else if ("file".equals(scheme)) {
                            File repoDir = new File(location);
                            final File parent = repoDir.getParentFile();
                            if (!parent.exists() || !parent.isDirectory()) {
                                error.addKey("errParentDoesntExist");
                            }
                            if (!parent.canWrite()) {
                                error.addKey("errParentReadOnly");
                            }
                            if (isNew) {
                                if (repoDir.exists()) {
                                    error.addKey("errDirectoryExists");
                                }
                            } else if (!isGeogigDirectory(repoDir)) {
                                error.addKey("notAGeogigRepository");
                            }
                        } else if ("postgresql".equals(scheme)) {
                            try {
                                if (isNew) {
                                    if (resolver.repoExists(location)) {
                                        error.addKey("errRepositoryExists");
                                    }
                                } else {
                                    // try to connect
                                    resolver.open(location);
                                }
                            } catch (Exception ex) {
                                // likely failed to connect
                                LOGGER.error("Failed to connect to PostgreSQL database", ex);
                                error.addKey("errCannotConnectToDatabase");
                                // find root cause
                                error.setVariable(
                                        "message", PostgresConnectionErrorHandler.getMessage(ex));
                            }
                        }
                        if (!error.getKeys().isEmpty()) {
                            validatable.error(error);
                        }
                    }
                });
    }

    @Override
    public void convertInput() {
        RepositoryInfo modelObject = getModelObject();
        setConvertedInput(modelObject);
    }
}
