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

package org.geoserver.importer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class MemoryImportStore implements ImportStore {

    AtomicLong idseq = new AtomicLong();

    Queue<ImportContext> imports = new ConcurrentLinkedQueue<>();

    @Override
    public String getName() {
        return "memory";
    }

    @Override
    public void init() {}

    @Override
    public ImportContext get(long id) {
        for (ImportContext context : imports) {
            if (context.getId() == id) {
                return context;
            }
        }
        return null;
    }

    @Override
    public Long advanceId(Long id) {
        if (id <= idseq.longValue()) {
            id = idseq.getAndIncrement();
        } else {
            idseq.set(id + 1);
        }
        return id;
    }

    @Override
    public void add(ImportContext context) {
        context.setId(idseq.getAndIncrement());
        imports.add(context);
        if (imports.size() > 100) {
            clearCompletedImports();
        }
    }

    void clearCompletedImports() {
        List<ImportContext> completed =
                collect(
                        new ImportCollector() {
                            @Override
                            protected boolean capture(ImportContext context) {
                                return context.getState() == ImportContext.State.COMPLETE;
                            }
                        });
        imports.removeAll(completed);
    }

    @Override
    public void save(ImportContext context) {
        imports.remove(context);
        imports.add(context);
    }

    @Override
    public void remove(ImportContext importContext) {
        imports.remove(importContext);
    }

    @Override
    public void removeAll() {
        imports.clear();
    }

    @Override
    public Iterator<ImportContext> iterator() {
        return imports.iterator();
    }

    public Iterator<ImportContext> iterator(String sortBy) {
        if (sortBy == null) {
            return iterator();
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<ImportContext> allNonCompleteImports() {
        return collect(
                        new ImportCollector() {
                            @Override
                            protected boolean capture(ImportContext context) {
                                return context.getState() != ImportContext.State.COMPLETE;
                            }
                        })
                .iterator();
    }

    @Override
    public Iterator<ImportContext> importsByUser(final String user) {
        return collect(
                        new ImportCollector() {
                            @Override
                            protected boolean capture(ImportContext context) {
                                return user.equals(context.getUser());
                            }
                        })
                .iterator();
    }

    @Override
    public void query(ImportVisitor visitor) {
        for (ImportContext context : imports) {
            visitor.visit(context);
        }
    }

    List<ImportContext> collect(ImportCollector collector) {
        query(collector);
        return collector.getCollected();
    }

    @Override
    public void destroy() {
        idseq.set(0);
        imports.clear();
    }

    abstract static class ImportCollector implements ImportVisitor {

        List<ImportContext> collected = new ArrayList<>();

        @Override
        public final void visit(ImportContext context) {
            if (capture(context)) {
                collected.add(context);
            }
        }

        public List<ImportContext> getCollected() {
            return collected;
        }

        protected abstract boolean capture(ImportContext context);
    }
}
