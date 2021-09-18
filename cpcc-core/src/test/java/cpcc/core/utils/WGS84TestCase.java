// This code is part of the JNavigator project.
//
// Copyright (c) 2009-2013 Clemens Krainer <clemens.krainer@gmail.com>
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

package cpcc.core.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.geojson.Polygon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import cpcc.core.entities.PolarCoordinate;

/**
 * This class verifies the implementation of the WGS84 class.
 */
class WGS84TestCase
{
    GeodeticSystem sut;

    @BeforeEach
    void setUp()
    {
        sut = new WGS84();
    }

    static Stream<Arguments> polarAndRectangularDataProvider()
    {
        return Stream.of(
            arguments(
                new PolarCoordinate(47.99043439493213, 12.93670580800686, 435.94417220),
                new CartesianCoordinate(4168246.0564496145, 957466.6063627704, 4716488.496489645)));
    };

    /**
     * This test case verifies the implementation of the rectangularToPolarCoordinates() and
     * polarToRectangularCoordinates() methods of class WGS84.
     */
    @ParameterizedTest
    @MethodSource("polarAndRectangularDataProvider")
    void shouldConvertCoodinates(PolarCoordinate wgs, CartesianCoordinate rect)
    {
        PolarCoordinate pos = sut.rectangularToPolarCoordinates(rect);
        assertThat(pos.getLatitude()).isEqualTo(wgs.getLatitude(), offset(1E-4));
        assertThat(pos.getLongitude()).isEqualTo(wgs.getLongitude(), offset(1E-4));
        assertThat(pos.getAltitude()).isEqualTo(wgs.getAltitude(), offset(1E-4));

        CartesianCoordinate rec = sut.polarToRectangularCoordinates(wgs);
        assertThat(rec.getX()).isEqualTo(rect.getX(), offset(1E-4));
        assertThat(rec.getY()).isEqualTo(rect.getY(), offset(1E-4));
        assertThat(rec.getZ()).isEqualTo(rect.getZ(), offset(1E-4));
    }

    /**
     * This test calculates the elevation of a course.
     */
    @Test
    void shouldCalculateElevation()
    {
        PolarCoordinate A = new PolarCoordinate(48, 13, 1010);
        PolarCoordinate B = new PolarCoordinate(48.001, 13.002, 1000);
        CartesianCoordinate a = sut.polarToRectangularCoordinates(A);
        CartesianCoordinate b = sut.polarToRectangularCoordinates(B);

        CartesianCoordinate mv = b.subtract(a);
        double distance = mv.norm();

        double x = a.multiply(mv) / (a.norm() * distance);
        if (x > 1)
            x = 1;
        else if (x < -1)
            x = -1;
        double elevation = Math.toDegrees(Math.asin(x));

        assertThat(elevation).isEqualTo(-3.1902590577710233, offset(1E-9));
    }

    static Stream<Arguments> walkAroundDataProvider()
    {
        return Stream.of(
            arguments(
                new PolarCoordinate(48, 13, 1010),
                new CartesianCoordinate(100, 0, 0),
                new PolarCoordinate(47.99910182694469, 13, 1010)),
            arguments(
                new PolarCoordinate(48, 13, 1010),
                new CartesianCoordinate(0, 100, 0),
                new PolarCoordinate(48, 13.001342298568892, 1010)),
            arguments(
                new PolarCoordinate(48, 13, 1010),
                new CartesianCoordinate(0, 0, 100),
                new PolarCoordinate(48, 13, 1110)));
    };

    /**
     * This test verifies the walk() method.
     */
    @ParameterizedTest
    @MethodSource("walkAroundDataProvider")
    void shouldWalkAround(PolarCoordinate startPos, CartesianCoordinate way, PolarCoordinate destPos)
    {
        PolarCoordinate b = sut.walk(startPos, way.getX(), way.getY(), way.getZ());
        assertThat(b.getLatitude()).isEqualTo(destPos.getLatitude(), offset(1E-8));
        assertThat(b.getLongitude()).isEqualTo(destPos.getLongitude(), offset(1E-8));
        assertThat(b.getAltitude()).isEqualTo(destPos.getAltitude(), offset(1E-8));
    }

