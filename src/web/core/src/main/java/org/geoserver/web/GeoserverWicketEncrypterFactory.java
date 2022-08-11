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

/**
 * Password Encoder for encrypting url params
 *
 * @author christian
 */
package org.geoserver.web;

import java.security.GeneralSecurityException;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.servlet.http.HttpSession;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.crypt.AbstractCrypt;
import org.apache.wicket.util.crypt.ICrypt;
import org.apache.wicket.util.crypt.ICryptFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.util.logging.Logging;
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor;

/**
 * Encryptor factory for apache wicket
 *
 * @author christian
 */
public class GeoserverWicketEncrypterFactory implements ICryptFactory {

    static ICryptFactory Factory;
    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");
    static final String ICRYPT_ATTR_NAME = "__ICRYPT";

    ICrypt NoCrypt =
            new ICrypt() {

                @Override
                public String decryptUrlSafe(String text) {
                    return text;
                }

                @Override
                public String encryptUrlSafe(String plainText) {
                    return plainText;
                }

                @Override
                public void setKey(String key) {}
            };

    class CryptImpl extends AbstractCrypt {
        protected StandardPBEByteEncryptor enc;

        CryptImpl(StandardPBEByteEncryptor enc) {
            this.enc = enc;
        }

        @Override
        protected byte[] crypt(byte[] input, int mode) throws GeneralSecurityException {
            if (mode == Cipher.ENCRYPT_MODE) {
                return enc.encrypt(input);
            } else {
                return enc.decrypt(input);
            }
        }
    };

    /**
     * Look up in the Spring Context for an implementation of {@link ICryptFactory} if nothing found
     * use this default.
     */
    public static ICryptFactory get() {
        if (Factory != null) return Factory;
        Factory = GeoServerExtensions.bean(ICryptFactory.class);
        if (Factory == null) Factory = new GeoserverWicketEncrypterFactory();
        return Factory;
    }

    protected GeoserverWicketEncrypterFactory() {}

    @Override
    public ICrypt newCrypt() {
        RequestCycle cycle = RequestCycle.get();
        ServletWebRequest req = (ServletWebRequest) cycle.getRequest();
        HttpSession s = req.getContainerRequest().getSession(false);
        if (s != null) {
            return getEncrypterFromSession(s);
        } else {
            LOGGER.warning("No session availabe to get url parameter encrypter");
            return NoCrypt;
        }
    }

    protected ICrypt getEncrypterFromSession(HttpSession s) {
        ICrypt result = (ICrypt) s.getAttribute(ICRYPT_ATTR_NAME);
        if (result != null) return result;

        GeoServerSecurityManager manager = GeoServerApplication.get().getSecurityManager();
        char[] key = manager.getRandomPassworddProvider().getRandomPasswordWithDefaultLength();

        StandardPBEByteEncryptor enc = new StandardPBEByteEncryptor();
        enc.setPasswordCharArray(key);
        // since the password is copied, we can scramble it
        manager.disposePassword(key);

        if (manager.isStrongEncryptionAvailable()) {
            enc.setProvider(new BouncyCastleProvider());
            enc.setAlgorithm("PBEWITHSHA256AND128BITAES-CBC-BC");
        } else // US export restrictions
        enc.setAlgorithm("PBEWITHMD5ANDDES");

        result = new CryptImpl(enc);
        s.setAttribute(ICRYPT_ATTR_NAME, result);
        return result;
    }
}
