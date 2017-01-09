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
package nl.tricode.magnolia.blogs.commands.activation;

import info.magnolia.commands.impl.BaseRepositoryCommand;
import info.magnolia.context.Context;
import nl.tricode.magnolia.blogs.BlogsNodeTypes;

import javax.jcr.Node;
import java.util.Calendar;

public class SetInitialActivationDateCommand extends BaseRepositoryCommand {

    @Override
    public boolean execute(final Context context) throws Exception {
        final Node blogNode = getJCRNode(context);

        if (!blogNode.hasProperty(BlogsNodeTypes.Blog.PROPERTY_INITIALACTIVATIONDATE)) {
            blogNode.setProperty(BlogsNodeTypes.Blog.PROPERTY_INITIALACTIVATIONDATE, Calendar.getInstance());
        }

        return true;
    }
}