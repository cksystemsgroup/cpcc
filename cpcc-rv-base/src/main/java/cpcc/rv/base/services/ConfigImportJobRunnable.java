// This code is part of the CPCC-NG project.
//
// Copyright (c) 2015 Clemens Krainer <clemens.krainer@gmail.com>
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

package cpcc.rv.base.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.ioc.ServiceResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlike.genson.Genson;

import cpcc.core.entities.RealVehicle;
import cpcc.core.entities.SensorDefinition;
import cpcc.core.services.QueryManager;
import cpcc.core.services.RealVehicleRepository;
import cpcc.core.services.jobs.JobRunnable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Configuration synchronization job runnable.
 */
public class ConfigImportJobRunnable implements JobRunnable
{
    private static final Logger LOG = LoggerFactory.getLogger(ConfigImportJobRunnable.class);

    private byte[] data;

    private ServiceResources serviceResources;
    private Genson genson;
    private HibernateSessionManager sessionManager;
    private QueryManager queryManager;
    private RealVehicleRepository rvRepo;

    /**
     * @param serviceResources the service resources.
     * @param data the data to import.
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Exposed on purpose.")
    public ConfigImportJobRunnable(ServiceResources serviceResources, byte[] data)
    {
        this.serviceResources = serviceResources;
        this.data = data;
        genson = new Genson();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run()
    {
        queryManager = serviceResources.getService(QueryManager.class);
        sessionManager = serviceResources.getService(HibernateSessionManager.class);
        rvRepo = serviceResources.getService(RealVehicleRepository.class);

        ConfigSyncData syncData = genson.deserialize(data, ConfigSyncData.class);

        syncSensorDefinitionConfig(syncData.getSen());
        syncRealVehicleConfig(syncData.getRvs());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean executionSucceeded()
    {
        return true;
    }

    /**
     * @param realVehicles the list of real vehicles.
     * @return the list of sent back real vehicles.
     */
    private void syncRealVehicleConfig(List<RealVehicle> realVehicles)
    {
        Map<Integer, RealVehicle> allRvsMap = getAllRvsFromDataBase();

        for (RealVehicle rv : realVehicles)
        {
            int rdId = rv.getId();
            boolean saveOnly = !allRvsMap.containsKey(rdId);

            RealVehicle dbRv = allRvsMap.computeIfAbsent(rdId, k -> {
                RealVehicle r = new RealVehicle();
                r.setId(rdId);
                r.setLastUpdate(new Date(0));
                LOG.info("### new RealVehicle id={}", rdId);
                return r;
            });

            int updated = 0;
            try
            {
                updated = fillInNewerRealVehicle(dbRv, rv);
            }
            catch (RuntimeException e)
            {
                LOG.error("Can not synchronize JSON object {}", rv, e);
                continue;
            }

            if (updated == 1)
            {
                updateSensorDefinitions(dbRv, rv);
                if (saveOnly)
                {
                    LOG.info("### save RealVehicle id={}", rdId);
                    sessionManager.getSession().save(dbRv);
                }
                else
                {
                    LOG.info("### update RealVehicle id={}", rdId);
                    sessionManager.getSession().saveOrUpdate(dbRv);
                }
            }
        }
    }

    /**
     * @return the map of all currently known Real Vehicles.
     */
    private Map<Integer, RealVehicle> getAllRvsFromDataBase()
    {
        Map<Integer, RealVehicle> rvMap = new HashMap<>();

        for (RealVehicle rv : rvRepo.findAllRealVehicles())
        {
            rvMap.put(rv.getId(), rv);
        }

        return rvMap;
    }

    /**
     * @param rv the real vehicle.
     * @param rvObj the other real vehicle.
     * @return 1 if an update has taken place, <= 0 if nothing has changed.
     */
    private int fillInNewerRealVehicle(RealVehicle rv, RealVehicle rvObj)
    {
        long lastUpdateNew = rvObj.getLastUpdate().getTime();
        long lastUpdateDb = rv.getLastUpdate().getTime();

        if (lastUpdateNew < lastUpdateDb)
        {
            return -1;
        }

        if (lastUpdateNew == lastUpdateDb)
        {
            return 0;
        }

        rv.setId(rvObj.getId());
        rv.setLastUpdate(rvObj.getLastUpdate());
        rv.setAreaOfOperation(rvObj.getAreaOfOperation());
        rv.setUrl(rvObj.getUrl());
        rv.setName(rvObj.getName());
        rv.setType(rvObj.getType());
        rv.setDeleted(rvObj.getDeleted());

        return 1;
    }

