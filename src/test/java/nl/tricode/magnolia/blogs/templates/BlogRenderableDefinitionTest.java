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
package nl.tricode.magnolia.blogs.templates;

import info.magnolia.cms.i18n.I18nContentSupport;
import info.magnolia.context.MgnlContext;
import info.magnolia.context.WebContext;
import info.magnolia.jcr.util.ContentMap;
import info.magnolia.objectfactory.ComponentProvider;
import info.magnolia.objectfactory.Components;
import info.magnolia.rendering.model.RenderingModel;
import info.magnolia.rendering.template.RenderableDefinition;
import info.magnolia.templating.functions.TemplatingFunctions;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.inject.Provider;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class BlogRenderableDefinitionTest {

    private static final String WORKSPACE = "collaboration";

    private BlogRenderableDefinition<RenderableDefinition> definition;

    @Mock
    private Node mockNode;
    @Mock
    private RenderableDefinition mockDefinition;
    @Mock
    private RenderingModel mockParent;

    private TemplatingFunctions spyTemplatingFunctions;
    @Mock
    private WebContext mockWebContext;
    @Mock
    private ComponentProvider mockComponentProvider;
    @Mock
    private I18nContentSupport i18nContentSupport;

    private Map<String, String> parameters = new HashMap<String, String>();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        MgnlContext.setInstance(mockWebContext);
        doReturn(parameters).when(mockWebContext).getParameters();

        Components.setComponentProvider(mockComponentProvider);
        doReturn(i18nContentSupport).when(mockComponentProvider).getComponent(I18nContentSupport.class);

        spyTemplatingFunctions = Mockito.spy(new TemplatingFunctions(mock(Provider.class)));
    }

    @Test
    public void testGetBlogs() throws Exception {
        createInstance();

        List<Node> nodes = Arrays.asList(createMockNode("mgnl:blog"));

        String expectedQuery = "SELECT p.* FROM [mgnl:blog] AS p WHERE ISDESCENDANTNODE(p, '/') ORDER BY p.[mgnl:created] desc";

        executeMockQuery(WORKSPACE, expectedQuery, nodes);

        List<ContentMap> blogs = definition.getBlogs("/", "10");
        Assert.assertEquals(nodes.size(), blogs.size());
    }

    @Test
    public void testGetBlogsWithResultSize() throws Exception {
        createInstance();

        List<Node> nodes = createMockNodes("mgnl:blog", 10);

        String expectedQuery = "SELECT p.* FROM [mgnl:blog] AS p WHERE ISDESCENDANTNODE(p, '/') ORDER BY p.[mgnl:created] desc";

        executeMockQuery(WORKSPACE, expectedQuery, nodes);

        List<ContentMap> blogs = definition.getBlogs("/", "5");
        Assert.assertEquals(5, blogs.size());
    }

    @Test
    public void testGetBlogsWithResultSizeAndPageNumber() throws Exception {
        parameters.put("page", "2");
        createInstance();

        List<Node> nodes = createMockNodes("mgnl:blog", 11);

        String expectedQuery = "SELECT p.* FROM [mgnl:blog] AS p WHERE ISDESCENDANTNODE(p, '/') ORDER BY p.[mgnl:created] desc";

        executeMockQuery(WORKSPACE, expectedQuery, nodes);

        List<ContentMap> blogs = definition.getBlogs("/", "5");
        Assert.assertEquals(5, blogs.size());
    }

    @Test
    public void testGetBlogsWithTooHighPageNumber() throws Exception {
        parameters.put("page", "2");
        createInstance();

        List<Node> nodes = createMockNodes("mgnl:blog", 2);

        String expectedQuery = "SELECT p.* FROM [mgnl:blog] AS p WHERE ISDESCENDANTNODE(p, '/') ORDER BY p.[mgnl:created] desc";

        executeMockQuery(WORKSPACE, expectedQuery, nodes);

        List<ContentMap> blogs = definition.getBlogs("/", "5");
        Assert.assertEquals(0, blogs.size());
    }

    @Test
    public void testAuthorPredicateWithoutParameters() {
        createInstance();

        String predicate = definition.getAuthorPredicate();
        Assert.assertEquals(StringUtils.EMPTY, predicate);
    }

    @Test
    public void testAuthorPredicateWithAuthorNotFound() {
        String id = "louisvutton";
        parameters.put("author", id);
        createInstance();

        String predicate = definition.getAuthorPredicate();
        Assert.assertEquals(StringUtils.EMPTY, predicate);
    }

    @Test
    public void testAuthorPredicateWithExistingAuthor() {
        String id = "louisvutton";
        parameters.put("author", id);
        createInstance();

        ContentMap contentMap = mock(ContentMap.class);

        doReturn(contentMap).when(spyTemplatingFunctions).contentByPath(id, "contacts");
        doReturn(id).when(contentMap).get("@id");

        String predicate = definition.getAuthorPredicate();
        Assert.assertEquals("AND p.author = 'louisvutton' ", predicate);
    }

    @Test
    public void testDateCreatedPredicateWithoutParameters() {
        createInstance();

        String predicate = definition.getDateCreatedPredicate();
        Assert.assertEquals(StringUtils.EMPTY, predicate);
    }

    @Test
    public void testDateCreatedPredicateWithYear() {
        parameters.put("year", "2011");
        createInstance();

        String predicate = definition.getDateCreatedPredicate();
        Assert.assertEquals("AND p.[mgnl:created] >= CAST('2011-01-01T00:00:00.000Z' AS DATE) " +
                "AND p.[mgnl:created] <= CAST('2011-12-31T23:59:59.999Z' AS DATE) ", predicate);
    }

    @Test
    public void testDateCreatedPredicateWithYearAndMonthEmpty() {
        parameters.put("year", "2011");
        parameters.put("month", "");
        createInstance();

        String predicate = definition.getDateCreatedPredicate();
        Assert.assertEquals("AND p.[mgnl:created] >= CAST('2011-01-01T00:00:00.000Z' AS DATE) " +
                "AND p.[mgnl:created] <= CAST('2011-12-31T23:59:59.999Z' AS DATE) ", predicate);
    }

    @Test
    public void testDateCreatedPredicateWithYearAndMonthZero() {
        parameters.put("year", "2011");
        parameters.put("month", "0");
        createInstance();

        String predicate = definition.getDateCreatedPredicate();
        Assert.assertEquals("AND p.[mgnl:created] >= CAST('2010-12-01T00:00:00.000Z' AS DATE) " +
                "AND p.[mgnl:created] <= CAST('2010-12-31T23:59:59.999Z' AS DATE) ", predicate);
    }

    @Test
    public void testDateCreatedPredicateWithYearAndMonthJanuary() {
        parameters.put("year", "2011");
        parameters.put("month", "1");
        createInstance();

        String predicate = definition.getDateCreatedPredicate();
        Assert.assertEquals("AND p.[mgnl:created] >= CAST('2011-01-01T00:00:00.000Z' AS DATE) " +
                "AND p.[mgnl:created] <= CAST('2011-01-31T23:59:59.999Z' AS DATE) ", predicate);
    }

    @Test
    public void testDateCreatedPredicateWithYearAndMonthFebruary() {
        parameters.put("year", "2011");
        parameters.put("month", "2");
        createInstance();

        String predicate = definition.getDateCreatedPredicate();
        Assert.assertEquals("AND p.[mgnl:created] >= CAST('2011-02-01T00:00:00.000Z' AS DATE) " +
                "AND p.[mgnl:created] <= CAST('2011-02-28T23:59:59.999Z' AS DATE) ", predicate);
    }

    @Test
    public void testDateCreatedPredicateWithYearAndMonthDecember() {
        parameters.put("year", "2011");
        parameters.put("month", "12");
        createInstance();

        String predicate = definition.getDateCreatedPredicate();
        Assert.assertEquals("AND p.[mgnl:created] >= CAST('2011-12-01T00:00:00.000Z' AS DATE) " +
                "AND p.[mgnl:created] <= CAST('2011-12-31T23:59:59.999Z' AS DATE) ", predicate);
    }

    @Test
    public void testDateCreatedPredicateWithYearAndMonthThirteen() {
        parameters.put("year", "2011");
        parameters.put("month", "13");
        createInstance();

        String predicate = definition.getDateCreatedPredicate();
        Assert.assertEquals("AND p.[mgnl:created] >= CAST('2012-01-01T00:00:00.000Z' AS DATE) " +
                "AND p.[mgnl:created] <= CAST('2012-01-31T23:59:59.999Z' AS DATE) ", predicate);
    }

    @Test
    public void testGetMonthName() {
        String may = definition.getMonthName("05");
        String december = definition.getMonthName("12");

        Assert.assertEquals("May", may);
        Assert.assertEquals("December", december);
    }

    private void executeMockQuery(String workspace, String expectedQuery, List<Node> results) throws Exception {
        Session mockSession = mock(Session.class);
        Workspace mockWorkspace = mock(Workspace.class);
        QueryManager mockQueryManager = mock(QueryManager.class);
        Query mockQuery = mock(Query.class);
        QueryResult mockQueryResult = mock(QueryResult.class);

        doReturn(mockSession).when(mockWebContext).getJCRSession(workspace);
        doReturn(mockWorkspace).when(mockSession).getWorkspace();
        doReturn(mockQueryManager).when(mockWorkspace).getQueryManager();

        doReturn(mockQuery).when(mockQueryManager).createQuery(expectedQuery, Query.JCR_SQL2);
        doReturn(mockQueryResult).when(mockQuery).execute();
        doReturn(new NodeIteratorAdapter(results)).when(mockQueryResult).getNodes();
    }

    private void createInstance() {
        definition = new BlogRenderableDefinition<>(mockNode, mockDefinition, mockParent, spyTemplatingFunctions);
    }

    private static List<Node> createMockNodes(String nodeType, int count) throws Exception {
        final List<Node> mockList = new ArrayList<Node>(count);
        for (int i = 0; i < count; i++) {
            mockList.add(createMockNode(nodeType));
        }
        return mockList;
    }

    private static Node createMockNode(String nodeType) throws Exception {
        Node mockNode = mock(Node.class);
        doReturn(true).when(mockNode).isNodeType(nodeType);
        doReturn(1).when(mockNode).getDepth();
        doReturn("/" + UUID.randomUUID().toString()).when(mockNode).getPath();
        return mockNode;
    }
}