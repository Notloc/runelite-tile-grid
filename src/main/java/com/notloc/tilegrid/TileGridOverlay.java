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

class TileGridOverlay extends Overlay {
    private final Client client;
    private final TileGridConfig config;

    private BufferedImage _bufferedImage;

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
        long currentTime = System.nanoTime();

        Player player = client.getLocalPlayer();
        WorldView wv = client.getTopLevelWorldView();

        WorldPoint wPos = player.getWorldLocation();
        LocalPoint pos = LocalPoint.fromWorld(wv, wPos);
        int plane = player.getWorldLocation().getPlane();

        BufferedImage bufferedImage = getBufferedImage();
        Graphics2D bufferedGraphics = bufferedImage.createGraphics();

        Color color = config.gridColor();
        int alpha = color.getAlpha();

        bufferedGraphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 255));
        bufferedGraphics.setStroke(new BasicStroke(1));

        int renderRange = config.gridDistance();
        // Remove skip the first line so that the grid is not surrounded by a box
        for (int y = -renderRange+1; y <= renderRange; y++) {
            Polygon p1 = new Polygon();

            for (int x = -renderRange; x <= renderRange; x++) {
                LocalPoint lp = new LocalPoint(pos.getX() + x*128, pos.getY() + y*128, wv);
                getCanvasTilePoint(p1, client, wv, lp.getX()-64, lp.getY()-64, plane, 0);
                getCanvasTilePoint(p1, client, wv, lp.getX()+63, lp.getY()-64, plane, 0);
            }
            bufferedGraphics.drawPolyline(p1.xpoints, p1.ypoints, p1.npoints);
        }

        for (int x = -renderRange+1; x <= renderRange; x++) {
            Polygon p1 = new Polygon();
            for (int y = -renderRange; y <= renderRange; y++) {
                LocalPoint lp = new LocalPoint(pos.getX() + x*128, pos.getY() + y*128, wv);
                getCanvasTilePoint(p1, client, wv, lp.getX()-64, lp.getY()-64, plane, 0);
                getCanvasTilePoint(p1, client, wv, lp.getX()-64, lp.getY()+63, plane, 0);
            }
            bufferedGraphics.drawPolyline(p1.xpoints, p1.ypoints, p1.npoints);
        }

        applyAlpha(bufferedImage, alpha);

        graphics.drawImage(bufferedImage, 0, 0, null);
        bufferedGraphics.dispose();

        long renderTime = System.nanoTime() - currentTime;
        System.out.println("Render time: " + renderTime / 1000000.0 + "ms");

        return null;
    }

    // Writes an alpha value to the image's pixel data
    // Ends up being significantly faster than drawing the image with a transparent color
    private static void applyAlpha(BufferedImage image, int alpha) {
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            if (pixel == 0) {
                continue;
            }
            pixels[i] = (alpha << 24) | (pixel & 0x00FFFFFF);
        }
    }

    // Manually clears the image, faster than using Graphics2D.clearRect and similar
    private static void clearImage(BufferedImage image) {
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = 0;
        }
    }

    // Copied and modified from Perspective.java
    public static boolean getCanvasTilePoint(Polygon poly, @Nonnull Client client, WorldView wv, @Nonnull int localX, int localY, int plane, int zOffset) {
        int msx = (localX >> 7) + 40;
        int msy = (localY >> 7) + 40;
        if (msx >= 0 && msy >= 0 && msx < 184 && msy < 184 && wv != null) {
            if (plane == -1) {
                plane = wv.getPlane();
            }

            Scene scene = wv.getScene();
            byte[][][] tileSettings = scene.getExtendedTileSettings();
            int tilePlane = plane;
            if (plane < 3 && (tileSettings[1][msx][msy] & 2) == 2) {
                tilePlane = plane + 1;
            }

            int swHeight = getHeight(scene, localX, localY, tilePlane) - zOffset;
            Point p1 = Perspective.localToCanvas(client, localX, localY, swHeight);
            if (p1 != null) {
                poly.addPoint(p1.getX(), p1.getY());
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    // Copied from Perspective.java
    private static int getHeight(@Nonnull Scene scene, int localX, int localY, int plane) {
        int sceneX = (localX >> 7) + 40;
        int sceneY = (localY >> 7) + 40;
        if (sceneX >= 0 && sceneY >= 0 && sceneX < 184 && sceneY < 184) {
            int[][][] tileHeights = scene.getTileHeights();
            int x = localX & 127;
            int y = localY & 127;
            int var8 = x * tileHeights[plane][sceneX + 1][sceneY] + (128 - x) * tileHeights[plane][sceneX][sceneY] >> 7;
            int var9 = tileHeights[plane][sceneX][sceneY + 1] * (128 - x) + x * tileHeights[plane][sceneX + 1][sceneY + 1] >> 7;
            return (128 - y) * var8 + y * var9 >> 7;
        } else {
            return 0;
        }
    }
}
