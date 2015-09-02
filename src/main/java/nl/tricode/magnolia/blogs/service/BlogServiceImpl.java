package nl.tricode.magnolia.blogs.service;

import info.magnolia.cms.util.QueryUtil;
import info.magnolia.jcr.util.NodeUtil;
import info.magnolia.jcr.wrapper.I18nNodeWrapper;
import nl.tricode.magnolia.blogs.BlogsNodeTypes;
import nl.tricode.magnolia.blogs.exception.BlogReadException;
import nl.tricode.magnolia.blogs.exception.UnableToGetBlogException;
import nl.tricode.magnolia.blogs.exception.UnableToGetLatestBlogsException;
import nl.tricode.magnolia.blogs.util.BlogWorkspaceUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.List;

/**
 * Blog content service
 */
public class BlogServiceImpl implements BlogService {

    private static final Logger log = LoggerFactory.getLogger(BlogServiceImpl.class);

    private static final String BASE_QUERY = "SELECT p.* FROM [%s] AS p " +
            "WHERE ISDESCENDANTNODE(p, '%s') %s ";
    private static final String BASE_QUERY_ORDERBY = BASE_QUERY + " ORDER BY %s";

    private List<Node> allBlogResults;

    /**
     * {@inheritDoc}
     */
    @Override
    public BlogResult getLatestBlogs(String searchRootPath, int pageNumber, int maxResultsPerPage, String categoryUuid) throws UnableToGetLatestBlogsException {
        BlogResult blogResult = new BlogResult();
        // jcr filter on category Uuid
        String customJcrFilter = "";
        if (StringUtils.isNotBlank(categoryUuid)) {
            customJcrFilter =  "AND p.categories like '%" + categoryUuid + "%' ";
        }

        final String orderBy = "p.[mgnl:created] desc";


        final String jcrQuery = buildQuery(BlogsNodeTypes.Blog.NAME, StringUtils.defaultString(searchRootPath,"/"), customJcrFilter, orderBy);

        try {
            allBlogResults = executeQuery(jcrQuery, BlogWorkspaceUtil.COLLABORATION, BlogsNodeTypes.Blog.NAME);

            blogResult.setTotalCount(allBlogResults.size());
            blogResult.setNumPages(getNumberOfPages( allBlogResults.size(), maxResultsPerPage) );
            blogResult.setResults(getPagedResults(allBlogResults, pageNumber, maxResultsPerPage));

        } catch (RepositoryException e) {
            log.error("Exception during fetch of blog items", e);
            throw new UnableToGetLatestBlogsException("Unable to read blogs for the given criteria.", e);
        }
        return blogResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlogResult getLatestBlogs(String searchRootPath, int pageNumber, int maxResultsPerPage, String categoryName, String categoryWorkspace) throws UnableToGetLatestBlogsException {
        String categoryUuid = findCategoryIdByName(categoryName, categoryWorkspace);

        return getLatestBlogs(searchRootPath, pageNumber, maxResultsPerPage, categoryUuid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Node getBlogById(String id) throws UnableToGetBlogException {
        Node blog = null;
        if (StringUtils.isBlank(id)) {
            return null;
        }
        try {
            blog = NodeUtil.getNodeByIdentifier(BlogWorkspaceUtil.COLLABORATION, id);
        } catch (RepositoryException e) {
            log.error("Exception during fetch of blog with id: {}",id, e);
            throw new UnableToGetBlogException("Unable to retrieve blog for given id.", e);
        }
        return blog;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Node getBlogByName(String name) throws UnableToGetBlogException{
        final String blogId = findBlogIdByName(name);

        return getBlogById(blogId);
    }

    /**
     * Execute JCR query returning Nodes matching given statement and return node type
     *
     * @param statement      JCR query string
     * @param workspace      Search in JCR workspace like website
     * @param returnItemType Return nodes based on primary node type
     * @return List<Node>
     * @throws javax.jcr.RepositoryException
     */
    private List<Node> executeQuery(String statement, String workspace, String returnItemType) throws RepositoryException {
        List<Node> nodeList = new ArrayList<Node>(0);

        NodeIterator items = QueryUtil.search(workspace, statement, Query.JCR_SQL2, returnItemType);
        log.debug("Query Executed: {}", statement);
        while (items.hasNext()) {
            Node node = items.nextNode();
            if (!filterNode(node)) {
                nodeList.add(new I18nNodeWrapper(node));
            }

        }
        return nodeList;
    }

    /**
     * Check if node is valid to return as a result.
     *
     * @param node
     * @return True when node needs to be filtered out of the result set
     */
    private Boolean filterNode(Node node) {
        // Additional filters after jcr query, defaults to false for now
        Boolean filterNode = false;

        return filterNode;
    }

    /**
     * Build JCR query string.
     *
     * @param from         JCR from clause eg. nt:base
     * @param searchPath   Start path where to start searches in eg /home
     * @param customFilter JCR based where clause
     * @param orderBy      JCR based order by (defaults to score() desc)
     * @return Full JCR query string
     */
    private String buildQuery(String from, String searchPath, String customFilter, String orderBy) {
        if (StringUtils.isNotBlank(orderBy)) {
            return String.format(BASE_QUERY_ORDERBY, from, StringUtils.defaultIfEmpty(searchPath, "/"),
                    customFilter, orderBy);
        }
        return String.format(BASE_QUERY, from, StringUtils.defaultIfEmpty(searchPath, "/"),
                customFilter);
    }

    private int getNumberOfPages(int total, int maxResultsPerPage) {
        int calcNumPages = total/maxResultsPerPage;
        if((total % maxResultsPerPage) > 0 ) {
            calcNumPages++;
        }
        return calcNumPages;
    }

    private List<Node> getPagedResults(List<Node> results, int pageNumber, int maxResultsPerPage) {
        List<Node> nodeListPaged = new ArrayList<Node>(0);
        final int total = results.size();
        final int startRow = (maxResultsPerPage * (pageNumber - 1));
        int newLimit = maxResultsPerPage;
        if(total > startRow) {
            if(total < startRow + maxResultsPerPage) {
                newLimit = total - startRow;
            }
            nodeListPaged = results.subList(startRow, startRow + newLimit);
        }
        return nodeListPaged;
    }

    private String findBlogIdByName(String blogName) throws UnableToGetBlogException {
        if (StringUtils.isBlank(blogName)) {
            return  "";
        }
        final String blogFilter = String.format(" and p.name = '%s'", blogName);
        final String jcrQuery = buildQuery(BlogsNodeTypes.Blog.NAME, StringUtils.defaultString("/"), blogFilter, "");
        try {
            List<Node> allBlogResults = executeQuery(jcrQuery, BlogWorkspaceUtil.COLLABORATION, BlogsNodeTypes.Blog.NAME);
            if (allBlogResults.size() > 0) {
                return allBlogResults.get(0).getIdentifier();
            }
        } catch (RepositoryException e) {
            log.error("Exception during fetch of blog by name: {}",blogName, e);
        }

        return "";
    }

    private String findCategoryIdByName(String categoryName, String workspace) {
        if (StringUtils.isBlank(categoryName) || StringUtils.isBlank(workspace)) {
            return  "";
        }

        final String catFilter = String.format(" and p.name = '%s'", categoryName);
        final String jcrQuery = buildQuery("mgnl:category", StringUtils.defaultString("/"), catFilter, "");
        try {
            List<Node> allCategoriesResults = executeQuery(jcrQuery, workspace, "mgnl:category");
            if (allCategoriesResults.size() > 0) {
                return allCategoriesResults.get(0).getIdentifier();
            }
        } catch (RepositoryException e) {
            log.error("Exception during fetch of category items. category: {}, workspace: {}", categoryName, workspace, e);
        }
        return "";
    }
}
