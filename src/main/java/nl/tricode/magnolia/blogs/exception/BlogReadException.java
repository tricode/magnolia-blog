package nl.tricode.magnolia.blogs.exception;

/**
 * Base class that handles blog read exceptions.
 */
public abstract class BlogReadException extends Exception {

    public BlogReadException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
