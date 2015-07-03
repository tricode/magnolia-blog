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
package nl.tricode.magnolia.blogs.app;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;
import info.magnolia.event.EventBus;
import info.magnolia.i18nsystem.SimpleTranslator;
import info.magnolia.ui.api.app.SubAppContext;
import info.magnolia.ui.api.event.AdmincentralEventBus;
import info.magnolia.ui.api.location.Location;
import info.magnolia.ui.contentapp.ContentSubAppView;
import info.magnolia.ui.contentapp.detail.DetailEditorPresenter;
import info.magnolia.ui.contentapp.detail.DetailSubApp;
import info.magnolia.ui.vaadin.actionbar.Actionbar;
import info.magnolia.ui.vaadin.integration.contentconnector.ContentConnector;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Temporary implementation of the blog detail subapp showing the action bar
 * Related Magnolia issue: http://jira.magnolia-cms.com/browse/MGNLUI-2293
 *
 * @author gtenham
 */
public class BlogDetailSubApp extends DetailSubApp {

    @Inject
    protected BlogDetailSubApp(final SubAppContext subAppContext, final ContentSubAppView view, @Named(AdmincentralEventBus.NAME) EventBus adminCentralEventBus,
                               DetailEditorPresenter workbench, SimpleTranslator i18n, ContentConnector contentConnector) {
        super(subAppContext, view, adminCentralEventBus, workbench, i18n, contentConnector);
    }

    private void hackActionbarVisibility(ComponentContainer componentContainer) {
        for (Component c : componentContainer) {
            if (c instanceof Actionbar) {
                Actionbar actionBar = (Actionbar) c;
                if (actionBar.getSections().size() > 0) {
                    actionBar.removeStyleName("stub");
                    actionBar.setOpen(true);
                }
            } else if (c instanceof ComponentContainer) {
                hackActionbarVisibility((ComponentContainer) c);
            }
        }
    }

    @Override
    public ContentSubAppView start(final Location location) {
        ContentSubAppView view = super.start(location);
        if (view instanceof ComponentContainer) {
            hackActionbarVisibility((ComponentContainer) view.asVaadinComponent());
        }
        return view;
    }
}