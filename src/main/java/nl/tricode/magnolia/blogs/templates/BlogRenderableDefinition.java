/*
 * Tricode Blog module
 * Is a Blog module for Magnolia CMS.
 * Copyright (C) 2015  Tricode Business Integrators B.V.
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.tricode.magnolia.blogs.templates;

import com.google.common.collect.Maps;
import info.magnolia.cms.util.QueryUtil;
import info.magnolia.context.MgnlContext;
import info.magnolia.context.WebContext;
import info.magnolia.jcr.util.ContentMap;
import info.magnolia.jcr.util.NodeUtil;
import info.magnolia.rendering.model.RenderingModel;
import info.magnolia.rendering.model.RenderingModelImpl;
import info.magnolia.rendering.template.RenderableDefinition;
import info.magnolia.templating.functions.TemplatingFunctions;
import nl.tricode.magnolia.blogs.BlogsNodeTypes;
import nl.tricode.magnolia.blogs.util.BlogRepositoryConstants;
import nl.tricode.magnolia.blogs.util.BlogJcrUtils;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Magnolia {@link RenderableDefinition} for blog items.
 */
public class BlogRenderableDefinition<RD extends RenderableDefinition> extends RenderingModelImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlogRenderableDefinition.class);

    private static final int DEFAULT_LATEST_COUNT = 5;
    private static final String DEFAULT_LANGUAGE = "en";
    private static final String PARAM_CATEGORY = "category";
    private static final String PARAM_AUTHOR = "author";
    private static final String PARAM_PAGE = "page";
    private static final String PARAM_YEAR = "year";
    private static final String PARAM_MONTH = "month";
    private static final List<String> WHITELISTED_PARAMETERS = Arrays.asList(PARAM_CATEGORY, PARAM_AUTHOR, PARAM_PAGE, PARAM_YEAR, PARAM_MONTH);

    private final WebContext webContext = MgnlContext.getWebContext();
    private final TemplatingFunctions templatingFunctions;
    private final Map<String, String> filter;

    @Inject
    public BlogRenderableDefinition(Node content, RD definition, RenderingModel<?> parent, TemplatingFunctions templatingFunctions) {
        super(content, definition, parent);
        this.templatingFunctions = templatingFunctions;

        filter = Maps.newHashMap();

        final Iterator<Entry<String, String>> it = MgnlContext.getWebContext().getParameters().entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<String, String> pairs = it.next();
            if (WHITELISTED_PARAMETERS.contains(pairs.getKey()) && StringUtils.isNotEmpty(pairs.getValue())) {
                filter.put(pairs.getKey(), pairs.getValue());
                LOGGER.debug("Added to filter: {}", pairs.getKey());
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
    }

    @Override
    public String execute() {
        webContext.getResponse().setHeader("Cache-Control", "no-cache");
        return super.execute();
    }

    /**
     * Get all available nodes of type mgnl:blog.
     *
     * @param path          Start node path in hierarchy
     * @param maxResultSize Number of items to return. When empty <code>Integer.MAX_VALUE</code> will be used.
     * @return List of blog nodes sorted by date created in descending order
     * @throws RepositoryException Handling RepositoryException.
     */
    @SuppressWarnings("unused") //Used in freemarker components.
    public List<ContentMap> getBlogs(String path, String maxResultSize) throws RepositoryException {
        int resultSize = Integer.MAX_VALUE;
        if (StringUtils.isNumeric(maxResultSize)) {
            resultSize = Integer.parseInt(maxResultSize);
        }
        final String customFilters = constructAuthorPredicate() + constructCategoryPredicate(filter) + constructDateCreatedPredicate();
        final String sqlBlogItems = BlogJcrUtils.buildQuery(path, BlogsNodeTypes.Blog.NAME, true, customFilters);
        return templatingFunctions.asContentMapList(BlogJcrUtils.getWrappedNodesFromQuery(sqlBlogItems, resultSize, getPageNumber(), BlogsNodeTypes.Blog.NAME));
    }

    /**
     * Get latest nodes of type mgnl:blog.
     *
     * @param path          Start node path in hierarchy
     * @param maxResultSize Number of items to return. When empty <code>5</code> will be used.
     * @return List of blog nodes sorted by date created in descending order
     * @throws RepositoryException RepositoryException Handling RepositoryException.
     */
    @SuppressWarnings("unused") //Used in freemarker components.
    public List<ContentMap> getLatestBlogs(String path, String maxResultSize, boolean publishedBlogsOnly) throws RepositoryException {
        return getLatest(path, maxResultSize, BlogsNodeTypes.Blog.NAME, getPageNumber(), BlogsNodeTypes.Blog.NAME, publishedBlogsOnly);
    }

    /**
     * @param path          Repository path
     * @param maxResultSize the result size that is returned
     * @param categoryUuid  the category uuid to take only the blogs from this category
     * @throws RepositoryException Handling RepositoryException.
     * @return a list of blog nodes sorted by date created in descending order for the specified maxResultSize parameter
     */
    public List<ContentMap> getLatestBlogs(String path, String maxResultSize, String categoryUuid, boolean publishedBlogsOnly) throws RepositoryException {
        int resultSize = DEFAULT_LATEST_COUNT;
        if (StringUtils.isNumeric(maxResultSize)) {
            resultSize = Integer.parseInt(maxResultSize);
        }
        StringBuilder queryString = formQueryString(new StringBuilder(), categoryUuid, publishedBlogsOnly);
        return templatingFunctions.asContentMapList(BlogJcrUtils.getWrappedNodesFromQuery(
                "SELECT p.* from [mgnl:blog] AS p WHERE ISDESCENDANTNODE(p,'/') AND CONTAINS(p.categories, '" +
                        categoryUuid + "') " + queryString + " ORDER BY p.[mgnl:created] desc",
                resultSize, 1, BlogsNodeTypes.Blog.NAME));
    }

    /**
     * Get total number of blog posts for current state.
     * (Performs additional JCR-SQL2 query to obtain count!)
     *
     * @param path       Start node path in hierarchy
     * @param useFilters <code>true</code> to use filters
     * @throws RepositoryException Handling RepositoryException.
     * @return long Number of blog posts
     */
    public int getBlogCount(String path, boolean useFilters) throws RepositoryException {
        final String customFilters = constructAuthorPredicate() + constructCategoryPredicate(filter) + constructDateCreatedPredicate();
        final String sqlBlogItems = BlogJcrUtils.buildQuery(path, BlogsNodeTypes.Blog.NAME, useFilters, customFilters);
        return IteratorUtils.toList(QueryUtil.search(BlogRepositoryConstants.COLLABORATION, sqlBlogItems, Query.JCR_SQL2, BlogsNodeTypes.Blog.NAME)).size();
    }

    /**
     * @param filterProperty filter property
     * @param filterIdentifier filter identifier
     * @return The related blog count
     * @throws RepositoryException Handling RepositoryException.
     */
    public int getRelatedBlogCount(String filterProperty, String filterIdentifier) throws RepositoryException {
        final String sqlBlogItems = BlogJcrUtils.buildBlogCountQuery(filterProperty, filterIdentifier);
        return IteratorUtils.toList(QueryUtil.search(BlogRepositoryConstants.COLLABORATION, sqlBlogItems, Query.JCR_SQL2, BlogsNodeTypes.Blog.NAME)).size();
    }

    /**
     * Determine if older blog posts exists
     *
     * @param path          Path in the repository.
     * @param maxResultSize Maximum result size.
     * @throws RepositoryException Handling RepositoryException.
     * @return Boolean true when older blog posts exists
     */
    public boolean hasOlderPosts(String path, int maxResultSize) throws RepositoryException {
        final long totalBlogs = getBlogCount(path, true);
        final int pageNumber = getPageNumber();

        return hasOlderPosts(maxResultSize, totalBlogs, pageNumber);
    }

    /**
     * Determine the next following page number containing older blog posts
     *
     * @param path          Path in the repository.
     * @param maxResultSize Maximum result size.
     * @throws RepositoryException Handling RepositoryException.
     * @return page number with older blog posts
     */
    public int pageOlderPosts(String path, int maxResultSize) throws RepositoryException {
        if (hasOlderPosts(path, maxResultSize)) {
            return getPageNumber() + 1;
        } else {
            return getPageNumber();
        }
    }

    /**
     * Determine if newer blog posts exists
     *
     * @return Boolean true when newer blog posts exists
     */
    public Boolean hasNewerPosts() {
        return getPageNumber() > 1;
    }

    /**
     * Determine the previous following page number containing newer blog posts
     *
     * @return page number with newer blog posts
     */
    public int pageNewerPosts() {
        if (hasNewerPosts()) {
            return getPageNumber() - 1;
        } else {
            return getPageNumber();
        }
    }

    /**
     * Get categories for given blog node
     *
     * @param blog ContentMap of blog.
     * @return List of category nodes
     */
    public List<ContentMap> getBlogCategories(final ContentMap blog) {
        final List<ContentMap> categories = new ArrayList<>(0);

        try {
            final Value[] values = blog.getJCRNode().getProperty(BlogsNodeTypes.Blog.PROPERTY_CATEGORIES).getValues();
            if (values != null) {
                for (Value value : values) {
                    categories.add(templatingFunctions.contentById(value.getString(), BlogRepositoryConstants.CATEGORY));
                }
            }
        } catch (RepositoryException e) {
            LOGGER.error("Exception while getting categories: {}", e.getMessage());
        }
        return categories;
    }

    /**
     * Get category cloud items having a score based on total blogs and referenced categories.
     *
     * @return Collection of categories with relative score
     */
    public List<CloudMap> getCategoryCloud() {
        try {
            final Iterable<Node> nodes = NodeUtil.asIterable(QueryUtil.search(BlogRepositoryConstants.COLLABORATION, "SELECT p.* from [mgnl:category] AS p WHERE ISDESCENDANTNODE(p,'/')"));
            return getCloudData(nodes, BlogsNodeTypes.Blog.PROPERTY_CATEGORIES, false);
        } catch (RepositoryException e) {
            LOGGER.error("Exception while getting category cloud", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get category cloud items having a score based on total blogs and referenced categories.
     *
     * @return Collection of categories with relative score
     */
    public List<CloudMap> getAuthorCloud() {
        try {
            final Iterable<Node> nodes = NodeUtil.asIterable(QueryUtil.search(BlogRepositoryConstants.CONTACTS, "SELECT p.* from [mgnl:contact] AS p WHERE ISDESCENDANTNODE(p,'/')"));
            return getCloudData(nodes, BlogsNodeTypes.Blog.PROPERTY_AUTHOR, true);
        } catch (RepositoryException e) {
            LOGGER.error("Exception while getting author cloud", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get distinct year and month list for all available blogs
     *
     * @return A list containing properties <i>year</i> and <i>month</i>
     */
    public List<Map<String, Object>> getArchivedDates() {
        final Set<Map<String, Object>> set = new HashSet<>();

        for (Node blog : getAllBlogs()) {
            try {
                final Calendar dateCreated = blog.getProperty("mgnl:created").getDate();
                final int year = dateCreated.get(Calendar.YEAR);
                final int month = dateCreated.get(Calendar.MONTH) + 1;

                final Map<String, Object> map = new HashMap<>();
                map.put("year", Integer.toString(year));
                map.put("month", String.format("%02d", month));
                set.add(map);
            } catch (RepositoryException e) {
                LOGGER.debug("Exception getting created date", e);
            }
        }
        return new ArrayList<>(set);
    }

    /**
     * Get all available blogs starting from root node
     *
     * @return All blogs
     */
    public List<Node> getAllBlogs() {
        final String sqlBlogItems = BlogJcrUtils.buildQuery("/", BlogsNodeTypes.Blog.NAME);
        try {
            final NodeIterator items = QueryUtil.search(BlogRepositoryConstants.COLLABORATION, sqlBlogItems, Query.JCR_SQL2, BlogsNodeTypes.Blog.NAME);
            return NodeUtil.asList(NodeUtil.asIterable(items));
        } catch (RepositoryException e) {
            LOGGER.error("Exception getting all blogs", e);
            return Collections.emptyList();
        }
    }

    public int getPageNumber() {
        int pageNumber = 1;
        if (filter.containsKey(PARAM_PAGE)) {
            pageNumber = Integer.parseInt(filter.get(PARAM_PAGE));
        }
        return pageNumber;
    }

    protected String constructAuthorPredicate() {
        // todo ENHANCEMENT: this method should be private, but us still accessed directly by a test

        if (!filter.containsKey(PARAM_AUTHOR)) {
            return StringUtils.EMPTY;
        }

        final ContentMap contentMap = templatingFunctions.contentByPath(filter.get(PARAM_AUTHOR), BlogRepositoryConstants.CONTACTS);

        if (contentMap == null) {
            LOGGER.debug("Author '{}' does not exist", filter.get(PARAM_AUTHOR));
            return StringUtils.EMPTY;
        }

        final String authorId = (String) contentMap.get("@id");

        if (StringUtils.isNotEmpty(authorId)) {
            return "AND p.author = '" + authorId + "' ";
        }

        return StringUtils.EMPTY;
    }

    protected String constructDateCreatedPredicate() {
        // todo ENHANCEMENT: this method should be private, but us still accessed directly by a test

        if (!filter.containsKey(PARAM_YEAR)) {
            return StringUtils.EMPTY;
        }

        final int year = Integer.parseInt(filter.get(PARAM_YEAR));

        final Calendar start = Calendar.getInstance();
        start.set(year, Calendar.JANUARY, 1, 0, 0, 0);
        start.set(Calendar.MILLISECOND, 0);

        final Calendar end = Calendar.getInstance();
        end.set(year, Calendar.DECEMBER, 1, 23, 59, 59);
        end.set(Calendar.MILLISECOND, 999);

        if (filter.containsKey(PARAM_MONTH)) {
            final int month = Integer.parseInt(filter.get(PARAM_MONTH)) - 1;
            start.set(Calendar.MONTH, month);
            end.set(Calendar.MONTH, month);
        }

        // Determine last day of the end month
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));

        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        // Format start date and end data for use in jcr sql predicate
        return "AND p.[mgnl:created] >= CAST('" + dateFormat.format(start.getTime()) + "' AS DATE) " +
                "AND p.[mgnl:created] <= CAST('" + dateFormat.format(end.getTime()) + "' AS DATE) ";
    }

    private List<ContentMap> getLatest(String path, String maxResultSize, String nodeType, int pageNumber, String nodeTypeName, boolean publishedBlogsOnly) throws RepositoryException {
        int resultSize = DEFAULT_LATEST_COUNT;
        if (StringUtils.isNumeric(maxResultSize)) {
            resultSize = Integer.parseInt(maxResultSize);
        }
        final String sqlBlogItems = BlogJcrUtils.buildQuery(path, nodeType, publishedBlogsOnly, constructPublishDatePredicate(publishedBlogsOnly));
        return templatingFunctions.asContentMapList(BlogJcrUtils.getWrappedNodesFromQuery(sqlBlogItems, resultSize, pageNumber, nodeTypeName));
    }

    private String constructCategoryPredicate(final Map<String, String> filter) {
        if (!filter.containsKey(PARAM_CATEGORY)) {
            return StringUtils.EMPTY;
        }

        final ContentMap contentMap = templatingFunctions.contentByPath(filter.get(PARAM_CATEGORY), BlogRepositoryConstants.CATEGORY);

        if (contentMap == null) {
            LOGGER.debug("Category '{}' does not exist", filter.get(PARAM_CATEGORY));
            return StringUtils.EMPTY;
        }

        final String categoryId = (String) contentMap.get("@id");

        if (StringUtils.isNotEmpty(categoryId)) {
            return "AND p.categories like '%" + categoryId + "%' ";
        }

        return StringUtils.EMPTY;
    }

    private String constructPublishDatePredicate(boolean publishedBlogsOnly) {
        if(publishedBlogsOnly) {
            StringBuilder filterProperty = new StringBuilder("");
            filterProperty.append("AND ( p.publishDate IS NULL OR p.publishDate <= CAST('").append(LocalDateTime.now()).append("' AS DATE)) ");
            return filterProperty.toString();
        }else{
            return null;
        }
    }


    /**
     * Forms a query string like this "OR CONTAINS(p.categories, '"uuid"')" and appends it to each other.
     *
     * @param query        a new StringBuilder to keep the content on recursive calls
     * @param categoryUuid the uuid of the category
     * @return a query string used to filter the blogs by categories
     * @throws RepositoryException Handling RepositoryException.
     */
    private StringBuilder formQueryString(StringBuilder query, String categoryUuid, boolean publishedBlogsOnly) throws RepositoryException {
        List<ContentMap> childCategories = templatingFunctions.children(templatingFunctions.contentById(categoryUuid, BlogRepositoryConstants.CATEGORY));

        for (ContentMap childCategory : childCategories) {
            if (!templatingFunctions.children(childCategory).isEmpty()) {
                formQueryString(query, childCategory.getJCRNode().getIdentifier(),publishedBlogsOnly);
            }
            query.append("OR CONTAINS(p.categories, '").append(childCategory.getJCRNode().getIdentifier()).append("') ");
        }
        if(publishedBlogsOnly){
            query.append("AND ( p.publishDate IS NULL OR p.publishDate <= CAST('").append(LocalDateTime.now()).append("' AS DATE)) ");
        }
        return query;
    }

    private List<CloudMap> getCloudData(Iterable<Node> nodeList, String propName, boolean excludeNonExisting) throws RepositoryException {
        final List<CloudMap> cloudData = new ArrayList<>(0);
        final int maxCloudUsage = getBlogCount("/", false);

        for (Node entry : nodeList) {
            try {
                final int count = getRelatedBlogCount(propName, entry.getIdentifier());
                if (count == 0 && excludeNonExisting) {
                    continue;
                }

                final int scale = getScale(count, maxCloudUsage);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("{} blogs out of {} leaving a score of {}", count, maxCloudUsage, scale);
                }

                cloudData.add(new CloudMap(entry, count, scale));
            } catch (RepositoryException e) {
                LOGGER.warn("Exception getting related blog count", e);
            }
        }
        return cloudData;
    }

    public static String getMonthName(String month) {
        int monthNr = Integer.parseInt(month);

        Locale locale = new Locale(DEFAULT_LANGUAGE);

        DateFormatSymbols symbols = new DateFormatSymbols(locale);
        String[] monthNames = symbols.getMonths();
        return monthNames[monthNr - 1];
    }

    /**
     * Determine if older blog posts exists
     *
     * @param maxResultSize Maximum result size.
     * @return Boolean true when older blog posts exists
     */
    private static boolean hasOlderPosts(int maxResultSize, long totalBlogs, int pageNumber) throws RepositoryException {
        final int maxPage = (int) Math.ceil((double) totalBlogs / (double) maxResultSize);
        return maxPage >= pageNumber + 1;
    }

    private static int getScale(int count, int max) {
        int scale = 0;
        if (max > 0) {
            scale = (count * 10) / max;
        }
        if (scale < 0) {
            scale = 0;
        }
        if (scale > 9) {
            scale = 9;
        }
        return scale;
    }
}