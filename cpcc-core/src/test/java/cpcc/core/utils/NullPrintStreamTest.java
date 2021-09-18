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

package cpcc.core.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * NullPrintStream Test
 */
class NullPrintStreamTest
{
    /**
     * Should behave like a {@code PrintStream}
     */
    @Test
    void shouldBehaveLikeAPrintStream()
    {
        NullPrintStream ps = new NullPrintStream(); // .newInstance();
        assertThat(ps).isNotNull();
        ps.append('c');
        ps.print(123);
        ps.println();
        ps.printf("%d", 123);
        ps.close();
    }
}
