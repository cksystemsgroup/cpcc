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

package cpcc.commons.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.FieldPosition;
import java.util.stream.Stream;

import org.apache.tapestry5.commons.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import cpcc.core.entities.RealVehicleType;

class EnumFormatterTest
{
    private EnumFormatter sut;

    @BeforeEach
    void setUp()
    {
        Messages messages = mock(Messages.class);
        when(messages.get("BOAT")).thenReturn("boat");

        sut = new EnumFormatter(messages);
    }

    static Stream<Arguments> enumDataProvider()
    {
        return Stream.of(
            arguments(null, ""),
            arguments(RealVehicleType.BOAT, "boat"));
    }

    @ParameterizedTest
    @MethodSource("enumDataProvider")
    void shouldFormatEnums(Enum<?> data, String expected)
    {
        StringBuffer actual = sut.format(data, new StringBuffer(), new FieldPosition(0));

        assertThat(actual).hasToString(expected);
    }

    @Test
    void shouldReturnNullOnParsing()
    {
        Object actual = sut.parseObject(null, null);

        assertThat(actual).isNull();
    }
}
