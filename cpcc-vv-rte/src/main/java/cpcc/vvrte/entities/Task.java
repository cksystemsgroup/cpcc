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

package cpcc.vvrte.entities;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.SerializationUtils;
import org.mozilla.javascript.ScriptableObject;

import cpcc.core.entities.PolarCoordinate;
import cpcc.core.entities.SensorDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Task
 */
@Entity
@Table(name = "tasks")
public class Task implements Serializable
{
    private static final long serialVersionUID = -3100648860303007085L;

    @Id
    @GeneratedValue
    private Integer id;

    @Embedded
    private PolarCoordinate position;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_state", nullable = false)
    private TaskState taskState = TaskState.INIT;

    @Column(name = "task_order", nullable = false)
    private int order = 0;

    @Column(name = "tolerance", nullable = false)
    private double tolerance = 5.0f;

    @Column(name = "distance_to_target")
    private Double distanceToTarget = null;

    @Column(name = "creation_time", nullable = false)
    private Date creationTime = new Date();

    @Column(name = "execution_start")
    private Date executionStart;

    @Column(name = "execution_end")
    private Date executionEnd;

    @Lob
    @Column(name = "sensor_values")
    private byte[] sensorValues;

    @ManyToMany(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinTable(name = "tasks_sensors", joinColumns = {@JoinColumn(name = "task_id")},
        inverseJoinColumns = {@JoinColumn(name = "sensor_id")})
    private List<SensorDefinition> sensors = new ArrayList<>();

    @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.EAGER)
    @JoinColumn(name = "vehicle_id", referencedColumnName = "id")
    private VirtualVehicle vehicle;

    /**
     * @return the id
     */
    public Integer getId()
    {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Integer id)
    {
        this.id = id;
    }

    /**
     * @return the position.
     */
    public PolarCoordinate getPosition()
    {
        return position;
    }

    /**
     * @param position the position to set.
     */
    public void setPosition(PolarCoordinate position)
    {
        this.position = position;
    }

    /**
     * @return the task state.
     */
    public TaskState getTaskState()
    {
        return taskState;
    }

    /**
     * @param taskState the task state to set.
     */
    public void setTaskState(TaskState taskState)
    {
        this.taskState = taskState;
    }

    /**
     * @return the scheduling order number of this task.
     */
    public int getOrder()
    {
        return order;
    }

    /**
     * @param order the order number to set.
     */
    public void setOrder(int order)
    {
        this.order = order;
    }

    /**
     * @return the tolerance distance
     */
    public double getTolerance()
    {
        return tolerance;
    }

    /**
     * @param tolerance the tolerance distance to set
     */
    public void setTolerance(double tolerance)
    {
        this.tolerance = tolerance;
    }

    /**
     * @return the distance to the target position.
     */
    public Double getDistanceToTarget()
    {
        return distanceToTarget;
    }

    /**
     * @param distanceToTarget the distance to the target position to set.
     */
    public void setDistanceToTarget(Double distanceToTarget)
    {
        this.distanceToTarget = distanceToTarget;
    }

    /**
     * @return the creation date.
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Exposed on purpose")
    public Date getCreationTime()
    {
        return creationTime;
    }

    /**
     * @param creationTime the creation time to set.
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Exposed on purpose")
    public void setCreationTime(Date creationTime)
    {
        this.creationTime = creationTime;
    }

    /**
     * @return the execution start time.
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Exposed on purpose")
    public Date getExecutionStart()
    {
        return executionStart;
    }

    /**
     * @param executionStart the execution start time to set.
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Exposed on purpose")
    public void setExecutionStart(Date executionStart)
    {
        this.executionStart = executionStart;
    }

    /**
     * @return the execution end time.
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Exposed on purpose")
    public Date getExecutionEnd()
    {
        return executionEnd;
    }

    /**
     * @param executionEnd the execution end time to set.
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Exposed on purpose")
    public void setExecutionEnd(Date executionEnd)
    {
        this.executionEnd = executionEnd;
    }

    /**
     * @return the sensors
     */
    public List<SensorDefinition> getSensors()
    {
        return sensors;
    }

    /**
     * @return the sensor values.
     */
    public ScriptableObject getSensorValues()
    {
        return (ScriptableObject) SerializationUtils.deserialize(sensorValues);
    }

    /**
     * @param sensorValues the sensor values to set.
     */
    public void setSensorValues(ScriptableObject sensorValues)
    {
        this.sensorValues = SerializationUtils.serialize(sensorValues);
    }

    /**
     * @return the vehicle.
     */
    public VirtualVehicle getVehicle()
    {
        return vehicle;
    }

    /**
     * @param vehicle the vehicle to set.
     */
    public void setVehicle(VirtualVehicle vehicle)
    {
        this.vehicle = vehicle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return "created " + sdf.format(creationTime)
            + ", started " + (executionStart != null ? sdf.format(executionStart) : "-")
            + ", ended " + (executionEnd != null ? sdf.format(executionEnd) : "-")
            + ", pos " + position
            + ", state " + taskState
            + ", order " + order
            + ", tolerance " + tolerance
            + ", " + (vehicle != null ? "VV " + vehicle.getName() + " (" + vehicle.getUuid() + ")" : "");
    }
}
