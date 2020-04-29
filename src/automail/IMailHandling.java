package automail;

/**
 * This class includes functions that handle with mail items
 */
public interface IMailHandling {
    /**
     * Wrap items
     */
    public void wrap(MailItem mailItem);
    /**
     * Unwrap items
     */
    public void unwrap(MailItem mailItem);
}
