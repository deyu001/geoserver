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

package org.geoserver.monitor.hib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.geoserver.platform.GeoServerExtensions;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

public class ErrorUserType implements UserType {

    /**
     * flag that determines if we should use the hibernate BlobImpl class when writing to the
     * database, since it does not work with oracle.
     *
     * <p>http://opensource.atlassian.com/projects/hibernate/browse/EJB-24
     */
    public static String USE_HIBERNATE_BLOB = "USE_HIBERNATE_BLOB";

    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    public Serializable disassemble(Object value) throws HibernateException {
        return (Throwable) value;
    }

    public boolean equals(Object x, Object y) throws HibernateException {
        return x == y;
    }

    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    public boolean isMutable() {
        return false;
    }

    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
            throws HibernateException, SQLException {
        Blob blob = rs.getBlob(names[0]);
        if (blob == null) {
            return null;
        }
        //        byte[] bytes = rs.getBytes(names[0]);
        //        if (bytes == null) {
        //            return null;
        //        }

        ObjectInputStream in = null;
        try {
            // in = new ObjectInputStream(new ByteArrayInputStream(bytes));
            in = new ObjectInputStream(blob.getBinaryStream());
            return in.readObject();
        } catch (IOException e) {
            throw new HibernateException(e);
        } catch (ClassNotFoundException e) {
            throw new HibernateException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public void nullSafeSet(PreparedStatement st, Object value, int index)
            throws HibernateException, SQLException {
        if (value != null) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bytes);
                out.writeObject(value);
                out.flush();

                if (useHibernateBlob()) {
                    st.setBlob(index, Hibernate.createBlob(bytes.toByteArray()));
                } else {
                    st.setBytes(index, bytes.toByteArray());
                }

                out.close();
            } catch (IOException e) {
                throw new HibernateException(e);
            }
        } else {
            st.setNull(index, Types.BLOB);
            // st.setBytes(index, null);
        }
    }

    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return target;
    }

    public Class returnedClass() {
        return Throwable.class;
    }

    public int[] sqlTypes() {
        return new int[] {Types.BLOB};
    }

    boolean useHibernateBlob() {
        String prop = GeoServerExtensions.getProperty(USE_HIBERNATE_BLOB);
        return !("no".equals(prop) || "false".equals(prop));
    }
}
