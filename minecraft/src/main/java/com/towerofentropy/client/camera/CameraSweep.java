package com.towerofentropy.client.camera;

/** Sweeps a point against boxes expanded by the camera's collision half-width. */
final class CameraSweep {
    private static final double EPSILON = 1e-9;
    private double x, y, z, dx, dy, dz, radius, skin, limit;
    private double enter, leave;

    void reset(double x, double y, double z, double dx, double dy, double dz, double radius, double skin) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.radius = radius;
        this.skin = skin;
        limit = 1;
    }

    double fraction() { return limit; }

    void include(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        minX -= radius;
        minY -= radius;
        minZ -= radius;
        maxX += radius;
        maxY += radius;
        maxZ += radius;
        boolean overlapping = x > minX && x < maxX && y > minY && y < maxY && z > minZ && z < maxZ;
        if (overlapping) {
            // An eye beside a wall can already overlap the margin. Permit departure through a nearest face.
            double nearest = Math.min(Math.min(x - minX, maxX - x),
                    Math.min(Math.min(y - minY, maxY - y), Math.min(z - minZ, maxZ - z))) + EPSILON;
            if ((dx < 0 && x - minX <= nearest) || (dx > 0 && maxX - x <= nearest)
                    || (dy < 0 && y - minY <= nearest) || (dy > 0 && maxY - y <= nearest)
                    || (dz < 0 && z - minZ <= nearest) || (dz > 0 && maxZ - z <= nearest)) return;
            limit = 0;
            return;
        }

        enter = 0;
        leave = limit;
        if (clipAxis(x, dx, minX, maxX) && clipAxis(y, dy, minY, maxY)
                && clipAxis(z, dz, minZ, maxZ) && leave > enter)
            limit = Math.max(0, enter - skin);
    }

    private boolean clipAxis(double origin, double direction, double min, double max) {
        if (Math.abs(direction) < EPSILON) return origin > min && origin < max;
        double a = (min - origin) / direction;
        double b = (max - origin) / direction;
        enter = Math.max(enter, Math.min(a, b));
        leave = Math.min(leave, Math.max(a, b));
        return leave > enter;
    }
}
