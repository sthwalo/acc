package fin.service.upload;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class DocumentTextExtractorParseTest {

    @Test
    void parseStatementPeriod_standardFormats() {
        DocumentTextExtractor extractor = new DocumentTextExtractor(null);

        String s1 = "16 February 2024 to 18 March 2024";
        var p1 = extractor.parseStatementPeriod(s1);
        assertNotNull(p1);
        assertEquals(LocalDate.of(2024,2,16), p1.getStart());
        assertEquals(LocalDate.of(2024,3,18), p1.getEnd());

        String s2 = "01/01/2024 to 31/01/2024";
        var p2 = extractor.parseStatementPeriod(s2);
        assertNotNull(p2);
        assertEquals(LocalDate.of(2024,1,1), p2.getStart());
        assertEquals(LocalDate.of(2024,1,31), p2.getEnd());

        String s3 = "2024-02-16 to 2024-03-16";
        var p3 = extractor.parseStatementPeriod(s3);
        assertNotNull(p3);
        assertEquals(LocalDate.of(2024,2,16), p3.getStart());
        assertEquals(LocalDate.of(2024,3,16), p3.getEnd());
    }

    @Test
    void parseStatementPeriod_handlesNoise() {
        DocumentTextExtractor extractor = new DocumentTextExtractor(null);
        String s = "Statomont rom 16 Fruary 2024 to 18 Mar 2024";
        var p = extractor.parseStatementPeriod(s);
        assertNotNull(p);
        assertEquals(LocalDate.of(2024,2,16), p.getStart());
        assertEquals(LocalDate.of(2024,3,18), p.getEnd());
    }

    @Test
    void parseStatementPeriod_invalidReturnsNull() {
        DocumentTextExtractor extractor = new DocumentTextExtractor(null);
        assertNull(extractor.parseStatementPeriod(null));
        assertNull(extractor.parseStatementPeriod("no dates here"));
    }
}