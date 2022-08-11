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

package org.geoserver.metadata.data.service.impl;

import com.thoughtworks.xstream.io.StreamException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.metadata.data.model.impl.ComplexMetadataMapImpl;
import org.geoserver.metadata.data.model.impl.MetadataTemplateImpl;
import org.geoserver.metadata.data.service.ComplexMetadataService;
import org.geoserver.metadata.data.service.CustomNativeMappingService;
import org.geoserver.metadata.data.service.GlobalModelService;
import org.geoserver.metadata.data.service.MetadataTemplateService;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceListener;
import org.geoserver.platform.resource.ResourceNotification;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Service that manages the list of templates. When the config of a template is updated all linked
 * metadata is also updated.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 * @author Niels Charlier
 */
@Component
public class MetadataTemplateServiceImpl implements MetadataTemplateService, ResourceListener {

    private static final Logger LOGGER = Logging.getLogger(MetadataTemplateServiceImpl.class);

    private XStreamPersister persister;

    private static String LIST_FILE = "templates.xml";

    @Autowired private GeoServerDataDirectory dataDirectory;

    @Autowired private ComplexMetadataService metadataService;

    @Autowired private CustomNativeMappingService nativeToCustomService;

    @Autowired private GlobalModelService globalModelService;

    @Autowired private Catalog rawCatalog;

    private List<MetadataTemplate> templates = new ArrayList<>();

    public MetadataTemplateServiceImpl() {
        this.persister = new XStreamPersisterFactory().createXMLPersister();
        this.persister.getXStream().processAnnotations(MetadataTemplateImpl.class);
        this.persister
                .getXStream()
                .allowTypesByWildcard(new String[] {"org.geoserver.metadata.data.model.**"});
    }

    private Resource getFolder() {
        return dataDirectory.get(MetadataConstants.TEMPLATES_DIRECTORY);
    }

    @PostConstruct
    public void init() {
        reload();
        getFolder().addListener(this);
    }

    @Override
    public void changed(ResourceNotification notify) {
        reload();
    }

