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

package org.geoserver.config.impl;

import java.io.Serializable;
import java.util.concurrent.ThreadPoolExecutor;
import org.geoserver.config.CoverageAccessInfo;

public class CoverageAccessInfoImpl implements Serializable, CoverageAccessInfo {

    private static final long serialVersionUID = 8909514231467268331L;

    transient ThreadPoolExecutor threadPoolExecutor;

    public static final int DEFAULT_MaxPoolSize = 5;
    int maxPoolSize = DEFAULT_MaxPoolSize;

    public static final int DEFAULT_CorePoolSize = 5;
    int corePoolSize = DEFAULT_CorePoolSize;

    public static final int DEFAULT_KeepAliveTime = 30000;
    int keepAliveTime = DEFAULT_KeepAliveTime;

    public static final QueueType DEFAULT_QUEUE_TYPE = QueueType.UNBOUNDED;
    QueueType queueType = DEFAULT_QUEUE_TYPE;

    public static final long DEFAULT_ImageIOCacheThreshold = 10 * 1024;
    long imageIOCacheThreshold = DEFAULT_ImageIOCacheThreshold;

    public CoverageAccessInfoImpl() {
        threadPoolExecutor = null;
    }

    public ThreadPoolExecutor getThreadPoolExecutor() {
        return threadPoolExecutor;
    }

    public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(int keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public QueueType getQueueType() {
        return queueType;
    }

    public void setQueueType(QueueType queueType) {
        this.queueType = queueType;
    }

    public void setImageIOCacheThreshold(long imageIOCacheThreshold) {
        this.imageIOCacheThreshold = imageIOCacheThreshold;
    }

    public long getImageIOCacheThreshold() {
        return imageIOCacheThreshold;
    }

    public void dispose() {}

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + corePoolSize;
        result = prime * result + (int) (imageIOCacheThreshold ^ (imageIOCacheThreshold >>> 32));
        result = prime * result + keepAliveTime;
        result = prime * result + maxPoolSize;
        result = prime * result + ((queueType == null) ? 0 : queueType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        CoverageAccessInfoImpl other = (CoverageAccessInfoImpl) obj;
        if (corePoolSize != other.corePoolSize) return false;
        if (imageIOCacheThreshold != other.imageIOCacheThreshold) return false;
        if (keepAliveTime != other.keepAliveTime) return false;
        if (maxPoolSize != other.maxPoolSize) return false;
        if (queueType == null) {
            if (other.queueType != null) return false;
        } else if (!queueType.equals(other.queueType)) return false;
        return true;
    }

    public CoverageAccessInfoImpl clone() {
        try {
            return (CoverageAccessInfoImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
