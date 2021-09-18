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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.ioc.ServiceResources;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpcc.com.services.CommunicationResponse;
import cpcc.com.services.CommunicationResponse.Status;
import cpcc.com.services.CommunicationService;
import cpcc.core.services.RealVehicleRepository;
import cpcc.core.services.jobs.JobService;
import cpcc.core.services.jobs.TimeService;
import cpcc.core.utils.PropertyUtils;
import cpcc.vvrte.base.VvRteConstants;
import cpcc.vvrte.entities.VirtualVehicle;
import cpcc.vvrte.entities.VirtualVehicleState;
import cpcc.vvrte.entities.VirtualVehicleStorage;
import cpcc.vvrte.services.db.VvRteRepository;

/**
 * VirtualVehicleMigratorImpl
 */
public class VirtualVehicleMigratorImpl implements VirtualVehicleMigrator
{
    private static final Logger LOG = LoggerFactory.getLogger(VirtualVehicleMigratorImpl.class);

    private static final String CHUNK = "chunk";
    private static final String LAST_CHUNK = "last.chunk";
    private static final String END_TIME = "end.time";
    private static final String START_TIME = "start.time";
    private static final String MIGRATION_SOURCE = "migration.source";
    private static final String STATE_INFO = "stateInfo";
    private static final String STATE = "state";
    private static final String API_VERSION = "api.version";
    private static final String UUID = "uuid";
    private static final String NAME = "name";
    private static final String CPCC = "cpcc";
    private static final String VVRTE = "vvrte";
    private static final String STORAGE = "storage/";
    private static final String DATA_VV_PROPERTIES = "vv/vv.properties";
    private static final String DATA_VV_SOURCE_JS = "vv/vv-source.js";
    private static final String DATA_VV_CONTINUATION_JS = "vv/vv-continuation.js";

    private HibernateSessionManager sessionManager;
    private VvRteRepository vvRepository;
    private VirtualVehicleLauncher launcher;
    private JobService jobService;
    private TimeService timeService;
    private RealVehicleRepository rvRepository;
    private CommunicationService com;
    private int chunkSize;

    /**
     * @param sessionManager the Hibernate session manager.
     * @param vvRepository the virtual vehicle repository.
     * @param launcher the virtual vehicle launcher.
     * @param jobService the job service.
     * @param timeService the time service.
     * @param rvRepository the real vehicle repository.
     * @param com the communication service.
     * @param chunkSize the migration chunk size.
     */
    public VirtualVehicleMigratorImpl(ServiceResources serviceResources,
        @Symbol(VvRteConstants.MIGRATION_CHUNK_SIZE) int chunkSize)
    {
        this.sessionManager = serviceResources.getService(HibernateSessionManager.class);
        this.vvRepository = serviceResources.getService(VvRteRepository.class);
        this.launcher = serviceResources.getService(VirtualVehicleLauncher.class);
        this.jobService = serviceResources.getService(JobService.class);
        this.timeService = serviceResources.getService(TimeService.class);
        this.rvRepository = serviceResources.getService(RealVehicleRepository.class);
        this.com = serviceResources.getService(CommunicationService.class);

        this.chunkSize = chunkSize;
    }

