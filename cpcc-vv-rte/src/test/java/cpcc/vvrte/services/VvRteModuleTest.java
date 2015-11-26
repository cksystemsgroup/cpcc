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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;

import java.lang.reflect.Constructor;

import org.apache.tapestry5.ioc.Configuration;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.ServiceBindingOptions;
import org.apache.tapestry5.ioc.services.cron.CronSchedule;
import org.apache.tapestry5.ioc.services.cron.PeriodicExecutor;
import org.testng.annotations.Test;

import cpcc.com.services.CommunicationService;
import cpcc.vvrte.services.BuiltInFunctionsImpl;
import cpcc.vvrte.services.MessageConverter;
import cpcc.vvrte.services.MessageConverterImpl;
import cpcc.vvrte.services.VirtualVehicleLauncher;
import cpcc.vvrte.services.VirtualVehicleLauncherImpl;
import cpcc.vvrte.services.VirtualVehicleMapper;
import cpcc.vvrte.services.VirtualVehicleMapperImpl;
import cpcc.vvrte.services.VirtualVehicleMigrator;
import cpcc.vvrte.services.VirtualVehicleMigratorImpl;
import cpcc.vvrte.services.VvJsonConverter;
import cpcc.vvrte.services.VvJsonConverterImpl;
import cpcc.vvrte.services.VvRteConstants;
import cpcc.vvrte.services.VvRteModule;
import cpcc.vvrte.services.VvRteRepository;
import cpcc.vvrte.services.VvRteRepositoryImpl;
import cpcc.vvrte.services.js.BuiltInFunctions;
import cpcc.vvrte.services.js.JavascriptService;
import cpcc.vvrte.services.js.JavascriptServiceImpl;
import cpcc.vvrte.task.TaskAnalyzer;
import cpcc.vvrte.task.TaskAnalyzerImpl;
import cpcc.vvrte.task.TaskExecutionService;
import cpcc.vvrte.task.TaskExecutionServiceImpl;
import cpcc.vvrte.task.TaskSchedulerService;
import cpcc.vvrte.task.TaskSchedulerServiceImpl;

/**
 * VvRteModuleTest
 */
public class VvRteModuleTest
{
    @Test
    public void shouldHavePrivateConstructor() throws Exception
    {
        Constructor<VvRteModule> cnt = VvRteModule.class.getDeclaredConstructor();
        assertFalse(cnt.isAccessible());
        cnt.setAccessible(true);
        cnt.newInstance();
    }

    @Test
    public void shouldBindServices()
    {
        ServiceBindingOptions options = mock(ServiceBindingOptions.class);
        ServiceBinder binder = mock(ServiceBinder.class);

        when(binder.bind(BuiltInFunctions.class, BuiltInFunctionsImpl.class)).thenReturn(options);
        when(binder.bind(MessageConverter.class, MessageConverterImpl.class)).thenReturn(options);
        when(binder.bind(VirtualVehicleLauncher.class, VirtualVehicleLauncherImpl.class)).thenReturn(options);
        when(binder.bind(VirtualVehicleMapper.class, VirtualVehicleMapperImpl.class)).thenReturn(options);
        when(binder.bind(VvRteRepository.class, VvRteRepositoryImpl.class)).thenReturn(options);
        when(binder.bind(JavascriptService.class, JavascriptServiceImpl.class)).thenReturn(options);
        when(binder.bind(TaskAnalyzer.class, TaskAnalyzerImpl.class)).thenReturn(options);
        when(binder.bind(TaskExecutionService.class, TaskExecutionServiceImpl.class)).thenReturn(options);
        when(binder.bind(TaskSchedulerService.class, TaskSchedulerServiceImpl.class)).thenReturn(options);
        when(binder.bind(VirtualVehicleMigrator.class, VirtualVehicleMigratorImpl.class)).thenReturn(options);
        when(binder.bind(VvJsonConverter.class, VvJsonConverterImpl.class)).thenReturn(options);

        VvRteModule.bind(binder);

        verify(binder).bind(BuiltInFunctions.class, BuiltInFunctionsImpl.class);
        verify(binder).bind(MessageConverter.class, MessageConverterImpl.class);
        verify(binder).bind(VirtualVehicleLauncher.class, VirtualVehicleLauncherImpl.class);
        verify(binder).bind(VirtualVehicleMapper.class, VirtualVehicleMapperImpl.class);
        verify(binder).bind(VvRteRepository.class, VvRteRepositoryImpl.class);
        verify(binder).bind(JavascriptService.class, JavascriptServiceImpl.class);
        verify(binder).bind(TaskAnalyzer.class, TaskAnalyzerImpl.class);
        verify(binder).bind(TaskExecutionService.class, TaskExecutionServiceImpl.class);
        verify(binder).bind(TaskSchedulerService.class, TaskSchedulerServiceImpl.class);
        verify(binder).bind(VirtualVehicleMigrator.class, VirtualVehicleMigratorImpl.class);
        verify(binder).bind(VvJsonConverter.class, VvJsonConverterImpl.class);
        verify(options, times(9)).eagerLoad();
    }

    @Test
    public void shouldContributeToHibernateEntityPackageManager()
    {
        @SuppressWarnings("unchecked")
        Configuration<String> configuration = mock(Configuration.class);

        VvRteModule.contributeHibernateEntityPackageManager(configuration);

        verify(configuration).add("cpcc.vvrte.entities");
    }

    @Test
    public void shouldSchedulePeriodicJobs()
    {
        PeriodicExecutor executor = mock(PeriodicExecutor.class);
        TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);

        VvRteModule.scheduleJobs(executor, taskExecutionService);

        verify(executor).addJob(any(CronSchedule.class), anyString(), any(Runnable.class));
    }

    @Test
    public void shouldResetVirtualVehicleStates()
    {
        VvRteRepository vvRteRepo = mock(VvRteRepository.class);

        VvRteModule.resetVirtualVehicleStates(vvRteRepo);

        verify(vvRteRepo).resetVirtualVehicleStates();
    }

    @Test
    public void shouldAddMigrationConnectorToCommunicationService()
    {
        CommunicationService communicationService = mock(CommunicationService.class);

        VvRteModule.setupCommunicationService(communicationService);

        verify(communicationService).addConnector(eq(VvRteConstants.MIGRATION_CONNECTOR), anyString());
    }
}