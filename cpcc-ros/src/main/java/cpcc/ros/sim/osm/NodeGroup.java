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

package cpcc.ros.sim.osm;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ros.node.DefaultNodeMainExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpcc.ros.sim.AbstractRosNodeGroup;
import cpcc.ros.sim.AnonymousNodeMain;
import sensor_msgs.NavSatFix;

/**
 * NodeGroup
 */
public class NodeGroup extends AbstractRosNodeGroup
{
    private static final Logger LOG = LoggerFactory.getLogger(NodeGroup.class);

    private AnonymousNodeMain<sensor_msgs.NavSatFix> imagePublisherNode;
    private AnonymousNodeMain<sensor_msgs.NavSatFix> listenerNode;

    /**
     * {@inheritDoc}
     */
    @Override
    public void start()
    {
        LOG.info("start()");

        getConfig().put("topicRoot", Arrays.asList(getTopicRoot()));

        Configuration config = new Configuration(getNodeConfiguration(), getConfig());

        imagePublisherNode = new ImagePublisherNode(config);
        listenerNode = new GpsListenerNode(config, imagePublisherNode);

        DefaultNodeMainExecutor.newDefault().execute(imagePublisherNode, getNodeConfiguration());
        DefaultNodeMainExecutor.newDefault().execute(listenerNode, getNodeConfiguration());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown()
    {
        LOG.info("shutdown()");
        DefaultNodeMainExecutor.newDefault().shutdownNodeMain(listenerNode);
        DefaultNodeMainExecutor.newDefault().shutdownNodeMain(imagePublisherNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, List<String>> getCurrentState()
    {
        Map<String, List<String>> map = super.getCurrentState();
        NavSatFix pos = imagePublisherNode.getReceivedMessage();
        if (pos != null)
        {
            map.put("image.position", Arrays.asList(
                String.format(Locale.US, "%.8f", pos.getLatitude()),
                String.format(Locale.US, "%.8f", pos.getLongitude()),
                String.format(Locale.US, "%.3f", pos.getAltitude())));
        }

        pos = listenerNode.getReceivedMessage();
        if (pos != null)
        {
            map.put("gps.position", Arrays.asList(
                String.format(Locale.US, "%.8f", pos.getLatitude()),
                String.format(Locale.US, "%.8f", pos.getLongitude()),
                String.format(Locale.US, "%.3f", pos.getAltitude())));
        }

        return map;
    }
}
