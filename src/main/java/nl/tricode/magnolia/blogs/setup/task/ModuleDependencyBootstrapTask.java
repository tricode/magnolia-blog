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
package nl.tricode.magnolia.blogs.setup.task;

import info.magnolia.module.InstallContext;
import info.magnolia.module.delta.BootstrapResourcesTask;
import info.magnolia.module.delta.IsModuleInstalledOrRegistered;
import org.apache.commons.lang.StringUtils;

import javax.jcr.ImportUUIDBehavior;

/**
 * A Task which will bootstrap files if an optional module is installed or registered:
 * any resource under path given.
 */
public class ModuleDependencyBootstrapTask extends IsModuleInstalledOrRegistered {
	public ModuleDependencyBootstrapTask(final String bootstrapResourcePath, final String dependencyName) {
		super("Bootstrap " + dependencyName, "Bootstraps " + dependencyName + " content if installed.", dependencyName,
				  new BootstrapResourcesTask(StringUtils.EMPTY, StringUtils.EMPTY, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING) {
					  @Override
					  protected boolean acceptResource(InstallContext ctx, String name) {
						  final String path = StringUtils.removeEnd(bootstrapResourcePath, "/") + "/" + dependencyName + "/";
						  return name.startsWith(path) && name.endsWith(".xml");
					  }
				  });
	}
}