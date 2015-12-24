/**
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
import nl.tricode.magnolia.blogs.util.BlogWorkspaceUtil;
import nl.tricode.magnolia.blogs.util.JcrUtils;
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
 * Magnolia Renderable Definition for blog items.
 */
public class BlogRenderableDefinition<RD extends RenderableDefinition> extends RenderingModelImpl {
	private static final Logger LOG = LoggerFactory.getLogger(BlogRenderableDefinition.class);

	private static final int DEFAULT_LATEST_COUNT = 5;
	private static final String BLOG_NODE_TYPE = "mgnl:blog";
	private static final String DEFAULT_LANGUAGE = "en";
	private static final String PARAM_CATEGORY = "category";
	private static final String PARAM_TAG = "tag";
	private static final String PARAM_AUTHOR = "author";
	private static final String PARAM_PAGE = "page";
	private static final String PARAM_YEAR = "year";
	private static final String PARAM_MONTH = "month";

	private final WebContext webContext = MgnlContext.getWebContext();

	private static final List<String> WHITELISTED_PARAMETERS = Arrays.asList(PARAM_CATEGORY, PARAM_TAG, PARAM_AUTHOR, PARAM_PAGE, PARAM_YEAR, PARAM_MONTH);

	private final Map<String, String> filter;

	protected final TemplatingFunctions templatingFunctions;

	@Override
	public String execute() {
		webContext.getResponse().setHeader("Cache-Control", "no-cache");
		return super.execute();
	}

	@Inject
	public BlogRenderableDefinition(Node content, RD definition, RenderingModel<?> parent, TemplatingFunctions templatingFunctions) {
		super(content, definition, parent);
		this.templatingFunctions = templatingFunctions;

		filter = new HashMap<>();
		final Iterator<Entry<String, String>> it = MgnlContext.getWebContext().getParameters().entrySet().iterator();
		while (it.hasNext()) {
			final Map.Entry<String, String> pairs = it.next();
			if (WHITELISTED_PARAMETERS.contains(pairs.getKey()) && StringUtils.isNotEmpty(pairs.getValue())) {
				filter.put(pairs.getKey(), pairs.getValue());
				LOG.debug("Added to filter: " + pairs.getKey());
			}
			it.remove(); // avoids a ConcurrentModificationException
		}
	}

	public TemplatingFunctions getTemplatingFunctions() {
		return templatingFunctions;
	}

	/**
	 * Get all available nodes of type mgnl:blog.
	 *
	 * @param path          Start node path in hierarchy
	 * @param maxResultSize Number of items to return. When empty <code>Integer.MAX_VALUE</code> will be used.
	 * @return List of blog nodes sorted by date created in descending order
	 * @throws RepositoryException
	 */
	public List<ContentMap> getBlogs(String path, String maxResultSize) throws RepositoryException {
		int resultSize = Integer.MAX_VALUE;
		if (StringUtils.isNumeric(maxResultSize)) {
			resultSize = Integer.parseInt(maxResultSize);
		}
		String customFilters = getAuthorPredicate() + getTagPredicate(filter) + getCategoryPredicate(filter) + getDateCreatedPredicate();
		final String sqlBlogItems = JcrUtils.buildQuery(path, BLOG_NODE_TYPE, true, customFilters);
		return templatingFunctions.asContentMapList(JcrUtils.getWrappedNodesFromQuery(sqlBlogItems, resultSize, getPageNumber(), BlogsNodeTypes.Blog.NAME));
	}

	/**
	 * Get latest nodes of type mgnl:blog.
	 *
	 * @param path          Start node path in hierarchy
	 * @param maxResultSize Number of items to return. When empty <code>5</code> will be used.
	 * @return List of blog nodes sorted by date created in descending order
	 * @throws RepositoryException
	 */
	@SuppressWarnings("unused") //Used in freemarker components.
	public List<ContentMap> getLatestBlogs(String path, String maxResultSize) throws RepositoryException {
		return getLatest(path, maxResultSize, BLOG_NODE_TYPE, getPageNumber(), BlogsNodeTypes.Blog.NAME);
	}

	public List<ContentMap> getLatest(String path, String maxResultSize, String nodeType, int pageNumber, String nodeTypeName) throws RepositoryException {
		int resultSize = DEFAULT_LATEST_COUNT;
		if (StringUtils.isNumeric(maxResultSize)) {
			resultSize = Integer.parseInt(maxResultSize);
		}
		final String sqlBlogItems = JcrUtils.buildQuery(path, nodeType);
		return templatingFunctions.asContentMapList(JcrUtils.getWrappedNodesFromQuery(sqlBlogItems, resultSize, pageNumber, nodeTypeName));
	}

