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
package nl.tricode.magnolia.blogs.util;

import info.magnolia.cms.util.QueryUtil;
import info.magnolia.jcr.wrapper.I18nNodeWrapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public final class JcrUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(JcrUtils.class);

    private JcrUtils() {
        // Util class, prevent instantiating
    }

    /**
     * Query blog items using JCR SQL2 syntax.
     *
     * @param query         Query string
     * @param maxResultSize Max results returned
     * @param pageNumber    paging number
     * @param nodeTypeName
     * @return List of blog nodes
     * @throws javax.jcr.RepositoryException In case of read error
     */
    public static List<Node> getWrappedNodesFromQuery(final String query,
                                                      final int maxResultSize,
                                                      final int pageNumber,
                                                      final String nodeTypeName) throws RepositoryException {
        return getWrappedNodesFromQuery(query, maxResultSize, pageNumber, nodeTypeName, BlogRepositoryConstants.COLLABORATION);
    }

    /**
     * @param path
     * @param contentType
     * @return
     */
    public static String buildQuery(final String path, final String contentType) {
        return buildQuery(path, contentType, false, null);
    }

    /**
     * @param path
     * @param contentType
     * @param useFilters
     * @param customFilters
     * @return
     */
    public static String buildQuery(final String path,
                                    final String contentType,
                                    final boolean useFilters,
                                    final String customFilters) {
        final StringBuilder query = new StringBuilder("SELECT p.* FROM [")
                .append(contentType)
                .append("] AS p WHERE ISDESCENDANTNODE(p, '")
                .append(StringUtils.defaultIfEmpty(path, "/"))
                .append("') ");

        if (useFilters) {
            query.append(customFilters);
        }

        query.append("ORDER BY p.[mgnl:created] desc");
        LOGGER.debug("BuildQuery [{}].", query.toString());
        return query.toString();
    }

    /**
     * @param filterProperty
     * @param filterIdentifier
     * @return
     */
    public static String buildBlogCountQuery(final String filterProperty, final String filterIdentifier) {
        final StringBuilder query = new StringBuilder("SELECT p.* from [mgnl:blog] AS p WHERE ISDESCENDANTNODE(p,'/') AND contains(p.")
                .append(filterProperty).append(", '")
                .append(filterIdentifier).append("')");

        LOGGER.debug("buildBlogCountQuery [{}].", query.toString());
        return query.toString();
    }

    private static List<Node> getWrappedNodesFromQuery(String query, int maxResultSize, int pageNumber, String nodeTypeName, String workspace) throws RepositoryException {
        final List<Node> itemsListPaged = new ArrayList<>(0);
        final NodeIterator items = QueryUtil.search(workspace, query, Query.JCR_SQL2, nodeTypeName);

        // Paging result set
        final int startRow = (maxResultSize * (pageNumber - 1));
        if (startRow > 0) {
            try {
                items.skip(startRow);
            } catch (NoSuchElementException e) {
                LOGGER.info("No more blog items found beyond this item number: {}", startRow);
            }
        }

        int count = 1;
        while (items.hasNext() && count <= maxResultSize) {
            itemsListPaged.add(new I18nNodeWrapper(items.nextNode()));
            count++;
        }

        return itemsListPaged;
    }

}