    /**
     * @param chunkSize the migration chunk size to set.
     */
    public void setChunkSize(int chunkSize)
    {
        this.chunkSize = chunkSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initiateMigration(VirtualVehicle vehicle)
    {
        jobService.addJobIfNotExists(VvRteConstants.MIGRATION_JOB_QUEUE_NAME,
            String.format(VvRteConstants.MIGRATION_FORMAT_SEND, vehicle.getId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] findChunk(VirtualVehicle virtualVehicle, String lastStorageName, int chunkNumber)
        throws IOException, ArchiveException
    {
        String name = lastStorageName != null && lastStorageName.startsWith(STORAGE)
            ? lastStorageName.substring(8) : "";

        List<VirtualVehicleStorage> storageChunk =
            vvRepository.findStorageItemsByVirtualVehicle(virtualVehicle.getId(), name, chunkSize);

        boolean lastChunk = storageChunk.isEmpty() || storageChunk.size() < chunkSize;

        ArchiveStreamFactory factory = new ArchiveStreamFactory(StandardCharsets.UTF_8.name());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ArchiveOutputStream outStream = factory.createArchiveOutputStream("tar", baos);

        writeVirtualVehicleProperties(virtualVehicle, outStream, chunkNumber, lastChunk);
        if (chunkNumber == 0)
        {
            writeVirtualVehicleSourceCode(virtualVehicle, outStream, chunkNumber);
            writeVirtualVehicleContinuation(virtualVehicle, outStream, chunkNumber);
        }

        writeVirtualVehicleStorageChunk(outStream, chunkNumber, storageChunk);

        outStream.close();
        baos.close();

        if (lastChunk)
        {
            virtualVehicle.setState(VirtualVehicleState.MIGRATION_COMPLETED_SND);
        }

        return baos.toByteArray();
    }

    /**
     * @param virtualVehicle the virtual vehicle.
     * @param os the output stream to write to.
     * @param chunkNumber the chunk number.
     * @throws IOException thrown in case of errors.
     */
    private void writeVirtualVehicleProperties(VirtualVehicle virtualVehicle, ArchiveOutputStream os, int chunkNumber,
        boolean lastChunk) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Properties virtualVehicleProps = fillVirtualVehicleProps(virtualVehicle, lastChunk);
        virtualVehicleProps.store(baos, "Virtual Vehicle Properties");
        baos.close();

        byte[] propBytes = baos.toByteArray();

        TarArchiveEntry entry = new TarArchiveEntry(DATA_VV_PROPERTIES);
        entry.setModTime(new Date());
        entry.setSize(propBytes.length);
        entry.setIds(0, chunkNumber);
        entry.setNames(VVRTE, CPCC);

        os.putArchiveEntry(entry);
        os.write(propBytes);
        os.closeArchiveEntry();
    }

    /**
     * @param virtualVehicle the virtual vehicle.
     * @param os the output stream to write to.
     * @param chunkNumber the chunk number.
     * @throws IOException thrown in case of errors.
     */
    private void writeVirtualVehicleContinuation(VirtualVehicle virtualVehicle, ArchiveOutputStream os, int chunkNumber)
        throws IOException
    {
        byte[] continuation = virtualVehicle.getContinuation();

        if (continuation == null)
        {
            return;
        }

        TarArchiveEntry entry = new TarArchiveEntry(DATA_VV_CONTINUATION_JS);
        entry.setModTime(new Date());
        entry.setSize(continuation.length);
        entry.setIds(0, chunkNumber);
        entry.setNames(VVRTE, CPCC);

        os.putArchiveEntry(entry);
        os.write(continuation);
        os.closeArchiveEntry();
    }

    /**
     * @param virtualVehicle the virtual vehicle.
     * @param os the output stream to write to.
     * @param chunkNumber the chunk number.
     * @throws IOException thrown in case of errors.
     */
    private void writeVirtualVehicleSourceCode(VirtualVehicle virtualVehicle, ArchiveOutputStream os, int chunkNumber)
        throws IOException
    {
        if (virtualVehicle.getCode() == null)
        {
            return;
        }

        byte[] source = virtualVehicle.getCode().getBytes(StandardCharsets.UTF_8.name());

        TarArchiveEntry entry = new TarArchiveEntry(DATA_VV_SOURCE_JS);
        entry.setModTime(new Date());
        entry.setSize(source.length);
        entry.setIds(0, chunkNumber);
        entry.setNames(VVRTE, CPCC);

        os.putArchiveEntry(entry);
        os.write(source);
        os.closeArchiveEntry();
    }

    /**
     * @param os the output stream to write to.
     * @param chunkNumber the chunk number.
     * @param storageChunk the storage chunk.
     * @throws IOException thrown in case of errors.
     */
    private void writeVirtualVehicleStorageChunk(ArchiveOutputStream os, int chunkNumber,
        List<VirtualVehicleStorage> storageChunk) throws IOException
    {
        for (VirtualVehicleStorage se : storageChunk)
        {
            LOG.debug("Writing storage entry '{}'", se.getName());

            byte[] content = se.getContentAsByteArray();
            TarArchiveEntry entry = new TarArchiveEntry(STORAGE + se.getName());
            entry.setModTime(se.getModificationTime());
            entry.setSize(content.length);
            entry.setIds(se.getId(), chunkNumber);
            entry.setNames(VVRTE, CPCC);

            os.putArchiveEntry(entry);
            os.write(content);
            os.closeArchiveEntry();
        }
    }

    /**
     * @param virtualVehicle the virtual vehicle.
     * @param lastChunk true if this property object is going to be transferred in the last data chunk.
     * @return the virtual vehicle properties.
     */
    private static Properties fillVirtualVehicleProps(VirtualVehicle virtualVehicle, boolean lastChunk)
    {
        Properties props = new Properties();
        props.setProperty(NAME, virtualVehicle.getName());
        props.setProperty(UUID, virtualVehicle.getUuid());
        PropertyUtils.setProperty(props, API_VERSION, virtualVehicle.getApiVersion());
        PropertyUtils.setProperty(props, STATE, virtualVehicle.getPreMigrationState());
        PropertyUtils.setProperty(props, STATE_INFO, virtualVehicle.getStateInfo());
        PropertyUtils.setProperty(props, CHUNK, virtualVehicle.getChunkNumber());

        if (virtualVehicle.getMigrationSource() != null)
        {
            PropertyUtils.setProperty(props, MIGRATION_SOURCE, virtualVehicle.getMigrationSource().getName());
        }

        if (virtualVehicle.getStartTime() != null)
        {
            PropertyUtils.setProperty(props, START_TIME, virtualVehicle.getStartTime().getTime());
        }

        if (virtualVehicle.getEndTime() != null)
        {
            PropertyUtils.setProperty(props, END_TIME, virtualVehicle.getEndTime().getTime());
        }

        if (lastChunk)
        {
            PropertyUtils.setProperty(props, LAST_CHUNK, Boolean.TRUE);
        }

        return props;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeChunk(InputStream inStream) throws ArchiveException, IOException
    {
        boolean lastChunk = false;
        String chunkName = "unknown-" + System.currentTimeMillis();
        ArchiveStreamFactory f = new ArchiveStreamFactory();

        VirtualVehicleHolder virtualVehicleHolder = new VirtualVehicleHolder();

        try (ArchiveInputStream ais = f.createArchiveInputStream("tar", inStream))
        {
            for (TarArchiveEntry entry = (TarArchiveEntry) ais.getNextEntry(); entry != null; entry =
                (TarArchiveEntry) ais.getNextEntry())
            {
                chunkName = entry.getName();

                if (chunkName.startsWith("vv/"))
                {
                    lastChunk |= storeVirtualVehicleEntry(ais, entry, virtualVehicleHolder);
                    logMigratedChunk(chunkName, virtualVehicleHolder.getVirtualVehicle(), lastChunk);
                }
                else if (chunkName.startsWith(STORAGE))
                {
                    storeStorageEntry(ais, entry, virtualVehicleHolder.getVirtualVehicle());
                    logMigratedChunk(chunkName, virtualVehicleHolder.getVirtualVehicle(), lastChunk);
                }
                // TODO message queue
                else
                {
                    throw new IOException("Can not store unknown type of entry " + chunkName);
                }
            }
        }

        sessionManager.commit();

        VirtualVehicle vv = virtualVehicleHolder.getVirtualVehicle();

        String result = new JSONObject(UUID, vv.getUuid(), CHUNK, vv.getChunkNumber()).toCompactString();

        CommunicationResponse response = com.transfer(vv.getMigrationSource(), VvRteConstants.MIGRATION_ACK_CONNECTOR,
            org.apache.commons.codec.binary.StringUtils.getBytesUtf8(result));

        if (response.getStatus() == Status.OK)
        {
            if (lastChunk)
            {
                VirtualVehicleState newState =
                    VirtualVehicleState.VV_NO_CHANGE_AFTER_MIGRATION.contains(vv.getPreMigrationState())
                        ? vv.getPreMigrationState()
                        : VirtualVehicleState.MIGRATION_COMPLETED_RCV;

                vv.setPreMigrationState(null);
                updateStateAndCommit(vv, newState, null);
                launcher.stateChange(vv.getId(), vv.getState());
            }
        }
        else
        {
            String content = org.apache.commons.codec.binary.StringUtils.newStringUtf8(response.getContent());
            LOG.error("Migration ACK failed! Virtual vehicle: {} ({}) {}", vv.getName(), vv.getUuid(), content);
            updateStateAndCommit(vv, VirtualVehicleState.MIGRATION_INTERRUPTED_RCV, content);
        }

    }

    /**
     * @param vv the virtual vehicle.
     * @param newState the new state to set.
     * @param stateInfo the state info to set.
     */
    private void updateStateAndCommit(VirtualVehicle vv, VirtualVehicleState newState, String stateInfo)
    {
        vv.setState(newState);
        if (newState != VirtualVehicleState.DEFECTIVE)
        {
            vv.setStateInfo(stateInfo);
        }
        vv.setUpdateTime(timeService.newDate());
        sessionManager.getSession().saveOrUpdate(vv);
        sessionManager.commit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queueChunk(InputStream inputStream) throws IOException
    {
        byte[] data = IOUtils.toByteArray(inputStream);
        if (data.length == 0)
        {
            throw new IOException("No data!");
        }
        jobService.addJobIfNotExists(VvRteConstants.MIGRATION_JOB_QUEUE_NAME, VvRteConstants.MIGRATION_RECEIVE, data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ackChunk(InputStream inputStream) throws IOException
    {
        byte[] data = IOUtils.toByteArray(inputStream);
        if (data.length == 0)
        {
            throw new IOException("No data!");
        }
        jobService.addJobIfNotExists(VvRteConstants.MIGRATION_JOB_QUEUE_NAME, VvRteConstants.MIGRATION_CONTINUE, data);
    }

    /**
     * @param chunkName the name of the migrated chunk.
     * @param virtualVehicleHolder the virtual vehicle.
     */
    private void logMigratedChunk(String chunkName, VirtualVehicle virtualVehicle, boolean lastChunk)
    {
        String name = virtualVehicle != null ? " name=" + virtualVehicle.getName() : "";
        String lastChunkMsg = lastChunk ? " (last)" : " (not last)";
        LOG.debug("Migration of {}{}{}", chunkName, name, lastChunkMsg);
    }

    /**
     * @param inStream the input stream containing a virtual vehicle chunk.
     * @param entry the previously read archive entry.
     * @param virtualVehicleHolder the holder object for the virtual vehicle read.
     * @throws IOException thrown in case of errors.
     */
    private boolean storeVirtualVehicleEntry(InputStream inStream, TarArchiveEntry entry,
        VirtualVehicleHolder virtualVehicleHolder) throws IOException
    {
        boolean lastChunk = false;

        if (DATA_VV_PROPERTIES.equals(entry.getName()))
        {
            Properties props = new Properties();
            props.load(inStream);

            lastChunk |= Boolean.parseBoolean(props.getProperty(LAST_CHUNK, "false"));

            VirtualVehicle vv = vvRepository.findVirtualVehicleByUUID(props.getProperty(UUID));
            if (vv == null)
            {
                vv = createVirtualVehicle(props);
            }
            else
            {
                updateVirtualVehicle(props, vv);
            }

            virtualVehicleHolder.setVirtualVehicle(vv);
            sessionManager.getSession().saveOrUpdate(vv);
        }
        else if (DATA_VV_CONTINUATION_JS.equals(entry.getName()))
        {
            byte[] continuation = IOUtils.toByteArray(inStream);
            virtualVehicleHolder.getVirtualVehicle().setContinuation(continuation);
            sessionManager.getSession().saveOrUpdate(virtualVehicleHolder.getVirtualVehicle());
        }
        else if (DATA_VV_SOURCE_JS.equals(entry.getName()))
        {
            byte[] source = IOUtils.toByteArray(inStream);
            String code = org.apache.commons.codec.binary.StringUtils.newStringUtf8(source);
            virtualVehicleHolder.getVirtualVehicle().setCode(code);
            sessionManager.getSession().saveOrUpdate(virtualVehicleHolder.getVirtualVehicle());
        }
        else
        {
            throw new IOException("Can not store unknown virtual vehicle entry " + entry.getName());
        }

        return lastChunk;
    }

    /**
     * @param props the virtual vehicle properties.
     * @param lastChunk true if this is the last migration chunk.
     * @param vv the virtual vehicle
     * @throws IOException thrown in case of errors.
     */
    private void updateVirtualVehicle(Properties props, VirtualVehicle vv) throws IOException
    {
        if (!VirtualVehicleState.VV_STATES_FOR_MIGRATION_RCV.contains(vv.getState()))
        {
            throw new IOException("Virtual vehicle " + vv.getName() + " (" + vv.getUuid() + ")"
                + " has not state " + VirtualVehicleState.MIGRATING_RCV + " but " + vv.getState()
                + " props=" + props);
        }

        if (vv.getMigrationDestination() != null)
        {
            throw new IOException("Virtual vehicle " + vv.getName() + " (" + vv.getUuid() + ") "
                + "is being migrated and can not be a migration target.");
        }

        LOG.debug("pre-migration:  {} ({}) {}", vv.getName(), vv.getUuid(), vv.getState());

        vv.setChunkNumber(Integer.parseInt(props.getProperty(CHUNK)));
        vv.setMigrationSource(rvRepository.findRealVehicleByName(props.getProperty(MIGRATION_SOURCE)));
        sessionManager.getSession().saveOrUpdate(vv);

        LOG.debug("post-migration: {} ({}) {}", vv.getName(), vv.getUuid(), vv.getState());
    }

    /**
     * @param props the virtual vehicle properties.
     * @return the created virtual vehicle
     */
    private VirtualVehicle createVirtualVehicle(Properties props)
    {
        VirtualVehicle vv = new VirtualVehicle();
        vv.setName(props.getProperty(NAME));
        vv.setUuid(props.getProperty(UUID));
        vv.setApiVersion(Integer.parseInt(props.getProperty(API_VERSION)));
        vv.setState(VirtualVehicleState.MIGRATING_RCV);
        vv.setPreMigrationState(getVehicleState(props.getProperty(STATE)));
        vv.setStateInfo(props.getProperty(STATE_INFO));

        String startTime = props.getProperty(START_TIME);
        if (startTime != null)
        {
            vv.setStartTime(new Date(Long.parseLong(startTime)));
        }

        String endTime = props.getProperty(END_TIME);
        if (endTime != null)
        {
            vv.setEndTime(new Date(Long.parseLong(endTime)));
        }

        vv.setChunkNumber(Integer.parseInt(props.getProperty(CHUNK)));
        vv.setMigrationDestination(null);
        vv.setMigrationSource(rvRepository.findRealVehicleByName(props.getProperty(MIGRATION_SOURCE)));
        vv.setUpdateTime(timeService.newDate());
        sessionManager.getSession().saveOrUpdate(vv);
        return vv;
    }

    /**
     * @param stateString the state as a {@code String}.
     * @return the state as an enumeration.
     */
    private VirtualVehicleState getVehicleState(String stateString)
    {
        return StringUtils.isBlank(stateString)
            ? null
            : VirtualVehicleState.valueOf(stateString);
    }

    /**
     * @param inStream the input stream containing a virtual vehicle chunk.
     * @param entry the previously read archive entry.
     * @param virtualVehicleHolder the holder object for the virtual vehicle read.
     * @throws IOException thrown in case of errors.
     */
    private void storeStorageEntry(InputStream inStream, TarArchiveEntry entry, VirtualVehicle virtualVehicle)
        throws IOException
    {
        String name = entry.getName().substring(8, entry.getName().length());
        VirtualVehicleStorage item = vvRepository.findStorageItemByVirtualVehicleAndName(virtualVehicle, name);

        if (item == null)
        {
            item = new VirtualVehicleStorage();
            item.setName(name);
            item.setVirtualVehicle(virtualVehicle);
        }

        item.setModificationTime(entry.getModTime());
        item.setContentAsByteArray(IOUtils.toByteArray(inStream));

        sessionManager.getSession().saveOrUpdate(item);
    }

    /**
     * VirtualVehicleHolder
     */
    private static class VirtualVehicleHolder
    {
        private VirtualVehicle virtualVehicle;

        /**
         * @return the virtual vehicle.
         */
        public VirtualVehicle getVirtualVehicle()
        {
            return virtualVehicle;
        }

        /**
         * @param virtualVehicle the virtual vehicle to set.
         */
        public void setVirtualVehicle(VirtualVehicle virtualVehicle)
        {
            this.virtualVehicle = virtualVehicle;
        }
    }
}
