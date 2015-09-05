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
     * throws UnableToGetBlogException throw when the blogs cannot be read from the repository.
     */
    public BlogResult allBlogs() throws UnableToGetLatestBlogsException {
            return blogService.getLatestBlogs("/", 1, Integer.MAX_VALUE, "");
    }

    /**
     * Returns all available blog entries starting from root filtered by given category name in category workspace
     *
     * @param categoryName Category (mgnl:category) name
     * @param workspace Category workspace name
     * @return BlogResult wrapper object
     * @throws nl.tricode.magnolia.blogs.exception.UnableToGetLatestBlogsException
     */
    public BlogResult allBlogsByCategory(String categoryName, String workspace) throws UnableToGetLatestBlogsException {
            return blogService.getLatestBlogs("/", 1, Integer.MAX_VALUE, categoryName, workspace);
    }

    /**
     * Returns all available blog entries starting from given path, page number and maximum results
     *
     * @param searchRootPath Start path to return blog items from
     * @param pageNumber page number
     * @param maxResultsPerPage Maximum results returned per page
     * @return BlogResult wrapper object
     * @throws  nl.tricode.magnolia.blogs.exception.UnableToGetLatestBlogsException
     */
    public BlogResult pagedBlogs(String searchRootPath, int pageNumber, int maxResultsPerPage) throws UnableToGetLatestBlogsException {
            return blogService.getLatestBlogs(searchRootPath, pageNumber, maxResultsPerPage,"");
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
     * @throws nl.tricode.magnolia.blogs.exception.UnableToGetLatestBlogsException
     */
    public BlogResult pagedBlogsByCategory(String searchRootPath, int pageNumber, int maxResultsPerPage, String categoryName, String workspace) throws UnableToGetLatestBlogsException {
            return blogService.getLatestBlogs(searchRootPath, pageNumber, maxResultsPerPage, categoryName, workspace);
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
     * @param id Blog identifier
     * @param maxResultsReturned Maximum returned blog items
     * @return BlogResult wrapper object
     * @throws UnableToGetBlogException
     * @throws UnableToGetLatestBlogsException
     */
    public BlogResult relatedBlogsById(String id, int maxResultsReturned) throws UnableToGetBlogException, UnableToGetLatestBlogsException {
        return blogService.getRelatedBlogsById(id, maxResultsReturned);
    }

    /**
     * Get related blog items for given blog name. Match will be made based on blog categories.
     *
     * @param name Unique blog name
     * @param maxResultsReturned Maximum returned blog items
     * @return BlogResult wrapper object
     * @throws UnableToGetBlogException
     * @throws UnableToGetLatestBlogsException
     */
    public BlogResult relatedBlogsByName(String name, int maxResultsReturned) throws UnableToGetBlogException, UnableToGetLatestBlogsException {
        return blogService.getRelatedBlogsByName(name, maxResultsReturned);
    }
}
