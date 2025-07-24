package com.notloc.tilegrid;

import net.runelite.api.*;

import javax.annotation.Nonnull;

/**
 * Some code copied from Perspective.java and modified to process points in bulk
 */
public class BulkPerspective {

    public static final int LOCAL_COORD_BITS = 7;
    public static final int SCENE_SIZE = Constants.SCENE_SIZE; // in tiles
    private static final int ESCENE_OFFSET = (Constants.EXTENDED_SCENE_SIZE - Constants.SCENE_SIZE) / 2;

    private static final int INVALID_INT = Integer.MIN_VALUE;

    public static Point[] getCanvasTilePoints(@Nonnull Client client, @Nonnull WorldView wv, int[] localXL, int[] localYL, int plane) {
        Scene scene = wv.getScene();

        int[] planes = new int[localXL.length];
        for (int i=0; i < localXL.length; i++) {
            int localX = localXL[i];
            int localY = localYL[i];

            int msx = (localX >> 7) + 40;
            int msy = (localY >> 7) + 40;
            if (msx >= 0 && msy >= 0 && msx < 184 && msy < 184) {
                if (plane == -1) {
                    plane = wv.getPlane();
                }

                byte[][][] tileSettings = scene.getExtendedTileSettings();
                int tilePlane = plane;
                if (plane < 3 && (tileSettings[1][msx][msy] & 2) == 2) {
                    tilePlane = plane + 1;
                }
                planes[i] = tilePlane;
            } else {
                planes[i] = INVALID_INT;
            }
        }

        int[] heights = getHeightBulk(scene, localXL, localYL, planes);
        return localToCanvasBulk(client, localXL, localYL, heights);
    }

    private static int[] getHeightBulk(@Nonnull Scene scene, int[] localXL, int[] localYL, int[] planes) {
        int[][][] tileHeights = scene.getTileHeights();
        int[] heights = new int[localXL.length];
        for (int i = 0; i < localXL.length; i++) {
            int localX = localXL[i];
            int localY = localYL[i];
            int plane = planes[i];
            if (plane == INVALID_INT) {
                heights[i] = INVALID_INT;
                continue;
            }

            int sceneX = (localX >> 7) + 40;
            int sceneY = (localY >> 7) + 40;
            if (sceneX >= 0 && sceneY >= 0 && sceneX < 184 && sceneY < 184) {
                int x = localX & 127;
                int y = localY & 127;
                int var8 = x * tileHeights[plane][sceneX + 1][sceneY] + (128 - x) * tileHeights[plane][sceneX][sceneY] >> 7;
                int var9 = tileHeights[plane][sceneX][sceneY + 1] * (128 - x) + x * tileHeights[plane][sceneX + 1][sceneY + 1] >> 7;
                heights[i] = (128 - y) * var8 + y * var9 >> 7;
            }
        }
        return heights;
    }

    public static Point[] localToCanvasBulk(Client client, int[] xL, int[] yL, int[] zL) {
        return client.isGpu() ? localToCanvasBulkGpu(client, xL, yL, zL) : localToCanvasBulkCpu(client, xL, yL, zL);
    }

    private static Point[] localToCanvasBulkCpu(Client client, int[] xL, int[] yL, int[] zL)
    {
        final int cameraPitch = client.getCameraPitch();
        final int cameraYaw = client.getCameraYaw();

        final int pitchSin = Perspective.SINE[cameraPitch];
        final int pitchCos = Perspective.COSINE[cameraPitch];
        final int yawSin = Perspective.SINE[cameraYaw];
        final int yawCos = Perspective.COSINE[cameraYaw];

        final int camX = client.getCameraX();
        final int camY = client.getCameraY();
        final int camZ = client.getCameraZ();
        final int scale = client.getScale();
        final int viewportWidthHalf = client.getViewportWidth() / 2;
        final int viewportHeightHalf = client.getViewportHeight() / 2;
        final int viewportXOffset = client.getViewportXOffset();
        final int viewportYOffset = client.getViewportYOffset();

        Point[] points = new Point[xL.length];

        int prevX = INVALID_INT;
        int prevY = INVALID_INT;
        int prevZ = INVALID_INT;

        for (int i=0; i<xL.length; i++) {
            int x = xL[i];
            int y = yL[i];
            int z = zL[i];
            if (z == INVALID_INT) {
                continue;
            }

            if (x >= -ESCENE_OFFSET << LOCAL_COORD_BITS && y >= -ESCENE_OFFSET << LOCAL_COORD_BITS &&
                    x <= SCENE_SIZE + ESCENE_OFFSET << LOCAL_COORD_BITS && y <= SCENE_SIZE + ESCENE_OFFSET << LOCAL_COORD_BITS)
            {
                x -= camX;
                y -= camY;
                z -= camZ;

                final int
                        x1 = x * yawCos + y * yawSin >> 16,
                        y1 = y * yawCos - x * yawSin >> 16,
                        y2 = z * pitchCos - y1 * pitchSin >> 16,
                        z1 = y1 * pitchCos + z * pitchSin >> 16;

                if (z1 >= 50)
                {
                    final int pointX = viewportWidthHalf + x1 * scale / z1;
                    final int pointY = viewportHeightHalf + y2 * scale / z1;
                    points[i] = new Point(
                            pointX + viewportXOffset,
                            pointY + viewportYOffset
                    );
                }
            }
        }
        return points;
    }

    private static Point[] localToCanvasBulkGpu(Client client, int[] xL, int[] yL, int[] zL)
    {
        final double
                cameraPitch = client.getCameraFpPitch(),
                cameraYaw = client.getCameraFpYaw();

        final float
                pitchSin = (float) Math.sin(cameraPitch),
                pitchCos = (float) Math.cos(cameraPitch),
                yawSin = (float) Math.sin(cameraYaw),
                yawCos = (float) Math.cos(cameraYaw),
                cameraFpX = (float) client.getCameraFpX(),
                cameraFpY = (float) client.getCameraFpY(),
                cameraFpZ = (float) client.getCameraFpZ();

        final int scale = client.getScale();
        final int viewportWidthHalf = client.getViewportWidth() / 2;
        final int viewportHeightHalf = client.getViewportHeight() / 2;
        final int viewportXOffset = client.getViewportXOffset();
        final int viewportYOffset = client.getViewportYOffset();

        Point[] points = new Point[xL.length];
        for (int i = 0; i < xL.length; i++) {
            int x = xL[i];
            int y = yL[i];
            int z = zL[i];
            if (z == INVALID_INT) {
                continue;
            }

            if (x >= -ESCENE_OFFSET << LOCAL_COORD_BITS && y >= -ESCENE_OFFSET << LOCAL_COORD_BITS &&
                    x <= SCENE_SIZE + ESCENE_OFFSET << LOCAL_COORD_BITS && y <= SCENE_SIZE + ESCENE_OFFSET << LOCAL_COORD_BITS)
            {
                final float
                        fx = x - cameraFpX,
                        fy = y - cameraFpY,
                        fz = z - cameraFpZ;

                final float
                        x1 = fx * yawCos + fy * yawSin,
                        y1 = fy * yawCos - fx * yawSin,
                        y2 = fz * pitchCos - y1 * pitchSin,
                        z1 = y1 * pitchCos + fz * pitchSin;

                if (z1 >= 50f)
                {
                    final int pointX = Math.round(viewportWidthHalf + x1 * scale / z1);
                    final int pointY = Math.round(viewportHeightHalf + y2 * scale / z1);
                    points[i] = new Point(
                            pointX + viewportXOffset,
                            pointY + viewportYOffset
                    );
                }
            }
        }
        return points;
    }
}