    static Stream<Arguments> polarCoordinatesDataProvider()
    {
        return Stream.of(
            arguments(new PolarCoordinate(48.001, 13.002, 10)),
            arguments(new PolarCoordinate(48.001, -12.002, 10)),
            arguments(new PolarCoordinate(48.001, -80.002, 10)),
            arguments(new PolarCoordinate(48.001, -122.002, 10)),
            arguments(new PolarCoordinate(-89.001, 80.002, 10)),
            arguments(new PolarCoordinate(89.001, 80.002, 10)),
            arguments(new PolarCoordinate(-89.001, 179.992, 10)),
            arguments(new PolarCoordinate(-89.001, -179.992, 10)),
            arguments(new PolarCoordinate(89.001, -179.992, 10)),
            arguments(new PolarCoordinate(-89.001, 44.992, 10)),
            arguments(new PolarCoordinate(-89.001, -44.992, 10)));
    };

    @ParameterizedTest
    @MethodSource("polarCoordinatesDataProvider")
    void shouldConvertCoordinatesToAndFro(PolarCoordinate pos)
    {
        CartesianCoordinate cartA = sut.polarToRectangularCoordinates(pos);
        PolarCoordinate posA = sut.rectangularToPolarCoordinates(cartA);

        assertThat(posA.getLatitude())
            .overridingErrorMessage("Latitude")
            .isEqualTo(pos.getLatitude(), offset(1E-8));
        assertThat(posA.getLongitude())
            .overridingErrorMessage("Longitude")
            .isEqualTo(pos.getLongitude(), offset(1E-8));
        assertThat(posA.getAltitude())
            .overridingErrorMessage("Altitude")
            .isEqualTo(pos.getAltitude(), offset(1E-4));
    }

    static Stream<Arguments> specialCasesDataProvider()
    {
        return Stream.of(
            arguments(
                new CartesianCoordinate(0, 0, 1),
                new PolarCoordinate(90.0, 0.0, 1.0 - 6356752.3142)),
            arguments(
                new CartesianCoordinate(0, 0, -1),
                new PolarCoordinate(-90.0, 0.0, -1.0 + 6356752.3142)),
            arguments(
                new CartesianCoordinate(0, 1, 1),
                new PolarCoordinate(-45.38862666932452, 90.0, -6388982.402021714)),
            arguments(
                new CartesianCoordinate(1, 0, 1),
                new PolarCoordinate(-45.38862666932452, 0.0, -6388982.402021714)));
    };

    @ParameterizedTest
    @MethodSource("specialCasesDataProvider")
    void shouldConsiderConvertingOfSpecialCases(CartesianCoordinate cartA, PolarCoordinate posA)
    {
        PolarCoordinate result = sut.rectangularToPolarCoordinates(cartA);
        assertThat(result.getLatitude())
            .overridingErrorMessage("Latitude")
            .isEqualTo(posA.getLatitude(), offset(1E-3));
        assertThat(result.getLongitude())
            .overridingErrorMessage("Longitude")
            .isEqualTo(posA.getLongitude(), offset(1E-3));
        assertThat(result.getAltitude())
            .overridingErrorMessage("Altitude")
            .isEqualTo(posA.getAltitude(), offset(1E-3));
    }

    static Stream<Arguments> distancesDataProvider()
    {
        return Stream.of(
            arguments(
                new PolarCoordinate(48.0, 13.0, 1010.0),
                new PolarCoordinate(47.99910182694469, 13.0, 1010.0),
                100.0D, 2E-1),
            arguments(
                new PolarCoordinate(48.0, 13.0, 1010.0),
                new PolarCoordinate(48.0, 13.001342298568892, 1010.0),
                100.0D, 2E-1),
            arguments(
                new PolarCoordinate(48.0, 13.0, 1010.0),
                new PolarCoordinate(48.0, 13.0, 1110.0),
                100.0D, 1E-4));
    };

    @ParameterizedTest
    @MethodSource("distancesDataProvider")
    void shouldCalculateDistances(PolarCoordinate a, PolarCoordinate b, double distance, double delta)
    {
        assertThat(sut.calculateDistance(a, b)).isEqualTo(distance, offset(delta));
    }

