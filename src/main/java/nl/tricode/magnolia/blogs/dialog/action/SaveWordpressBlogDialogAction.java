/**
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
package nl.tricode.magnolia.blogs.dialog.action;

import com.sun.org.apache.xalan.internal.xsltc.runtime.*;
import com.vaadin.data.Item;
import info.magnolia.cms.core.Path;
import info.magnolia.cms.util.QueryUtil;
import info.magnolia.context.MgnlContext;
import info.magnolia.jcr.util.NodeUtil;
import info.magnolia.ui.api.action.AbstractAction;
import info.magnolia.ui.api.action.ActionExecutionException;
import info.magnolia.ui.api.shell.Shell;
import info.magnolia.ui.form.EditorCallback;
import info.magnolia.ui.form.EditorValidator;
import info.magnolia.ui.vaadin.overlay.MessageStyleTypeEnum;
import nl.tricode.magnolia.blogs.BlogsNodeTypes;
import nl.tricode.magnolia.blogs.util.BlogWorkspaceUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.Hashtable;

public class SaveWordpressBlogDialogAction extends AbstractAction<SaveWordpressBlogDialogActionDefinition> {
	private static final Logger log = LoggerFactory.getLogger(SaveWordpressBlogDialogAction.class);

	private static final String FIRSTNAME = "first_name";
	private static final String LASTNAME = "last_name";
	private static final String EMAIL = "email";
	private static final String URL = "url";

	private final Item item;
	private final EditorValidator validator;
	private final EditorCallback callback;
	private final Shell shell;
	private Session blogSession;
	private Session contactSession;
	private Session damSession;
	private XmlRpcClient client;
	private boolean shouldImportImages;
	private boolean shouldImportContacts;
	private HashMap<String, String> recentlyCreatedMagnoliaContacts;

	public SaveWordpressBlogDialogAction(SaveWordpressBlogDialogActionDefinition definition, Item item, EditorValidator validator, EditorCallback callback, Shell shell) throws ActionExecutionException {
		super(definition);
		this.item = item;
		this.validator = validator;
		this.callback = callback;
		this.shell = shell;
		this.shouldImportImages = (Boolean) item.getItemProperty("shouldImportImages").getValue();
		this.shouldImportContacts = (Boolean) item.getItemProperty("shouldImportContacts").getValue();
		this.validator.showValidation(true);

		if (this.validator.isValid()) {
			try {
				this.blogSession = MgnlContext.getJCRSession(BlogWorkspaceUtil.COLLABORATION);

				/** Only open contacts session when needed.*/
				if (this.shouldImportContacts) {
					this.recentlyCreatedMagnoliaContacts = new HashMap<String, String>();
					this.contactSession = MgnlContext.getJCRSession(BlogWorkspaceUtil.CONTACTS);
				}

				/** Only open DAM session when needed. */
				if (this.shouldImportImages) {
					this.damSession = MgnlContext.getJCRSession(BlogWorkspaceUtil.DAM);
				}

				this.client = new XmlRpcClient((String) this.item.getItemProperty("endpoint").getValue());
			} catch (RepositoryException e) {
				log.error("Error getting session: " + e.getMessage(), e);
				shell.showError("Error getting session: " + e.getMessage(), e);
				callback.onCancel();
				throw new ActionExecutionException(e);
			} catch (MalformedURLException e) {
				log.error("Bad URL entered: " + e.getMessage(), e);
				shell.showError("Bad URL entered: " + e.getMessage(), e);
				callback.onCancel();
				throw new ActionExecutionException(e);
			}
		}
	}

	@Override
	public void execute() throws ActionExecutionException {
		if (validator.isValid()) {
			try {
				log.debug("Start wordPress import.");
				log.debug("Import into DAM [" + this.shouldImportImages + "].");
				log.debug("Import into Contacts [" + this.shouldImportContacts + "].");
				Vector<Hashtable<String, Object>> blog = getWordpressPosts();
				if (!blog.isEmpty()) {
					log.debug("Blog size [" + blog.size() + "]");
					for (Hashtable<String, Object> blogPost : blog) {
						processPost(blogPost);
					}
					finishImport();
				} else {
					shell.openNotification(MessageStyleTypeEnum.INFO, false, "No WordPress posts were found.");
					callback.onCancel();
				}
			} catch (ActionExecutionException e) {
				cancelImport();
				throw new ActionExecutionException(e);
			}
		} else {
			log.error("Validation error(s). Import cancelled.");
		}
	}

	/**
	 * Gets all the posts from WordPress for a given blog ID (obtained through the Magnolia dialog).
	 *
	 * @return all posts
	 * @throws info.magnolia.ui.api.action.ActionExecutionException if an IOException or XmlRpcException occurs
	 */
	private Vector<Hashtable<String, Object>> getWordpressPosts() throws ActionExecutionException {
		try {
			return (Vector<Hashtable<String, Object>>) client.execute("wp.getPosts", buildGetPostsRequest());
		} catch (IOException e) {
			log.error("Error while getting the blog posts from WordPress: " + e.getMessage(), e);
			throw new ActionExecutionException(e);
		} catch (XmlRpcException e) {
			log.error("Error while getting the blog posts from WordPress: " + e.getMessage(), e);
			throw new ActionExecutionException(e);
		}
	}

	/**
	 * Builds the getPosts request for the WordPress XML-RPC call.
	 *
	 * @return the request
	 */
	private Vector<Object> buildGetPostsRequest() {
		Integer blogID = Integer.valueOf((String) item.getItemProperty("blogID").getValue());
		String username = (String) item.getItemProperty("username").getValue();
		String password = (String) item.getItemProperty("password").getValue();

		Vector<Object> request = new Vector<Object>();
		Vector<String> expectedReturnValues = new Vector<String>();
		expectedReturnValues.addElement("post_content");
		expectedReturnValues.addElement("post_title");
		expectedReturnValues.addElement("post_author");
		expectedReturnValues.addElement("post_date");
		expectedReturnValues.addElement("post_modified");
		expectedReturnValues.addElement("post_name");
		expectedReturnValues.addElement("terms");

		Hashtable hashTable = new Hashtable();
		hashTable.put("number", Integer.MAX_VALUE);

      request.addElement(blogID);
		request.addElement(username);
		request.addElement(password);
		request.addElement(hashTable);
		request.addElement(expectedReturnValues);

		return request;
	}

	/**
	 * Adds a blog post.
	 *
	 * @param blogPost The blog post in Hashtable format
	 * @throws info.magnolia.ui.api.action.ActionExecutionException if a RepositoryException occurs
	 */
	private void processPost(Hashtable<String, Object> blogPost) throws ActionExecutionException {
		String name = (String) blogPost.get("post_name");
		String title = (String) blogPost.get("post_title");

		Calendar date = Calendar.getInstance();
		date.setTime((Date) blogPost.get("post_date"));
		Calendar dateModified = Calendar.getInstance();
		dateModified.setTime((Date) blogPost.get("post_modified"));
		String message = (String) blogPost.get("post_content");

		try {
			if (shouldImportImages) {
				BlogPostImageImporter contentProcessor = new BlogPostImageImporter(message, damSession);
				message = contentProcessor.startImporting();
			}

			log.debug("Process blog [" + title + "].");
			Node blogPostNode = blogSession.getRootNode().addNode("temporaryBlogPostNodeName", BlogsNodeTypes.Blog.NAME);
			blogPostNode.setProperty(BlogsNodeTypes.Blog.PROPERTY_TITLE, title);
			blogPostNode.setProperty(BlogsNodeTypes.Blog.PROPERTY_MESSAGE, message);

			if (shouldImportContacts) {
				String author = getMagnoliaContact((String) blogPost.get("post_author"));
				blogPostNode.setProperty(BlogsNodeTypes.Blog.PROPERTY_AUTHOR, author);
			}

         blogPostNode.setProperty(BlogsNodeTypes.Blog.PROPERTY_COMMENTS_ENABLED, true);
			blogPostNode.setProperty("mgnl:created", date);
			blogPostNode.setProperty("mgnl:lastModified", dateModified);

			if (name.isEmpty()) {
				NodeUtil.renameNode(blogPostNode, BlogWorkspaceUtil.generateUniqueNodeName(blogPostNode, BlogsNodeTypes.Blog.PROPERTY_TITLE));
			} else {
				NodeUtil.renameNode(blogPostNode, name);
			}
		} catch (RepositoryException e) {
			log.error("Error while adding post: " + e.getMessage(), e);
			throw new ActionExecutionException(e);
		}
	}

	/**
	 * Gets the magnolia contact reference for a given Wordpress User ID.
	 * NOTE: If no existing contact is found, a new one will be created.
	 *
	 * @param wordpressUserID the WordPress user ID
	 * @return UUID of the Magnolia contact
	 * @throws info.magnolia.ui.api.action.ActionExecutionException if an XmlRpcException or IOException occurs
	 */
	private String getMagnoliaContact(String wordpressUserID) throws ActionExecutionException {
		try {
			Hashtable<String, Object> author = (Hashtable<String, Object>) client.execute("wp.getUser", buildGetUserRequest(wordpressUserID));
			String magnoliaContactReference = findMagnoliaContact((String) author.get("email"));
			if (magnoliaContactReference == null) {
				magnoliaContactReference = createMagnoliaContact(author);
			}
			return magnoliaContactReference;
		} catch (IOException e) {
			log.error("Error retrieving the author from WordPress " + e.getMessage(), e);
			throw new ActionExecutionException(e);
		} catch (XmlRpcException e) {
			log.error("Error retrieving the author from WordPress " + e.getMessage(), e);
			throw new ActionExecutionException(e);
		}
	}

	/**
	 * Builds the getUser request for the WordPress XML-RPC call.
	 *
	 * @param wordpressUserID the user to get
	 * @return the request
	 */
	private Vector<Object> buildGetUserRequest(String wordpressUserID) {
		Vector<Object> request = new Vector<Object>();
		Vector<String> expectedReturnValues = new Vector<String>();
		expectedReturnValues.addElement(FIRSTNAME);
		expectedReturnValues.addElement(LASTNAME);
		expectedReturnValues.addElement(EMAIL);
		expectedReturnValues.addElement(URL);

		request.addElement(item.getItemProperty("blogID").getValue());
		request.addElement(item.getItemProperty("username").getValue());
		request.addElement(item.getItemProperty("password").getValue());
		request.addElement(wordpressUserID);
		request.addElement(expectedReturnValues);

		return request;
	}

	private String findRecentlyCreatedMagnoliaContact(String email) {
		if (recentlyCreatedMagnoliaContacts.containsKey(email)) {
			return recentlyCreatedMagnoliaContacts.get(email);
		} else {
			return null;
		}
	}

	/**
	 * Searches for an existing Magnolia contact.
	 * NOTE: Will return the first result only.
	 *
	 * @param email Used to match the contact
	 * @return UUID of the Magnolia contact or null if no contact was found
	 * @throws info.magnolia.ui.api.action.ActionExecutionException if a RepositoryException occurs
	 */
	private String findMagnoliaContact(String email) throws ActionExecutionException {
		try {
			NodeIterator nodes = QueryUtil.search(BlogWorkspaceUtil.CONTACTS, "SELECT * FROM [mgnl:contact] WHERE email  = '" + email + "'");
			if (nodes.hasNext()) {
				return nodes.nextNode().getIdentifier();
			} else {
				return findRecentlyCreatedMagnoliaContact(email);
			}
		} catch (RepositoryException e) {
			log.error("Error while searching for contact " + e.getMessage(), e);
			throw new ActionExecutionException(e);
		}
	}

	/**
	 * Creates a new Magnolia contact with a first name, last name, email address and website.
	 *
	 * @param contactDetails contact details
	 * @return UUID of the Magnolia contact
	 * @throws info.magnolia.ui.api.action.ActionExecutionException if a RepositoryException occurs
	 */
	private String createMagnoliaContact(Hashtable<String, Object> contactDetails) throws ActionExecutionException {
		try {
			Node newContactNode = contactSession.getRootNode().addNode("temporaryContactNodeName", "mgnl:contact");
			newContactNode.setProperty("firstName", (String) contactDetails.get(FIRSTNAME));
			newContactNode.setProperty("lastName", (String) contactDetails.get(LASTNAME));
			newContactNode.setProperty("email", (String) contactDetails.get(EMAIL));
			newContactNode.setProperty("website", (String) contactDetails.get(URL));
			NodeUtil.renameNode(newContactNode, generateUniqueNodeNameForContact(newContactNode));
			recentlyCreatedMagnoliaContacts.put((String) contactDetails.get(EMAIL), newContactNode.getIdentifier());
			return newContactNode.getIdentifier();
		} catch (RepositoryException e) {
			log.error("Error while creating contact " + e.getMessage(), e);
			throw new ActionExecutionException(e);
		}
	}

	/**
	 * Create a new Node Unique NodeName.
	 */
	private String generateUniqueNodeNameForContact(final Node node) throws RepositoryException {
		String newNodeName = defineNodeName(node);
		return Path.getUniqueLabel(node.getSession(), node.getParent().getPath(), newNodeName);
	}

	/**
	 * Define the Node Name. Node Name = First Char of the lastName + the full
	 * firstName. lastName = eric firstName = tabli The node name is etabli
	 */
	private String defineNodeName(final Node node) throws RepositoryException {
		String result;
		String intitialFirstName = node.getProperty("firstName").getString();
		String firstName = StringUtils.isNotBlank(intitialFirstName) ? intitialFirstName.trim() : intitialFirstName;

		if (StringUtils.isNotBlank(firstName)) {
			String lastName = node.getProperty("lastName").getString().trim();
			result = Path.getValidatedLabel((firstName.charAt(0) + lastName.replaceAll("\\s+", "")).toLowerCase());
		} else {
			log.debug("Incomplete wordpress userdetails");
			result = "Anonymous";
		}
		return result;
	}

	/**
	 * Persists the changes made to the repository and closes the dialog.
	 *
	 * @throws info.magnolia.ui.api.action.ActionExecutionException if a RepositoryException occurs
	 */
	private void finishImport() throws ActionExecutionException {
		try {
			blogSession.save();

			if (shouldImportContacts) {
				contactSession.save();
			}

			if (shouldImportImages) {
				damSession.save();
			}
			BlogPostImageImporter.cleanRecentUrlList();
			callback.onSuccess(getDefinition().getName());
			shell.openNotification(MessageStyleTypeEnum.INFO, true, "Import completed successfully.");
		} catch (RepositoryException e) {
			log.error("Error while saving sessions: " + e.getMessage(), e);
			throw new ActionExecutionException(e);
		}
	}

	/**
	 * Will discard all changes made to the repository and closes the dialog.
	 *
	 * @throws info.magnolia.ui.api.action.ActionExecutionException if a RepositoryException occurs
	 */
	private void cancelImport() throws ActionExecutionException {
		try {
			blogSession.refresh(false);

			if (shouldImportContacts) {
				contactSession.refresh(false);
			}

			if (shouldImportImages) {
				damSession.refresh(false);
			}
			BlogPostImageImporter.cleanRecentUrlList();
			callback.onCancel();
		} catch (RepositoryException e) {
			log.error("Error while discarding changes", e);
			throw new ActionExecutionException(e);
		}
	}
}