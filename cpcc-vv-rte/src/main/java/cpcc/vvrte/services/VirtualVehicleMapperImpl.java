// This code is part of the CPCC-NG project.
//
// Copyright (c) 2013 Clemens Krainer <clemens.krainer@gmail.com>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software Foundation,
// Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

package cpcc.vvrte.services;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpcc.core.entities.RealVehicle;
import cpcc.core.entities.RealVehicleType;
import cpcc.core.entities.SensorDefinition;
import cpcc.core.services.RealVehicleRepository;
import cpcc.core.utils.RealVehicleUtils;
import cpcc.vvrte.base.VirtualVehicleMappingDecision;
import cpcc.vvrte.entities.Task;

/**
 * VirtualVehicleMapperImpl
 */
public class VirtualVehicleMapperImpl implements VirtualVehicleMapper
{
    private static final Logger LOG = LoggerFactory.getLogger(VirtualVehicleMapperImpl.class);

    private RealVehicleRepository rvRepo;

    /**
     * @param rvRepo the real vehicle repository.
     */
    public VirtualVehicleMapperImpl(RealVehicleRepository rvRepo)
    {
        this.rvRepo = rvRepo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VirtualVehicleMappingDecision findMappingDecision(Task task)
    {
        VirtualVehicleMappingDecision decision = new VirtualVehicleMappingDecision()
            .setTask(task)
            .setMigration(true);

        RealVehicle rv = rvRepo.findOwnRealVehicle();
        if (rv == null)
        {
            return migrateTask(decision);
        }

        boolean migration = !RealVehicleUtils.isInsideAreaOfOperation(rv.getAreaOfOperation(), task.getPosition());

        if (migration || !rv.getSensors().containsAll(task.getSensors()))
        {
            return migrateTask(decision);
        }

        return decision.setMigration(false);
    }

    /**
     * @param decision the migration decision.
     * @return the migration decision.
     */
    private VirtualVehicleMappingDecision migrateTask(VirtualVehicleMappingDecision decision)
    {
        Task task = decision.getTask();
        List<RealVehicle> groundStations = new ArrayList<>();
        List<RealVehicle> destinationRealVehicles = new ArrayList<>();

        for (RealVehicle rv : rvRepo.findAllActiveRealVehiclesExceptOwn())
        {
            if (rv.getType() == RealVehicleType.GROUND_STATION)
            {
                groundStations.add(rv);
            }

            if (RealVehicleUtils.isInsideAreaOfOperation(rv.getAreaOfOperation(), task.getPosition()))
            {
                if (rv.getSensors().containsAll(task.getSensors()))
                {
                    LOG.info("Found migration candidate {} for task at {}", rv.getName(), task.getPosition());
                    destinationRealVehicles.add(rv);
                }
                else
                {
                    String sensorString = getSensorString(task.getSensors(), rv.getSensors());
                    LOG.debug("Migrate not to {} because of sensors {}", rv.getName(), sensorString);
                }
            }
            else
            {
                LOG.debug("Migrate not to {} because of position {}", rv.getName(), task.getPosition());
            }
        }

        decision.setRealVehicles(destinationRealVehicles.isEmpty() ? groundStations : destinationRealVehicles);
        return decision;
    }

    /**
     * @param requiredSensors the list of required sensors.
     * @param availableSensors the list of available sensors.
     * @return the sensors as a {@code String} object.
     */
    private static String getSensorString(List<SensorDefinition> requiredSensors,
        List<SensorDefinition> availableSensors)
    {
        StringBuilder b = new StringBuilder("required: ");
        sd(b, requiredSensors);
        b.append(" available: ");
        sd(b, availableSensors);
        return b.toString();
    }

    /**
     * @param toAppendTo the {@code StringBuilder} instance.
     * @param sensors the list of sensors to be appended.
     */
    private static void sd(StringBuilder toAppendTo, List<SensorDefinition> sensors)
    {
        boolean first = true;

        for (SensorDefinition s : sensors)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                toAppendTo.append(", ");
            }

            toAppendTo.append(s.getDescription());
        }
    }
}
