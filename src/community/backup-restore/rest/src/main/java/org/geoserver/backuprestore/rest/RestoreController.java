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

package org.geoserver.backuprestore.rest;

import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.logging.Level;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.RestoreExecutionAdapter;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * REST Restore Controller
 *
 * <pre>/br/restore[/&lt;restoreId&gt;][.zip]</pre>
 *
 * @author "Alessio Fabiani" <alessio.fabiani@geo-solutions.it>, GeoSolutions
 */
@RestController
@ControllerAdvice
@RequestMapping(
    path = RestBaseController.ROOT_PATH + "/br/",
    produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE,
        MediaType.TEXT_HTML_VALUE
    }
)
public class RestoreController extends AbstractBackupRestoreController {

    @Autowired
    public RestoreController(@Qualifier("backupFacade") Backup backupFacade) {
        assert backupFacade != null;
        this.backupFacade = backupFacade;
    }

    @GetMapping(
        path = "restore{.+}",
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE
        }
    )
    public RestWrapper restoreGet(@RequestParam(name = "format", required = false) String format) {

        Object lookup = lookupRestoreExecutionsContext(null, true, false);

        if (lookup != null) {
            if (lookup instanceof RestoreExecutionAdapter) {
                return wrapObject((RestoreExecutionAdapter) lookup, RestoreExecutionAdapter.class);
            } else {
                return wrapList(
                        (List<RestoreExecutionAdapter>) lookup, RestoreExecutionAdapter.class);
            }
        }

        return null;
    }

    @GetMapping(
        path = "restore/{restoreId:.+}",
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.ALL_VALUE
        }
    )
    public RestWrapper restoreGet(
            @RequestParam(name = "format", required = false) String format,
            @PathVariable String restoreId,
            HttpServletResponse response) {

        Object lookup =
                lookupRestoreExecutionsContext(getExecutionIdFilter(restoreId), true, false);

        if (lookup != null) {
            if (lookup instanceof RestoreExecutionAdapter) {
                if (restoreId.endsWith(".zip")) {
                    try {
                        // get your file as InputStream
                        File file = ((RestoreExecutionAdapter) lookup).getArchiveFile().file();
                        InputStream is = new FileInputStream(file);
                        // copy it to response's OutputStream
                        org.apache.commons.io.IOUtils.copy(is, response.getOutputStream());
                        response.flushBuffer();
                    } catch (IOException ex) {
                        LOGGER.log(Level.INFO, "Error writing file to output stream.", ex);
                        throw new RuntimeException("IOError writing file to output stream");
                    }
                } else {
                    return wrapObject(
                            (RestoreExecutionAdapter) lookup, RestoreExecutionAdapter.class);
                }
            } else {
                return wrapList(
                        (List<RestoreExecutionAdapter>) lookup, RestoreExecutionAdapter.class);
            }
        }

        return null;
    }

    @DeleteMapping(
        path = "restore/{restoreId:.+}",
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE
        }
    )
    public RestWrapper restoreDelete(
            @RequestParam(name = "format", required = false) String format,
            @PathVariable String restoreId)
            throws IOException {

        final String executionId = getExecutionIdFilter(restoreId);
        Object lookup = lookupRestoreExecutionsContext(executionId, true, false);

        if (lookup != null) {
            if (lookup instanceof RestoreExecutionAdapter) {
                try {
                    getBackupFacade().abandonExecution(Long.valueOf(executionId));
                } catch (Exception e) {
                    throw new IOException(e);
                }
                return wrapObject((RestoreExecutionAdapter) lookup, RestoreExecutionAdapter.class);
            } else {
                return wrapList(
                        (List<RestoreExecutionAdapter>) lookup, RestoreExecutionAdapter.class);
            }
        }

        return null;
    }

    @PostMapping(
        value = {"/restore"},
        consumes = {
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE
        },
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public RestWrapper restorePost(
            @RequestBody(required = true) RestoreExecutionAdapter restore,
            @RequestHeader("Content-Type") String contentType,
            UriComponentsBuilder builder)
            throws IOException {
        RestoreExecutionAdapter execution = null;

        if (restore.getId() != null) {
            Object lookup =
                    lookupBackupExecutionsContext(String.valueOf(restore.getId()), false, false);
            if (lookup != null) {
                // Restore instance already exists... trying to restart it.
                try {
                    getBackupFacade().restartExecution(restore.getId());

                    LOGGER.log(Level.INFO, "Restore restarted: " + restore.getArchiveFile());

                    return wrapObject(
                            (RestoreExecutionAdapter) lookup, RestoreExecutionAdapter.class);
                } catch (Exception e) {

                    LOGGER.log(
                            Level.WARNING,
                            "Could not restart the restore: " + restore.getArchiveFile());

                    throw new IOException(e);
                }
            }
        } else {
            // Start a new execution asynchronously. You will need to query for the status in order
            // to follow the progress.
            execution =
                    getBackupFacade()
                            .runRestoreAsync(
                                    restore.getArchiveFile(),
                                    restore.getWsFilter(),
                                    restore.getSiFilter(),
                                    restore.getLiFilter(),
                                    asParams(restore.getOptions()));

            LOGGER.log(Level.INFO, "Restore file: " + restore.getArchiveFile());

            return wrapObject((RestoreExecutionAdapter) execution, RestoreExecutionAdapter.class);
        }

        return null;
    }

    /**
     * From {@link RestBaseController}
     *
     * <p>... * Any extending classes which override {@link #configurePersister(XStreamPersister,
     * XStreamMessageConverter)}, and require this configuration for reading objects from incoming
     * requests must also be annotated with {@link
     * org.springframework.web.bind.annotation.ControllerAdvice} and override the {@link
     * #supports(MethodParameter, Type, Class)} method...
     */
    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return RestoreExecutionAdapter.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xstream = persister.getXStream();
        intializeXStreamContext(xstream);
    }
}
