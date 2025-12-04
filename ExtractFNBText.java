import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.PDDocument;
import java.io.File;
import java.io.IOException;

public class ExtractFNBText {
    public static void main(String[] args) throws IOException {
        File pdfFile = new File("input/GHC:FNB/GOLD_BUSINESS_ACCOUNT_109.pdf");
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            String[] lines = text.split("\\r?\\n");
            for (int i = 0; i < Math.min(100, lines.length); i++) {
                if (!lines[i].trim().isEmpty()) {
                    System.out.println(lines[i]);
                }
            }
        }
    }
}