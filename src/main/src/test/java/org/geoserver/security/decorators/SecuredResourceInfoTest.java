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

package org.geoserver.security.decorators;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.security.AccessLimits;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WrapperPolicy;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public abstract class SecuredResourceInfoTest<D extends ResourceInfo, S extends ResourceInfo>
        extends GeoServerSystemTestSupport {

    protected final WrapperPolicy policy =
            WrapperPolicy.readOnlyHide(new AccessLimits(CatalogMode.HIDE));

    /**
     * Creates an instance of a non-secure wrapped ResourceInfo.
     *
     * @return An instance of a non-secure wrapped ResourceInfo implementation.
     */
    abstract D createDelegate();

    /**
     * Retrieves the Class of the non-secure wrapped ResourceInfo type.
     *
     * @return the Class of the non-secure wrapped ResourceInfo type.
     */
    abstract Class<?> getDelegateClass();

    /**
     * Wraps a non-Secured ResourceInfo with an appropriate security {@link Wrapper}.
     *
     * @param delegate An instance of the associated non-secure wrapped ResourceInfo type.
     * @return A secured instance wrapping the supplied non-secure ResourceInfo instance.
     */
    abstract S createSecuredDecorator(D delegate);

    /**
     * Retrieves the Class of the secure wrapped ResourceInfo type.
     *
     * @return the Class of the secure wrapped ResourceInfo type.
     */
    abstract Class<?> getSecuredDecoratorClass();

    /**
     * Retrieves the Class of the secure wrapped StoreInfo type associated with the secure wrapped
     * ResourceInfo type.
     *
     * @return the Class of the secure wrapped StoreInfo type associated with the secure wrapped
     *     ResourceInfo type.
     */
    abstract Class<?> getSecuredStoreInfoClass();

    /**
     * Retrieves the minimum number of times a secure {@link Wrapper} needs to re-wrap an object to
     * cause a {@link java.lang.StackOverflowError} when setting a StoreInfo on a ResourceInfo, or
     * when unwrapping nested {@link Wrapper}s.
     *
     * @return the number of nestings required to cause a {@link java.lang.StackOverflowError}.
     */
    abstract int getStackOverflowCount();

    /**
     * Creates a Thread that will repeatedly set the StoreInfo on the target ResourceInfo with the
     * StoreInfo retrieved from the source ResourceInfo. When the source ResourceInfo is a
     * secure-wrapped instance, this process should not continually nest secure {@link Wrapper}s
     * around the StoreInfo instance. If it does, a {@link java.lang.StackOverflowError} could
     * result.
     *
     * @param source A secure wrapped ResourceInfo instance with a non-null StoreInfo attribute.
     * @param target A secure wrapped ResourceInfo instance in which to store the StoreInfo
     *     retrieved from the source.
     * @return A Thread instance that will repeated call target.setStore(source.getStore());
     */
    @SuppressWarnings("PMD.AvoidThreadGroup")
    private Thread getRoundTripThread(final S source, final S target) {
        // This is just a simple thread that will loop a bunch of times copying the info onto
        // itself.
        // If the info is secured, and the copy causes nested Secure wrappings of the data or its
        // attributes, this will
        // eventually throw a StackOverflowError.
        final Runnable runnable =
                new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < getStackOverflowCount(); ++i) {
                            target.setStore(source.getStore());
                        }
                    }
                };
        // use a very small stack size so the stack overflow happens quickly if it's going to
        // happen.
        // this may not fail on all platforms if set/get is broken however, as some platforms may
        // ignore the stack size in the Thread constructor.
        return new Thread(Thread.currentThread().getThreadGroup(), runnable, "RoundTripThread", 5);
    }

    @Test
    public void testCanSecure() throws Exception {
        // get a delegate
        final D delegate = createDelegate();
        // secure it
        Object secure = SecuredObjects.secure(delegate, policy);
        assertTrue(
                "Unable to secure ResourceInfo",
                getSecuredDecoratorClass().isAssignableFrom(secure.getClass()));
    }

    @Test
    public void testCanSecureProxied() throws Exception {
        // get a delegate
        final D delegate = createDelegate();
        // wrap the delegate in a ModificationProxy
        ResourceInfo proxy = (ResourceInfo) ModificationProxy.create(delegate, getDelegateClass());
        // secure it
        Object secure = SecuredObjects.secure(proxy, policy);
        assertTrue(
                "Unable to secure proxied Resourceinfo",
                getSecuredDecoratorClass().isAssignableFrom(secure.getClass()));
    }

    @Test
    public void testSecureWrapping() throws Exception {
        // get a delegate
        final D delegate = createDelegate();
        // assert the delegate is not secured
        assertFalse(
                "ResourceInfo delegate should not be Secured",
                getSecuredDecoratorClass().isAssignableFrom(delegate.getClass()));
        // create a Secure wrapped instance
        S secured = createSecuredDecorator(delegate);
        assertTrue(
                "ResourceInfo delegate should be Secured",
                getSecuredDecoratorClass().isAssignableFrom(secured.getClass()));
        // get the StoreInfo
        final StoreInfo securedStore = secured.getStore();
        assertTrue(
                "Secured ResourceInfo should return a Secured StoreInfo",
                getSecuredStoreInfoClass().isAssignableFrom(securedStore.getClass()));
        // copy non secured into secured
        Thread roundTripThread = getRoundTripThread(secured, secured);
        // catch Errors
        final StringWriter sw = new StringWriter();
        roundTripThread.setUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        // print the stack to the StringWriter
                        e.printStackTrace(new PrintWriter(sw, true));
                    }
                });
        // start the thread and wait for it to finish
        roundTripThread.start();
        roundTripThread.join();
        // If there was an Error in the thread, the StringWriter will have it
        StringBuffer buffer = sw.getBuffer();
        if (buffer.length() > 0) {
            fail(buffer.toString());
        }
        // just in case, unwrap the StoreInfo and ensure it doesn't throw a StackOverflow
        try {
            SecureCatalogImpl.unwrap(secured.getStore());
        } catch (Throwable t) {
            t.printStackTrace(new PrintWriter(sw, true));
        }
        buffer = sw.getBuffer();
        if (buffer.length() > 0) {
            fail(buffer.toString());
        }
    }
}
