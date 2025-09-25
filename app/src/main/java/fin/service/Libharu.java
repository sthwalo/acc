
package fin.service;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * JNA binding for libharu PDF library
 * Provides Java access to libharu's native C functions for manual PDF generation
 */
public interface Libharu extends Library {
    // Load the library explicitly from the Homebrew path
    Libharu INSTANCE = (Libharu) Native.load("/opt/homebrew/lib/libhpdf.dylib", Libharu.class);

    // PDF document creation and management
    Pointer HPDF_New(Pointer errorHandler, Pointer userData);
    void HPDF_Free(Pointer pdf);

    // Page management
    Pointer HPDF_AddPage(Pointer pdf);
    void HPDF_Page_SetSize(Pointer page, int size, int direction);

    // Font management
    Pointer HPDF_GetFont(Pointer pdf, String fontName, String encodingName);
    void HPDF_Page_SetFontAndSize(Pointer page, Pointer font, float size);

    // Text drawing
    void HPDF_Page_BeginText(Pointer page);
    void HPDF_Page_TextOut(Pointer page, float x, float y, String text);
    void HPDF_Page_EndText(Pointer page);

    // Drawing operations
    void HPDF_Page_MoveTo(Pointer page, float x, float y);
    void HPDF_Page_LineTo(Pointer page, float x, float y);
    void HPDF_Page_Rectangle(Pointer page, float x, float y, float width, float height);
    void HPDF_Page_Stroke(Pointer page);
    void HPDF_Page_Fill(Pointer page);
    void HPDF_Page_SetLineWidth(Pointer page, float width);

    // Colors
    void HPDF_Page_SetRGBFill(Pointer page, float r, float g, float b);
    void HPDF_Page_SetRGBStroke(Pointer page, float r, float g, float b);

    // Image handling
    Pointer HPDF_LoadPngImageFromFile(Pointer pdf, String filename);
    void HPDF_Page_DrawImage(Pointer page, Pointer image, float x, float y, float width, float height);

    // Saving
    int HPDF_SaveToFile(Pointer pdf, String filename);

    // Constants
    public static final int HPDF_PAGE_SIZE_A4 = 0;
    public static final int HPDF_PAGE_PORTRAIT = 0;
    public static final int HPDF_PAGE_LANDSCAPE = 1;
}