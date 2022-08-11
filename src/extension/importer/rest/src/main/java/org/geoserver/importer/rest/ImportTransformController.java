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

package org.geoserver.importer.rest;

import freemarker.template.ObjectWrapper;
import freemarker.template.Template;
import java.io.IOException;
import java.lang.reflect.Type;
import org.apache.commons.lang3.NotImplementedException;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.transform.ImportTransform;
import org.geoserver.importer.transform.TransformChain;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.FreemarkerHTMLMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@ControllerAdvice
@RequestMapping(
    path = RestBaseController.ROOT_PATH + "/imports/{importId}",
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE}
)
public class ImportTransformController extends ImportBaseController {

    @Autowired
    protected ImportTransformController(Importer importer) {
        super(importer);
    }

    @PostMapping(path = {"/tasks/{taskId}/transforms"})
    public ResponseEntity postTransform(
            @PathVariable Long importId,
            @PathVariable Integer taskId,
            @RequestBody ImportTransform tx,
            UriComponentsBuilder builder)
            throws IOException {

        ImportTask task = task(importId, taskId);
        @SuppressWarnings("unchecked")
        TransformChain<ImportTransform> transforms = (TransformChain) task.getTransform();
        transforms.add(tx);
        importer.changed(task);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(
                builder.path("/imports/{importId}/tasks/{taskId}/transforms/{transformId}")
                        .buildAndExpand(
                                importId.toString(),
                                taskId.toString(),
                                transforms.getTransforms().size() - 1)
                        .toUri());
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>("", headers, HttpStatus.CREATED);
    }

    @GetMapping(path = {"/tasks/{taskId}/transforms", "/tasks/{taskId}/transforms/{transformId}"})
    public ImportWrapper getTransform(
            @PathVariable Long importId,
            @PathVariable Integer taskId,
            @PathVariable(required = false) Integer transformId,
            @RequestParam(value = "expand", required = false) String expand) {

        return (writer, builder, converter) -> {
            ImportTransform tx = transform(importId, taskId, transformId, true);
            if (tx == null) {
                converter.transformChain(
                        builder, task(importId, taskId), true, converter.expand(expand, 1));
            } else {
                ImportTask task = task(importId, taskId);
                int index = task.getTransform().getTransforms().indexOf(tx);

                converter.transform(builder, tx, index, task, true, converter.expand(expand, 1));
            }
        };
    }

    @PutMapping(
        path = {"/tasks/{taskId}/transforms/{transformId}"},
        consumes = {MediaType.APPLICATION_JSON_VALUE, MediaTypeExtensions.TEXT_JSON_VALUE}
    )
    public ImportWrapper putTransform(
            @PathVariable Long importId,
            @PathVariable Integer taskId,
            @PathVariable Integer transformId,
            @RequestParam(value = "expand", required = false) String expand,
            @RequestBody ImportTransform importTransform)
            throws IOException {

        ImportTask task = task(importId, taskId);
        ImportTransform orig = transform(task, transformId, false);
        @SuppressWarnings("unchecked")
        Class<ImportTransform> txc = (Class) orig.getClass();
        OwsUtils.copy(importTransform, orig, txc);
        importer.changed(task);

        return (writer, builder, converter) -> {
            int index = task.getTransform().getTransforms().indexOf(orig);

            converter.transform(builder, orig, index, task, true, converter.expand(expand, 1));
        };
    }

    @DeleteMapping(path = {"/tasks/{taskId}/transforms/{transformId}"})
    public ResponseEntity deleteTransform(
            @PathVariable Long importId,
            @PathVariable Integer taskId,
            @PathVariable Integer transformId)
            throws IOException {

        ImportTask task = task(importId, taskId);
        ImportTransform tx = transform(task, transformId, true);
        @SuppressWarnings("unchecked")
        TransformChain<ImportTransform> transforms = (TransformChain) task.getTransform();
        boolean result = transforms.remove(tx);

        if (result) {
            importer.changed(task);
            return new ResponseEntity<>("", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("", HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return ImportTransform.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    protected String getTemplateName(Object object) {
        throw new NotImplementedException(
                "ImportTransformController::getTemplateName() is not implemented");
    }

    @Override
    protected <T> ObjectWrapper createObjectWrapper(Class<T> clazz) {
        throw new NotImplementedException(
                "ImportTransformController::createObjectWrapper() is not implemented");
    }

    @Override
    public void configureFreemarker(FreemarkerHTMLMessageConverter converter, Template template) {
        throw new NotImplementedException(
                "ImportTransformController::configueFreemarker() is not implemented");
    }
}
