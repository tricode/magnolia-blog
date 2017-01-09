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
package nl.tricode.magnolia.blogs.service;

import nl.tricode.magnolia.blogs.exception.UnableToGetBlogException;
import nl.tricode.magnolia.blogs.exception.UnableToGetLatestBlogsException;

import javax.jcr.Node;

/**
 * Blog content service
 */
public interface BlogService {

    /**
     * Returns all available blog entries starting from given path, page number and maximum results filtered by given category identifier.
     *
     * @param searchRootPath    Start path to return blog items from
     * @param pageNumber        page number
     * @param maxResultsPerPage Maximum results returned per page
     * @param categoryUuid      Category (mgnl:category) identifier
     * @return BlogItemsWrapper wrapper object
     * @throws UnableToGetLatestBlogsException When there was an error fetching the latest blog items
     */
    BlogItemsWrapper getLatestBlogItems(String searchRootPath, int pageNumber, int maxResultsPerPage, String categoryUuid)
            throws UnableToGetLatestBlogsException;

    /**
     * Returns all available blog entries starting from given path, page number and maximum results filtered by given category name
     * in category workspace.
     *
     * @param searchRootPath    Start path to return blog items from
     * @param pageNumber        page number
     * @param maxResultsPerPage Maximum results returned per page
     * @param categoryName      Category (mgnl:category) name
     * @param categoryWorkspace Category workspace name
     * @return BlogItemsWrapper wrapper object
     * @throws UnableToGetLatestBlogsException When there was an error fetching the latest blog items
     */
    BlogItemsWrapper getLatestBlogItems(String searchRootPath, int pageNumber, int maxResultsPerPage, String categoryName, String categoryWorkspace)
            throws UnableToGetLatestBlogsException;

    /**
     * Return the blog node for given identifier.
     *
     * @param id Blog Node identifier
     * @return Blog Node
     * @throws UnableToGetBlogException When the specified blog item could not be read
     */
    Node getBlogById(String id) throws UnableToGetBlogException;

    /**
     * Return the blog node for given unique name.
     *
     * @param name Unique blog name
     * @return Blog Node
     * @throws UnableToGetBlogException When the specified blog item could not be read
     */
    Node getBlogByName(String name) throws UnableToGetBlogException;

    /**
     * Get related blog items for given blog id. Match will be made based on blog categories
     *
     * @param id                 Blog Node identifier
     * @param maxResultsReturned Maximum returned blog items
     * @return BlogItemsWrapper wrapper object
     * @throws UnableToGetBlogException        When the specified blog item could not be read
     * @throws UnableToGetLatestBlogsException When the related items could not be read
     */
    BlogItemsWrapper getRelatedBlogItemsById(String id, int maxResultsReturned)
            throws UnableToGetBlogException, UnableToGetLatestBlogsException;

    /**
     * Get related blog items for given blog name. Match will be made based on blog categories.
     *
     * @param name               Unique blog name
     * @param maxResultsReturned Maximum returned blog items
     * @return BlogItemsWrapper wrapper object
     * @throws UnableToGetBlogException        When the specified blog item could not be read
     * @throws UnableToGetLatestBlogsException When the releated items could not be read
     */
    BlogItemsWrapper getRelatedBlogItemsByName(String name, int maxResultsReturned)
            throws UnableToGetBlogException, UnableToGetLatestBlogsException;

}
