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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.KeywordInfo;

/**
 * Form component to edit a List<String> that makes up the keywords field of various catalog
 * objects.
 */
public class KeywordsEditor extends FormComponentPanel<List<KeywordInfo>> {

    private static final long serialVersionUID = 1L;
    ListMultipleChoice<KeywordInfo> choices;
    TextField<String> newKeyword;
    TextField<String> vocabTextField;
    DropDownChoice<String> langChoice;

    /**
     * Creates a new keywords editor.
     *
     * @param keywords The module should return a non null collection of strings.
     */
    public KeywordsEditor(String id, final IModel<List<KeywordInfo>> keywords) {
        super(id, keywords);

        choices =
                new ListMultipleChoice<>(
                        "keywords",
                        new Model<ArrayList<KeywordInfo>>(),
                        new ArrayList<>(keywords.getObject()),
                        new ChoiceRenderer<KeywordInfo>() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public Object getDisplayValue(KeywordInfo kw) {
                                StringBuffer sb = new StringBuffer(kw.getValue());
                                if (kw.getLanguage() != null) {
                                    sb.append(" (").append(kw.getLanguage()).append(")");
                                }
                                if (kw.getVocabulary() != null) {
                                    sb.append(" [").append(kw.getVocabulary()).append("]");
                                }
                                return sb.toString();
                            }
                        });
        choices.setOutputMarkupId(true);
        add(choices);
        add(removeKeywordsButton());
        newKeyword = new TextField<>("newKeyword", new Model<>());
        newKeyword.setOutputMarkupId(true);
        add(newKeyword);

        langChoice =
                new DropDownChoice<>(
                        "lang",
                        new Model<>(),
                        Arrays.asList(Locale.getISOLanguages()),
                        new ChoiceRenderer<String>() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public String getDisplayValue(String object) {
                                return new Locale(object).getDisplayLanguage();
                            }

                            @Override
                            public String getIdValue(String object, int index) {
                                return object;
                            }
                        });

        langChoice.setNullValid(true);
        langChoice.setOutputMarkupId(true);
        add(langChoice);

        vocabTextField = new TextField<>("vocab", new Model<>());
        vocabTextField.setOutputMarkupId(true);

        add(vocabTextField);

        add(addKeywordsButton());
    }

    private AjaxButton addKeywordsButton() {
        AjaxButton button =
                new AjaxButton("addKeyword") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        String value = newKeyword.getInput();
                        String lang = langChoice.getInput();
                        String vocab = vocabTextField.getInput();

                        KeywordInfo keyword = new Keyword(value);
                        if (lang != null && !"".equals(lang.trim())) {
                            keyword.setLanguage(lang);
                        }
                        if (vocab != null && !"".equals(vocab.trim())) {
                            keyword.setVocabulary(vocab);
                        }

                        @SuppressWarnings("unchecked")
                        List<KeywordInfo> choiceList = (List<KeywordInfo>) choices.getChoices();
                        choiceList.add(keyword);
                        choices.setChoices(choiceList);

                        langChoice.setModelObject(null);
                        langChoice.modelChanged();

                        vocabTextField.setModelObject(null);
                        vocabTextField.modelChanged();

                        target.add(newKeyword);
                        target.add(langChoice);
                        target.add(vocabTextField);
                        target.add(choices);
                    }
                };
        button.setDefaultFormProcessing(false);
        return button;
    }

    private AjaxButton removeKeywordsButton() {
        AjaxButton button =
                new AjaxButton("removeKeywords") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        Collection<KeywordInfo> selection = choices.getModelObject();
                        @SuppressWarnings("unchecked")
                        List<KeywordInfo> keywords = (List<KeywordInfo>) choices.getChoices();
                        for (KeywordInfo selected : selection) {
                            keywords.remove(selected);
                        }
                        choices.setChoices(keywords);
                        choices.modelChanged();
                        target.add(choices);
                    }
                };
        // button.setDefaultFormProcessing(false);
        return button;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        updateFields();
    }

    private void updateFields() {
        choices.setChoices(getModel());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void convertInput() {
        setConvertedInput((List<KeywordInfo>) choices.getChoices());
    }
}
