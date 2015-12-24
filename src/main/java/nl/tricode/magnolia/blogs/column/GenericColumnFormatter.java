/**
 *      Tricode Blog module
 *      Is a Blog module for Magnolia CMS.
 *      Copyright (C) 2015  Tricode Business Integrators B.V.
 *
 * 	  This program is free software: you can redistribute it and/or modify
 *		  it under the terms of the GNU General Public License as published by
 *		  the Free Software Foundation, either version 3 of the License, or
 *		  (at your option) any later version.
 *
 *		  This program is distributed in the hope that it will be useful,
 *		  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *		  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *		  GNU General Public License for more details.
 *
 *		  You should have received a copy of the GNU General Public License
 *		  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.tricode.magnolia.blogs.column;

import info.magnolia.jcr.util.NodeTypes;
import info.magnolia.jcr.util.NodeUtil;
import info.magnolia.jcr.util.PropertyUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class GenericColumnFormatter {
	private static final Logger LOG = LoggerFactory.getLogger(GenericColumnFormatter.class);

	public static Object generateCellHelper(Item jcrItem, String nodeTypeName, String propertyTitle) {
		if (jcrItem != null && jcrItem.isNode()) {
			Node node = (Node) jcrItem;

			try {
				if (NodeUtil.isNodeType(node, NodeTypes.Folder.NAME)) {
					return node.getName();
				}

				if (NodeUtil.isNodeType(node, NodeTypes.Deleted.NAME)) {
					return node.getName();
				}

				if (NodeUtil.isNodeType(node, nodeTypeName)) {
					Object result = PropertyUtil.getString(node, propertyTitle, StringUtils.EMPTY);
					if (result != null) {
						return result;
					} else {
						return PropertyUtil.getString(node, propertyTitle, StringUtils.EMPTY);
					}
				}
			} catch (RepositoryException e) {
				LOG.info("Unable to get " + propertyTitle + " of blog for column", e);
			}
		}
		return StringUtils.EMPTY;
	}
}