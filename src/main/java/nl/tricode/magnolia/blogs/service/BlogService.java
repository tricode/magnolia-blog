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
     * @param searchRootPath Start path to return blog items from
     * @param pageNumber page number
     * @param maxResultsPerPage Maximum results returned per page
     * @param categoryUuid Category (mgnl:category) identifier
     * @return BlogResult wrapper object
     * @throws nl.tricode.magnolia.blogs.exception.UnableToGetLatestBlogsException
     */
    BlogResult getLatestBlogs(String searchRootPath, int pageNumber, int maxResultsPerPage, String categoryUuid) throws UnableToGetLatestBlogsException;

    /**
     * Returns all available blog entries starting from given path, page number and maximum results filtered by given category name
     * in category workspace.
     *
     * @param searchRootPath Start path to return blog items from
     * @param pageNumber page number
     * @param maxResultsPerPage Maximum results returned per page
     * @param categoryName Category (mgnl:category) name
     * @param categoryWorkspace Category workspace name
     * @return BlogResult wrapper object
     * @throws nl.tricode.magnolia.blogs.exception.UnableToGetLatestBlogsException
     */
    BlogResult getLatestBlogs(String searchRootPath, int pageNumber, int maxResultsPerPage, String categoryName, String categoryWorkspace) throws UnableToGetLatestBlogsException;

    /**
     * Return the blog node for given identifier.
     *
     * @param id Blog Node identifier
     * @throws nl.tricode.magnolia.blogs.exception.UnableToGetBlogException
     * @return Blog Node
     */
    Node getBlogById(String id) throws UnableToGetBlogException;

    /**
     * Return the blog node for given unique name.
     *
     * @param name Unique blog name
     * @throws nl.tricode.magnolia.blogs.exception.UnableToGetBlogException
     * @return Blog Node
     */
    Node getBlogByName(String name) throws UnableToGetBlogException;

    /**
     * Get related blog items for given blog id. Match will be made based on blog categories
     *
     * @param id Blog Node identifier
     * @param maxResultsReturned Maximum returned blog items
     * @return BlogResult wrapper object
     * @throws UnableToGetBlogException
     * @throws UnableToGetLatestBlogsException
     */
    BlogResult getRelatedBlogsById(String id, int maxResultsReturned) throws UnableToGetBlogException, UnableToGetLatestBlogsException;

    /**
     * Get related blog items for given blog name. Match will be made based on blog categories.
     *
     * @param name Unique blog name
     * @param maxResultsReturned Maximum returned blog items
     * @return BlogResult wrapper object
     * @throws UnableToGetBlogException
     * @throws UnableToGetLatestBlogsException
     */
    BlogResult getRelatedBlogsByName(String name, int maxResultsReturned) throws UnableToGetBlogException, UnableToGetLatestBlogsException;
}
