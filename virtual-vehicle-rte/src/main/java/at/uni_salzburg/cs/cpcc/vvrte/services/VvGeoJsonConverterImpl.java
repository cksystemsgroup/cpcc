/*
 * This code is part of the CPCC-NG project.
 *
 * Copyright (c) 2014 Clemens Krainer <clemens.krainer@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package at.uni_salzburg.cs.cpcc.vvrte.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geojson.Feature;

import at.uni_salzburg.cs.cpcc.vvrte.entities.VirtualVehicle;
import at.uni_salzburg.cs.cpcc.vvrte.entities.VirtualVehicleState;

/**
 * VvGeoJsonConverterImpl
 */
public class VvGeoJsonConverterImpl implements VvGeoJsonConverter
{
    @SuppressWarnings("serial")
    private static final Map<VirtualVehicleState, String> VV_COLOR_MAP = new HashMap<VirtualVehicleState, String>()
    {
        {
            put(VirtualVehicleState.DEFECTIVE, "red");
            put(VirtualVehicleState.FINISHED, "green");
            put(VirtualVehicleState.INIT, "gray");
            put(VirtualVehicleState.INTERRUPTED, "yellow");
            put(VirtualVehicleState.MIGRATING, "yellow");
            put(VirtualVehicleState.MIGRATION_AWAITED, "yellow");
            put(VirtualVehicleState.MIGRATION_COMPLETED, "yellow");
            put(VirtualVehicleState.RUNNING, "yellow");
            put(VirtualVehicleState.WAITING, "yellow");
        }
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public Feature toFeature(VirtualVehicle vv)
    {
        Feature f = new Feature();
        f.setProperty("type", "vv");
        f.setProperty("name", vv.getName());
        f.setProperty("state", VV_COLOR_MAP.get(vv.getState()));
        return f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Feature> toFeatureList(List<VirtualVehicle> vvList)
    {
        List<Feature> fc = new ArrayList<Feature>();

        for (VirtualVehicle vv : vvList)
        {
            fc.add(toFeature(vv));
        }

        return fc;
    }
}