	/**
	 * @param path          Repository path
	 * @param maxResultSize the result size that is returned
	 * @param categoryUuid  the category uuid to take only the blogs from this category
	 * @throws RepositoryException
	 * @returns a list of blog nodes sorted by date created in descending order for the specified maxResultSize parameter
	 */
	@SuppressWarnings("unused") //Used in freemarker components.
	public List<ContentMap> getLatestBlogs(String path, String maxResultSize, String categoryUuid) throws RepositoryException {
		int resultSize = DEFAULT_LATEST_COUNT;
		if (StringUtils.isNumeric(maxResultSize)) {
			resultSize = Integer.parseInt(maxResultSize);
		}
		StringBuilder queryString = formQueryString(new StringBuilder(), categoryUuid);
		return templatingFunctions.asContentMapList(JcrUtils.getWrappedNodesFromQuery(
				  "SELECT p.* from [mgnl:blog] AS p WHERE ISDESCENDANTNODE(p,'/') AND CONTAINS(p.categories, '" +
							 categoryUuid + "') " + queryString + " ORDER BY p.[mgnl:created] desc",
				  resultSize, 1, BlogsNodeTypes.Blog.NAME));
	}

	/**
	 * Forms a query string like this "OR CONTAINS(p.categories, '"uuid"')" and appends it to each other.
	 *
	 * @param query        a new StringBuilder to keep the content on recursive calls
	 * @param categoryUuid the uuid of the category
	 * @return a query string used to filter the blogs by categories
	 * @throws RepositoryException
	 */
	private StringBuilder formQueryString(StringBuilder query, String categoryUuid) throws RepositoryException {
		List<ContentMap> childCategories;
		if (categoryUuid.equalsIgnoreCase("cafebabe-cafe-babe-cafe-babecafebabe")) {
			childCategories = templatingFunctions.children(templatingFunctions.contentByPath("/", "categories"));
		} else {
			childCategories = templatingFunctions.children(templatingFunctions.contentById(categoryUuid, "categories"));
		}
		for (ContentMap childCategory : childCategories) {
			if (!templatingFunctions.children(childCategory).isEmpty()) {
				formQueryString(query, childCategory.getJCRNode().getIdentifier());
			}
			query.append("OR CONTAINS(p.categories, '").append(childCategory.getJCRNode().getIdentifier()).append("') ");
		}
		return query;
	}

	/**
	 * Get total number of blog posts for current state.
	 * (Performs additional JCR-SQL2 query to obtain count!)
	 *
	 * @param path       Start node path in hierarchy
	 * @param useFilters <code>true</code> to use filters
	 * @return long Number of blog posts
	 */
	public int getBlogCount(String path, boolean useFilters) throws RepositoryException {
		String customFilters = getAuthorPredicate() + getTagPredicate(filter) + getCategoryPredicate(filter) + getDateCreatedPredicate();
		final String sqlBlogItems = JcrUtils.buildQuery(path, BLOG_NODE_TYPE, useFilters, customFilters);
		return IteratorUtils.toList(QueryUtil.search(BlogWorkspaceUtil.COLLABORATION, sqlBlogItems, Query.JCR_SQL2, BlogsNodeTypes.Blog.NAME)).size();
	}

	public int getRelatedBlogCount(String filterProperty, String filterIdentifier) throws RepositoryException {
		final String sqlBlogItems = JcrUtils.buildBlogCountQuery(filterProperty, filterIdentifier);
		return IteratorUtils.toList(QueryUtil.search(BlogWorkspaceUtil.COLLABORATION, sqlBlogItems, Query.JCR_SQL2, BlogsNodeTypes.Blog.NAME)).size();
	}

	/**
	 * Determine if older blog posts exists
	 *
	 * @param path          Path in the repository.
	 * @param maxResultSize Maximum result size.
	 * @return Boolean true when older blog posts exists
	 */
	public boolean hasOlderPosts(String path, int maxResultSize) throws RepositoryException {
		final long totalBlogs = getBlogCount(path, true);
		final int pageNumber = getPageNumber();

		return hasOlderPosts(path, maxResultSize, totalBlogs, pageNumber);
	}

