// This code is part of the CPCC-NG project.
//
// Copyright (c) 2009-2016 Clemens Krainer <clemens.krainer@gmail.com>
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.tapestry5.ioc.ServiceResources;
import org.apache.tapestry5.ioc.services.PerthreadManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpcc.core.services.jobs.JobRunnable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * MigrationReceiveJobRunnable implementation.
 */
public class MigrationReceiveJobRunnable implements JobRunnable
{
    private static final Logger LOG = LoggerFactory.getLogger(MigrationReceiveJobRunnable.class);

    private ServiceResources serviceResources;
    private byte[] data;

    private boolean succeeded = true;

    /**
     * @param serviceResources the service resources.
     * @param data the job data.
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "This is exposed on purpose")
    public MigrationReceiveJobRunnable(ServiceResources serviceResources, byte[] data)
    {
        this.serviceResources = serviceResources;
        this.data = data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run()
    {
        PerthreadManager perthreadManager = serviceResources.getService(PerthreadManager.class);
        VirtualVehicleMigrator migrator = serviceResources.getService(VirtualVehicleMigrator.class);

        String name = Thread.currentThread().getName();
        Thread.currentThread().setName("MIG-RCV-" + name);

        try (InputStream inStream = new ByteArrayInputStream(data))
        {
            migrator.storeChunk(inStream);
        }
        catch (Throwable e)
        {
            LOG.error("Migration receive aborted!", e);
            succeeded = false;
        }
        finally
        {
            perthreadManager.cleanup();
            Thread.currentThread().setName(name);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean executionSucceeded()
    {
        return succeeded;
    }
}
