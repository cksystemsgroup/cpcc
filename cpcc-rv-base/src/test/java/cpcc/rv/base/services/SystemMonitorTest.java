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

package cpcc.rv.base.services;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

/**
 * SystemMonitorTest implementation.
 */
public class SystemMonitorTest
{
    private Logger logger;
    private SystemMonitor sut;
    private Map<String, Object> config = new HashMap<>();

    @BeforeEach
    public void setUp()
    {
        logger = mock(Logger.class);

        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                System.out.println(invocation.getArgument(0, String.class));
                return null;
            }
        }).when(logger).info(anyString());

        config.put("logger", logger);

        sut = new SystemMonitorImpl(config);
    }

    @Test
    public void shouldWriteMonitoringMessages()
    {
        sut.writeLogEntry();
        sut.writeLogEntry();
        sut.writeLogEntry();
        sut.writeLogEntry();

        InOrder o = inOrder(logger);

        o.verify(logger).info(eq(";SH;{}"), eq("osName;osVersion;processors;jvmName;jvmArch;startTime"));
        o.verify(logger).info(eq(";SV;{}"), anyString());
        o.verify(logger).info(matches(";H;time;uptime;processCpuTime;processCpuLoad;systemCpuLoad;systemLoadAvg;"
            + "openFileCount;threadCount;peakThreadCount;daemonThreadCount;totalStartedTheadCount;"
            + "freeMemory;maxMemory;totalMemory;heap.init;heap.used;heap.max;heap.committed;"
            + "nonheap.init;nonheap.used;nonheap.max;nonheap.committed;objPendingFinCount;.*"));

        o.verify(logger, times(4)).info(eq(";V;{}"), anyString());
        o.verifyNoMoreInteractions();
    }
}
