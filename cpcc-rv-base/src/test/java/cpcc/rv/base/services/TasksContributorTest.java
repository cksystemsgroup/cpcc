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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import cpcc.core.entities.PolarCoordinate;
import cpcc.core.entities.SensorDefinition;
import cpcc.core.entities.SensorType;
import cpcc.vvrte.entities.Task;

class TasksContributorTest
{
    private TasksContributor sut;
    private SensorDefinition s1;
    private SensorDefinition s2;
    private Task task;
    private PolarCoordinate taskPosition;
    private PolarCoordinate position;

    @BeforeEach
    void setUp()
    {
        s1 = mock(SensorDefinition.class);
        when(s1.getType()).thenReturn(SensorType.ALTIMETER);

        s2 = mock(SensorDefinition.class);
        when(s2.getType()).thenReturn(SensorType.GPS);

        taskPosition = mock(PolarCoordinate.class);
        when(taskPosition.getLatitude()).thenReturn(1.2);
        when(taskPosition.getLongitude()).thenReturn(3.4);
        when(taskPosition.getAltitude()).thenReturn(5.6);

        task = mock(Task.class);
        when(task.getPosition()).thenReturn(taskPosition);
        when(task.getSensors()).thenReturn(Arrays.asList(s1, s2));

        position = mock(PolarCoordinate.class);
        when(position.getLatitude()).thenReturn(21.6);
        when(position.getLongitude()).thenReturn(33.5);
        when(position.getAltitude()).thenReturn(54.1);

        sut = new TasksContributor();
    }

    static final String EMPTY_PATH_FEATURE = "{\"type\":\"Feature\""
        + ",\"properties\":{\"type\":\"rvPath\"}"
        + ",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[33.5,21.6,54.1]]}}";

    @Test
    void shouldContributeEmptyFeatureCollectionWhenNoTaskExecutes()
        throws JsonProcessingException, JSONException
    {
        FeatureCollection featureCollection = mock(FeatureCollection.class);

        sut.contribute(featureCollection, position, Collections.<Task> emptyList());

        ArgumentCaptor<Feature> captor = ArgumentCaptor.forClass(Feature.class);

        verify(featureCollection).add(captor.capture());

        Feature argument = captor.getValue();

        String actual = new ObjectMapper().disable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(argument);
        // System.out.println("actual: " + actual.replace("\"", "\\\""));

        JSONAssert.assertEquals(EMPTY_PATH_FEATURE, actual, false);
        JSONAssert.assertEquals(actual, EMPTY_PATH_FEATURE, false);
    }

    static final String EXPECTED_01 = "{\"type\":\"Feature\""
        + ",\"properties\":{\"type\":\"rvPath\"}"
        + ",\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[33.5,21.6,54.1],[3.4,1.2,5.6]]}}";

    @Test
    void shouldContributePointFeature() throws JsonProcessingException, JSONException
    {
        FeatureCollection featureCollection = mock(FeatureCollection.class);

        sut.contribute(featureCollection, position, Arrays.asList(task));

        ArgumentCaptor<Feature> captor = ArgumentCaptor.forClass(Feature.class);

        verify(featureCollection).add(captor.capture());

        Feature argument = captor.getValue();
        assertThat(argument).isNotNull();

        String actual = new ObjectMapper().disable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(argument);
        // System.out.println("actual: " + actual.replace("\"", "\\\""));

        JSONAssert.assertEquals(EXPECTED_01, actual, false);
        JSONAssert.assertEquals(actual, EXPECTED_01, false);
    }

    @Test
    void shouldNotContributeOnMissingRvPosition()
    {
        FeatureCollection featureCollection = mock(FeatureCollection.class);

        sut.contribute(featureCollection, null, Arrays.asList(task));

        verifyNoInteractions(featureCollection);
    }
}
