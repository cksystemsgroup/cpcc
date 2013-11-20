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
package at.uni_salzburg.cs.cpcc.rv.components;

import java.util.ArrayList;
import java.util.List;

import org.apache.tapestry5.ValueEncoder;
import org.apache.tapestry5.annotations.Component;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SessionState;
import org.apache.tapestry5.corelib.components.PageLink;
import org.apache.tapestry5.tree.DefaultTreeExpansionModel;
import org.apache.tapestry5.tree.DefaultTreeModel;
import org.apache.tapestry5.tree.TreeExpansionModel;
import org.apache.tapestry5.tree.TreeModel;
import org.apache.tapestry5.tree.TreeNode;

import at.uni_salzburg.cs.cpcc.persistence.entities.Device;
import at.uni_salzburg.cs.cpcc.persistence.entities.ITreeNode;
import at.uni_salzburg.cs.cpcc.persistence.entities.TreeNodeAdapter;

/**
 * DeviceTree
 */
public class DeviceTree
{
    @Parameter
    private Iterable<Device> devices;

    @Component(parameters = {"page=ros/deviceDetail", "context=deviceDetailLinkContext"})
    private PageLink deviceDetailLink;

    @Property
    private TreeNode<ITreeNode> treeNode;

    @Property
    private ITreeNode currentNode;

    @SessionState(create = false)
    private TreeExpansionModel<ITreeNode> expansionModel;

    /**
     * @return the tree model
     */
    public TreeModel<ITreeNode> getTreeModel()
    {
        TreeModel<ITreeNode> treeModel;

        ValueEncoder<ITreeNode> encoder = new ValueEncoder<ITreeNode>()
        {
            @Override
            public String toClient(ITreeNode value)
            {
                return value.getLabel();
            }
            @Override
            public ITreeNode toValue(String clientValue)
            {
                return null;
            }
        };

        List<ITreeNode> ms = new ArrayList<ITreeNode>();
        for (Device m : devices)
        {
            ms.add(m);
        }

        treeModel = new DefaultTreeModel<ITreeNode>(encoder, new TreeNodeAdapter(), ms);
        return treeModel;
    }

    /**
     * @return the expansion model.
     */
    public TreeExpansionModel<ITreeNode> getExpansionModel()
    {
        if (expansionModel == null)
        {
            expansionModel = new DefaultTreeExpansionModel<ITreeNode>();
        }

        return expansionModel;
    }

    /**
     * @return the device detail link context.
     */
    public Object[] getDeviceDetailLinkContext()
    {
        return new Object[]{currentNode.getLabel()};
    }

}