    @SuppressWarnings("unchecked")
    public void reload() {
        Resource folder = getFolder();

        synchronized (templates) {
            templates.clear();

            Resource listFile = folder.get(LIST_FILE);

            if (Resources.exists(listFile)) {
                try (InputStream inPriorities = listFile.in()) {
                    List<String> priorities = persister.load(inPriorities, List.class);

                    for (String id : priorities) {
                        Resource templateFile = folder.get(id + ".xml");
                        try (InputStream inTemplate = templateFile.in()) {
                            MetadataTemplate template =
                                    persister.load(inTemplate, MetadataTemplate.class);
                            templates.add(template);
                        } catch (StreamException | IOException e) {
                            LOGGER.log(Level.SEVERE, e.getMessage(), e);
                        }
                    }
                } catch (StreamException e) {
                    LOGGER.warning("Priorities file is empty.");
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void save(MetadataTemplate template) throws IOException {
        // validate
        if (template.getId() == null) {
            throw new IllegalArgumentException("template without id not allowed.");
        }
        if (template.getName() == null) {
            throw new IllegalArgumentException("template without name not allowed.");
        }

        boolean isNew;
        synchronized (templates) {
            for (MetadataTemplate other : templates) {
                if (!other.equals(template) && other.getName().equals(template.getName())) {
                    throw new IllegalArgumentException(
                            "template name " + template.getName() + " not unique.");
                }
            }

            // add or replace in list
            int index = templates.indexOf(template);
            isNew = index < 0;
            if (isNew) {
                templates.add(template);
            } else {
                templates.set(index, template);
            }
        }

        // update layers
        Set<String> deletedLayers = new HashSet<>();
        for (String key : template.getLinkedLayers()) {
            ResourceInfo resource = rawCatalog.getResource(key, ResourceInfo.class);

            if (resource == null) {
                // remove the link because the layer cannot be found.
                deletedLayers.add(key);
                LOGGER.log(
                        Level.INFO,
                        "Link to resource "
                                + key
                                + " link removed from template "
                                + template.getName()
                                + " because it doesn't exist anymore.");
            }
        }
        template.getLinkedLayers().removeAll(deletedLayers);

        getFolder().removeListener(this);

        // persist
        try (OutputStream out = getFolder().get(template.getId() + ".xml").out()) {
            persister.save(template, out);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }

        if (isNew) {
            persistList();
        }

        getFolder().addListener(this);
    }

    @Override
    public void saveList(List<MetadataTemplate> newList) throws IOException {
        List<MetadataTemplate> deleted;
        synchronized (templates) {
            if (!templates.containsAll(newList)) {
                throw new IllegalArgumentException("Use save to add new templates.");
            }
            deleted = new ArrayList<>(templates);
            deleted.removeAll(newList);

            templates.clear();
            templates.addAll(newList);
        }

        getFolder().removeListener(this);

        // persist
        persistList();

        // remove deleted
        for (MetadataTemplate item : deleted) {
            if (!getFolder().get(item.getId() + ".xml").delete()) {
                LOGGER.warning("Failed to delete template " + item + " from hard drive.");
            }
        }

        getFolder().addListener(this);
    }

    private void persistList() throws IOException {
        synchronized (templates) {
            List<String> priorities =
                    templates
                            .stream()
                            .map(template -> template.getId())
                            .collect(Collectors.toList());
            try (OutputStream out = getFolder().get(LIST_FILE).out()) {
                persister.save(priorities, out);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                throw e;
            }
        }
    }

    @Override
    public List<MetadataTemplate> list() {
        synchronized (templates) {
            return templates
                    .stream()
                    .map(template -> template.clone())
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void update(Collection<String> resourceIds, UUID progressKey) {
        int counter = 0;
        for (String resourceId : resourceIds) {
            if (progressKey != null) {
                globalModelService.put(progressKey, ((float) counter++) / resourceIds.size());
            }
            ResourceInfo resource = rawCatalog.getResource(resourceId, ResourceInfo.class);

            if (resource != null) {
                update(resource);
            }
        }
        if (progressKey != null) {
            globalModelService.put(progressKey, 1.0f);
        }
    }

    private void update(ResourceInfo resource) {
        Serializable custom = resource.getMetadata().get(MetadataConstants.CUSTOM_METADATA_KEY);
        @SuppressWarnings("unchecked")
        ComplexMetadataMapImpl model =
                new ComplexMetadataMapImpl((HashMap<String, Serializable>) custom);

        ArrayList<ComplexMetadataMap> sources = new ArrayList<>();
        synchronized (templates) {
            for (MetadataTemplate template : templates) {
                if (template.getLinkedLayers().contains(resource.getId())) {
                    sources.add(new ComplexMetadataMapImpl(template.getMetadata()));
                }
            }
        }

        if (sources.size() > 0) {
            @SuppressWarnings("unchecked")
            HashMap<String, List<Integer>> derivedAtts =
                    (HashMap<String, List<Integer>>)
                            resource.getMetadata()
                                    .computeIfAbsent(
                                            MetadataConstants.DERIVED_KEY, key -> new HashMap<>());
            metadataService.merge(model, sources, derivedAtts);
            // derived atts
            metadataService.derive(model);
            // update timestamp
            model.get(Date.class, MetadataConstants.TIMESTAMP_KEY).setValue(new Date());

            resource.getMetadata().put(MetadataConstants.DERIVED_KEY, derivedAtts);

            // custom-to-native mapping
            for (LayerInfo layer : rawCatalog.getLayers(resource)) {
                layer.setResource(resource);
                nativeToCustomService.mapCustomToNative(layer);
                rawCatalog.save(layer);
            }

            rawCatalog.save(resource);
        }
    }

    @Override
    public MetadataTemplate findByName(String name) {
        synchronized (templates) {
            for (MetadataTemplate template : templates) {
                if (template.getName().equals(name)) {
                    return template.clone();
                }
            }
            return null;
        }
    }

    @Override
    public MetadataTemplate getById(String id) {
        synchronized (templates) {
            for (MetadataTemplate template : templates) {
                if (template.getId().equals(id)) {
                    return template.clone();
                }
            }
            return null;
        }
    }
}
