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

package cpcc.vvrte.services.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.mockito.InOrder;
import org.slf4j.Logger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import cpcc.core.entities.PolarCoordinate;
import cpcc.vvrte.base.VvRteConstants;
import cpcc.vvrte.entities.Task;
import cpcc.vvrte.entities.TaskState;
import cpcc.vvrte.services.db.TaskRepository;

/**
 * TaskSchedulerServiceTest
 */
public class TaskSchedulerServiceTest
{
    private PolarCoordinate rvPosition;

    private PolarCoordinate posA;
    private PolarCoordinate posB;
    private PolarCoordinate posC;
    private PolarCoordinate posD;

    private Task taskA;
    private Task taskB;
    private Task taskC;
    private Task taskD;

    private ArrayList<Task> scheduledTasks;
    private ArrayList<Task> pendingTasks;

    private TaskSchedulerServiceImpl scheduler;
    private Logger logger = mock(Logger.class);
    private Session session;
    private TaskRepository taskRepository;

    private List<PolarCoordinate> depotList;

    @BeforeMethod
    public void setUp()
    {
        depotList = Collections.<PolarCoordinate> emptyList();

        rvPosition = mock(PolarCoordinate.class);

        posA = mock(PolarCoordinate.class);
        when(posA.getLatitude()).thenReturn(47.1234);
        when(posA.getLongitude()).thenReturn(13.7897);
        when(posA.getAltitude()).thenReturn(8.0);

        taskA = mock(Task.class);
        when(taskA.getPosition()).thenReturn(posA);
        when(taskA.getCreationTime()).thenReturn(new Date(1L));
        when(taskA.toString()).thenReturn("taskA (47.1234, 13.7897, 8), time=1");

        posB = mock(PolarCoordinate.class);
        when(posB.getLatitude()).thenReturn(47.2345);
        when(posB.getLongitude()).thenReturn(13.1234);
        when(posB.getAltitude()).thenReturn(23.0);

        taskB = mock(Task.class);
        when(taskB.getPosition()).thenReturn(posB);
        when(taskB.getCreationTime()).thenReturn(new Date(2L));
        when(taskB.toString()).thenReturn("taskB (47.2345, 13.1234, 23), time=2");

        posC = mock(PolarCoordinate.class);
        when(posC.getLatitude()).thenReturn(47.3345);
        when(posC.getLongitude()).thenReturn(13.5234);
        when(posC.getAltitude()).thenReturn(13.0);

        taskC = mock(Task.class);
        when(taskC.getPosition()).thenReturn(posC);
        when(taskC.getCreationTime()).thenReturn(new Date(3L));
        when(taskC.toString()).thenReturn("taskC (47.3345, 13.5234, 13), time=3");

        posD = mock(PolarCoordinate.class);
        when(posD.getLatitude()).thenReturn(47.4345);
        when(posD.getLongitude()).thenReturn(13.3234);
        when(posD.getAltitude()).thenReturn(18.0);

        taskD = mock(Task.class);
        when(taskD.getPosition()).thenReturn(posD);
        when(taskD.getCreationTime()).thenReturn(new Date(4L));
        when(taskD.toString()).thenReturn("taskD (47.4345, 13.3234, 18), time=4");

        scheduledTasks = new ArrayList<Task>();
        assertThat(scheduledTasks).isNotNull().isEmpty();

        pendingTasks = new ArrayList<Task>();
        assertThat(pendingTasks).isNotNull().isEmpty();

        session = mock(Session.class);

        taskRepository = mock(TaskRepository.class);
        when(taskRepository.getScheduledTasks()).thenReturn(scheduledTasks);
        when(taskRepository.getPendingTasks()).thenReturn(pendingTasks);

        scheduler = new TaskSchedulerServiceImpl(VvRteConstants.PROP_DEFAULT_SCHEDULER_CLASS_NAME
            , logger, session, taskRepository);

        assertThat(scheduler).isNotNull();
    }

    @Test
    public void shouldHaveDefaultSchedulingAlgorithm()
    {
        pendingTasks.addAll(Arrays.asList(taskA, taskB, taskC, taskD));
        when(taskRepository.getCurrentRunningTask()).thenReturn(null);

        Task actual = scheduler.schedule(rvPosition, depotList);

        assertThat(actual).isNotNull();

        verify(session).update(taskA);
        verify(session).update(taskB);
        verify(session).update(taskC);
        verify(session).update(taskD);
    }

