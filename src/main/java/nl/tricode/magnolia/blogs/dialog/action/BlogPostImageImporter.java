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

import com.google.common.net.MediaType;
import info.magnolia.cms.core.Path;
import info.magnolia.dam.jcr.AssetNodeTypes;
import info.magnolia.jcr.util.NodeUtil;
import info.magnolia.link.LinkUtil;
import info.magnolia.ui.api.action.ActionExecutionException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.UnsupportedDataTypeException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class ..
 * <ul>
 * <li>Imports WordPress images into the workspace of the specified Session</li>
 * <li>Updates WordPress hyperlinks (a href) to match the location of the imported image</li>
 * <li>Updates WordPress images (img src) to match the location of the imported image</li>
 * </ul>
 */
public class BlogPostImageImporter {
	private static final Logger log = LoggerFactory.getLogger(BlogPostImageImporter.class);
	private static HashMap<String, String> recentlyProcessedURLs = new HashMap<>();
	private Element blogPostContent;
	private Session damSession;

	/**
	 * Constructs a BlogPostImageImporter and parses the blog post content.
	 *
	 * @param blogPostContent the content to process
	 * @param damSession      the session to write the assets to
	 */
	public BlogPostImageImporter(String blogPostContent, Session damSession) {
		this.blogPostContent = Jsoup.parseBodyFragment(blogPostContent).body();
		this.damSession = damSession;
	}

	/**
	 * Will import images inside the blog post content and will update hyperlink urls and image sources.
	 * NOTE: will only import wordpress images; external images will be ignored.
	 *
	 * @return the updated blog post content
	 * @throws ActionExecutionException
	 */
	public String startImporting() throws ActionExecutionException {
		importImagesByTag("a", "href");
		importImagesByTag("img", "src");
		return blogPostContent.html();
	}

	/**
	 * Loops through all elements of a specific tag in the blog post content and starts processing each element.
	 *
	 * @param elementTag the element to process
	 * @param urlTag     the url tag to process
	 * @throws ActionExecutionException
	 */
	private void importImagesByTag(String elementTag, String urlTag) throws ActionExecutionException {
		Elements elements = blogPostContent.getElementsByTag(elementTag);
		for (Element element : elements) {
			importImage(element, urlTag);
		}
	}

	/**
	 * Imports the image referenced in the specified tag and updates the url-tag to match the new location of the image.
	 *
	 * @param element the element to update
	 * @param urlTag  the tag to get the url from and also the tag that will be updated
	 * @throws ActionExecutionException
	 */
	private void importImage(Element element, String urlTag) throws ActionExecutionException {
		try {
			String processedUrl = getRecentlyProcessedURL(element.attr(urlTag));
			if (processedUrl == null) {
				URL url = new URL(element.attr(urlTag));
				String mediaType = getMediaType(url);
				if (isWordpressContentUrl(url) && isImageMediaType(mediaType)) {
					String magnoliaUrl = LinkUtil.createAbsoluteLink(addImageToMagnolia(getImageInputStream(url), getFileName(url), mediaType));
					recentlyProcessedURLs.put(element.attr(urlTag), magnoliaUrl);
					element.attr(urlTag, magnoliaUrl);
				}
			} else {
				element.attr(urlTag, processedUrl);
			}
		} catch (IOException e) {
			log.error("Error importing image", e);
			throw new ActionExecutionException(e);
		}
	}

	private String getRecentlyProcessedURL(String oldUrl) {
		if (recentlyProcessedURLs.containsKey(oldUrl)) {
			return recentlyProcessedURLs.get(oldUrl);
		} else {
			return null;
		}
	}

	/**
	 * Clears recentlyProcessedURLs. This should be called after the import of all blog posts has been completed.
	 */
	public static void cleanRecentUrlList() {
		recentlyProcessedURLs.clear();
	}

