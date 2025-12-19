package fin.exception;

/**
 * Runtime exception representing failures during PDF processing/extraction.
 */
public class PdfProcessingException extends RuntimeException {
    public PdfProcessingException() { super(); }
    public PdfProcessingException(String message) { super(message); }
    public PdfProcessingException(String message, Throwable cause) { super(message, cause); }
    public PdfProcessingException(Throwable cause) { super(cause); }
}