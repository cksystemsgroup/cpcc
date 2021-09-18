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

package cpcc.core.services.jobs;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.tapestry5.hibernate.HibernateSessionManager;
import org.apache.tapestry5.ioc.ServiceResources;
import org.apache.tapestry5.ioc.services.PerthreadManager;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import cpcc.core.entities.Job;
import cpcc.core.entities.JobStatus;

class JobQueueTest
{
    private static final int QUICK_JOB_ID = 101;
    private static final int SLOW_JOB_ID = 202;
    private static final int REUSABLE_JOB_ID = 31337;

    private HibernateSessionManager sessionManager;
    private Job quickJob;
    private Job slowJob;
    private boolean jobEnded;
    private JobQueue sut;
    private Date queuedDate;
    private Date startDate;
    private Date endDate;
    private TimeService timeService;
    private Session session;
    private JobRunnableFactory factory;
    private JobRunnable quickJobRunnable;
    private JobRunnable slowJobRunnable;
    private int numberOfPoolThreads;
    private ServiceResources serviceResources;
    private PerthreadManager perthreadManager;
    private JobRepository jobRepository;

    @BeforeEach
    void setUp() throws Exception
    {
        jobEnded = false;
        numberOfPoolThreads = 3;

        queuedDate = mock(Date.class);
        startDate = mock(Date.class);
        endDate = mock(Date.class);

        timeService = mock(TimeService.class);
        when(timeService.newDate()).thenAnswer(new Answer<Date>()
        {
            int counter = 0;
            Date[] dates = {queuedDate, startDate, endDate, queuedDate, startDate, endDate};

            @Override
            public Date answer(InvocationOnMock invocation) throws Throwable
            {
                return counter < dates.length ? dates[counter++] : null;
            }
        });

        session = mock(Session.class);

        sessionManager = mock(HibernateSessionManager.class);
        when(sessionManager.getSession()).thenReturn(session);

        perthreadManager = mock(PerthreadManager.class);

        quickJob = mock(Job.class);
        when(quickJob.getId()).thenReturn(QUICK_JOB_ID);

        quickJobRunnable = mock(JobRunnable.class);
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                jobEnded = true;
                return null;
            }
        }).when(quickJob).setEnd(any(Date.class));

        slowJob = mock(Job.class);
        when(slowJob.getId()).thenReturn(SLOW_JOB_ID);

        slowJobRunnable = mock(JobRunnable.class);
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                TimeUnit.SECONDS.sleep(1);
                return null;
            }
        }).when(slowJobRunnable).run();

        jobRepository = mock(JobRepository.class);
        when(jobRepository.findJobById(QUICK_JOB_ID)).thenReturn(quickJob);
        when(jobRepository.findJobById(SLOW_JOB_ID)).thenReturn(slowJob);

        serviceResources = mock(ServiceResources.class);
        when(serviceResources.getService(PerthreadManager.class)).thenReturn(perthreadManager);
        when(serviceResources.getService(HibernateSessionManager.class)).thenReturn(sessionManager);
        when(serviceResources.getService(TimeService.class)).thenReturn(timeService);
        when(serviceResources.getService(JobRepository.class)).thenReturn(jobRepository);

        factory = mock(JobRunnableFactory.class);
        when(factory.createRunnable(serviceResources, quickJob)).thenReturn(quickJobRunnable);
        when(factory.createRunnable(serviceResources, slowJob)).thenReturn(slowJobRunnable);

        sut = new JobQueue("JQ", sessionManager, timeService, Arrays.asList(factory), numberOfPoolThreads);
        sut.setServiceResources(serviceResources);
    }

    @Test
    void shouldExecuteJob() throws JobExecutionException, InterruptedException
    {
        sut.execute(quickJob);

        int counter = 0;
        while (counter++ < 10 && !jobEnded)
        {
            TimeUnit.SECONDS.sleep(1);
        }

        assertThat(jobEnded)
            .overridingErrorMessage("Job did not terminate within %.1f seconds!", counter / 10.0)
            .isTrue();

        final InOrder inOrder = Mockito.inOrder(session, sessionManager, factory, quickJob);
        inOrder.verify(quickJob).setQueued(queuedDate);
        inOrder.verify(quickJob).setStatus(JobStatus.QUEUED);
        inOrder.verify(session).update(quickJob);
        inOrder.verify(sessionManager).commit();

        inOrder.verify(quickJob).setStart(startDate);
        inOrder.verify(quickJob).setStatus(JobStatus.RUNNING);
        inOrder.verify(session).update(quickJob);
        inOrder.verify(sessionManager).commit();

        inOrder.verify(quickJob).setStatus(JobStatus.OK);
        inOrder.verify(quickJob).setEnd(endDate);
        inOrder.verify(session).update(quickJob);
        inOrder.verify(sessionManager).commit();
    }

    @Test
    void shouldThrowExceptionIfJobAlreadyRuns() throws JobExecutionException
    {
        sut.execute(slowJob);

        catchException(() -> sut.execute(slowJob));

        assertThat((Throwable) caughtException())
            .overridingErrorMessage("Second invocation of execute() does not throw an exception!")
            .isNotNull()
            .isInstanceOf(JobExecutionException.class);
    }

    @Test
    void shouldReExecuteAnAlreadyFinishedJob() throws Exception
    {
        Job reusableJob = new Job();
        reusableJob.setId(REUSABLE_JOB_ID);

        when(jobRepository.findJobById(REUSABLE_JOB_ID)).thenReturn(reusableJob);

        JobRunnable reusableJobRunnable = mock(JobRunnable.class);

        when(factory.createRunnable(serviceResources, reusableJob)).thenReturn(reusableJobRunnable);

        sut.execute(reusableJob);

        await()
            .atMost(Duration.ofSeconds(1))
            .until(() -> reusableJob.getEnd() != null);

        sut.execute(reusableJob);

        await()
            .atMost(Duration.ofSeconds(2))
            .until(() -> reusableJob.getEnd() != null);

        verify(reusableJobRunnable, times(2)).run();

        verify(session, times(6)).update(reusableJob);
        verify(sessionManager, times(6)).commit();

        assertThat(reusableJob.getQueued()).isEqualTo(queuedDate);
        assertThat(reusableJob.getStart()).isEqualTo(startDate);
        assertThat(reusableJob.getEnd()).isEqualTo(endDate);
        assertThat(reusableJob.getStatus()).isEqualTo(JobStatus.OK);
    }
}
