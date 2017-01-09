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
package nl.tricode.magnolia.blogs.column;

import com.vaadin.ui.Table;
import info.magnolia.contacts.app.ContactsNodeTypes;
import info.magnolia.context.MgnlContext;
import info.magnolia.context.WebContext;
import info.magnolia.jcr.util.NodeTypes;
import info.magnolia.ui.vaadin.integration.jcr.JcrItemAdapter;
import info.magnolia.ui.workbench.column.definition.PropertyColumnDefinition;
import nl.tricode.magnolia.blogs.BlogsNodeTypes;
import nl.tricode.magnolia.blogs.util.BlogRepositoryConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Unit test(s) for {@link AuthorNameColumnFormatter}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthorNameColumnFormatterTest {

    private AuthorNameColumnFormatter formatter;

    @Mock
    private WebContext mockWebContext;
    @Mock
    private Table mockTable;

    @Before
    public void setUp() throws Exception {
        formatter = new AuthorNameColumnFormatter(new PropertyColumnDefinition());

        MgnlContext.setInstance(mockWebContext);
    }

    @Test
    public void testGenerateCellWithUnknownId() throws Exception {
        Object itemId = "1";

        Object result = formatter.generateCell(mockTable, itemId, null);
        assertEquals(StringUtils.EMPTY, result);
    }

    @Test
    public void testGenerateCellWithProperty() throws Exception {
        Object itemId = "1";

        JcrItemAdapter mockItem = mock(JcrItemAdapter.class);
        Property mockProperty = mock(Property.class);

        doReturn(mockItem).when(mockTable).getItem(itemId);
        doReturn(mockProperty).when(mockItem).getJcrItem();
        doReturn(false).when(mockProperty).isNode();

        Object result = formatter.generateCell(mockTable, itemId, null);
        assertEquals(StringUtils.EMPTY, result);
    }

    @Test
    public void testGenerateCellWithoutBlogNode() throws Exception {
        Object itemId = "1";

        JcrItemAdapter mockItem = mock(JcrItemAdapter.class);
        Node mockNode = mock(Node.class);

        doReturn(mockItem).when(mockTable).getItem(itemId);
        doReturn(mockNode).when(mockItem).getJcrItem();
        doReturn(true).when(mockItem).isNode();
        doReturn(true).when(mockNode).isNode();
        doReturnIsNodeType(mockNode, NodeTypes.Page.NAME);

        Object result = formatter.generateCell(mockTable, itemId, null);
        assertEquals(StringUtils.EMPTY, result);
    }

    @Test
    public void testGenerateCellWithBlogNodeWithoutAuthorId() throws Exception {
        Object itemId = "1";

        JcrItemAdapter mockItem = mock(JcrItemAdapter.class);
        Node mockNode = mock(Node.class);

        doReturn(mockItem).when(mockTable).getItem(itemId);
        doReturn(mockNode).when(mockItem).getJcrItem();
        doReturn(true).when(mockItem).isNode();
        doReturn(true).when(mockNode).isNode();
        doReturnIsNodeType(mockNode, BlogsNodeTypes.Blog.NAME);

        Object result = formatter.generateCell(mockTable, itemId, null);
        assertEquals(StringUtils.EMPTY, result);
    }

    @Test
    public void testGenerateCellWithBlogNodeWithEmptyAuthorId() throws Exception {
        Object itemId = "1";

        JcrItemAdapter mockItem = mock(JcrItemAdapter.class);
        Node mockNode = mock(Node.class);

        doReturn(mockItem).when(mockTable).getItem(itemId);
        doReturn(mockNode).when(mockItem).getJcrItem();
        doReturn(true).when(mockItem).isNode();
        doReturn(true).when(mockNode).isNode();
        doReturnIsNodeType(mockNode, BlogsNodeTypes.Blog.NAME);
        doReturnProperty(mockNode, BlogsNodeTypes.Blog.PROPERTY_AUTHOR, StringUtils.EMPTY);

        Object result = formatter.generateCell(mockTable, itemId, null);
        assertEquals(StringUtils.EMPTY, result);
    }

    @Test
    public void testGenerateCellWithBlogNodeWithoutAuthor() throws Exception {
        Object itemId = "1";
        String authorId = "123";

        JcrItemAdapter mockItem = mock(JcrItemAdapter.class);
        Node mockNode = mock(Node.class);

        doReturn(mockItem).when(mockTable).getItem(itemId);
        doReturn(mockNode).when(mockItem).getJcrItem();
        doReturn(true).when(mockItem).isNode();
        doReturn(true).when(mockNode).isNode();
        doReturnIsNodeType(mockNode, BlogsNodeTypes.Blog.NAME);
        doReturnProperty(mockNode, BlogsNodeTypes.Blog.PROPERTY_AUTHOR, authorId);

        Object result = formatter.generateCell(mockTable, itemId, null);
        assertEquals(StringUtils.EMPTY, result);
    }

    @Test
    public void testGenerateCellWithBlogNodeWithFullAuthorName() throws Exception {
        Object itemId = "1";
        String authorId = "123";
        String firstName = "hans";
        String lastName = "de boer";

        JcrItemAdapter mockItem = mock(JcrItemAdapter.class);
        Node mockNode = mock(Node.class);
        Session mockSession = mock(Session.class);
        Node mockAuthorNode = mock(Node.class);

        doReturn(mockItem).when(mockTable).getItem(itemId);
        doReturn(mockNode).when(mockItem).getJcrItem();
        doReturn(true).when(mockItem).isNode();
        doReturn(true).when(mockNode).isNode();

        doReturnIsNodeType(mockNode, BlogsNodeTypes.Blog.NAME);
        doReturnProperty(mockNode, BlogsNodeTypes.Blog.PROPERTY_AUTHOR, authorId);

        doReturn(mockSession).when(mockWebContext).getJCRSession(BlogRepositoryConstants.CONTACTS);
        doReturn(mockAuthorNode).when(mockSession).getNodeByIdentifier(authorId);

        doReturnProperty(mockAuthorNode, ContactsNodeTypes.Contact.PROPERTY_FIRST_NAME, firstName);
        doReturnProperty(mockAuthorNode, ContactsNodeTypes.Contact.PROPERTY_LAST_NAME, lastName);

        Object result = formatter.generateCell(mockTable, itemId, null);
        assertEquals("hans de boer", result);
    }

    @Test
    public void testGenerateCellWithBlogNodeWithOnlyFirstName() throws Exception {
        Object itemId = "1";
        String authorId = "123";
        String firstName = "hans";

        JcrItemAdapter mockItem = mock(JcrItemAdapter.class);
        Node mockNode = mock(Node.class);
        Session mockSession = mock(Session.class);
        Node mockAuthorNode = mock(Node.class);

        doReturn(mockItem).when(mockTable).getItem(itemId);
        doReturn(mockNode).when(mockItem).getJcrItem();
        doReturn(true).when(mockItem).isNode();
        doReturn(true).when(mockNode).isNode();

        doReturnIsNodeType(mockNode, BlogsNodeTypes.Blog.NAME);
        doReturnProperty(mockNode, BlogsNodeTypes.Blog.PROPERTY_AUTHOR, authorId);

        doReturn(mockSession).when(mockWebContext).getJCRSession(BlogRepositoryConstants.CONTACTS);
        doReturn(mockAuthorNode).when(mockSession).getNodeByIdentifier(authorId);

        doReturnProperty(mockAuthorNode, ContactsNodeTypes.Contact.PROPERTY_FIRST_NAME, firstName);

        Object result = formatter.generateCell(mockTable, itemId, null);
        assertEquals("hans", result);
    }

    @Test
    public void testGenerateCellWithBlogNodeWithOnlyLastName() throws Exception {
        Object itemId = "1";
        String authorId = "123";
        String lastName = "de boer";

        JcrItemAdapter mockItem = mock(JcrItemAdapter.class);
        Node mockNode = mock(Node.class);
        Session mockSession = mock(Session.class);
        Node mockAuthorNode = mock(Node.class);

        doReturn(mockItem).when(mockTable).getItem(itemId);
        doReturn(mockNode).when(mockItem).getJcrItem();
        doReturn(true).when(mockItem).isNode();
        doReturn(true).when(mockNode).isNode();

        doReturnIsNodeType(mockNode, BlogsNodeTypes.Blog.NAME);
        doReturnProperty(mockNode, BlogsNodeTypes.Blog.PROPERTY_AUTHOR, authorId);

        doReturn(mockSession).when(mockWebContext).getJCRSession(BlogRepositoryConstants.CONTACTS);
        doReturn(mockAuthorNode).when(mockSession).getNodeByIdentifier(authorId);

        doReturnProperty(mockAuthorNode, ContactsNodeTypes.Contact.PROPERTY_LAST_NAME, lastName);

        Object result = formatter.generateCell(mockTable, itemId, null);
        assertEquals("de boer", result);
    }

    @Test
    public void testGenerateCellWithBlogNodeWithEmptyAuthorNameIsTrimmed() throws Exception {
        Object itemId = "1";
        String authorId = "123";

        JcrItemAdapter mockItem = mock(JcrItemAdapter.class);
        Node mockNode = mock(Node.class);
        Session mockSession = mock(Session.class);
        Node mockAuthorNode = mock(Node.class);

        doReturn(mockItem).when(mockTable).getItem(itemId);
        doReturn(mockNode).when(mockItem).getJcrItem();
        doReturn(true).when(mockItem).isNode();
        doReturn(true).when(mockNode).isNode();

        doReturnIsNodeType(mockNode, BlogsNodeTypes.Blog.NAME);
        doReturnProperty(mockNode, BlogsNodeTypes.Blog.PROPERTY_AUTHOR, authorId);

        doReturn(mockSession).when(mockWebContext).getJCRSession(BlogRepositoryConstants.CONTACTS);
        doReturn(mockAuthorNode).when(mockSession).getNodeByIdentifier(authorId);

        doReturnProperty(mockAuthorNode, ContactsNodeTypes.Contact.PROPERTY_FIRST_NAME, StringUtils.EMPTY);
        doReturnProperty(mockAuthorNode, ContactsNodeTypes.Contact.PROPERTY_LAST_NAME, StringUtils.EMPTY);

        Object result = formatter.generateCell(mockTable, itemId, null);
        assertEquals(StringUtils.EMPTY, result);
    }

    private static void doReturnIsNodeType(Node node, String nodeType) throws Exception {
        Property mockProperty = mock(Property.class);

        doReturn(mockProperty).when(node).getProperty(JcrConstants.JCR_PRIMARYTYPE);
        doReturn(nodeType).when(mockProperty).getString();
        doReturn(true).when(node).isNodeType(nodeType);
    }

    private static void doReturnProperty(Node node, String propertyName, String value) throws Exception {
        Property mockProperty = mock(Property.class);

        doReturn(true).when(node).hasProperty(propertyName);
        doReturn(mockProperty).when(node).getProperty(propertyName);
        doReturn(value).when(mockProperty).getString();
    }
}