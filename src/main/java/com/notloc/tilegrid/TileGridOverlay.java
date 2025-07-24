package com.notloc.tilegrid;

import java.awt.*;
import javax.annotation.Nonnull;
import javax.inject.Inject;

import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;

class TileGridOverlay extends Overlay {
    private final Client client;
    private final TileGridConfig config;

    private BufferedImage _bufferedImage;

    private ArrayList<Long> totalTimes = new ArrayList<>();
    private ArrayList<Long> buildTimes = new ArrayList<>();
    private ArrayList<Long> renderTimes = new ArrayList<>();

    /* NOTE:
     * Polygons when viewed from the top down, north, begin in the bottom left corner and wind counter-clockwise.
     */

    @Inject
    private TileGridOverlay(Client client, TileGridConfig config) {
        this.client = client;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(PRIORITY_LOW);
    }

    private BufferedImage getBufferedImage() {
        if (_bufferedImage == null || _bufferedImage.getWidth() != client.getCanvas().getWidth() || _bufferedImage.getHeight() != client.getCanvas().getHeight()) {
            _bufferedImage = new BufferedImage(client.getCanvas().getWidth(), client.getCanvas().getHeight(), BufferedImage.TYPE_INT_ARGB);
        } else {
            clearImage(_bufferedImage);
        }
        return _bufferedImage;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        long startTime = System.nanoTime();

        Player player = client.getLocalPlayer();
        WorldView wv = client.getTopLevelWorldView();

        WorldPoint wPos = player.getWorldLocation();
        LocalPoint pos = LocalPoint.fromWorld(wv, wPos);
        if (pos == null) {
            return null;
        }

        LocalPoint lPos = player.getLocalLocation();

        final int playerX = pos.getX();
        final int playerY = pos.getY();
        final int playerLX = lPos.getX();
        final int playerLY = lPos.getY();
        final int plane = player.getWorldLocation().getPlane();

        BufferedImage bufferedImage = getBufferedImage();
        Graphics2D bufferedGraphics = bufferedImage.createGraphics();

        Color realColor = config.gridColor();
        int alpha = realColor.getAlpha();

        // We write the alpha into the B component for now
        Color alphaColor = new Color(0, 0, realColor.getAlpha(), 255);
        int rgbInt = realColor.getRGB() & 0x00FFFFFF; // Removes the alpha component

        bufferedGraphics.setColor(alphaColor);
        bufferedGraphics.setStroke(new BasicStroke(1));

        int renderRange = config.gridDistance();
        int lineCount = (renderRange * 2 + 1) * renderRange * 2;

        // Xs
        int[] hLineXs = new int[lineCount * 2];
        int[] hLineYs = new int[lineCount * 2];
        float[] hDists = new float[lineCount];

        int xi = 0;
        int yi = 0;
        int di = 0;
        for (int y = -renderRange+1; y <= renderRange; y++) {
            for (int x = -renderRange; x <= renderRange; x++) {
                int xP = (playerX + x*128);
                int yP = (playerY + y*128);

                hLineXs[xi]   = xP - 64;
                hLineXs[xi+1] = xP + 63;

                hLineYs[yi]   = yP - 64;
                hLineYs[yi+1] = yP - 64;

                float xDist = (xP - playerLX) / 128f;
                float yDist = (yP - 64 - playerLY) / 128f;
                hDists[di] = xDist * xDist + yDist * yDist;

                xi += 2;
                yi += 2;
                di += 1;
            }
        }

        // Ys
        int[] vLineXs = new int[lineCount * 2];
        int[] vLineYs = new int[lineCount * 2];
        float[] vDists = new float[lineCount];

        xi = 0;
        yi = 0;
        di = 0;
        for (int x = -renderRange+1; x <= renderRange; x++) {
            for (int y = -renderRange; y <= renderRange; y++) {
                int xP = (playerX + x*128);
                int yP = (playerY + y*128);

                vLineXs[xi]   = xP - 64;
                vLineXs[xi+1] = xP - 64;

                vLineYs[yi]   = yP - 64;
                vLineYs[yi+1] = yP + 63;

                float xDist = (xP - 64 - playerLX) / 128f;
                float yDist = (yP - playerLY) / 128f;
                vDists[di] = xDist * xDist + yDist * yDist;

                xi += 2;
                yi += 2;
                di += 1;
            }
        }

        long buildStart =  System.nanoTime();
        Point[] hPoints = BulkPerspective.getCanvasTilePoint(client, wv, hLineXs, hLineYs, plane);
        Point[] vPoints = BulkPerspective.getCanvasTilePoint(client, wv, vLineXs, vLineYs, plane);
        long buildTime = System.nanoTime() - buildStart;

        int fadeOutDistanceSqr = config.fadeOutDistance() * config.fadeOutDistance();
        double fadeOutTaper = config.fadeOutTaper() * config.fadeOutTaper();
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        drawLines(bufferedGraphics, alpha, hDists, hPoints, fadeOutDistanceSqr, fadeOutTaper, width, height);
        drawLines(bufferedGraphics, alpha, vDists, vPoints, fadeOutDistanceSqr, fadeOutTaper, width, height);

        applyColorAndAlpha(bufferedImage, rgbInt);
        graphics.drawImage(bufferedImage, 0, 0, null);
        bufferedGraphics.dispose();

        long totalTime = System.nanoTime() - startTime;
        long renderTime = totalTime - buildTime;

        if (totalTimes.size() > 500) {
            totalTimes.clear();
            renderTimes.clear();
            buildTimes.clear();
        }
        totalTimes.add(totalTime);
        buildTimes.add(buildTime);
        renderTimes.add(renderTime);

        if (totalTimes.size() % 20 == 0) {
            System.out.println("Build time: " + buildTime / 1000000.0 + "ms");

            System.out.println("Render time: " + renderTime / 1000000.0 + "ms");

            System.out.println("Total time: " + totalTime / 1000000.0 + "ms");
            long averageRenderTime = totalTimes.stream().mapToLong(Long::longValue).sum() / totalTimes.size();
            System.out.println("Average Total time: " + averageRenderTime / 1000000.0 + "ms");
        }

        return null;
    }

