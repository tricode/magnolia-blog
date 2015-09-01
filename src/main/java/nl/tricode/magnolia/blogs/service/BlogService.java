package nl.tricode.magnolia.blogs.service;

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
     */
    BlogResult getLatestBlogs(String searchRootPath, int pageNumber, int maxResultsPerPage, String categoryUuid);

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
     */
    BlogResult getLatestBlogs(String searchRootPath, int pageNumber, int maxResultsPerPage, String categoryName, String categoryWorkspace);

    /**
     * Return the blog node for given identifier.
     *
     * @param id Node identifier
     * @return Blog Node
     */
    Node getBlogById(String id);

    /**
     * Return the blog node for given unique name.
     *
     * @param name Unique blog name
     * @return Blog Node
     */
    Node getBlogByName(String name);

}
