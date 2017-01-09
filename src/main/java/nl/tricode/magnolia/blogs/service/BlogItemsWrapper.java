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

import javax.jcr.Node;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Blog result items wrapper class
 */
public final class BlogItemsWrapper implements Serializable {

    private int totalCount;
    private int numPages;
    private List<Node> results;

    private BlogItemsWrapper() {
        // Enforce using the Builder
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getNumPages() {
        return numPages;
    }

    public List<Node> getResults() {
        return results;
    }

    public static final class Builder {
        private final int totalCount;
        private int numPages;
        private List<Node> results = new ArrayList<>();

        private Builder(int totalCount) {
            this.totalCount = totalCount;
        }

        public Builder withNumPages(int numPages) {
            this.numPages = numPages;
            return this;
        }

        public Builder withResults(List<Node> results) {
            this.results = results;
            return this;
        }

        public BlogItemsWrapper createInstance() {
            final BlogItemsWrapper instance = new BlogItemsWrapper();
            instance.totalCount = totalCount;
            instance.numPages = numPages;
            instance.results = results;
            return instance;
        }

        public static Builder withTotalCount(int totalCount) {
            return new Builder(totalCount);
        }
    }
}