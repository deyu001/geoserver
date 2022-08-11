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

package org.geoserver.security.password;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.DataAccessFactory.Param;
import org.geotools.util.logging.Logging;

/**
 * Helper class for encryption of passwords in connection parameters for {@link StoreInfo} objects.
 *
 * <p>This class will encrypt any password parameter from {@link
 * StoreInfo#getConnectionParameters()}.
 *
 * @author christian
 */
public class ConfigurationPasswordEncryptionHelper {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    /** cache of datastore factory class to fields to encrypt */
    protected static ConcurrentMap<Class<? extends DataAccessFactory>, Set<String>> CACHE =
            new ConcurrentHashMap<>();
    /**
     * cache of {@link StoreInfo#getType()} to fields to encrypt, if key not found defer to full
     * DataAccessFactory lookup
     */
    protected static ConcurrentMap<String, Set<String>> STORE_INFO_TYPE_CACHE =
            new ConcurrentHashMap<>();

    GeoServerSecurityManager securityManager;

    public ConfigurationPasswordEncryptionHelper(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public Catalog getCatalog() {
        // JD: this class gets called during catalog initialization when reading store instances
        // that
        // potentially have encrypted parameters, so we have to be careful about how we access the
        // catalog, raw catalog directly to avoid triggering the initialization of the secure
        // catalog as we are reading the raw catalog contents (this could for instance cause a rule
        // to be ignored since a workspace has not been read)
        return (Catalog) GeoServerExtensions.bean("rawCatalog");
    }

    /**
     * Determines the fields in {@link StoreInfo#getConnectionParameters()} that require encryption
     * for this type of store object.
     */
    public Set<String> getEncryptedFields(StoreInfo info) {
        if (!(info instanceof DataStoreInfo)) {
            // only datastores supposed at this time, TODO: fix this

            List<EncryptedFieldsProvider> encryptedFieldsProviders =
                    GeoServerExtensions.extensions(EncryptedFieldsProvider.class);
            if (!encryptedFieldsProviders.isEmpty()) {
                Set<String> fields = new HashSet<>();
                for (EncryptedFieldsProvider provider : encryptedFieldsProviders) {
                    Set<String> providedFields = provider.getEncryptedFields(info);
                    if (providedFields != null && !providedFields.isEmpty()) {
                        fields.addAll(providedFields);
                    }
                }
                if (!fields.isEmpty()) {
                    return fields;
                }
            }

            return Collections.emptySet();
        }

        Set<String> toEncrypt;

        // fast lookup by store type
        final String storeType = info.getType();
        if (storeType != null) {
            toEncrypt = STORE_INFO_TYPE_CACHE.get(storeType);
            if (toEncrypt != null) {
                return toEncrypt;
            }
        }

        // store type not cached, find this store object data access factory
        DataAccessFactory factory;
        try {
            factory = getCatalog().getResourcePool().getDataStoreFactory((DataStoreInfo) info);
        } catch (IOException e) {
            LOGGER.log(
                    Level.WARNING,
                    "Error looking up factory for store : "
                            + info
                            + ". Unable to "
                            + "encrypt connection parameters.",
                    e);
            return Collections.emptySet();
        }

        if (factory == null) {
            LOGGER.warning(
                    "Could not find factory for store : "
                            + info
                            + ". Unable to encrypt "
                            + "connection parameters.");
            return Collections.emptySet();
        }

        // if factory returns no info no need to continue
        if (factory.getParametersInfo() == null) {
            return Collections.emptySet();
        }

        toEncrypt = CACHE.get(factory.getClass());
        if (toEncrypt != null) {
            return toEncrypt;
        }

        toEncrypt = CACHE.get(info.getClass());
        if (toEncrypt != null) {
            return toEncrypt;
        }

        toEncrypt = Collections.emptySet();
        if (info != null && info.getConnectionParameters() != null) {
            toEncrypt = new HashSet<>(3);
            for (Param p : factory.getParametersInfo()) {
                if (p.isPassword()) {
                    toEncrypt.add(p.getName());
                }
            }
        }
        CACHE.put(factory.getClass(), toEncrypt);
        if (storeType != null) {
            STORE_INFO_TYPE_CACHE.put(storeType, toEncrypt);
        }
        return toEncrypt;
    }

    /**
     * Encrypts a parameter value.
     *
     * <p>If no encoder is configured then the value is returned as is.
     */
    public String encode(String value) {
        String encoderName = securityManager.getSecurityConfig().getConfigPasswordEncrypterName();
        if (encoderName != null) {
            GeoServerPasswordEncoder pwEncoder = securityManager.loadPasswordEncoder(encoderName);
            if (pwEncoder != null) {
                String prefix = pwEncoder.getPrefix();
                if (value.startsWith(prefix + GeoServerPasswordEncoder.PREFIX_DELIMTER)) {
                    throw new RuntimeException(
                            "Cannot encode a password with prefix: "
                                    + prefix
                                    + GeoServerPasswordEncoder.PREFIX_DELIMTER);
                }
                value = pwEncoder.encodePassword(value, null);
            }
        } else {
            LOGGER.warning("Encryption disabled, no password encoder set");
        }
        return value;
    }

    /** Decrypts previously encrypted store connection parameters. */
    public void decode(StoreInfo info) {
        List<GeoServerPasswordEncoder> encoders =
                securityManager.loadPasswordEncoders(null, true, null);

        Set<String> encryptedFields = getEncryptedFields(info);
        if (info.getConnectionParameters() != null) {
            for (String key : info.getConnectionParameters().keySet()) {
                if (encryptedFields.contains(key)) {
                    String value = (String) info.getConnectionParameters().get(key);
                    if (value != null) {
                        info.getConnectionParameters().put(key, decode(value, encoders));
                    }
                }
            }
        }
    }

    /** Decrypts a previously encrypted value. */
    public String decode(String value) {
        return decode(value, securityManager.loadPasswordEncoders(null, true, null));
    }

    String decode(String value, List<GeoServerPasswordEncoder> encoders) {
        for (GeoServerPasswordEncoder encoder : encoders) {
            if (encoder.isReversible() == false) continue; // should not happen
            if (encoder.isResponsibleForEncoding(value)) {
                return encoder.decode(value);
            }
        }
        return value;
    }
}
