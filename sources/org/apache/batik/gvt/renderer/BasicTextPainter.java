/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.gvt.renderer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;

import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;

import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.text.AttributedCharacterIterator;

import org.apache.batik.gvt.GraphicsNodeRenderContext;
import org.apache.batik.gvt.TextNode;
import org.apache.batik.gvt.TextPainter;

import org.apache.batik.gvt.text.ConcreteTextLayoutFactory;
import org.apache.batik.gvt.text.GVTAttributedCharacterIterator;
import org.apache.batik.gvt.text.Mark;
import org.apache.batik.gvt.text.TextHit;
import org.apache.batik.gvt.text.TextLayoutFactory;
import org.apache.batik.gvt.text.TextSpanLayout;

/**
 * Basic implementation of TextPainter which
 * renders the attributed character iterator of a <tt>TextNode</tt>.
 * Suitable for use with "standard" java.awt.font.TextAttributes only.
 * @see java.awt.font.TextAttribute
 *
 * @author <a href="bill.haneman@ireland.sun.com>Bill Haneman</a>
 * @author <a href="vincent.hardy@sun.com>Vincent Hardy</a>
 * @version $Id$
 */
public abstract class BasicTextPainter implements TextPainter {

    private static TextLayoutFactory textLayoutFactory =
                               new ConcreteTextLayoutFactory();

    protected TextLayoutFactory getTextLayoutFactory() {
        return textLayoutFactory;
    }

    /**
     * Given an X, y coordinate,
     * AttributedCharacterIterator, and GraphicsNodeRenderContext,
     * return a Mark which encapsulates a "selection start" action.
     * The standard order of method calls for selection is:
     * selectAt(); [selectTo(),...], selectTo(); getSelection().
     */
    public org.apache.batik.gvt.text.Mark selectAt(double x, double y,
                         AttributedCharacterIterator aci,
                         TextNode node,
                         GraphicsNodeRenderContext context) {

        org.apache.batik.gvt.text.Mark
              newMark = hitTest(x, y, aci, node, context);
        cachedHit = null;
        return newMark;
    }

    /**
     * Given an X, y coordinate, starting Mark,
     * AttributedCharacterIterator, and GraphicsNodeRenderContext,
     * return a Mark which encapsulates a "selection continued" action.
     * The standard order of method calls for selection is:
     * selectAt(); [selectTo(),...], selectTo(); getSelection().
     */
    public org.apache.batik.gvt.text.Mark selectTo(double x, double y,
                            org.apache.batik.gvt.text.Mark beginMark,
                            AttributedCharacterIterator aci,
                            TextNode node,
                            GraphicsNodeRenderContext context) {
        org.apache.batik.gvt.text.Mark newMark =
             hitTest(x, y, aci, node, context);

        return newMark;
    }

    /**
     * Select the entire contents of an
     * AttributedCharacterIterator, and
     * return a Mark which encapsulates that selection action.
     */
    public org.apache.batik.gvt.text.Mark selectAll(double x, double y,
                            AttributedCharacterIterator aci,
                            TextNode node,
                            GraphicsNodeRenderContext context) {
        org.apache.batik.gvt.text.Mark newMark =
                              hitTest(x, y, aci, node, context);
        return newMark;
    }


    /*
     * Get a Rectangle2D in userspace coords which encloses the textnode
     * glyphs composed from an AttributedCharacterIterator.
     * @param node the TextNode to measure
     * @param g2d the Graphics2D to use
     * @param context rendering context.
     */
     public Rectangle2D getBounds(TextNode node,
               FontRenderContext frc) {
         return getBounds(node, frc, false, false);
     }

    /*
     * Get a Rectangle2D in userspace coords which encloses the textnode
     * glyphs composed from an AttributedCharacterIterator, inclusive of
     * glyph decoration (underline, overline, strikethrough).
     * @param node the TextNode to measure
     * @param g2d the Graphics2D to use
     * @param context rendering context.
     */
     public Rectangle2D getDecoratedBounds(TextNode node,
               FontRenderContext frc) {
         return getBounds(node, frc, true, false);
     }

    /*
     * Get a Rectangle2D in userspace coords which encloses the
     * textnode glyphs (as-painted, inclusive of decoration and stroke, but
     * exclusive of filters, etc.) composed from an AttributedCharacterIterator.
     * @param node the TextNode to measure
     * @param g2d the Graphics2D to use
     * @param context rendering context.
     */
     public Rectangle2D getPaintedBounds(TextNode node,
               FontRenderContext frc) {
         Rectangle2D r = getBounds(node, frc, true, true);
         return r;
     }

    /*
     * Get a Rectangle2D in userspace coords which encloses the textnode
     * glyphs composed from an AttributedCharacterIterator.
     * @param node the TextNode to measure
     * @param g2d the Graphics2D to use
     * @param context rendering context.
     * @param includeDecoration whether to include text decoration
     *            in bounds computation.
     * @param includeStrokeWidth whether to include the effect of stroke width
     *            in bounds computation.
     */
     protected abstract Rectangle2D getBounds(TextNode node,
               FontRenderContext context,
               boolean includeDecoration,
               boolean includeStrokeWidth);

   /*
    * Get a Shape in userspace coords which defines the textnode glyph outlines.
    * @param node the TextNode to measure
    * @param frc the font rendering context.
    * @param includeDecoration whether to include text decoration
    *            outlines.
    */
    protected abstract Shape getOutline(TextNode node, FontRenderContext frc,
                                    boolean includeDecoration);
   /*
    * Get a Shape in userspace coords which defines the textnode glyph outlines.
    * @param node the TextNode to measure
    * @param frc the font rendering context.
    */
    public Shape getShape(TextNode node, FontRenderContext frc) {
        return getOutline(node, frc, false);
    }

   /*
    * Get a Shape in userspace coords which defines the
    * decorated textnode glyph outlines.
    * @param node the TextNode to measure
    * @param frc the font rendering context.
    */
    public Shape getDecoratedShape(TextNode node, FontRenderContext frc) {
          return getOutline(node, frc, true);
    }

   /*
    * Get a Shape in userspace coords which defines the
    * stroked textnode glyph outlines.
    * @param node the TextNode to measure
    * @param frc the font rendering context.
    * @param includeDecoration whether to include text decoration
    *            outlines.
    */
    protected abstract Shape getStrokeOutline(TextNode node, FontRenderContext frc,
                                    boolean includeDecoration);

    protected Mark cachedMark = null;
    protected AttributedCharacterIterator cachedACI = null;
    protected TextHit cachedHit = null;


    protected abstract org.apache.batik.gvt.text.Mark hitTest(
                         double x, double y, AttributedCharacterIterator aci,
                         TextNode node,
                         GraphicsNodeRenderContext context);

    /**
     * This TextPainter's implementation of the Mark interface.
     */
    class Mark implements org.apache.batik.gvt.text.Mark {

        private TextHit hit;
        private TextSpanLayout layout;
        private double x;
        private double y;

        Mark(double x, double y, TextSpanLayout layout, TextHit hit) {
            this.x = x;
            this.y = y;
            this.layout = layout;
            this.hit = hit;
        }


        TextHit getHit() {
            return hit;
        }

        TextSpanLayout getLayout() {
            return layout;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

    }
}


