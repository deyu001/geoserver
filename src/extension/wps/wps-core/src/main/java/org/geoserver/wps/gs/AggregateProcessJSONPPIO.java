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

package org.geoserver.wps.gs;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONObject;
import org.geoserver.wps.ppio.CDataPPIO;
import org.geotools.process.vector.AggregateProcess;

/**
 * Provides a JSON output for the aggregate process result. The result are encoded in a tabular
 * format very similar to the output of SQL query.
 *
 * <p>The tabular data is the composition of the group by attributes values and the aggregation
 * functions results. Both of these values appear in the order they are declared, the group by
 * values appear first and the aggregation values after. If there is no group by attributes, only
 * the aggregations values appear.
 *
 * <p>Follow some examples:
 *
 * <p>The max and min energy consumption:
 *
 * <pre>
 * <code>{
 *   "AggregationAttribute": "energy_consumption",
 *   "AggregationFunctions": ["Max", "Min"],
 *   "GroupByAttributes": ["building_type"],
 *   "AggregationResults": [
 *     ["School", 500.0, 80.0],
 *     ["Fabric", 850.0, 120.0]
 *   ]
 * }</code>
 * </pre>
 *
 * <p>The max and min energy consumption per building type and energy type:
 *
 * <pre>
 * <code>{
 *   "AggregationAttribute": "energy_consumption",
 *   "AggregationFunctions": ["Max", "Min"],
 *   "GroupByAttributes": ["building_type", "energy_type"],
 *   "AggregationResults": [
 *     ["School", "Nuclear", 500.0, 220.0],
 *     ["School", "Wind", 200.0, 120.0],
 *     ["Fabric", "Nuclear", 230.0, 80.0],
 *     ["Fabric", "Fuel", 850.0, 370.0]
 *   ]
 * }</code>
 * </pre>
 */
public class AggregateProcessJSONPPIO extends CDataPPIO {

    protected AggregateProcessJSONPPIO() {
        super(AggregateProcess.Results.class, AggregateProcess.Results.class, "application/json");
    }

    @Override
    public void encode(Object value, OutputStream output) throws Exception {
        AggregateProcess.Results processResult = (AggregateProcess.Results) value;
        Map<Object, Object> json = new HashMap<>();
        // we encode the common parts regardless of the presence of group by attributes
        json.put("AggregationAttribute", processResult.getAggregateAttribute());
        json.put("AggregationFunctions", extractAggregateFunctionsNames(processResult));
        if (processResult.getGroupByAttributes() == null
                || processResult.getGroupByAttributes().isEmpty()) {
            // if there is no group by attributes we only to encode the aggregations function
            // results
            json.put("GroupByAttributes", new String[0]);
            json.put("AggregationResults", new Number[][] {encodeSimpleResult(processResult)});
        } else {
            // there is group by values so we need to encode all the grouped results
            json.put("GroupByAttributes", processResult.getGroupByAttributes().toArray());
            json.put("AggregationResults", processResult.getGroupByResult().toArray());
        }
        output.write(JSONObject.fromObject(json).toString().getBytes());
    }

    /**
     * Helper method that encodes the result of an aggregator process when there is no group by
     * attributes. We encode the value of each aggregation function producing an output very similar
     * of an SQL query result.
     *
     * @param processResult the result of the aggregator process
     * @return aggregation functions result values
     */
    private Number[] encodeSimpleResult(AggregateProcess.Results processResult) {
        return processResult
                .getFunctions()
                .stream()
                .map(function -> processResult.getResults().get(function))
                .toArray(Number[]::new);
    }

    /**
     * Helper that extract the name of the aggregation functions.
     *
     * @param result the result of the aggregator process
     * @return an array that contain the aggregation functions names
     */
    private String[] extractAggregateFunctionsNames(AggregateProcess.Results result) {
        return result.getFunctions().stream().map(Enum::name).toArray(String[]::new);
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        throw new UnsupportedOperationException("JSON parsing is not supported");
    }

    @Override
    public Object decode(String input) throws Exception {
        throw new UnsupportedOperationException("JSON parsing is not supported");
    }

    @Override
    public final String getFileExtension() {
        return "json";
    }
}
