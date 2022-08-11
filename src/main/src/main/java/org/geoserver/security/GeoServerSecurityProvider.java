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

package org.geoserver.security;

import com.thoughtworks.xstream.converters.SingleValueConverter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.security.validation.SecurityConfigValidator;

/**
 * Extension point for backend authentication and authorization services.
 *
 * <p>Subclasses of this class should be registered in the spring context.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class GeoServerSecurityProvider {

    /**
     * Find the provider for a service type and a concrete class name. May return <code>null</code>
     */
    public static GeoServerSecurityProvider getProvider(Class<?> serviceClass, String className) {

        for (GeoServerSecurityProvider prov :
                GeoServerExtensions.extensions(GeoServerSecurityProvider.class)) {

            if (GeoServerAuthenticationProvider.class == serviceClass
                    && prov.getAuthenticationProviderClass() != null) {
                if (prov.getAuthenticationProviderClass().getName().equals(className)) return prov;
            }
            if (GeoServerUserGroupService.class == serviceClass
                    && prov.getUserGroupServiceClass() != null) {
                if (prov.getUserGroupServiceClass().getName().equals(className)) return prov;
            }
            if (GeoServerRoleService.class == serviceClass && prov.getRoleServiceClass() != null) {
                if (prov.getRoleServiceClass().getName().equals(className)) return prov;
            }
            if (PasswordValidator.class == serviceClass
                    && prov.getPasswordValidatorClass() != null) {
                if (prov.getPasswordValidatorClass().getName().equals(className)) return prov;
            }
            if (GeoServerSecurityFilter.class == serviceClass && prov.getFilterClass() != null) {
                if (prov.getFilterClass().getName().equals(className)) return prov;
            }
            if (MasterPasswordProvider.class == serviceClass
                    && prov.getMasterPasswordProviderClass() != null) {
                if (prov.getMasterPasswordProviderClass().getName().equals(className)) {
                    return prov;
                }
            }
        }
        return null;
    }

    /**
     * An implementation of {@link SingleValueConverter} for encryption and decryption of
     * configuration passwords.
     *
     * <p>Register the fields in {@link #configure(XStreamPersister)} <code>
     * xp.getXStream().registerLocalConverter(class, fieldName, encrypter);
     * </code> TODO: remove the GeoServerExtensions looksups in this class
     */
    public SingleValueConverter encrypter =
            new SingleValueConverter() {

                @Override
                public boolean canConvert(Class type) {
                    return type.equals(String.class);
                }

                @Override
                public String toString(Object obj) {
                    String source = obj == null ? "" : (String) obj;
                    GeoServerSecurityManager manager =
                            GeoServerExtensions.bean(GeoServerSecurityManager.class);
                    return manager.getConfigPasswordEncryptionHelper().encode(source);
                };

                @Override
                public Object fromString(String str) {
                    GeoServerSecurityManager manager =
                            GeoServerExtensions.bean(GeoServerSecurityManager.class);
                    return manager.getConfigPasswordEncryptionHelper().decode(str);
                }
            };

    /**
     * Flag determining if this provider is available.
     *
     * <p>This default implementation returns <code>true</code>, subclasses should override in cases
     * where a meaningful check can be made... for instance checking for a jdbc driver, etc...
     */
    public boolean isAvailable() {
        return true;
    }

    /** Configures the xstream instance used to serialize/deserialize provider configuration. */
    public void configure(XStreamPersister xp) {

        // register converter for fields to be encrypted
        for (Entry<Class<?>, Set<String>> entry : getFieldsForEncryption().entrySet()) {
            for (String fieldName : entry.getValue()) {
                xp.getXStream().registerLocalConverter(entry.getKey(), fieldName, encrypter);
            }
        }
    }

    /**
     * Returns the concrete class of authentication provider created by {@link
     * #createAuthenticationProvider(SecurityNamedServiceConfig)}.
     *
     * <p>If the extension does not provide an authentication provider this method should simply
     * return <code>null</code>. TODO: change this interface to GeoServerAuthenticationProvider
     */
    public Class<? extends GeoServerAuthenticationProvider> getAuthenticationProviderClass() {
        return null;
    }

    /**
     * Creates an authentication provider.
     *
     * <p>If the extension does not provide an authentication provider this method should simply
     * return <code>null</code>.
     */
    public GeoServerAuthenticationProvider createAuthenticationProvider(
            SecurityNamedServiceConfig config) {
        return null;
    }

    /**
     * Returns the concrete class of security filter created by {@link
     * #createFilter(SecurityNamedServiceConfig)}.
     *
     * <p>If the extension does not provide an filter this method should simply return <code>null
     * </code>.
     */
    public Class<? extends GeoServerSecurityFilter> getFilterClass() {
        return null;
    }

    /**
     * Creates a security filter.
     *
     * <p>If the extension does not provide an filter this method should simply return <code>null
     * </code>.
     */
    public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
        return null;
    }

    /**
     * Returns the specific class of the user group service created by {@link
     * #createUserGroupService(SecurityNamedServiceConfig)}.
     *
     * <p>If the extension does not provide a user group service this method should simply return
     * <code>null</code>.
     */
    public Class<? extends GeoServerUserGroupService> getUserGroupServiceClass() {
        return null;
    }

    /**
     * Creates a new user group service.
     *
     * <p>If the extension does not provide a user group service this method should simply return
     * <code>null</code>.
     */
    public GeoServerUserGroupService createUserGroupService(SecurityNamedServiceConfig config)
            throws IOException {
        return null;
    }

    /**
     * Returns the specific class of the role service created by {@link
     * #createRoleService(SecurityNamedServiceConfig)}
     *
     * <p>If the extension does not provide a role service this method should simply return <code>
     * null</code>.
     */
    public Class<? extends GeoServerRoleService> getRoleServiceClass() {
        return null;
    }

    /**
     * Creates a new role group service.
     *
     * <p>If the extension does not provide a role service this method should simply return <code>
     * null</code>.
     */
    public GeoServerRoleService createRoleService(SecurityNamedServiceConfig config)
            throws IOException {
        return null;
    }

    /**
     * Returns the specific class of the master password provider created by {@link
     * #createMasterPasswordProvider(MasterPasswordProviderConfig)}
     *
     * <p>If the extension does not provide a master password provider e this method should simply
     * return <code>null</code>.
     */
    public Class<? extends MasterPasswordProvider> getMasterPasswordProviderClass() {
        return null;
    }

    /**
     * Creates a new role group service.
     *
     * <p>If the extension does not provide a master password provider this method should simply
     * return <code>null</code>.
     */
    public MasterPasswordProvider createMasterPasswordProvider(MasterPasswordProviderConfig config)
            throws IOException {
        return null;
    }

    /**
     * Returns the specific class of the password validator created by {@link
     * #createPasswordValidator(PasswordPolicyConfig)}.
     *
     * <p>If the extension does not provide a validator this method should simply return <code>null
     * </code>.
     */
    public Class<? extends PasswordValidator> getPasswordValidatorClass() {
        return null;
    }

    /** Create the standard password validator or return <code>null</code> */
    public PasswordValidator createPasswordValidator(
            PasswordPolicyConfig config, GeoServerSecurityManager securityManager) {
        return null;
    }

    /**
     * Returns a map containing the field names which should be encrypted. (backend store passwords
     * as an example)
     */
    public Map<Class<?>, Set<String>> getFieldsForEncryption() {
        return Collections.emptyMap();
    }

    /** Return true if the {@link GeoServerRoleService} implementation is not thread safe. */
    public boolean roleServiceNeedsLockProtection() {
        return false;
    }

    /** Return true if the {@link GeoServerUserGroupService} implementation is not thread safe. */
    public boolean userGroupServiceNeedsLockProtection() {
        return false;
    }

    /** Return a configuration validator, subclass of {@link SecurityConfigValidator} */
    public SecurityConfigValidator createConfigurationValidator(
            GeoServerSecurityManager securityManager) {
        return new SecurityConfigValidator(securityManager);
    }

    /** Configures the security filter chain. */
    public void configureFilterChain(GeoServerSecurityFilterChain filterChain) {}

    /**
     * Startup hook - this will be executed after loading the security configuration, allowing
     * plugins to apply custom modifications to the security settings.
     */
    public void init(GeoServerSecurityManager manager) {}

    /** Shutdown hook - this will be executed before unloading the security configuration. */
    public void destroy(GeoServerSecurityManager manager) {}
}
