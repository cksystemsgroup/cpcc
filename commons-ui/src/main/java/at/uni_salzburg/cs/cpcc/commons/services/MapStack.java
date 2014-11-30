/*
 * This code is part of the CPCC-NG project.
 *
 * Copyright (c) 2013 Clemens Krainer <clemens.krainer@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package at.uni_salzburg.cs.cpcc.commons.services;

import org.apache.tapestry5.Asset;
import org.apache.tapestry5.internal.TapestryInternalUtils;
import org.apache.tapestry5.services.AssetSource;
import org.apache.tapestry5.services.javascript.StylesheetLink;

/**
 * MapStack
 */
public class MapStack extends AbstractJavaScriptStack
{
    /**
     * @param assetSource the asset source
     */
    public MapStack(final AssetSource assetSource)
    {
        stylesheets =
            new StylesheetLink[]
            {
                TapestryInternalUtils.assetToStylesheetLink.map(assetSource
                    .getUnlocalizedAsset("/at/uni_salzburg/cs/cpcc/commons/css/leaflet-0.7.2.css")),
                TapestryInternalUtils.assetToStylesheetLink.map(assetSource
                    .getUnlocalizedAsset("/at/uni_salzburg/cs/cpcc/commons/css/Vehicle.css")),
            };

        javaScriptLibraries =
            new Asset[]
            {
                // assetSource.getUnlocalizedAsset("/at/uni_salzburg/cs/cpcc/commons/js/kinetic-v4.4.3.min.js"),
                assetSource.getUnlocalizedAsset("/at/uni_salzburg/cs/cpcc/commons/js/leaflet-0.7.2.min.js"),
                assetSource.getUnlocalizedAsset("/at/uni_salzburg/cs/cpcc/commons/js/underscore-1.4.4.min.js"),
                // assetSource.getUnlocalizedAsset("/at/uni_salzburg/cs/cpcc/commons/js/backbone-1.0.0.min.js"),
                assetSource.getUnlocalizedAsset("/at/uni_salzburg/cs/cpcc/commons/js/Location.js"),
                assetSource.getUnlocalizedAsset("/at/uni_salzburg/cs/cpcc/commons/js/Vehicle.js"),
                assetSource.getUnlocalizedAsset("/at/uni_salzburg/cs/cpcc/commons/js/viewer.js"),
            };
    }

}