	/**
	 * Determine if older blog posts exists
	 *
	 * @param path          Path in the repository.
	 * @param maxResultSize Maximum result size.
	 * @return Boolean true when older blog posts exists
	 */
	public boolean hasOlderPosts(String path, int maxResultSize, long totalBlogs, int pageNumber) throws RepositoryException {
		final int maxPage = (int) Math.ceil((double) totalBlogs / (double) maxResultSize);
		return maxPage >= pageNumber + 1;
	}

	/**
	 * Determine the next following page number containing older blog posts
	 *
	 * @param path          Path in the repository.
	 * @param maxResultSize Maximum result size.
	 * @return page number with older blog posts
	 */
	@SuppressWarnings("unused") //Used in freemarker components.
	public int pageOlderPosts(String path, int maxResultSize) throws RepositoryException {
		return (hasOlderPosts(path, maxResultSize)) ? getPageNumber() + 1 : getPageNumber();
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
	@SuppressWarnings("unused") //Used in freemarker components.
	public int pageNewerPosts() {
		return (hasNewerPosts()) ? getPageNumber() - 1 : getPageNumber();
	}

	/**
	 * Get tags for given blog node
	 *
	 * @param blog ContentMap of blogs.
	 * @return List of tag nodes
	 */
	@SuppressWarnings("unused") //Used in freemarker components.
	public List<ContentMap> getBlogTags(ContentMap blog) {
		return getItems(blog.getJCRNode(), BlogsNodeTypes.Blog.PROPERTY_TAGS, BlogWorkspaceUtil.COLLABORATION);
	}

	/**
	 * Get tag cloud items having a score based on total blogs and referenced tags.
	 *
	 * @return Collection of tags with relative score
	 */
	@SuppressWarnings("unused") //Used in freemarker components.
	public List<CloudMap> getTagCloud() {
		try {
			final Iterable<Node> nodes = NodeUtil.asIterable(QueryUtil.search(BlogWorkspaceUtil.COLLABORATION, "SELECT p.* from [mgnl:tag] AS p WHERE ISDESCENDANTNODE(p,'/')"));
			return getCloudData(nodes, BlogsNodeTypes.Blog.PROPERTY_TAGS, false);
		} catch (RepositoryException e) {
			LOG.error("Exception while getting tag cloud", e.getMessage());
			return Collections.emptyList();
		}
	}

	/**
	 * Get categories for given blog node
	 *
	 * @param blog ContentMap of blogs.
	 * @return List of category nodes
	 */
	@SuppressWarnings("unused") //Used in freemarker components.
	public List<ContentMap> getBlogCategories(ContentMap blog) {
		return getItems(blog.getJCRNode(), BlogsNodeTypes.Blog.PROPERTY_CATEGORIES, BlogWorkspaceUtil.COLLABORATION);
	}

	/**
	 * Get category cloud items having a score based on total blogs and referenced categories.
	 *
	 * @return Collection of categories with relative score
	 */
	@SuppressWarnings("unused") //Used in freemarker components.
	public List<CloudMap> getCategoryCloud() {
		try {
			final Iterable<Node> nodes = NodeUtil.asIterable(QueryUtil.search(BlogWorkspaceUtil.COLLABORATION, "SELECT p.* from [mgnl:category] AS p WHERE ISDESCENDANTNODE(p,'/')"));
			return getCloudData(nodes, BlogsNodeTypes.Blog.PROPERTY_CATEGORIES, false);
		} catch (RepositoryException e) {
			LOG.error("Exception while getting category cloud", e.getMessage());
			return Collections.emptyList();
		}
	}

	/**
	 * Get category cloud items having a score based on total blogs and referenced categories.
	 *
	 * @return Collection of categories with relative score
	 */
	@SuppressWarnings("unused") //Used in freemarker components.
	public List<CloudMap> getAuthorCloud() {
		try {
			final Iterable<Node> nodes = NodeUtil.asIterable(QueryUtil.search(BlogWorkspaceUtil.CONTACTS, "SELECT p.* from [mgnl:contact] AS p WHERE ISDESCENDANTNODE(p,'/')"));
			return getCloudData(nodes, BlogsNodeTypes.Blog.PROPERTY_AUTHOR, true);
		} catch (RepositoryException e) {
			LOG.error("Exception while getting author cloud", e.getMessage());
			return Collections.emptyList();
		}
	}

	/**
	 * Get distinct year and month list for all available blogs
	 *
	 * @return List<Map<String, Object>> containing properties <i>year</i> and <i>month</i>
	 */
	@SuppressWarnings("unused") //Used in freemarker components.
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
				LOG.debug("Exception getting created date", e.getMessage());
			}
		}
		return new ArrayList<>(set);
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
				if (LOG.isDebugEnabled()) {
					LOG.debug(count + " blogs out of " + maxCloudUsage + " leaving a score of " + scale);
				}

				cloudData.add(new CloudMap(entry, count, scale));
			} catch (RepositoryException e) {
				LOG.warn("Exception getting related blog count", e.getMessage());
			}
		}
		return cloudData;
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

	/**
	 * Get all available blogs starting from root node
	 *
	 * @return List<Node> blogs
	 */
	public List<Node> getAllBlogs() {
		final String sqlBlogItems = JcrUtils.buildQuery("/", BLOG_NODE_TYPE);
		try {
			final NodeIterator items = QueryUtil.search(BlogWorkspaceUtil.COLLABORATION, sqlBlogItems, Query.JCR_SQL2, BlogsNodeTypes.Blog.NAME);
			return NodeUtil.asList(NodeUtil.asIterable(items));
		} catch (RepositoryException e) {
			LOG.error("Exception getting all blogs", e.getMessage());
			return Collections.emptyList();
		}
	}

	private int getPageNumber() {
		int pageNumber = 1;
		if (filter.containsKey(PARAM_PAGE)) {
			pageNumber = Integer.parseInt(filter.get(PARAM_PAGE));
		}
		return pageNumber;
	}

	protected String getAuthorPredicate() {
		if (filter.containsKey(PARAM_AUTHOR)) {
			final ContentMap contentMap = templatingFunctions.contentByPath(filter.get(PARAM_AUTHOR), BlogWorkspaceUtil.CONTACTS);
			if (contentMap != null) {
				final String authorId = (String) contentMap.get("@id");
				if (StringUtils.isNotEmpty(authorId)) {
					return "AND p.author = '" + authorId + "' ";
				}
			} else {
				LOG.debug("Author [{}] not found", filter.get(PARAM_AUTHOR));
			}
		}
		return StringUtils.EMPTY;
	}

	protected String getDateCreatedPredicate() {
		if (filter.containsKey(PARAM_YEAR)) {
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
		return StringUtils.EMPTY;
	}

	public static String getMonthName(String month) {
		int monthNr = Integer.parseInt(month);

		Locale locale = new Locale(DEFAULT_LANGUAGE);

		DateFormatSymbols symbols = new DateFormatSymbols(locale);
		String[] monthNames = symbols.getMonths();
		return monthNames[monthNr - 1];
	}

	protected String getTagPredicate(Map<String, String> filter) {
		if (filter.containsKey(PARAM_TAG)) {
			final ContentMap contentMap = templatingFunctions.contentByPath(filter.get(PARAM_TAG), BlogWorkspaceUtil.COLLABORATION);
			if (contentMap != null) {
				final String tagId = (String) contentMap.get("@id");
				if (StringUtils.isNotEmpty(tagId)) {
					return "AND p.tags like '%" + tagId + "%' ";
				}
			} else {
				LOG.debug("Tag [{}] not found", filter.get(PARAM_TAG));
			}
		}
		return StringUtils.EMPTY;
	}

	protected String getCategoryPredicate(Map<String, String> filter) {
		if (filter.containsKey(PARAM_CATEGORY)) {
			final ContentMap contentMap = templatingFunctions.contentByPath(filter.get(PARAM_CATEGORY), BlogWorkspaceUtil.CATEGORIES);
			if (contentMap != null) {
				final String categoryId = (String) contentMap.get("@id");
				if (StringUtils.isNotEmpty(categoryId)) {
					return "AND p.categories like '%" + categoryId + "%' ";
				}
			} else {
				LOG.debug("Category [{}] not found", filter.get(PARAM_CATEGORY));
			}
		}
		return StringUtils.EMPTY;
	}

	public List<ContentMap> getItems(Node item, String nodeType, String workspace) {
		final List<ContentMap> items = new ArrayList<>(0);

		try {
			final Value[] values = item.getProperty(nodeType).getValues();
			if (values != null) {
				for (Value value : values) {
					items.add(templatingFunctions.contentById(value.getString(), workspace));
				}
			}
		} catch (RepositoryException e) {
			LOG.error("Exception while getting items", e.getMessage());
		}
		return items;
	}
}