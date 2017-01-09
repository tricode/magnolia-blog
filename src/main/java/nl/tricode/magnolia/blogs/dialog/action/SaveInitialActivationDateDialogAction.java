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
package nl.tricode.magnolia.blogs.dialog.action;

import com.vaadin.data.Item;
import info.magnolia.ui.api.action.AbstractAction;
import info.magnolia.ui.api.action.ActionExecutionException;
import info.magnolia.ui.form.EditorCallback;
import info.magnolia.ui.form.EditorValidator;
import info.magnolia.ui.vaadin.integration.jcr.JcrNodeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Save Initial activation data action for a blog
 */
public class SaveInitialActivationDateDialogAction<T extends SaveInitialActivationDateDialogActionDefinition> extends AbstractAction<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaveInitialActivationDateDialogAction.class);

    private final Item item;
    private final EditorValidator validator;
    private final EditorCallback callback;

    @Inject
    public SaveInitialActivationDateDialogAction(T definition,
                                                 Item item,
                                                 EditorValidator validator,
                                                 EditorCallback callback) {
        super(definition);
        this.item = item;
        this.validator = validator;
        this.callback = callback;
    }

    @Override
    public void execute() throws ActionExecutionException {
        // First Validate
        validator.showValidation(true);

        if (!validator.isValid()) {
            LOGGER.info("Validation error(s) occurred. No save performed.");
            return;
        }

        final JcrNodeAdapter itemChanged = (JcrNodeAdapter) item;

        try {
            final Node node = itemChanged.applyChanges();
            node.getSession().save();
        } catch (final RepositoryException e) {
            throw new ActionExecutionException(e);
        }

        callback.onSuccess(getDefinition().getName());
    }

}
