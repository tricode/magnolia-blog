package nl.tricode.magnolia.blogs.exception;

/**
 * An exception that is throw when latest blogs cannot be fetched.
 */
public class UnableToGetLatestBlogsException extends BlogReadException {

    public UnableToGetLatestBlogsException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
