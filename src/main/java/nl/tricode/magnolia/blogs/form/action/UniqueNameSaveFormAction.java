/*
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
package nl.tricode.magnolia.blogs.form.action;

import info.magnolia.jcr.util.NodeUtil;
import info.magnolia.ui.form.EditorCallback;
import info.magnolia.ui.form.EditorValidator;
import info.magnolia.ui.form.action.SaveFormAction;
import info.magnolia.ui.vaadin.integration.jcr.JcrNodeAdapter;
import nl.tricode.magnolia.blogs.util.BlogWorkspaceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * Action for saving Items in Forms providing a unique name based on the value for propertyName in definition.
 */
public class UniqueNameSaveFormAction extends SaveFormAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(UniqueNameSaveFormAction.class);

    public UniqueNameSaveFormAction(UniqueNameSaveFormActionDefinition definition, JcrNodeAdapter item,
                                    EditorCallback callback, EditorValidator validator) {
        super(definition, item, callback, validator);
    }

    @Override
    protected UniqueNameSaveFormActionDefinition getDefinition() {
        return (UniqueNameSaveFormActionDefinition) super.getDefinition();
    }

    @Override
    protected void setNodeName(Node node, JcrNodeAdapter item) throws RepositoryException {
        try {
            if (item.isNew() || BlogWorkspaceUtil.hasNameChanged(node, getDefinition().getPropertyName())) {
                final String newNodeName = BlogWorkspaceUtil.generateUniqueNodeName(node, getDefinition().getPropertyName());
                item.setNodeName(newNodeName);
                NodeUtil.renameNode(node, newNodeName);
            }
        } catch (PathNotFoundException e) {
            LOGGER.error("Trying to fetch value of a non-existent propertyName", e);
        }
    }
}