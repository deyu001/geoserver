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

package org.geoserver.web;

import static org.geoserver.web.GeoServerApplication.GEOSERVER_CSRF_DISABLED;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerSecurityTestSupport;
import org.geoserver.web.wicket.WicketHierarchyPrinter;
import org.junit.After;
import org.junit.BeforeClass;

public abstract class GeoServerWicketTestSupport extends GeoServerSecurityTestSupport {
    public static WicketTester tester;

    @BeforeClass
    public static void disableBrowserDetection() {
        // disable browser detection, makes testing harder for nothing
        GeoServerApplication.DETECT_BROWSER = false;
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // prevent Wicket from bragging about us being in dev mode (and run
        // the tests as if we were in production all the time)
        System.setProperty("wicket.configuration", "deployment");
        // Disable CSRF protection for tests, since the test framework doesn't set the Referer
        System.setProperty(GEOSERVER_CSRF_DISABLED, "true");

        // make sure that we check the english i18n when needed
        Locale.setDefault(Locale.ENGLISH);

        GeoServerApplication app =
                (GeoServerApplication) applicationContext.getBean("webApplication");
        tester = new WicketTester(app, false);
        app.init();
    }

    @After
    public void clearErrorMessages() {
        if (tester != null && !tester.getFeedbackMessages(IFeedbackMessageFilter.ALL).isEmpty()) {
            tester.cleanupFeedbackMessages();
        }
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        super.onTearDown(testData);
        tester.destroy();
    }

    public GeoServerApplication getGeoServerApplication() {
        return GeoServerApplication.get();
    }

    /** Logs in as administrator. */
    public void login() {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    public void logout() {
        login("anonymousUser", "", "ROLE_ANONYMOUS");
    }

    /**
     * Prints the specified component/page containment hierarchy to the standard output
     *
     * <p>Each line in the dump looks like: <componentId>(class) 'value'
     *
     * @param c the component to be printed
     * @param dumpClass if enabled, the component classes are printed as well
     * @param dumpValue if enabled, the component values are printed as well
     */
    public void print(Component c, boolean dumpClass, boolean dumpValue) {
        if (isQuietTests()) {
            return;
        }

        WicketHierarchyPrinter.print(c, dumpClass, dumpValue);
    }

    /**
     * Prints the specified component/page containment hierarchy to the standard output
     *
     * <p>Each line in the dump looks like: <componentId>(class) 'value'
     *
     * @param c the component to be printed
     * @param dumpClass if enabled, the component classes are printed as well
     * @param dumpValue if enabled, the component values are printed as well
     */
    public void print(Component c, boolean dumpClass, boolean dumpValue, boolean dumpPath) {
        if (isQuietTests()) {
            return;
        }

        WicketHierarchyPrinter.print(c, dumpClass, dumpValue);
    }

    /**
     * Finds the component whose model value equals to the specified content, and the component
     * class is equal, subclass or implementor of the specified class
     *
     * @param root the component under which the search is to be performed
     * @param componentClass the target class, or null if any component will do
     */
    public Component findComponentByContent(
            MarkupContainer root, Object content, Class<?> componentClass) {
        ComponentContentFinder finder = new ComponentContentFinder(content);
        root.visitChildren(componentClass, finder);
        return finder.candidate;
    }

    /**
     * Returns the first child component found, that matches the desired target clas
     *
     * @param container The component container
     * @param targetClass The desired component's class
     * @return The first child component matching the target class, or null if not found
     */
    protected String getComponentPath(
            WebMarkupContainer container, Class<? extends Component> targetClass) {
        AtomicReference<String> result = new AtomicReference<>();
        container.visitChildren(
                (component, visit) -> {
                    if (targetClass.isInstance(component)) {
                        result.set(component.getPageRelativePath());
                        visit.stop();
                    }
                });
        return result.get();
    }

    protected String getNthComponentPath(
            WebMarkupContainer container, Class<? extends Component> targetClass, int n) {
        ArrayList<String> results = new ArrayList<>();

        container.visitChildren(
                (component, visit) -> {
                    if (targetClass.isInstance(component)) {
                        results.add(component.getPageRelativePath());
                    }
                });
        return results.get(n);
    }

    class ComponentContentFinder implements IVisitor<Component, Void> {
        Component candidate;
        Object content;

        ComponentContentFinder(Object content) {
            this.content = content;
        }

        @Override
        public void component(Component component, IVisit<Void> visit) {
            if (content.equals(component.getDefaultModelObject())) {
                this.candidate = component;
                visit.stop();
            }
        }
    }

    /**
     * Helper method to initialize a standalone WicketTester with the proper customizations to do
     * message lookups.
     */
    public static void initResourceSettings(WicketTester tester) {
        tester.getApplication()
                .getResourceSettings()
                .setResourceStreamLocator(new GeoServerResourceStreamLocator());
        tester.getApplication()
                .getResourceSettings()
                .getStringResourceLoaders()
                .add(0, new GeoServerStringResourceLoader());
    }

    /**
     * Get Ajax Event Behavior attached to a component.
     *
     * @param path path to component
     * @param event the name of the event
     */
    protected AjaxEventBehavior getAjaxBehavior(String path, String event) {
        for (Behavior b : tester.getComponentFromLastRenderedPage(path).getBehaviors()) {
            if (b instanceof AjaxEventBehavior
                    && ((AjaxEventBehavior) b).getEvent().equals(event)) {
                return (AjaxEventBehavior) b;
            }
        }
        return null;
    }

    /** Execute Ajax Event Behavior with attached value. */
    protected void executeExactAjaxEventBehavior(String path, String event, String value) {
        tester.getRequest().setParameter(path, value);
        tester.getRequest().setMethod("GET");
        tester.executeAjaxEvent(path, event);
    }
    /** Execute Ajax Event Behavior with attached value. */
    protected void executeAjaxEventBehavior(String path, String event, String value) {
        String[] ids = path.split(":");
        String id = ids[ids.length - 1];
        tester.getRequest().setParameter(id, value);
        tester.getRequest().setMethod("GET");
        tester.executeAjaxEvent(path, event);
    }
    /**
     * Sets the value of a form component that might not be included in a form (because maybe we are
     * using it via Ajax). By itself it just prepares the stage for a subsequent Ajax request
     *
     * @param component The {@link FormComponent} whose value we are going to set
     * @param value The form value (as we'd set it in a HTML form)
     */
    protected void setFormComponentValue(FormComponent component, String value) {
        tester.getRequest().getPostParameters().setParameterValue(component.getInputName(), value);
    }
}