    @Test
    public void shouldNotRescheduleTasksIfNoneIsPending()
    {
        scheduledTasks.addAll(Arrays.asList(taskA, taskB, taskC, taskD));
        when(taskRepository.getCurrentRunningTask()).thenReturn(null);

        Task actual = scheduler.schedule(rvPosition, depotList);

        assertThat(actual).isSameAs(taskA);

        verify(taskA).setOrder(0);
        verify(taskA).setTaskState(TaskState.RUNNING);
        verify(session).update(taskA);

        verify(taskRepository).getCurrentRunningTask();
        verify(taskRepository).getScheduledTasks();
        verify(taskRepository).getPendingTasks();
        verifyZeroInteractions(logger);

    }

    @Test
    public void shouldLoadSchedulingAlgorithm() throws Exception
    {
        pendingTasks.addAll(Arrays.asList(taskA, taskB, taskC, taskD));
        when(taskRepository.getCurrentRunningTask()).thenReturn(null);

        scheduler.setAlgorithm(ReverseScheduler.class.getName());

        Task actual = scheduler.schedule(rvPosition, depotList);

        assertThat(actual).isSameAs(taskD);

        InOrder io = inOrder(taskA, taskB, taskC, taskD, session);

        io.verify(taskD).setOrder(1);
        io.verify(taskD).setTaskState(TaskState.SCHEDULED);

        io.verify(taskC).setOrder(2);
        io.verify(taskC).setTaskState(TaskState.SCHEDULED);

        io.verify(taskB).setOrder(3);
        io.verify(taskB).setTaskState(TaskState.SCHEDULED);

        io.verify(taskA).setOrder(4);
        io.verify(taskA).setTaskState(TaskState.SCHEDULED);

        io.verify(taskD).setOrder(0);
        io.verify(taskD).setTaskState(TaskState.RUNNING);

        io.verify(session).update(taskD);
        io.verify(session).update(taskC);
        io.verify(session).update(taskB);
        io.verify(session).update(taskA);
    }

    @Test
    public void shouldScheduleCurrentRunningTaskFirst()
    {
        scheduledTasks.addAll(Arrays.asList(taskB));
        pendingTasks.addAll(Arrays.asList(taskC, taskD));

        when(taskRepository.getCurrentRunningTask()).thenReturn(taskA);

        Task actual = scheduler.schedule(rvPosition, depotList);

        assertThat(actual).isSameAs(taskA);

        verify(taskRepository).getCurrentRunningTask();
        verifyZeroInteractions(taskA);
        verifyZeroInteractions(taskB);
        verifyZeroInteractions(taskC);
        verifyZeroInteractions(taskD);
        verifyZeroInteractions(session);
        verifyZeroInteractions(logger);
    }

    @Test
    public void shouldReturnNullOnNoTasksToHandle()
    {
        Task actual = scheduler.schedule(rvPosition, depotList);

        assertThat(actual).isNull();

        verify(taskRepository).getCurrentRunningTask();
        verify(taskRepository).getScheduledTasks();
        verify(taskRepository).getPendingTasks();
        verifyZeroInteractions(session);
        verifyZeroInteractions(logger);
    }

    @Test
    public void shouldLogNotExistingSchedulingAlgorithm() throws Exception
    {
        pendingTasks.addAll(Arrays.asList(taskA, taskB));
        scheduledTasks.addAll(Arrays.asList(taskC, taskD));

        when(taskRepository.getCurrentRunningTask()).thenReturn(null);

        TaskSchedulerServiceImpl scheduler2 =
            new TaskSchedulerServiceImpl("cpcc.notExistingAlgorithmImpl", logger, session, taskRepository);

        verify(logger).error(anyString(), any(ClassNotFoundException.class));

        Task actual = scheduler2.schedule(rvPosition, depotList);

        assertThat(actual).isNull();

        verify(logger).error(anyString());
        verifyZeroInteractions(taskA);
        verifyZeroInteractions(taskB);
        verifyZeroInteractions(taskC);
        verifyZeroInteractions(taskD);
        verifyZeroInteractions(session);
        verifyZeroInteractions(taskRepository);
    }

    /**
     * ReverseScheduler
     */
    public static class ReverseScheduler implements TaskSchedulingAlgorithm
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean schedule(PolarCoordinate position, List<PolarCoordinate> depot, List<Task> scheduledTasks
            , List<Task> pendingTasks)
        {
            for (int k = pendingTasks.size(); k > 0; --k)
            {
                scheduledTasks.add(pendingTasks.get(k - 1));
            }

            pendingTasks.clear();
            return true;
        }
    }
}