	/**
	 * Adds an image as a Magnolia asset.
	 *
	 * @param imageStream the image as an InputStream
	 * @param fileName    the file name of the image
	 * @param mediaType   the media type of the image
	 * @return the created asset Node
	 * @throws ActionExecutionException
	 */
	private Node addImageToMagnolia(InputStream imageStream, String fileName, String mediaType) throws ActionExecutionException {
		try {
			ByteArrayInputStream savedImageStream = getByteArrayInputStream(imageStream);
			String formatName = getImageReader(savedImageStream).getFormatName();
			savedImageStream.reset();
			Binary imageBinary = getBinary(savedImageStream);
			savedImageStream.reset();
			BufferedImage image = ImageIO.read(savedImageStream);

			Node assetNode = damSession.getRootNode().addNode(fileName, AssetNodeTypes.Asset.NAME);
			assetNode.setProperty(AssetNodeTypes.Asset.ASSET_NAME, fileName);
			Node assetResourceNode = assetNode.addNode(AssetNodeTypes.AssetResource.RESOURCE_NAME, AssetNodeTypes.AssetResource.NAME);
			assetResourceNode.setProperty(AssetNodeTypes.AssetResource.EXTENSION, formatName);
			assetResourceNode.setProperty(AssetNodeTypes.AssetResource.FILENAME, fileName);
			assetResourceNode.setProperty(AssetNodeTypes.AssetResource.SIZE, imageBinary.getSize());
			assetResourceNode.setProperty(AssetNodeTypes.AssetResource.HEIGHT, image.getHeight());
			assetResourceNode.setProperty(AssetNodeTypes.AssetResource.WIDTH, image.getWidth());
			assetResourceNode.setProperty(AssetNodeTypes.AssetResource.MIMETYPE, mediaType);
			assetResourceNode.setProperty(AssetNodeTypes.AssetResource.DATA, imageBinary);
			NodeUtil.renameNode(assetNode, generateUniqueNodeNameForAsset(assetNode, fileName));
			return assetNode;
		} catch (RepositoryException e) {
			log.error("Error while adding image to Magnolia", e);
			throw new ActionExecutionException(e);
		} catch (IOException e) {
			log.error("Error while reading image stream", e);
			throw new ActionExecutionException(e);
		}
	}

	private String getFileName(URL url) {
		String urlFile = url.getFile();
		return urlFile.substring(urlFile.lastIndexOf("/") + 1);
	}

	private InputStream getImageInputStream(URL url) throws IOException {
		return url.openStream();
	}

	/**
	 * Reads an InputStream into a ByteArrayInputStream for accessing multiple times.
	 * NOTE: Closes the original InputStream since the ByteArrayInputStream should now be used.
	 *
	 * @param inputStream the InputStream to read
	 * @return the ByteArrayInputStream
	 * @throws IOException
	 */
	private ByteArrayInputStream getByteArrayInputStream(InputStream inputStream) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int length;
		while ((length = inputStream.read()) > -1) {
			out.write(length);
		}
		out.flush();
		inputStream.close();
		return new ByteArrayInputStream(out.toByteArray());
	}

	private Binary getBinary(InputStream inputStream) throws RepositoryException, IOException {
		return damSession.getValueFactory().createBinary(inputStream);
	}

	private boolean isWordpressContentUrl(URL url) {
		return url.getPath().contains("wp-content");
	}

	private String getMediaType(URL url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("HEAD");
		return connection.getContentType();
	}

	private boolean isImageMediaType(String mediaType) {
		return MediaType.parse(mediaType).is(MediaType.ANY_IMAGE_TYPE);
	}

	private ImageReader getImageReader(InputStream imageStream) throws IOException {
		Iterator<ImageReader> iterator = ImageIO.getImageReaders(ImageIO.createImageInputStream(imageStream));
		if (iterator.hasNext()) {
			return iterator.next();
		} else {
			throw new UnsupportedDataTypeException("No readers for image type");
		}
	}

	private String generateUniqueNodeNameForAsset(final Node node, String newNodeName) throws RepositoryException {
		return Path.getUniqueLabel(node.getSession(), node.getParent().getPath(), Path.getValidatedLabel(newNodeName));
	}
}
