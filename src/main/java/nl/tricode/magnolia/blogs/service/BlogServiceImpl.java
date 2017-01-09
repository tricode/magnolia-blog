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

import info.magnolia.cms.util.QueryUtil;
import info.magnolia.jcr.util.NodeUtil;
import info.magnolia.jcr.util.PropertyUtil;
import info.magnolia.jcr.wrapper.I18nNodeWrapper;
import nl.tricode.magnolia.blogs.BlogsNodeTypes;
import nl.tricode.magnolia.blogs.exception.UnableToGetBlogException;
import nl.tricode.magnolia.blogs.exception.UnableToGetLatestBlogsException;
import nl.tricode.magnolia.blogs.util.BlogRepositoryConstants;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Blog content service implementation.
 */
public class BlogServiceImpl implements BlogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlogServiceImpl.class);

    private static final String SEARCH_BOOST_VERY_IMPORTANT_FACTOR = "^10";
    private static final String SEARCH_BOOST_MEDIUM_IMPORTANT_FACTOR = "^5";
    private static final String SEARCH_BOOST_LESS_IMPORTANT_FACTOR = "^2";

    private static final String BASE_QUERY = "SELECT p.* FROM [%s] AS p WHERE ISDESCENDANTNODE(p, '%s') %s ";
    private static final String BASE_QUERY_ORDERBY = BASE_QUERY + " ORDER BY %s";

    @Override
    public BlogItemsWrapper getLatestBlogItems(final String searchRootPath,
                                               final int pageNumber,
                                               final int maxResultsPerPage,
                                               final String categoryUuid)
            throws UnableToGetLatestBlogsException {
        // jcr filter on category Uuid
        String customJcrFilter = "";
        if (StringUtils.isNotBlank(categoryUuid)) {
            customJcrFilter = "AND p.categories like '%" + categoryUuid + "%' ";
        }

        final String orderBy = "p.initialActivationDate desc, p.[mgnl:created] desc";

        return findBlogItems(searchRootPath, pageNumber, maxResultsPerPage, customJcrFilter, orderBy);
    }

    @Override
    public BlogItemsWrapper getLatestBlogItems(final String searchRootPath,
                                               final int pageNumber,
                                               final int maxResultsPerPage,
                                               final String categoryName,
                                               final String categoryWorkspace)
            throws UnableToGetLatestBlogsException {
        String categoryUuid = findCategoryIdByName(categoryName, categoryWorkspace);

        return getLatestBlogItems(searchRootPath, pageNumber, maxResultsPerPage, categoryUuid);
    }

    @Override
    public Node getBlogById(final String id) throws UnableToGetBlogException {
        if (StringUtils.isBlank(id)) {
            return null;
        }

        try {
            return NodeUtil.getNodeByIdentifier(BlogRepositoryConstants.COLLABORATION, id);
        } catch (RepositoryException e) {
            LOGGER.error("Exception during fetch of blog by id.", e);
            throw new UnableToGetBlogException("Unable to retrieve blog for given id.", e);
        }
    }

    @Override
    public Node getBlogByName(final String name) throws UnableToGetBlogException {
        final String blogId = findBlogIdByName(name);

        return getBlogById(blogId);
    }

    @Override
    public BlogItemsWrapper getRelatedBlogItemsById(final String id, final int maxResultsReturned)
            throws UnableToGetBlogException, UnableToGetLatestBlogsException {
        final String searchRootPath = "/";
        final int pageNumber = 1;
        final String orderBy = "score() desc";
        final Node blog = getBlogById(id);
        final String filterPredicate = getBlogRelatedSearchPredicate(blog);

        return findBlogItems(searchRootPath, pageNumber, maxResultsReturned, filterPredicate, orderBy);
    }

    @Override
    public BlogItemsWrapper getRelatedBlogItemsByName(final String name, final int maxResultsReturned)
            throws UnableToGetBlogException, UnableToGetLatestBlogsException {
        final String blogId = findBlogIdByName(name);
        return getRelatedBlogItemsById(blogId, maxResultsReturned);
    }

    private static BlogItemsWrapper findBlogItems(final String searchRootPath,
                                                  final int pageNumber,
                                                  final int maxResultsPerPage,
                                                  final String filter,
                                                  final String orderBy) throws UnableToGetLatestBlogsException {
        final String jcrQuery = buildQuery(BlogsNodeTypes.Blog.NAME, StringUtils.defaultString(searchRootPath, "/"), filter, orderBy);

        try {
            final List<Node> allBlogResults = executeQuery(jcrQuery, BlogRepositoryConstants.COLLABORATION, BlogsNodeTypes.Blog.NAME);

            return BlogItemsWrapper.Builder
                    .withTotalCount(allBlogResults.size())
                    .withNumPages(determineNumberOfPages(allBlogResults.size(), maxResultsPerPage))
                    .withResults(getPagedResults(allBlogResults, pageNumber, maxResultsPerPage))
                    .createInstance();

        } catch (RepositoryException e) {
            LOGGER.error("Exception during fetch of blog items", e);
            throw new UnableToGetLatestBlogsException("Unable to read blogs for the given criteria.", e);
        }
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
    private static List<Node> executeQuery(final String statement,
                                           final String workspace,
                                           final String returnItemType) throws RepositoryException {
        List<Node> nodeList = new ArrayList<>(0);

        NodeIterator items = QueryUtil.search(workspace, statement, Query.JCR_SQL2, returnItemType);

        LOGGER.debug("Query Executed: {}", statement);

        while (items.hasNext()) {
            Node node = items.nextNode();
            nodeList.add(new I18nNodeWrapper(node));
        }
        return nodeList;
    }

    private static String findBlogIdByName(final String blogName) throws UnableToGetBlogException {
        if (StringUtils.isBlank(blogName)) {
            return "";
        }
        final String blogFilter = String.format(" and name(p) = '%s'", blogName);
        final String jcrQuery = buildQuery(BlogsNodeTypes.Blog.NAME, StringUtils.defaultString("/"), blogFilter, "");
        try {
            List<Node> allBlogResults = executeQuery(jcrQuery, BlogRepositoryConstants.COLLABORATION, BlogsNodeTypes.Blog.NAME);
            if (allBlogResults.size() > 0) {
                return allBlogResults.get(0).getIdentifier();
            }
        } catch (RepositoryException e) {
            LOGGER.error("Exception during fetch of blog by it's name.", e);
        }

        return "";
    }

    private static String findCategoryIdByName(final String categoryName, final String workspace) {
        if (StringUtils.isBlank(categoryName) || StringUtils.isBlank(workspace)) {
            return "";
        }

        final String catFilter = String.format(" and p.name = '%s'", categoryName);
        final String jcrQuery = buildQuery("mgnl:category", StringUtils.defaultString("/"), catFilter, "");

        try {
            List<Node> allCategoriesResults = executeQuery(jcrQuery, workspace, "mgnl:category");
            if (allCategoriesResults.size() > 0) {
                return allCategoriesResults.get(0).getIdentifier();
            }
        } catch (RepositoryException e) {
            LOGGER.error("Exception during fetch of category items.", e);
        }
        return "";
    }

    private static String getBlogRelatedSearchPredicate(final Node blog) {
        String searchTermPredicate = StringUtils.EMPTY;

        try {
            final List<String> categoryNames = convertCategoryValuesToNamesList(blog.getProperty("categories"));

            // Start with excluding the origin blog node
            StringBuilder predicate = new StringBuilder(MessageFormat.format(" AND name(p) <> ''{0}'' ", new Object[]{blog.getName()}));
            // Start fuzzy match group
            predicate.append(" AND (");
            predicate.append(MessageFormat.format(" contains(p.title, ''{0}'')", new Object[]{getCategoriesLucenePredicate(categoryNames, SEARCH_BOOST_VERY_IMPORTANT_FACTOR)}));
            predicate.append(MessageFormat.format(" OR contains(p.summary, ''{0}'') ", new Object[]{getCategoriesLucenePredicate(categoryNames, SEARCH_BOOST_MEDIUM_IMPORTANT_FACTOR)}));
            predicate.append(MessageFormat.format(" OR contains(p.message, ''{0}'')", new Object[]{getCategoriesLucenePredicate(categoryNames, SEARCH_BOOST_LESS_IMPORTANT_FACTOR)}));
            // End fuzzy match group
            predicate.append(" )");

            searchTermPredicate = predicate.toString();
        } catch (RepositoryException e) {
            LOGGER.debug("An error occurred when getting blog data", e);
        }

        return searchTermPredicate;
    }

    private static String getCategoriesLucenePredicate(final List<String> categories, final String boostFactor) {
        final StringBuilder searchTermPredicate = new StringBuilder();

        for (String categoryName : categories) {
            if (categoryName.contains(" ")) {
                searchTermPredicate.append("\"").append(categoryName).append("\"");
            } else {
                searchTermPredicate.append(categoryName);
            }
            searchTermPredicate.append(boostFactor);
            searchTermPredicate.append(" OR ");
        }

        return StringUtils.stripEnd(searchTermPredicate.toString(), "OR ");
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
    private static String buildQuery(final String from,
                                     final String searchPath,
                                     final String customFilter,
                                     final String orderBy) {
        if (StringUtils.isNotBlank(orderBy)) {
            return String.format(BASE_QUERY_ORDERBY, from, StringUtils.defaultIfEmpty(searchPath, "/"),
                    customFilter, orderBy);
        }

        return String.format(BASE_QUERY, from, StringUtils.defaultIfEmpty(searchPath, "/"), customFilter);
    }

    private static int determineNumberOfPages(final int total, final int maxResultsPerPage) {
        int calcNumPages = total / maxResultsPerPage;
        if ((total % maxResultsPerPage) > 0) {
            calcNumPages++;
        }
        return calcNumPages;
    }

    private static List<Node> getPagedResults(final List<Node> results,
                                              final int pageNumber,
                                              final int maxResultsPerPage) {
        List<Node> nodeListPaged = new ArrayList<>(0);
        final int total = results.size();
        final int startRow = maxResultsPerPage * (pageNumber - 1);
        int newLimit = maxResultsPerPage;

        if (total > startRow) {
            if (total < startRow + maxResultsPerPage) {
                newLimit = total - startRow;
            }
            nodeListPaged = results.subList(startRow, startRow + newLimit);
        }

        return nodeListPaged;
    }

    private static List<String> convertCategoryValuesToNamesList(final Property categories)
            throws RepositoryException {
        List<String> categoryNames = new ArrayList<>(0);

        Value[] values = categories.getValues();
        String[] categoryIds = new String[values.length];

        for (int j = 0; j < values.length; j++) {
            try {
                categoryIds[j] = values[j].getString();
            } catch (RepositoryException e) {
                LOGGER.debug(e.getMessage());
            }
        }

        for (String categoryId : categoryIds) {
            try {
                Node category = NodeUtil.getNodeByIdentifier("category", categoryId);
                String displayName = PropertyUtil.getString(category, "displayName");
                categoryNames.add(displayName);
            } catch (RepositoryException e) {
                LOGGER.debug("An error occurred when getting category data", e);
            }
        }
        return categoryNames;
    }
}