    private static final double COS_60 = 0.5;
    private static final double SIN_60 = 0.8660;
    private static final double DIAMETER = 100.0;

    static Stream<Arguments> startPositionDataProvider()
    {
        WGS84 mygs = new WGS84();
        PolarCoordinate origin = new PolarCoordinate(47.8220, 13.0408, 50);
        PolarCoordinate rv01 = mygs.walk(origin, -DIAMETER, -DIAMETER * (2.0 + COS_60), 0.0);
        PolarCoordinate rv02 = mygs.walk(rv01, -DIAMETER * SIN_60, DIAMETER * (1.0 + COS_60), 0.0);
        PolarCoordinate rv03 = mygs.walk(rv01, 0.0, DIAMETER * 3.0, 0.0);
        PolarCoordinate rv04 = mygs.walk(rv01, 2.0 * DIAMETER * SIN_60, DIAMETER * 3.0, 0.0);
        PolarCoordinate rv05 = mygs.walk(rv01, 3.0 * DIAMETER * SIN_60, DIAMETER * (1.0 + COS_60), 0.0);
        PolarCoordinate rv06 = mygs.walk(rv01, 2.0 * DIAMETER * SIN_60, 0.0, 0.0);
        PolarCoordinate rv07 = mygs.walk(rv01, DIAMETER * SIN_60, DIAMETER * (1.0 + COS_60), 0.0);

        return Stream.of(
            arguments("RV01", rv01),
            arguments("RV02", rv02),
            arguments("RV03", rv03),
            arguments("RV04", rv04),
            arguments("RV05", rv05),
            arguments("RV06", rv06),
            arguments("RV07", rv07));
    }

    //    @ParameterizedTest
    //    @MethodSource("startPositionDataProvider")
    void shouldRunAroundALittleBit(String name, PolarCoordinate startPosition)
        throws JsonProcessingException
    {
        PolarCoordinate depotPos = sut.walk(startPosition, 0.0, DIAMETER, 0.0);
        PolarCoordinate p1 = sut.walk(startPosition, -DIAMETER * SIN_60, DIAMETER * COS_60, 0.0);
        PolarCoordinate p2 = sut.walk(startPosition, -DIAMETER * SIN_60, DIAMETER * COS_60 + DIAMETER, 0.0);
        PolarCoordinate p3 = sut.walk(startPosition, 0.0, DIAMETER * 2.0, 0.0);
        PolarCoordinate p4 = sut.walk(startPosition, DIAMETER * SIN_60, DIAMETER * COS_60 + DIAMETER, 0.0);
        PolarCoordinate p5 = sut.walk(startPosition, DIAMETER * SIN_60, DIAMETER * COS_60, 0.0);

        Polygon poly = new Polygon();
        poly.add(Arrays.asList(toLLA(startPosition), toLLA(p1), toLLA(p2), toLLA(p3), toLLA(p4), toLLA(p5),
            toLLA(startPosition)));

        Feature aooPolygon = new Feature();
        aooPolygon.setGeometry(poly);
        aooPolygon.setProperty("minAlt", 0);
        aooPolygon.setProperty("maxAlt", 20);

        Point depotPoint = new Point();
        depotPoint.setCoordinates(toLLA(depotPos));

        Feature depot = new Feature();
        depot.setProperty("type", "depot");
        depot.setGeometry(depotPoint);

        FeatureCollection fc = new FeatureCollection();
        fc.setProperty("center", toLL(depotPos));
        fc.setProperty("zoom", 18);
        fc.setProperty("layer", "Minimal");
        fc.add(aooPolygon);
        fc.add(depot);

        String actual = new ObjectMapper().disable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(fc);
        System.out.println("var " + name + " = " + actual);
        System.out.println();
    }

    private static LngLatAlt toLLA(PolarCoordinate pos)
    {
        // return new LngLatAlt(pos.getLongitude(), pos.getLatitude(), pos.getAltitude());
        return new LngLatAlt(pos.getLongitude(), pos.getLatitude());
    }

    @SuppressWarnings("serial")
    private static Map<String, Object> toLL(PolarCoordinate pos)
    {
        return new HashMap<String, Object>(2)
        {
            {
                put("lat", pos.getLatitude());
                put("lng", pos.getLongitude());
            }
        };
    }
}
