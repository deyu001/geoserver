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

package org.geoserver.gwc.web.layer;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geowebcache.filter.parameters.CaseNormalizer;
import org.geowebcache.filter.parameters.CaseNormalizingParameterFilter;
import org.geowebcache.filter.parameters.ParameterFilter;

/**
 * Subform for a ParameterFilter
 *
 * @author Kevin Smith, OpenGeo
 */
public abstract class AbstractParameterFilterSubform<T extends ParameterFilter>
        extends FormComponentPanel<T> {

    private static final long serialVersionUID = -213688039804104263L;

    protected Component normalize;

    public AbstractParameterFilterSubform(String id, IModel<T> model) {
        super(id, model);
    }

    @Override
    public void convertInput() {
        visitChildren(
                (component, visit) -> {
                    if (component instanceof FormComponent) {
                        FormComponent<?> formComponent = (FormComponent<?>) component;
                        formComponent.processInput();
                    }
                });
        T filter = getModelObject();
        setConvertedInput(filter);
    }

    /**
     * Adds the {@link CaseNormalizerSubform} component.
     *
     * @implNote Note we're calling {@code filter.setNormalize(filter.getNormalize());} to make the
     *     {@link CaseNormalizer} instance variable explicitly set, because otherwise a new instance
     *     is returned upon each invocation of {@link
     *     CaseNormalizingParameterFilter#getNormalize()}. This is a workaround for a side effect of
     *     an implementation detail in CaseNormalizingParameterFilter (super class of
     *     StringParameterFilter and RegExParameterFilter) that makes this form's updated value not
     *     to be set if a CaseNormalizer wasn't explicitly set before. Rationale being that before
     *     GWC configuration objects properly implemented equals() and hashCode(),
     *     CaseNormalizer.equals() always returned false, and hence
     *     org.apache.wicket.Component.setDefaultModelObject() (from new
     *     PropertyModel<CaseNormalizer>(model, "normalize") bellow) will always update the model.
     *     With equals and hashCode properly implemented though, Component.setDefaultModelObject()
     *     will not enter the {@code if (!getModelComparator().compare(this, object))} condition and
     *     hence won't reach {@code model.setObject(object);}, since it will be comparing two
     *     equivalent instances, product of CaseNormalizingParameterFilter.getNormalize() returning
     *     a new CaseNormalizer instance when a value is not explicitly set.
     *     <p>This workaround is side effect free since the net result of Filter having an explicit
     *     value for its normalize instance variable is guaranteed anyways by the way the form
     *     works.
     *     <p>In the long run what should happen is that StringParameterFilter and CaseNormalizer
     *     behave like simple POJOs instead of being clever about returning a new default value on
     *     each accessor invocation.
     *     <p>This same workaround is applied to RegExParameterFilterSubform
     */
    protected void addNormalize(IModel<? extends CaseNormalizingParameterFilter> model) {
        CaseNormalizingParameterFilter filter = model.getObject();
        filter.setNormalize(filter.getNormalize());
        normalize = new CaseNormalizerSubform("normalize", new PropertyModel<>(model, "normalize"));
        add(normalize);
    }
}
