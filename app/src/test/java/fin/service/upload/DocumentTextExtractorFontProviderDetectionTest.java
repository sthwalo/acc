package fin.service.upload;

import fin.config.PdfBoxConfigurator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class DocumentTextExtractorFontProviderDetectionTest {

    @Test
    void detectsNoClassDefFoundErrorInChain() throws Exception {
        PdfBoxConfigurator cfg = mock(PdfBoxConfigurator.class);
        DocumentTextExtractor extractor = new DocumentTextExtractor(cfg);

        Throwable cause = new NoClassDefFoundError("org/apache/pdfbox/pdmodel/font/FontMapperImpl$DefaultFontProvider");
        Throwable top = new RuntimeException("wrapper", cause);

        Method m = DocumentTextExtractor.class.getDeclaredMethod("isFontProviderError", Throwable.class);
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(extractor, top);
        assertTrue(result, "Should detect NoClassDefFoundError in cause chain");
    }

    @Test
    void detectsMessageContainingFontMapper() throws Exception {
        PdfBoxConfigurator cfg = mock(PdfBoxConfigurator.class);
        DocumentTextExtractor extractor = new DocumentTextExtractor(cfg);

        Throwable t = new RuntimeException("Exception in FontMapperImpl$DefaultFontProvider during init");

        Method m = DocumentTextExtractor.class.getDeclaredMethod("isFontProviderError", Throwable.class);
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(extractor, t);
        assertTrue(result, "Should detect message referencing FontMapperImpl");
    }
}
