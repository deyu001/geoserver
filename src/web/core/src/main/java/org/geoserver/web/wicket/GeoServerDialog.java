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

package org.geoserver.web.wicket;

import java.io.Serializable;
import java.util.Arrays;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * An abstract OK/cancel dialog, subclasses will have to provide the actual contents and behavior
 * for OK/cancel
 */
@SuppressWarnings("serial")
public class GeoServerDialog extends Panel {

    ModalWindow window;
    Component userPanel;
    DialogDelegate delegate;

    public GeoServerDialog(String id) {
        super(id);
        add(window = new ModalWindow("dialog"));
    }

    /** Sets the window title */
    public void setTitle(IModel<String> title) {
        window.setTitle(title);
    }

    public String getHeightUnit() {
        return window.getHeightUnit();
    }

    public int getInitialHeight() {
        return window.getInitialHeight();
    }

    public int getInitialWidth() {
        return window.getInitialWidth();
    }

    public String getWidthUnit() {
        return window.getWidthUnit();
    }

    public void setHeightUnit(String heightUnit) {
        window.setHeightUnit(heightUnit);
    }

    public void setInitialHeight(int initialHeight) {
        window.setInitialHeight(initialHeight);
    }

    public void setInitialWidth(int initialWidth) {
        window.setInitialWidth(initialWidth);
    }

    public void setWidthUnit(String widthUnit) {
        window.setWidthUnit(widthUnit);
    }

    public int getMinimalHeight() {
        return window.getMinimalHeight();
    }

    public int getMinimalWidth() {
        return window.getMinimalWidth();
    }

    public void setMinimalHeight(int minimalHeight) {
        window.setMinimalHeight(minimalHeight);
    }

    public void setMinimalWidth(int minimalWidth) {
        window.setMinimalWidth(minimalWidth);
    }

    /**
     * Shows an OK/cancel dialog. The delegate will provide contents and behavior for the OK button
     * (and if needed, for the cancel one as well)
     */
    public void showOkCancel(AjaxRequestTarget target, final DialogDelegate delegate) {
        // wire up the contents
        userPanel = delegate.getContents("userPanel");
        window.setContent(new ContentsPage(userPanel));

        // make sure close == cancel behavior wise
        window.setCloseButtonCallback(
                new ModalWindow.CloseButtonCallback() {

                    public boolean onCloseButtonClicked(AjaxRequestTarget target) {
                        return delegate.onCancel(target);
                    }
                });
        window.setWindowClosedCallback(
                new ModalWindow.WindowClosedCallback() {

                    public void onClose(AjaxRequestTarget target) {
                        delegate.onClose(target);
                    }
                });

        // show the window
        this.delegate = delegate;
        window.show(target);
    }

    /**
     * Shows an information style dialog box.
     *
     * @param heading The heading of the information topic.
     * @param messages A list of models, displayed each as a separate paragraphs, containing the
     *     information dialog content.
     */
    @SafeVarargs
    public final void showInfo(
            AjaxRequestTarget target,
            final IModel<String> heading,
            final IModel<String>... messages) {
        window.setPageCreator(
                new ModalWindow.PageCreator() {
                    public Page createPage() {
                        return new InfoPage(heading, messages);
                    }
                });
        window.show(target);
    }

    /**
     * Forcibly closes the dialog.
     *
     * <p>Note that calling this method does not result in any {@link DialogDelegate} callbacks
     * being called.
     */
    public void close(AjaxRequestTarget target) {
        window.close(target);
        delegate = null;
        userPanel = null;
    }

    /** Submits the dialog. */
    public void submit(AjaxRequestTarget target) {
        submit(target, userPanel);
    }

    void submit(AjaxRequestTarget target, Component contents) {
        if (delegate.onSubmit(target, contents)) {
            close(target);
        }
    }

    /** Submit link that will forward to the {@link DialogDelegate} */
    AjaxSubmitLink sumbitLink(Component contents) {
        AjaxSubmitLink link =
                new AjaxSubmitLink("submit") {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        submit(target, (Component) this.getDefaultModelObject());
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        delegate.onError(target, form);
                    }
                };
        link.setDefaultModel(new Model<>(contents));
        return link;
    }

    /** Link that will forward to the {@link DialogDelegate} */
    Component cancelLink() {
        return new AjaxLink<Void>("cancel") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (delegate.onCancel(target)) {
                    window.close(target);
                    delegate = null;
                }
            }
        };
    }

    /**
     * This represents the contents of the dialog.
     *
     * <p>As of wicket 1.3.6 it still has to be a page, see
     * http://www.nabble.com/Nesting-ModalWindow-td19925848.html for details (ajax submit buttons
     * won't work with a panel)
     */
    protected class ContentsPage extends Panel {

        public ContentsPage(Component contents) {
            super("content");
            Form<?> form = new Form<>("form");
            add(form);
            form.add(contents);
            AjaxSubmitLink submit = sumbitLink(contents);
            form.add(submit);
            form.add(cancelLink());
            form.setDefaultButton(submit);
        }
    }

    protected class InfoPage extends WebPage {
        @SafeVarargs
        public InfoPage(IModel<String> title, IModel<String>... messages) {
            add(new Label("title", title));
            add(
                    new ListView<IModel<String>>("messages", Arrays.asList(messages)) {
                        @Override
                        protected void populateItem(ListItem<IModel<String>> item) {
                            item.add(
                                    new Label("message", item.getModelObject())
                                            .setEscapeModelStrings(false));
                        }
                    });
        }
    }

    /**
     * A {@link DialogDelegate} provides the bits needed to actually open a dialog:
     *
     * <ul>
     *   <li>a content pane, that will be hosted inside a {@link Form}
     *   <li>a behavior for the OK button
     *   <li>an eventual behavior for the Cancel button (the base implementation just returns true
     *       to make the window close)
     */
    public abstract static class DialogDelegate implements Serializable {

        /** Builds the contents for this dialog */
        protected abstract Component getContents(String id);

        /**
         * Called when the form inside the dialog breaks. By default adds all feedback panels to the
         * target
         */
        public void onError(final AjaxRequestTarget target, Form<?> form) {
            form.getPage()
                    .visitChildren(
                            IFeedback.class,
                            (component, visit) -> {
                                if (component.getOutputMarkupId()) {
                                    target.add(component);
                                }
                            });
        }

        /**
         * Called when the dialog is closed, allows the delegate to perform ajax updates on the page
         * underlying the dialog
         */
        public void onClose(AjaxRequestTarget target) {
            // by default do nothing
        }

        /**
         * Called when the dialog is submitted
         *
         * @return true if the dialog is to be closed, false otherwise
         */
        protected abstract boolean onSubmit(AjaxRequestTarget target, Component contents);

        /**
         * Called when the dialog is cancelled.
         *
         * @return true if the dialog is to be closed, false otherwise
         */
        protected boolean onCancel(AjaxRequestTarget target) {
            return true;
        }
    }

    public void setResizable(boolean resizable) {
        window.setResizable(resizable);
    }
}
