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

package cpcc.core.entities;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Parameter
 */
@Entity
@Table(name = "parameters")
public class Parameter implements Serializable
{
    private static final long serialVersionUID = -3149631712982577428L;

    public static final String MASTER_SERVER_URI = "masterServerURI";
    public static final String USE_INTERNAL_ROS_CORE = "useInternalRosCore";
    public static final String REAL_VEHICLE_NAME = "realVehicleName";

    @Id
    @GeneratedValue
    private Integer id;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 255, name = "STRING_VALUE")
    private String value;

    @Column(nullable = false)
    private Integer sort;

    public Parameter()
    {
        // Intentionally empty.
    }

    public Parameter(Integer id, String name, String value, Integer sort)
    {
        this.id = id;
        this.name = name;
        this.value = value;
        this.sort = sort;
    }

    /**
     * @return the parameter ID.
     */
    public Integer getId()
    {
        return id;
    }

    /**
     * @param id the parameter ID.
     */
    public void setId(Integer id)
    {
        this.id = id;
    }

    /**
     * @return the parameter name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name the parameter name.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the parameter value.
     */
    public String getValue()
    {
        return value;
    }

    /**
     * @param value the parameter value.
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    /**
     * @return the sorting rank.
     */
    public Integer getSort()
    {
        return sort;
    }

    /**
     * @param sort the sorting rank.
     */
    public void setSort(Integer sort)
    {
        this.sort = sort;
    }

}
