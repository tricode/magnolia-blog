package nl.tricode.magnolia.blogs.service;

import javax.jcr.Node;
import java.io.Serializable;
import java.util.List;

/**
 * Blog result items wrapper class
 */
public class BlogResult implements Serializable {

    private int totalCount;
    private int numPages;
    private List<Node> results;

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getNumPages() {
        return numPages;
    }

    public void setNumPages(int numPages) {
        this.numPages = numPages;
    }

    public List<Node> getResults() {
        return results;
    }

    public void setResults(List<Node> results) {
        this.results = results;
    }
}
