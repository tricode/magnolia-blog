package nl.tricode.magnolia.blogs.util;

import info.magnolia.cms.util.QueryUtil;
import info.magnolia.jcr.wrapper.I18nNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author mvdmark
 */
public class JcrUtils {
	private static final Logger LOG = LoggerFactory.getLogger(JcrUtils.class);

	/**
	 * Query blog items using JCR SQL2 syntax.
	 *
	 * @param query         Query string
	 * @param maxResultSize Max results returned
	 * @param pageNumber    paging number
	 * @return List<Node> List of blog nodes
	 * @throws javax.jcr.RepositoryException
	 */
	public static List<Node> getWrappedNodesFromQuery(String query, int maxResultSize, int pageNumber, String nodeTypeName) throws RepositoryException {
		return getWrappedNodesFromQuery(query, maxResultSize, pageNumber, nodeTypeName, BlogWorkspaceUtil.COLLABORATION);
	}

	/**
	 * Query items using JCR SQL2 syntax.
	 *
	 * @param query         Query string
	 * @param maxResultSize Max results returned
	 * @param pageNumber    paging number
	 * @return List<Node> List of nodes
	 * @throws javax.jcr.RepositoryException
	 */
	public static List<Node> getWrappedNodesFromQuery(String query, int maxResultSize, int pageNumber, String nodeTypeName, String workspace) throws RepositoryException {
		final List<Node> itemsListPaged = new ArrayList<>(0);
		final NodeIterator items = QueryUtil.search(workspace, query, Query.JCR_SQL2, nodeTypeName);

		// Paging result set
		final int startRow = (maxResultSize * (pageNumber - 1));
		if (startRow > 0) {
			try {
				items.skip(startRow);
			} catch (NoSuchElementException e) {
				LOG.error("No more blog items found beyond this item number: " + startRow);
			}
		}

		int count = 1;
		while (items.hasNext() && count <= maxResultSize) {
			itemsListPaged.add(new I18nNodeWrapper(items.nextNode()));
			count++;
		}

		return itemsListPaged;
	}

	public static String buildQuery(String path, String contentType) {
		return buildQuery(path, contentType, false, null);
	}

	public static String buildQuery(String path, String contentType, boolean useFilters, String customFilters) {
		StringBuilder query = new StringBuilder();

		query.append("SELECT p.* FROM [").append(contentType).append("] AS p ");
		query.append("WHERE ISDESCENDANTNODE(p, '").append(org.apache.commons.lang.StringUtils.defaultIfEmpty(path, "/")).append("') ");

		if (useFilters) {
			query.append(customFilters);
		}

		query.append("ORDER BY p.[mgnl:created] desc");
		LOG.debug("BuildQuery [" + query.toString() + "].");
		return query.toString();
	}

	public static String buildBlogCountQuery(String filterProperty, String filterIdentifier) {
		StringBuilder query = new StringBuilder();

		query.append("SELECT p.* from [mgnl:blog] AS p WHERE ISDESCENDANTNODE(p,'/') AND contains(p.")
				  .append(filterProperty).append(", '")
				  .append(filterIdentifier).append("')");

		LOG.debug("buildBlogCountQuery [" + query.toString() + "].");
		return query.toString();
	}
}