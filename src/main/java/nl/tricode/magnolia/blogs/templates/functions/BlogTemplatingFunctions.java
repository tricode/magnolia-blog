package nl.tricode.magnolia.blogs.templates.functions;

import info.magnolia.jcr.util.ContentMap;
import info.magnolia.templating.functions.TemplatingFunctions;
import nl.tricode.magnolia.blogs.exception.UnableToGetBlogException;
import nl.tricode.magnolia.blogs.exception.UnableToGetLatestBlogsException;
import nl.tricode.magnolia.blogs.service.BlogResult;
import nl.tricode.magnolia.blogs.service.BlogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.jcr.Node;

/**
 * An object exposing several methods useful for blog related templates. It is exposed in templates as <code>blogfn</code>.
 */
public class BlogTemplatingFunctions {

    private static final Logger log = LoggerFactory.getLogger(BlogTemplatingFunctions.class);

    private BlogService blogService;
    private TemplatingFunctions templatingFunctions;

    @Inject
    public BlogTemplatingFunctions(BlogService blogService, TemplatingFunctions templatingFunctions) {
        this.blogService = blogService;
        this.templatingFunctions = templatingFunctions;
    }

    /**
     * Returns all available blog entries starting from root and no additional filters
     *
     * @return BlogResult wrapper object
     */
    public BlogResult allBlogs() {
        try {
            return blogService.getLatestBlogs("/", 1, Integer.MAX_VALUE, "");
        } catch (UnableToGetLatestBlogsException e) {
            log.error("Unable to get latest blogs.", e);
        }
        return null;
    }

    /**
     * Returns all available blog entries starting from root filtered by given category name in category workspace
     *
     * @param categoryName Category (mgnl:category) name
     * @param workspace Category workspace name
     * @return BlogResult wrapper object
     */
    public BlogResult allBlogsByCategory(String categoryName, String workspace) {
        try {
            return blogService.getLatestBlogs("/", 1, Integer.MAX_VALUE, categoryName, workspace);
        } catch (UnableToGetLatestBlogsException e) {
            log.error("Unable to get latest blogs.");
        }
        return null;
    }

    /**
     * Returns all available blog entries starting from given path, page number and maximum results
     *
     * @param searchRootPath Start path to return blog items from
     * @param pageNumber page number
     * @param maxResultsPerPage Maximum results returned per page
     * @return BlogResult wrapper object
     */
    public BlogResult pagedBlogs(String searchRootPath, int pageNumber, int maxResultsPerPage) {
        try {
            return blogService.getLatestBlogs(searchRootPath, pageNumber, maxResultsPerPage,"");
        } catch (UnableToGetLatestBlogsException e) {
            log.error("Unable to get latest blogs. searchRootPath: {}, pageNumber: {}, maxResultsPerPage: {}",
                    searchRootPath, pageNumber, maxResultsPerPage);
        }
        return null;
    }

    /**
     * Returns all available blog entries starting from given path, page number and maximum results filtered by given category name in category workspace
     *
     * @param searchRootPath Start path to return blog items from
     * @param pageNumber page number
     * @param maxResultsPerPage Maximum results returned per page
     * @param categoryName Category (mgnl:category) name
     * @param workspace Category workspace name
     * @return BlogResult wrapper object
     */
    public BlogResult pagedBlogsByCategory(String searchRootPath, int pageNumber, int maxResultsPerPage, String categoryName, String workspace) {
        try {
            return blogService.getLatestBlogs(searchRootPath, pageNumber, maxResultsPerPage, categoryName, workspace);
        } catch (UnableToGetLatestBlogsException e) {
            log.error("Unable to get the latest blogs. searchRootPath: {}, pageNumber: {}, maxResultsPerPage: {}, categoryName: {}, workspace: {}",
                    searchRootPath, pageNumber, maxResultsPerPage, categoryName, workspace);
        }
        return null;
    }

    /**
     * Return the ContentMap for the blog id.
     *
     * @param id Blog identifier
     * @return Blog content
     */
    public ContentMap blogContentById(String id) {
        return templatingFunctions.asContentMap(blogById(id));
    }

    /**
     * Return the ContentMap for the blog name.
     *
     * @param name Unique blog name
     * @return Blog content
     */
    public ContentMap blogContentByName(String name) {
        return templatingFunctions.asContentMap(blogByName(name));
    }

    /**
     * Return the Node for the blog id.
     *
     * @param id Blog identifier
     * @return Blog node
     */
    public Node blogById(String id) {
        try {
            return blogService.getBlogById(id);
        } catch (UnableToGetBlogException e) {
            log.error("Unable to get blog by id: {}", id);
        }
        return null;
    }

    /**
     * Return the Node for the blog name.
     *
     * @param name Unique blog name
     * @return Blog node
     */
    public Node blogByName(String name) {
        try {
            return blogService.getBlogByName(name);
        } catch (UnableToGetBlogException e) {
            log.error("Unable to get blog by it's name: {}", name);
        }
        return null;
    }
}
