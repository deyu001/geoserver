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

package org.geoserver.security.rememberme;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.security.GeoServerSecurityManager;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;

/**
 * Factory bean that proxies for the global remember me service.
 *
 * <p>The actual underlying rememberme service is determined by {@link
 * RememberMeServicesConfig#getClassName()}, obtained from {@link
 * GeoServerSecurityManager#getSecurityConfig()}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class RememberMeServicesFactoryBean implements FactoryBean<RememberMeServices> {

    static final String PARAMETER_NAME = "_spring_security_remember_me";
    GeoServerSecurityManager securityManager;

    public RememberMeServicesFactoryBean(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public RememberMeServices getObject() throws Exception {
        // we return a proxy for the rms, one that instantiates the underlying rms lazily on demand
        // we do this to avoid trigging the security manager configuration from being loaded
        // during startup, before the app context is fully loaded
        return new RememberMeServicesProxy(securityManager);
    }

    @Override
    public Class<?> getObjectType() {
        return RememberMeServices.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    static class RememberMeServicesProxy implements RememberMeServices, LogoutHandler {

        GeoServerSecurityManager securityManager;
        RememberMeServices rms;

        RememberMeServicesProxy(GeoServerSecurityManager securityManager) {
            this.securityManager = securityManager;
        }

        @Override
        public Authentication autoLogin(HttpServletRequest request, HttpServletResponse response) {
            return rms().autoLogin(request, response);
        }

        @Override
        public void loginFail(HttpServletRequest request, HttpServletResponse response) {
            rms().loginFail(request, response);
        }

        @Override
        public void loginSuccess(
                HttpServletRequest request,
                HttpServletResponse response,
                Authentication successfulAuthentication) {
            rms().loginSuccess(request, response, successfulAuthentication);
        }

        @Override
        public void logout(
                HttpServletRequest request,
                HttpServletResponse response,
                Authentication authentication) {
            RememberMeServices rms = rms();
            if (rms instanceof LogoutHandler) {
                ((LogoutHandler) rms).logout(request, response, authentication);
            }
        }

        RememberMeServices rms() {
            if (rms != null) {
                return rms;
            }

            RememberMeServicesConfig rmsConfig =
                    securityManager.getSecurityConfig().getRememberMeService();
            try {
                @SuppressWarnings("unchecked")
                Class<RememberMeServices> rmsClass =
                        (Class<RememberMeServices>) Class.forName(rmsConfig.getClassName());
                rms =
                        rmsClass.getConstructor(String.class, UserDetailsService.class)
                                .newInstance(
                                        rmsConfig.getKey(),
                                        new RememberMeUserDetailsService(securityManager));
                if (rms instanceof AbstractRememberMeServices) {
                    ((AbstractRememberMeServices) rms).setParameter(PARAMETER_NAME);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            //            if (rms instanceof GeoServerTokenBasedRememberMeServices) {
            //                ((GeoServerTokenBasedRememberMeServices)
            // rms).setUserGroupServiceName(rmsConfig.getUserGroupService());
            //            }
            return rms;
        }
    }
}
