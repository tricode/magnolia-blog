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
package nl.tricode.magnolia.blogs.templates.functions;

import info.magnolia.jcr.util.ContentMap;
import info.magnolia.templating.functions.TemplatingFunctions;
import nl.tricode.magnolia.blogs.exception.UnableToGetBlogException;
import nl.tricode.magnolia.blogs.exception.UnableToGetLatestBlogsException;
import nl.tricode.magnolia.blogs.service.BlogItemsWrapper;
import nl.tricode.magnolia.blogs.service.BlogService;

import javax.inject.Inject;
import javax.jcr.Node;

/**
 * An object exposing several methods useful for blog related templates. It is exposed in templates as <code>blogfn</code>.
 */
@SuppressWarnings("unused") //Used in freemarker components.
public class BlogTemplatingFunctions {

    private final BlogService blogService;
    private final TemplatingFunctions templatingFunctions;

    @Inject
    public BlogTemplatingFunctions(BlogService blogService, TemplatingFunctions templatingFunctions) {
        this.blogService = blogService;
        this.templatingFunctions = templatingFunctions;
    }

    /**
     * Returns all available blog entries starting from root and no additional filters
     *
     * @return wrapper object
     * throws UnableToGetBlogException throw when the blogs cannot be read from the repository.
     */
    public BlogItemsWrapper allBlogs() throws UnableToGetLatestBlogsException {
        return blogService.getLatestBlogItems("/", 1, Integer.MAX_VALUE, "");
    }

    /**
     * Returns all available blog entries starting from root filtered by given category name in category workspace
     *
     * @param categoryName Category (mgnl:category) name
     * @param workspace    Category workspace name
     * @return wrapper object
     * @throws nl.tricode.magnolia.blogs.exception.UnableToGetLatestBlogsException
     */
    public BlogItemsWrapper allBlogsByCategory(String categoryName, String workspace)
            throws UnableToGetLatestBlogsException {
        return blogService.getLatestBlogItems("/", 1, Integer.MAX_VALUE, categoryName, workspace);
    }

    /**
     * Returns all available blog entries starting from given path, page number and maximum results
     *
     * @param searchRootPath    Start path to return blog items from
     * @param pageNumber        page number
     * @param maxResultsPerPage Maximum results returned per page
     * @return wrapper object
     * @throws nl.tricode.magnolia.blogs.exception.UnableToGetLatestBlogsException
     */
    public BlogItemsWrapper pagedBlogs(String searchRootPath, int pageNumber, int maxResultsPerPage)
            throws UnableToGetLatestBlogsException {
        return blogService.getLatestBlogItems(searchRootPath, pageNumber, maxResultsPerPage, "");
    }

    /**
     * Returns all available blog entries starting from given path, page number and maximum results filtered by given category name in category workspace
     *
     * @param searchRootPath    Start path to return blog items from
     * @param pageNumber        page number
     * @param maxResultsPerPage Maximum results returned per page
     * @param categoryName      Category (mgnl:category) name
     * @param workspace         Category workspace name
     * @return wrapper object
     * @throws nl.tricode.magnolia.blogs.exception.UnableToGetLatestBlogsException
     */
    public BlogItemsWrapper pagedBlogsByCategory(String searchRootPath, int pageNumber, int maxResultsPerPage, String categoryName, String workspace)
            throws UnableToGetLatestBlogsException {
        return blogService.getLatestBlogItems(searchRootPath, pageNumber, maxResultsPerPage, categoryName, workspace);
    }

    /**
     * Return the ContentMap for the blog id.
     *
     * @param id Blog identifier
     * @return Blog content
     * @throws nl.tricode.magnolia.blogs.exception.UnableToGetBlogException
     */
    public ContentMap blogContentById(String id) throws UnableToGetBlogException {
        return templatingFunctions.asContentMap(blogById(id));
    }

    /**
     * Return the ContentMap for the blog name.
     *
     * @param name Unique blog name
     * @return Blog content
     * @throws nl.tricode.magnolia.blogs.exception.UnableToGetBlogException
     */
    public ContentMap blogContentByName(String name) throws UnableToGetBlogException {
        return templatingFunctions.asContentMap(blogByName(name));
    }

    /**
     * Return the Node for the blog id.
     *
     * @param id Blog identifier
     * @return Blog node
     * @throws nl.tricode.magnolia.blogs.exception.UnableToGetBlogException
     */
    public Node blogById(String id) throws UnableToGetBlogException {
        return blogService.getBlogById(id);
    }

    /**
     * Return the Node for the blog name.
     *
     * @param name Unique blog name
     * @return Blog node
     * @throws nl.tricode.magnolia.blogs.exception.UnableToGetBlogException
     */
    public Node blogByName(String name) throws UnableToGetBlogException {
        return blogService.getBlogByName(name);
    }

    /**
     * Get related blog items for given blog id. Match will be made based on blog categories.
     *
     * @param id                 Blog identifier
     * @param maxResultsReturned Maximum returned blog items
     * @return wrapper object
     * @throws UnableToGetBlogException
     * @throws UnableToGetLatestBlogsException
     */
    public BlogItemsWrapper relatedBlogsById(String id, int maxResultsReturned)
            throws UnableToGetBlogException, UnableToGetLatestBlogsException {
        return blogService.getRelatedBlogItemsById(id, maxResultsReturned);
    }

    /**
     * Get related blog items for given blog name. Match will be made based on blog categories.
     *
     * @param name               Unique blog name
     * @param maxResultsReturned Maximum returned blog items
     * @return wrapper object
     * @throws UnableToGetBlogException
     * @throws UnableToGetLatestBlogsException
     */
    public BlogItemsWrapper relatedBlogsByName(String name, int maxResultsReturned)
            throws UnableToGetBlogException, UnableToGetLatestBlogsException {
        return blogService.getRelatedBlogItemsByName(name, maxResultsReturned);
    }

}