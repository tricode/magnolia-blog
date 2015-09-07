package nl.tricode.magnolia.blogs.service;

import info.magnolia.cms.util.QueryUtil;
import info.magnolia.jcr.util.NodeUtil;
import info.magnolia.jcr.util.PropertyUtil;
import info.magnolia.jcr.wrapper.I18nNodeWrapper;
import nl.tricode.magnolia.blogs.BlogsNodeTypes;
import nl.tricode.magnolia.blogs.exception.UnableToGetBlogException;
import nl.tricode.magnolia.blogs.exception.UnableToGetLatestBlogsException;
import nl.tricode.magnolia.blogs.util.BlogWorkspaceUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.jcr.query.Query;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Blog content service
 */
public class BlogServiceImpl implements BlogService {

    private static final Logger log = LoggerFactory.getLogger(BlogServiceImpl.class);

    private static final String SEARCH_BOOST_VERY_IMPORTANT_FACTOR = "^10";
    private static final String SEARCH_BOOST_MEDIUM_IMPORTANT_FACTOR = "^5";
    private static final String SEARCH_BOOST_LESS_IMPORTANT_FACTOR = "^2";
    private static final String SEARCH_FUZZY_FACTOR = "~0.8";


    private static final String BASE_QUERY = "SELECT p.* FROM [%s] AS p " +
            "WHERE ISDESCENDANTNODE(p, '%s') %s ";
    private static final String BASE_QUERY_ORDERBY = BASE_QUERY + " ORDER BY %s";

    private List<Node> allBlogResults;

    /**
     * {@inheritDoc}
     */
    @Override
    public BlogResult getLatestBlogs(String searchRootPath, int pageNumber, int maxResultsPerPage, String categoryUuid) throws UnableToGetLatestBlogsException {
        // jcr filter on category Uuid
        String customJcrFilter = "";
        if (StringUtils.isNotBlank(categoryUuid)) {
            customJcrFilter =  "AND p.categories like '%" + categoryUuid + "%' ";
        }

        final String orderBy = "p.initialActivationDate desc, p.[mgnl:created] desc";

        return findBlogs(searchRootPath, pageNumber, maxResultsPerPage, customJcrFilter, orderBy);
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
            log.error("Exception during fetch of blog by id.", e);
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
     * {@inheritDoc}
     */
    @Override
    public BlogResult getRelatedBlogsById(String id, int maxResultsReturned) throws UnableToGetBlogException, UnableToGetLatestBlogsException {
        final String searchRootPath = "/";
        final int pageNumber = 1;
        final String orderBy = "score() desc";
        final Node blog = getBlogById(id);
        final String filterPredicate = getBlogRelatedSearchPredicate(blog);

        return findBlogs(searchRootPath, pageNumber, maxResultsReturned, filterPredicate, orderBy);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlogResult getRelatedBlogsByName(String name, int maxResultsReturned) throws UnableToGetBlogException, UnableToGetLatestBlogsException {
        final String blogId = findBlogIdByName(name);
        return getRelatedBlogsById(blogId, maxResultsReturned);
    }

    /**
     *
     * @param searchRootPath Start path to return blog items from
     * @param pageNumber page number
     * @param maxResultsPerPage Maximum results returned per page
     * @param filter Lucene filter predicate
     * @param orderBy Lucene order by
     * @return
     * @throws UnableToGetLatestBlogsException
     */
    private BlogResult findBlogs(String searchRootPath, int pageNumber, int maxResultsPerPage, String filter, String orderBy) throws UnableToGetLatestBlogsException {
        BlogResult blogResult = new BlogResult();

        final String jcrQuery = buildQuery(BlogsNodeTypes.Blog.NAME, StringUtils.defaultString(searchRootPath,"/"), filter, orderBy);

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

    private String getBlogRelatedSearchPredicate(Node blog) {
        String searchTermPredicate = StringUtils.EMPTY;

        try {
            List<String> categoryNames = convertCategoryValuesToNamesList(blog.getProperty("categories"));


            // Start with excluding the origin blog node
            StringBuilder predicate = new StringBuilder(MessageFormat.format(" AND name(p) <> ''{0}'' ", new Object[]{blog.getName()}));
            // Start fuzzy match group
            predicate.append(" AND (");
            predicate.append(MessageFormat.format("  contains(p.title, ''{0}'')", new Object[]{ getCategoriesLucenePredicate(categoryNames, SEARCH_BOOST_VERY_IMPORTANT_FACTOR)} ));
            predicate.append( MessageFormat.format(" OR contains(p.summary, ''{0}'') ", new Object[]{ getCategoriesLucenePredicate(categoryNames, SEARCH_BOOST_MEDIUM_IMPORTANT_FACTOR) } ));
            predicate.append( MessageFormat.format(" OR contains(p.message, ''{0}'')", new Object[]{ getCategoriesLucenePredicate(categoryNames, SEARCH_BOOST_LESS_IMPORTANT_FACTOR) } ));
            // End fuzzy match group
            predicate.append(" )");

            searchTermPredicate = predicate.toString();
        } catch (RepositoryException e) {
            log.debug("An error occurred when getting blog data", e);
        }

        return searchTermPredicate;
    }

    private String getCategoriesLucenePredicate(List<String> categories, String boostFactor) {
        StringBuffer searchTermPredicate = new StringBuffer();

        for (String categoryName : categories) {
            if (categoryName.contains(" ")) {
                searchTermPredicate.append("\"" + categoryName + "\"");
            } else {
                searchTermPredicate.append(categoryName);
            }
            searchTermPredicate.append(boostFactor);
            //searchTermPredicate.append(SEARCH_FUZZY_FACTOR);
            searchTermPredicate.append(" OR ");

        }
        return StringUtils.stripEnd(searchTermPredicate.toString(),"OR ");
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
        final String blogFilter = String.format(" and name(p) = '%s'", blogName);
        final String jcrQuery = buildQuery(BlogsNodeTypes.Blog.NAME, StringUtils.defaultString("/"), blogFilter, "");
        try {
            List<Node> allBlogResults = executeQuery(jcrQuery, BlogWorkspaceUtil.COLLABORATION, BlogsNodeTypes.Blog.NAME);
            if (allBlogResults.size() > 0) {
                return allBlogResults.get(0).getIdentifier();
            }
        } catch (RepositoryException e) {
            log.error("Exception during fetch of blog by it's name.", e);
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
            log.error("Exception during fetch of category items.", e);
        }
        return "";
    }

    private List<String> convertCategoryValuesToNamesList(Property categories) throws RepositoryException {
        List<String> categoryNames = new ArrayList<String>(0);

        Value[] values = categories.getValues();

        String[] categoryIds = new String[values.length];

        for (int j = 0; j < values.length; j++) {
            try {
                categoryIds[j] = values[j].getString();
            } catch (RepositoryException e) {
                log.debug(e.getMessage());
            }
        }

        for (int i = 0; i < categoryIds.length; i++) {
            try {
                Node category = NodeUtil.getNodeByIdentifier("category", categoryIds[i]);
                String displayName = PropertyUtil.getString(category,"displayName");
                categoryNames.add(displayName);
            } catch (RepositoryException e) {
                log.debug("An error occurred when getting category data", e);
            }
        }
        return categoryNames;
    }

}
