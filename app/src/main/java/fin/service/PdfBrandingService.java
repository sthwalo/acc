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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import fin.license.LicenseManager;

import java.awt.Color;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Centralized service for PDF branding using Apache PDFBox (open source).
 */
public class PdfBrandingService {
    
    private static final float FOOTER_FONT_SIZE = 8f;
    private static final float COPYRIGHT_FONT_SIZE = 7f;
    private static final String SYSTEM_NAME = "FIN Financial Management System";
    private static final String COPYRIGHT_HOLDER = "Immaculate Nyoni";
    private static final String COMPANY_NAME = "Sthwalo Holdings (Pty) Ltd";
    private static final String CONTACT_EMAIL = "sthwaloe@gmail.com";
    private static final String CONTACT_PHONE = "+27 61 514 6185";
    
    public void addStandardFooter(PDDocument document, PDPage page, int pageNumber) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, page, 
            PDPageContentStream.AppendMode.APPEND, true, true);
        
        try {
            PDRectangle mediaBox = page.getMediaBox();
            float centerX = mediaBox.getWidth() / 2;
            float yPosition = 30f;
            
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            String footerText = String.format("Generated using %s | Page %d | Generated: %s",
                SYSTEM_NAME, pageNumber, now.format(formatter));
            
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float textWidth = font.getStringWidth(footerText) / 1000 * FOOTER_FONT_SIZE;
            
            contentStream.beginText();
            contentStream.setFont(font, FOOTER_FONT_SIZE);
            contentStream.setNonStrokingColor(Color.GRAY);
            contentStream.newLineAtOffset(centerX - textWidth / 2, yPosition);
            contentStream.showText(footerText);
            contentStream.endText();
            
        } finally {
            contentStream.close();
        }
    }
    
    public void addCopyrightNotice(PDDocument document, PDPage page) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, page, 
            PDPageContentStream.AppendMode.APPEND, true, true);
        
        try {
            PDRectangle mediaBox = page.getMediaBox();
            float centerX = mediaBox.getWidth() / 2;
            float yPosition = 15f;
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            
            String line1 = String.format("Copyright © 2025 %s / %s", COPYRIGHT_HOLDER, COMPANY_NAME);
            float line1Width = font.getStringWidth(line1) / 1000 * COPYRIGHT_FONT_SIZE;
            
            contentStream.beginText();
            contentStream.setFont(font, COPYRIGHT_FONT_SIZE);
            contentStream.setNonStrokingColor(Color.LIGHT_GRAY);
            contentStream.newLineAtOffset(centerX - line1Width / 2, yPosition);
            contentStream.showText(line1);
            contentStream.endText();
            
            String line2 = String.format("Contact: %s | %s", CONTACT_EMAIL, CONTACT_PHONE);
            float line2Width = font.getStringWidth(line2) / 1000 * COPYRIGHT_FONT_SIZE;
            
            contentStream.beginText();
            contentStream.setFont(font, COPYRIGHT_FONT_SIZE);
            contentStream.setNonStrokingColor(Color.LIGHT_GRAY);
            contentStream.newLineAtOffset(centerX - line2Width / 2, yPosition - 10);
            contentStream.showText(line2);
            contentStream.endText();
            
            String line3 = "Licensed under Apache License 2.0 | Proprietary algorithms and business logic";
            float line3Width = font.getStringWidth(line3) / 1000 * COPYRIGHT_FONT_SIZE;
            
            contentStream.beginText();
            contentStream.setFont(font, COPYRIGHT_FONT_SIZE);
            contentStream.setNonStrokingColor(Color.LIGHT_GRAY);
            contentStream.newLineAtOffset(centerX - line3Width / 2, yPosition - 20);
            contentStream.showText(line3);
            contentStream.endText();
            
            String line4 = "Unauthorized commercial use is strictly prohibited";
            float line4Width = font.getStringWidth(line4) / 1000 * COPYRIGHT_FONT_SIZE;
            
            contentStream.beginText();
            contentStream.setFont(font, COPYRIGHT_FONT_SIZE);
            contentStream.setNonStrokingColor(Color.LIGHT_GRAY);
            contentStream.newLineAtOffset(centerX - line4Width / 2, yPosition - 30);
            contentStream.showText(line4);
            contentStream.endText();
            
        } finally {
            contentStream.close();
        }
    }
    
    public void addFullBranding(PDDocument document, PDPage page, int pageNumber) throws IOException {
        addStandardFooter(document, page, pageNumber);
        addCopyrightNotice(document, page);
    }
    
    public String getCopyrightText() {
        return "Copyright © 2025 " + COPYRIGHT_HOLDER + " / " + COMPANY_NAME;
    }
    
    public String getLicenseInfo() {
        return getCopyrightText() + "\n" +
               "Contact: " + CONTACT_EMAIL + " | " + CONTACT_PHONE + "\n" +
               "Licensed under Apache License 2.0\n" +
               "Commercial use requires separate licensing";
    }
    
    public String getSystemName() {
        return SYSTEM_NAME;
    }
    
    public String getCopyrightHolder() {
        return COPYRIGHT_HOLDER;
    }
    
    public String getCompanyName() {
        return COMPANY_NAME;
    }
    
    public boolean isCommercialUseAllowed() {
        return LicenseManager.checkLicenseCompliance();
    }
    
    public String getPersonalUseWatermark() {
        return "PERSONAL USE ONLY - NOT FOR COMMERCIAL DISTRIBUTION";
    }
}
