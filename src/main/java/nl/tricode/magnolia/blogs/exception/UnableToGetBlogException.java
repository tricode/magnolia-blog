package nl.tricode.magnolia.blogs.exception;

/**
 * An exception that is throw when a blog item cannot be fetched.
 */
public class UnableToGetBlogException extends BlogReadException {

    public UnableToGetBlogException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
