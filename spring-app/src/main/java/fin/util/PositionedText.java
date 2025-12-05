/*
 * Copyright (c) 2024 Immaculate Nyoni <sthwaloe@gmail.com>
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package fin.util;

/**
 * Represents a text element with its X,Y position coordinates.
 * Used for coordinate-based parsing of bank statements.
 */
public class PositionedText {
    public final String text;
    public final float x;
    public final float y;
    
    public PositionedText(String text, float x, float y) {
        this.text = text;
        this.x = x;
        this.y = y;
    }
    
    @Override
    public String toString() {
        return String.format("'%s'@(%.1f,%.1f)", text, x, y);
    }
}
