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

package org.geoserver.taskmanager.schedule;

import java.util.Collection;
import java.util.function.Consumer;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Configuration;

/**
 * The batch job is responsible for scheduling the batches.
 *
 * @author Niels Charlier
 */
public interface BatchJobService {

    /**
     * Saves this batch and update the schedule according to its new settings. This may also mean
     * that the batch is actually unscheduled, or that it scheduling is changed, if it was
     * previously scheduled.
     *
     * <p>Batches are scheduled if they are ACTIVE, ENABLED and have a FREQUENCY set. Batches which
     * are ACTIVE, but not ENABLED _or_ have FREQUENCY set to NULL, are known by the quartz
     * scheduler but are never triggered unless explicitly done. Batches which are NOT ACTIVE, are
     * entirely removed from the quartz scheduler.
     *
     * @param batch the batch.
     * @return the saved batch.
     */
    Batch saveAndSchedule(Batch batch);

    /** Refreshes the scheduler completely based on all of the batches in the database. */
    void reloadFromData();

    /**
     * Saves this configuration and update the schedule according to its new settings of each batch.
     *
     * @param config the Configuration.
     * @return the saved config.
     */
    Configuration saveAndSchedule(Configuration config);

    /**
     * Interrupt a batch run. This method will also check if it can verify the batch run has
     * actually already ended (for example when the server was restarted), and if that is the case
     * update its status.
     *
     * @param batchRun the batch run to interrupt
     */
    void interrupt(BatchRun batchRun);

    /**
     * Start a batch right now.
     *
     * @return a (unique) scheduler reference that can be used to identify the batch run
     */
    String scheduleNow(Batch batch);

    /** Remove configuration from database and scheduler at once */
    Configuration remove(Configuration config);

    /** Remove batch from database and scheduler at once */
    Batch remove(Batch batch);

    /**
     * Start a collection of batches right now.
     *
     * @param batches the batches to be run
     * @param waitInSeconds number of seconds to wait before the first batch
     * @param intervalInSeconds number of seconds to wait between batches, may be be zero to
     *     schedule all at once.
     */
    void scheduleNow(Collection<Batch> batches, int waitInSeconds, int intervalInSeconds);

    /**
     * Start a collection of batches right now.
     *
     * @param batches the batches to be run
     * @param waitInSeconds number of seconds to wait before the first batch
     * @param intervalInSeconds number of seconds to wait between batches, may be be zero to
     *     schedule all at once.
     * @param callback run afterwards
     */
    void scheduleNow(
            Collection<Batch> batches,
            int waitInSeconds,
            int intervalInSeconds,
            Consumer<Batch> callback);
}
