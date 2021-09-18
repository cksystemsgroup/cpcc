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

package cpcc.commons.pages.ros;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.inject.Inject;

import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.PageActivationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpcc.core.utils.PngImageStreamResponse;
import cpcc.ros.base.AbstractRosAdapter;
import cpcc.ros.sensors.ImageProvider;
import cpcc.ros.services.RosImageConverter;
import cpcc.ros.services.RosNodeService;

/**
 * RosCameraImage
 */
public class RosCameraImage
{
    private static final Logger LOG = LoggerFactory.getLogger(RosCameraImage.class);

    @Inject
    private RosNodeService rns;

    @Inject
    private RosImageConverter imageConverter;

    @PageActivationContext
    private String ctx;

    /**
     * @param rootTopicParam the root topic.
     * @return the currently available camera snapshot as <code>StreamResponse</code> image.
     */
    public StreamResponse onActivate(String rootTopicParam)
    {
        String rootTopic = rootTopicParam.replace(".", "/");

        if (LOG.isDebugEnabled())
        {
            LOG.debug("ctx={}, rootTopic={}", ctx, rootTopic);
            LOG.debug("Preparing image for root topic {}", rootTopicParam);
        }

        AbstractRosAdapter adapter = rns.getAdapterNodeByTopic(rootTopic);

        sensor_msgs.Image image = null;
        if (adapter instanceof ImageProvider)
        {
            image = ((ImageProvider) adapter).getImage();
        }

        if (image == null)
        {
            LOG.error("No image adapter found for root topic {}", rootTopic);
            return new PngImageStreamResponse();
        }

        BufferedImage bufferedImage = imageConverter.messageToBufferedImage(image);

        try
        {
            return PngImageStreamResponse.convertImageToStreamResponse(bufferedImage);
        }
        catch (IOException e)
        {
            LOG.error("Can not convert image to StreamResponse.", e);
            return new PngImageStreamResponse();
        }
    }

}
