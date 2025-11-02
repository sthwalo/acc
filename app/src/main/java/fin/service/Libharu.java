/*
 * FIN Financial Management System
 * 
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 * 
 * This source code is licensed under the Apache License 2.0.
 * Commercial use of the APPLICATION requires separate licensing.
 * 
 * Contains proprietary algorithms and business logic.
 * Unauthorized commercial use is strictly prohibited.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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