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

import info.magnolia.jcr.util.PropertyUtil;
import info.magnolia.link.LinkException;
import info.magnolia.link.LinkTransformerManager;
import info.magnolia.link.LinkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CloudMap implements Map<String, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudMap.class);

    private final Map<String, Object> cloudProperties = new HashMap<String, Object>(6);
    private final Node content;

    public CloudMap(Node content, int count, int scale) {
        if (content == null) {
            throw new IllegalArgumentException("CloudMap doesn't accept null content");
        }
        this.content = content;

        try {
            final PropertyIterator props = content.getProperties();
            while (props.hasNext()) {
                final Property prop = props.nextProperty();
                cloudProperties.put(prop.getName(), getPropertyValue(prop.getName()));
            }

            // Add special properties
            cloudProperties.put("id", content.getIdentifier());
            cloudProperties.put("name", content.getName());
            cloudProperties.put("path", content.getPath());
            cloudProperties.put("depth", content.getDepth());
            cloudProperties.put("count", count);
            cloudProperties.put("scale", scale);
        } catch (RepositoryException e) {
            LOGGER.debug("Exception while creating cloud map", e);
        }
    }

    @Override
    public boolean containsKey(Object key) {
        return cloudProperties.containsKey(key);
    }

    @Override
    public Object get(Object key) {
        return cloudProperties.get(key);
    }

    @Override
    public int size() {
        return cloudProperties.size();
    }

    @Override
    public Collection<Object> values() {
        return cloudProperties.values();
    }

    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return cloudProperties.entrySet();
    }

    @Override
    public Set<String> keySet() {
        return cloudProperties.keySet();
    }

    @Override
    public boolean containsValue(Object value) {
        return cloudProperties.containsValue(value);
    }

    @Override
    public boolean isEmpty() {
        // can never be empty because of the node props themselves (name, uuid, ...)
        return false;
    }

    @Override
    public Object put(String key, Object value) {
        // ignore, read only
        return null;
    }

    @Override
    public Object remove(Object key) {
        // ignore, read only
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        // ignore, read only
    }

    @Override
    public void clear() {
        // ignore, read only
    }

    private Object getPropertyValue(String propertyName) throws RepositoryException {
        final Object value = PropertyUtil.getPropertyValueObject(content, propertyName);
        if (value instanceof String) {
            final String stringValue = (String) value;
            if (LinkUtil.UUID_PATTERN.matcher(stringValue).find()) {
                try {
                    return LinkUtil.convertLinksFromUUIDPattern(stringValue,
                            LinkTransformerManager.getInstance().getBrowserLink(content.getPath()));
                } catch (LinkException e) {
                    LOGGER.warn("Failed to parse links with from " + stringValue, e);
                }
            }
        }
        return value;
    }
}