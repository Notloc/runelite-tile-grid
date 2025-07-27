package com.notloc.tilegrid;

import java.awt.*;
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

    private static final int UNWALKABLE_MASK =
                CollisionDataFlag.BLOCK_MOVEMENT_FULL |
                CollisionDataFlag.BLOCK_MOVEMENT_FLOOR |
                CollisionDataFlag.BLOCK_MOVEMENT_OBJECT |
                CollisionDataFlag.BLOCK_MOVEMENT_FLOOR_DECORATION;

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
        Player player = client.getLocalPlayer();
        WorldView wv = client.getTopLevelWorldView();
        if (wv == null) {
            return null;
        }

        WorldPoint wPos = player.getWorldLocation();
        LocalPoint truePos = LocalPoint.fromWorld(wv, wPos);
        LocalPoint smoothPos = player.getLocalLocation();
        if (truePos == null) {
            return null;
        }

        final int playerTrueX = truePos.getX();
        final int playerTrueY = truePos.getY();
        final int playerSmoothX = smoothPos.getX();
        final int playerSmoothY = smoothPos.getY();
        final int plane = player.getWorldLocation().getPlane();

        int[][] collisionData = null;
        boolean doWalkableCheck = config.doWalkableCheck();
        if (doWalkableCheck) {
            CollisionData[] collisionDataList = wv.getCollisionMaps();
            if (collisionDataList != null && plane >= 0 && plane < collisionDataList.length) {
                collisionData = collisionDataList[plane].getFlags();
            } else {
                doWalkableCheck = false;
            }
        }

        int renderRadius = config.gridDistance();
        int lineCount = (renderRadius * 2 + 1) * renderRadius * 2;

        // Xs
        int[] hLineXs = new int[lineCount * 2];
        int[] hLineYs = new int[lineCount * 2];
        float[] hDists = new float[lineCount];

        int xi = 0;
        int yi = 0;
        int di = 0;
        boolean previousTileBlocked;
        for (int x = -renderRadius; x <= renderRadius; x++) {
            int xP = (playerTrueX + x*128);
            int tileX = xP / 128;

            previousTileBlocked = true;
            for (int y = -renderRadius+1; y <= renderRadius; y++) {
                int yP = (playerTrueY + y*128);
                int tileY = yP / 128;

                boolean isBlocked = false;
                if (doWalkableCheck && tileX >= 5 && tileX < 99 && tileY >= 5 && tileY < 99) {
                    int collisionMask = collisionData[tileX][tileY];
                    isBlocked = (collisionMask & UNWALKABLE_MASK) != 0;
                }
                int correction = isBlocked ? 1 : 0;

                if (isBlocked && previousTileBlocked) {
                    hLineXs[xi] = Integer.MIN_VALUE;
                    hLineXs[xi+1] = Integer.MIN_VALUE;
                    hLineYs[yi] = Integer.MIN_VALUE;
                    hLineYs[yi+1] = Integer.MIN_VALUE;
                } else {
                    hLineXs[xi]   = xP - 64;
                    hLineXs[xi+1] = xP + 63;

                    hLineYs[yi]   = yP - 64 - correction;
                    hLineYs[yi+1] = yP - 64 - correction;

                    float xDist = (xP - playerSmoothX) / 128f;
                    float yDist = (yP - 64 - playerSmoothY) / 128f;
                    hDists[di] = xDist * xDist + yDist * yDist;
                }

                previousTileBlocked = isBlocked;
                xi += 2;
                yi += 2;
                di += 1;
            }
        }

        // Vertical Lines
        int[] vLineXs = new int[lineCount * 2];
        int[] vLineYs = new int[lineCount * 2];
        float[] vDists = new float[lineCount];

        xi = 0;
        yi = 0;
        di = 0;
        for (int y = -renderRadius; y <= renderRadius; y++) {
            int yP = (playerTrueY + y*128);
            int tileY = yP / 128;

            previousTileBlocked = true;
            for (int x = -renderRadius+1; x <= renderRadius; x++) {
                int xP = (playerTrueX + x*128);
                int tileX = xP / 128;

                boolean isBlocked = false;
                if (doWalkableCheck && tileX >= 5 && tileX < 99 && tileY >= 5 && tileY < 99) {
                    int collisionMask = collisionData[tileX][tileY];
                    isBlocked = (collisionMask & UNWALKABLE_MASK) != 0;
                }

                // Used to move the calculation onto the edge of the previous tile
                // This way, a walkable tile next to an unwalkable tile with a large height difference
                // (i.e. a bridges), will have all its sides rendered at bridge level, instead of some at water level.
                int correction = isBlocked ? 1 : 0;

                if (isBlocked && previousTileBlocked) {
                    vLineXs[xi] = Integer.MIN_VALUE;
                    vLineXs[xi+1] = Integer.MIN_VALUE;

                    vLineYs[yi] = Integer.MIN_VALUE;
                    vLineYs[yi+1] = Integer.MIN_VALUE;
                } else {
                    vLineXs[xi]   = xP - 64 - correction;
                    vLineXs[xi+1] = xP - 64 - correction;

                    vLineYs[yi]   = yP - 64;
                    vLineYs[yi+1] = yP + 63;

                    float xDist = (xP - 64 - playerSmoothX) / 128f;
                    float yDist = (yP - playerSmoothY) / 128f;
                    vDists[di] = xDist * xDist + yDist * yDist;
                }

                previousTileBlocked = isBlocked;
                xi += 2;
                yi += 2;
                di += 1;
            }
        }

        Point[] hPoints = BulkPerspective.getCanvasTilePoints(client, wv, hLineXs, hLineYs, plane);
        Point[] vPoints = BulkPerspective.getCanvasTilePoints(client, wv, vLineXs, vLineYs, plane);

        {
            BufferedImage bufferedImage = getBufferedImage();
            Graphics2D bufferedGraphics = bufferedImage.createGraphics();

            boolean isOverworld = WorldPoint.getMirrorPoint(wPos, true).getY() < Constants.OVERWORLD_MAX_Y;
            Color realColor = isOverworld ? config.overworldGridColor() : config.otherGridColor();
            int alpha = realColor.getAlpha();

            // Intentionally write the ALPHA into BLUE
            Color alphaColor = new Color(0, 0, realColor.getAlpha(), 255);
            // Isolate the color, with no alpha
            int rgbInt = realColor.getRGB() & 0x00FFFFFF;

            bufferedGraphics.setColor(alphaColor);
            bufferedGraphics.setStroke(new BasicStroke(config.lineThickness()));

            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            drawLines(bufferedGraphics, alpha, hDists, hPoints, width, height);
            drawLines(bufferedGraphics, alpha, vDists, vPoints, width, height);
            applyColorAndAlpha(bufferedImage, rgbInt);

            graphics.drawImage(bufferedImage, 0, 0, null);
            bufferedGraphics.dispose();
        }

        return null;
    }

    private void drawLines(Graphics2D bufferedGraphics, int alpha, float[] distances, Point[] points, int w, int h) {
        boolean doFadeOut = config.doFadeOut();
        int fadeOutDistanceSqr = config.fadeOutDistance() * config.fadeOutDistance();
        double fadeOutTaper = config.fadeOutTaper() * config.fadeOutTaper();

        for (int i = 0; i < points.length; i+=2) {
            Point p1 = points[i];
            Point p2 = points[i + 1];
            if (p1 != null && p2 != null && inBounds(p1, p2, w, h)) {
                if (doFadeOut) {
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
