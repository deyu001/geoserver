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

package org.geoserver.catalog.impl;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MapInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.LoggingInfoImpl;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;

public enum ClassMappings {

    //
    // catalog
    //
    WORKSPACE {
        @Override
        public Class<? extends Info> getInterface() {
            return WorkspaceInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return WorkspaceInfoImpl.class;
        };
    },
    NAMESPACE {
        @Override
        public Class<? extends Info> getInterface() {
            return NamespaceInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return NamespaceInfoImpl.class;
        };
    },

    // stores, order matters
    DATASTORE {
        @Override
        public Class<? extends Info> getInterface() {
            return DataStoreInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return DataStoreInfoImpl.class;
        };
    },
    COVERAGESTORE {
        @Override
        public Class<? extends Info> getInterface() {
            return CoverageStoreInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return CoverageStoreInfoImpl.class;
        };
    },
    WMSSTORE {
        @Override
        public Class<? extends Info> getInterface() {
            return WMSStoreInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return WMSStoreInfoImpl.class;
        };
    },
    WMTSSTORE {
        @Override
        public Class<? extends Info> getInterface() {
            return WMTSStoreInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return WMTSStoreInfoImpl.class;
        };
    },
    STORE {
        @Override
        public Class<? extends Info> getInterface() {
            return StoreInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return StoreInfoImpl.class;
        };

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends CatalogInfo>[] concreteInterfaces() {
            return new Class[] {
                CoverageStoreInfo.class,
                DataStoreInfo.class,
                WMSStoreInfo.class,
                WMTSStoreInfo.class
            };
        }
    },

    // resources, order matters
    FEATURETYPE {
        @Override
        public Class<? extends Info> getInterface() {
            return FeatureTypeInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return FeatureTypeInfoImpl.class;
        };
    },
    COVERAGE {
        @Override
        public Class<? extends Info> getInterface() {
            return CoverageInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return CoverageInfoImpl.class;
        };
    },
    WMSLAYER {
        @Override
        public Class<? extends Info> getInterface() {
            return WMSLayerInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return WMSLayerInfoImpl.class;
        };
    },
    WMTSLAYER {
        @Override
        public Class<? extends Info> getInterface() {
            return WMTSLayerInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return WMTSLayerInfoImpl.class;
        };
    },
    RESOURCE {
        @Override
        public Class<? extends Info> getInterface() {
            return ResourceInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return ResourceInfoImpl.class;
        };

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends CatalogInfo>[] concreteInterfaces() {
            return new Class[] {
                CoverageInfo.class, FeatureTypeInfo.class, WMSLayerInfo.class, WMTSLayerInfo.class
            };
        }
    },
    PUBLISHED {
        @Override
        public Class<? extends Info> getInterface() {
            return PublishedInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return null;
        };

        @Override
        @SuppressWarnings("unchecked")
        public Class<? extends CatalogInfo>[] concreteInterfaces() {
            return new Class[] {LayerInfo.class, LayerGroupInfo.class};
        }
    },
    LAYER {
        @Override
        public Class<? extends Info> getInterface() {
            return LayerInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return LayerInfoImpl.class;
        };
    },
    LAYERGROUP {
        @Override
        public Class<? extends Info> getInterface() {
            return LayerGroupInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return LayerGroupInfoImpl.class;
        };
    },
    MAP {
        @Override
        public Class<? extends Info> getInterface() {
            return MapInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return MapInfoImpl.class;
        };
    },
    STYLE {
        @Override
        public Class<? extends Info> getInterface() {
            return StyleInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return StyleInfoImpl.class;
        };
    },

    //
    // config
    //
    GLOBAL {
        @Override
        public Class<? extends Info> getInterface() {
            return GeoServerInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return GeoServerInfoImpl.class;
        };
    },

    LOGGING {
        @Override
        public Class<? extends Info> getInterface() {
            return LoggingInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return LoggingInfoImpl.class;
        };
    },

    SETTINGS {
        @Override
        public Class<? extends Info> getInterface() {
            return SettingsInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return SettingsInfoImpl.class;
        };
    },

    // // services, order matters
    // WMS {
    // @Override public Class getInterface() { return WMSInfo.class; }
    // @Override public Class getImpl() { return WMSInfoImpl.class; };
    // },
    //
    // WFS {
    // @Override public Class getInterface() { return WFSInfo.class; }
    // @Override public Class getImpl() { return WFSInfoImpl.class; };
    // },
    //
    // WCS {
    // @Override public Class getInterface() { return WCSInfo.class; }
    // @Override public Class getImpl() { return WCSInfoImpl.class; };
    // },

    SERVICE {
        @Override
        public Class<? extends Info> getInterface() {
            return ServiceInfo.class;
        }

        @Override
        public Class<? extends Info> getImpl() {
            return ServiceInfoImpl.class;
        };
    };

    public abstract Class<? extends Info> getInterface();

    public abstract Class<? extends Info> getImpl();

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Class<? extends Info>[] concreteInterfaces() {
        Class<? extends Info> interf = getInterface();
        return new Class[] {interf};
    }

    public static ClassMappings fromInterface(Class<? extends Info> interfce) {
        if (ServiceInfo.class.isAssignableFrom(interfce)) {
            return SERVICE;
        }
        for (ClassMappings cm : values()) {
            if (interfce.equals(cm.getInterface())) {
                return cm;
            }
        }
        return null;
    }

    public static ClassMappings fromImpl(Class<?> clazz) {
        if (ServiceInfo.class.isAssignableFrom(clazz)) {
            return SERVICE;
        }
        for (ClassMappings cm : values()) {
            if (clazz == cm.getImpl()) return cm;
        }
        return null;
    }
}