    private void drawLines(Graphics2D bufferedGraphics, int alpha, float[] distances, Point[] points, int fadeOutDistanceSqr, double fadeOutTaper, int w, int h) {
        for (int i = 0; i < points.length; i+=2) {
            Point p1 = points[i];
            Point p2 = points[i + 1];
            if (p1 != null && p2 != null && inBounds(p1, p2, w, h)) {
                if (fadeOutDistanceSqr > 0) {
                    double dist = (distances[i / 2] - fadeOutDistanceSqr) / fadeOutTaper;
                    if (dist <= 1) {
                        dist = 1;
                    }
                    Color color = new Color(0, 0, (int) (alpha / dist), 255);
                    bufferedGraphics.setColor(color);
                }
                bufferedGraphics.drawLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
            }
        }
    }

    private static boolean inBounds(Point p1, Point p2, int w, int h) {
        boolean xVisible = p1.getX() >= 0 || p1.getX() <= w || p2.getX() >= 0 || p2.getX() <= w;
        boolean yVisible = p1.getY() >= 0 || p1.getY() <= h || p2.getY() >= 0 || p2.getY() <= h;
        return xVisible && yVisible;
    }

    // Shifts the image data into the desired format.
    // B becomes A and RGB is injected
    // Ends up being significantly faster than just drawing the image normally with transparent colors
    private static void applyColorAndAlpha(BufferedImage image, int rgb) {
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            if (pixel == 0) {
                continue;
            }
            pixel = (pixel << 24) | rgb;
            pixels[i] = pixel;
        }
    }


    // Manually clears the image, faster than using Graphics2D.clearRect and similar
    private static void clearImage(BufferedImage image) {
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = 0;
        }
    }
}
