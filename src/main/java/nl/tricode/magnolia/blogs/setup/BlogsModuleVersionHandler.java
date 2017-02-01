/*
 * Tricode Blog module
 * Is a Blog module for Magnolia CMS.
 * Copyright (C) 2015  Tricode Business Integrators B.V.
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.tricode.magnolia.blogs.setup;

import info.magnolia.module.DefaultModuleVersionHandler;
import info.magnolia.module.InstallContext;
import info.magnolia.module.delta.BootstrapSingleModuleResource;
import info.magnolia.module.delta.BootstrapSingleResource;
import info.magnolia.module.delta.DeltaBuilder;
import info.magnolia.module.delta.ModuleBootstrapTask;
import info.magnolia.module.delta.RemoveNodeTask;
import info.magnolia.module.delta.Task;

import javax.jcr.ImportUUIDBehavior;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles installation and updates of the module.
 */
public class BlogsModuleVersionHandler extends DefaultModuleVersionHandler {

    private static final String MODULE_NAME = "magnolia-blogs-module";

    /**
     * Constructor.
     * Here you can register deltas for tasks that need to be run when UPDATING an EXISTING module.
     */
    public BlogsModuleVersionHandler() {
        register(DeltaBuilder.update("1.1.1", "Add a userrole blog-editor")
                .addTask(new BootstrapSingleModuleResource("Userrole config", "Installing a userrole for the blog module",
                        "/userroles/userroles.blog-editor.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING))
        );

        register(DeltaBuilder.update("1.1.2", "Updating blog module")
                .addTask(new BootstrapSingleModuleResource("Rendering config", "Installing new freemarker context attributes for blogfn alias",
                        "/config/config.modules.rendering.renderers.freemarker.contextAttributes.blogfn.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING))
        );

        register(DeltaBuilder.update("1.1.3", "Updating blog module")
                .addTask(new BootstrapSingleResource("Update config", "Fix dialog setting on edit blog folder",
                        "/mgnl-bootstrap/updates/config.modules.magnolia-blogs-module.apps.tricode-blogs.subApps.browser.actions.editFolder.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING))
                .addTask(new BootstrapSingleResource("Update config", "Add new action for editing initial activation date",
                        "/mgnl-bootstrap/updates/config.modules.magnolia-blogs-module.apps.tricode-blogs.subApps.browser.actions.editActivationDate.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING))
                .addTask(new BootstrapSingleResource("Update config", "Add new edit activation date action to action bar",
                        "/mgnl-bootstrap/updates/config.modules.magnolia-blogs-module.apps.tricode-blogs.subApps.browser.actionbar.sections.blog.groups.activationActions.items.editActivationDate.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING))
        );

        register(DeltaBuilder.update("1.1.5", "Upgrading blog module to Magnolia 5.5")
                .addTask(new RemoveNodeTask("Remove old nodes", "/modules/" + MODULE_NAME + "/apps"))
                .addTask(new RemoveNodeTask("Remove old nodes", "/modules/" + MODULE_NAME + "/dialogs"))
        );
    }

    /**
     * Override this method when defining tasks that need to be executed when INITIALLY INSTALLING the module.
     *
     * @param installContext Context of the install, can be used to display messages
     * @return A list of tasks to execute on initial install
     */
    @Override
    protected List<Task> getExtraInstallTasks(final InstallContext installContext) {
        final List<Task> tasks = new ArrayList<>();
        tasks.addAll(super.getExtraInstallTasks(installContext));

        return tasks;
    }

    @Override
    protected List<Task> getStartupTasks(final InstallContext installContext) {
        final List<Task> startupTasks = new ArrayList<>(0);
        startupTasks.addAll(super.getStartupTasks(installContext));

        if ("SNAPSHOT".equals(installContext.getCurrentModuleDefinition().getVersion().getClassifier())) {
            // force updates for snapshots
            startupTasks.add(new RemoveNodeTask("Remove snapshot information", "", "config", "/modules/" + MODULE_NAME + "/commands"));
            startupTasks.add(new ModuleBootstrapTask());
        }

        return startupTasks;
    }

}