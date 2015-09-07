package nl.tricode.magnolia.blogs.dialog.action;

import com.vaadin.data.Item;
import info.magnolia.ui.admincentral.dialog.action.SaveDialogActionDefinition;
import info.magnolia.ui.api.action.AbstractAction;
import info.magnolia.ui.api.action.ActionExecutionException;
import info.magnolia.ui.api.shell.Shell;
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

    private static final Logger log = LoggerFactory.getLogger(SaveInitialActivationDateDialogAction.class);

    private final Item item;
    private final EditorValidator validator;
    private final EditorCallback callback;

    @Inject
    public SaveInitialActivationDateDialogAction(T definition, Item item, EditorValidator validator, EditorCallback callback) {
        super(definition);
        this.item = item;
        this.validator = validator;
        this.callback = callback;
    }

    @Override
    public void execute() throws ActionExecutionException {
        // First Validate
        validator.showValidation(true);
        if (validator.isValid()) {
            final JcrNodeAdapter itemChanged = (JcrNodeAdapter) item;
            try {
                final Node node = itemChanged.applyChanges();
                node.getSession().save();
            } catch (final RepositoryException e) {
                throw new ActionExecutionException(e);
            }
            callback.onSuccess(getDefinition().getName());
        } else {
            log.info("Validation error(s) occurred. No save performed.");
        }
    }
}