    /**
     * @param rv the new real vehicle as a JSON object.
     * @param dbRv the real vehicle currently in the database.
     */
    private void updateSensorDefinitions(RealVehicle dbRv, RealVehicle rv)
    {
        List<SensorDefinition> sensors = rv.getSensors();

        Set<Integer> assignedSdIds = new HashSet<>();
        for (SensorDefinition s : dbRv.getSensors())
        {
            assignedSdIds.add(s.getId());
        }

        Set<Integer> newSdIds = new HashSet<>();
        for (SensorDefinition sdx : sensors)
        {
            int sdId = sdx.getId();
            newSdIds.add(sdId);

            if (!assignedSdIds.contains(sdId))
            {
                SensorDefinition sd = queryManager.findSensorDefinitionById(sdId);
                dbRv.getSensors().add(sd);
            }
        }

        Set<SensorDefinition> removedSds = new HashSet<>();
        for (SensorDefinition s : dbRv.getSensors())
        {
            if (!newSdIds.contains(s.getId()))
            {
                removedSds.add(s);
            }
        }

        dbRv.getSensors().removeAll(removedSds);
    }

    /**
     * @param sensorDefs the sensor definitions to be synchronized with the local database.
     * @return the sensor definitions that are newer in the local database.
     */
    private List<SensorDefinition> syncSensorDefinitionConfig(List<SensorDefinition> sensorDefs)
    {
        Map<Integer, SensorDefinition> allSdsMap = getAllSensorDefinitionsFromDatabase();

        List<SensorDefinition> back = new ArrayList<>();
        Map<Integer, SensorDefinition> incoming = new HashMap<>();

        for (SensorDefinition sd : sensorDefs)
        {
            boolean saveOnly = false;

            int sdId = sd.getId();
            SensorDefinition dbSd = allSdsMap.get(sdId);

            if (dbSd == null)
            {
                dbSd = new SensorDefinition();
                dbSd.setId(sdId);
                dbSd.setLastUpdate(new Date(0));
                allSdsMap.put(sdId, dbSd);
                saveOnly = true;
                LOG.info("### new SensorDefinition id={}", sdId);
            }

            int updated = 0;
            try
            {
                updated = fillInNewerSensorDefinition(dbSd, sd);
            }
            catch (RuntimeException e)
            {
                LOG.error("Can not synchronize JSON object {}", sd, e);
                continue;
            }

            incoming.put(sdId, dbSd);

            if (updated == 1)
            {
                if (saveOnly)
                {
                    LOG.info("### save SensorDefinition id={} {}", sdId, dbSd.getLastUpdate());
                    sessionManager.getSession().save(dbSd);
                }
                else
                {
                    LOG.info("### update SensorDefinition id={} {}", sdId, dbSd.getLastUpdate());
                    sessionManager.getSession().saveOrUpdate(dbSd);
                }
            }
        }

        return back;
    }

    /**
     * @return all the sensor definitions in the database.
     */
    private Map<Integer, SensorDefinition> getAllSensorDefinitionsFromDatabase()
    {
        Map<Integer, SensorDefinition> sdMap = new HashMap<>();

        for (SensorDefinition sd : queryManager.findAllSensorDefinitions())
        {
            sdMap.put(sd.getId(), sd);
        }

        return sdMap;
    }

    /**
     * @param sensorDatabase the sensor definition from the database.
     * @param sensorNew the new sensor definition.
     * @return
     */
    private int fillInNewerSensorDefinition(SensorDefinition sensorDatabase, SensorDefinition sensorNew)
    {
        long lastUpdateNew = sensorNew.getLastUpdate().getTime();
        long lastUpdateDb = sensorDatabase.getLastUpdate().getTime();

        if (lastUpdateNew < lastUpdateDb)
        {
            return -1;
        }

        if (lastUpdateNew == lastUpdateDb)
        {
            return 0;
        }

        sensorDatabase.setId(sensorNew.getId());
        sensorDatabase.setDescription(sensorNew.getDescription());
        sensorDatabase.setType(sensorNew.getType());
        sensorDatabase.setMessageType(sensorNew.getMessageType());
        sensorDatabase.setVisibility(sensorNew.getVisibility());
        sensorDatabase.setParameters(sensorNew.getParameters());
        sensorDatabase.setLastUpdate(sensorNew.getLastUpdate());
        sensorDatabase.setDeleted(sensorNew.getDeleted());

        return 1;
    }
}
