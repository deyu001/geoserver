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

package org.geoserver.platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Bean used to register module installation in applicationContext.xml.
 *
 * <p>Bean completely defined by applicationContext.xml - no dynamic content.
 *
 * <pre>
 * &lt!-- code example needed --&gt;>
 * </pre>
 *
 * @author Morgan Thompson - Boundless
 */
public class ModuleStatusImpl implements ModuleStatus, Serializable {

    private static final Logger LOGGER = Logging.getLogger(ModuleStatusImpl.class);

    /** serialVersionUID */
    private static final long serialVersionUID = -5759469520194940051L;

    private String module;

    private String name;

    private String component;

    private String version;

    private String documentation;

    private String message;

    private boolean isEnabled;

    private boolean isAvailable;

    public ModuleStatusImpl() {}

    /**
     * Copy-constructor, used to construct model objects from beans
     *
     * @param status The {@link ModuleStatus} to copy
     */
    public ModuleStatusImpl(ModuleStatus status) {
        this.module = status.getModule();
        this.name = status.getName();
        this.component = status.getComponent().orElse(null);
        this.version = status.getVersion().orElse(getVersionInternal());
        this.documentation = status.getDocumentation().orElse(null);
        this.message = status.getMessage().orElse(null);
        this.isEnabled = status.isEnabled();
        this.isAvailable = status.isAvailable();
    }

    /**
     * Bean constructor used in applicationContext.xml
     *
     * @param module The module identifier, e.g. "gs-main"
     * @param name The module name.
     */
    public ModuleStatusImpl(String module, String name) {
        this.module = module;
        this.name = name;
        this.isAvailable = true;
        this.isEnabled = true;
    }

    /** @return the machine readable name */
    public String getModule() {
        return module;
    }

    /** @param module the module name to set */
    public void setModule(String module) {
        this.module = module;
    }

    public Optional<String> getComponent() {
        return Optional.ofNullable(component);
    }

    /** @param component the component to set */
    public void setComponent(String component) {
        this.component = component;
    }

    /** @return the name */
    public String getName() {
        return name;
    }

    /** @param name the name to set */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the version */
    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

    /** @param version the version to set */
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean isAvailable() {
        return this.isAvailable;
    }

    public void setAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    @Override
    public boolean isEnabled() {
        return this.isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    @Override
    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    @Override
    public String toString() {
        return "ModuleStatusImpl [module="
                + module
                + ", component="
                + component
                + ", version="
                + version
                + "]";
    }

    /**
     * Reads the version for the module from the pom.properties.
     *
     * <p>WARNING: This method reads every pom.properties on the classpath. It should only be used
     * if absolutely necessary
     */
    protected String getVersionInternal() {
        List<Properties> matches = new ArrayList<>();
        try {
            Resource[] resources =
                    new PathMatchingResourcePatternResolver()
                            .getResources("classpath*:META-INF/maven/*/*/pom.properties");
            for (Resource resource : resources) {
                try (InputStream in = resource.getInputStream()) {
                    Properties properties = new Properties();
                    properties.load(in);
                    if (module != null) {
                        if (module.equals(properties.getProperty("artifactId"))) {
                            matches.add(properties);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.log(
                            Level.WARNING,
                            "Error reading pom.properties: " + resource.getFilename(),
                            e);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error listing pom.properties", e);
        }
        if (matches.isEmpty()) {
            return null;
        } else {
            if (matches.size() > 1) {
                LOGGER.log(
                        Level.WARNING,
                        "Found "
                                + matches.size()
                                + " matching pom.properties for module \""
                                + name
                                + "\", using the first one. This may mean you have duplicate jars on your classpath");
            }
            return matches.get(0).getProperty("version");
        }
    }
}
