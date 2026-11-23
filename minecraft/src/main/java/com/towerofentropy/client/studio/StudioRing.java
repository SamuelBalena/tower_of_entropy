package com.towerofentropy.client.studio;

import org.joml.Vector3f;

/** Orthographic image of a rotation circle, with a frozen drag coordinate system. */
record StudioRing(int channel, Vector3f pivot, Vector3f u, Vector3f v) {
    static final int SEGMENTS = 48;
    static final double STEP = 2 * Math.PI / SEGMENTS;

    Vector3f point(double t) {
        return new Vector3f(pivot).fma((float) Math.cos(t), u).fma((float) Math.sin(t), v);
    }

    record Hit(StudioRing ring, double angle, double distance) {}

    Hit hit(double x, double y) {
        Vector3f a = point(0);
        double best = Double.POSITIVE_INFINITY, depth = Double.NEGATIVE_INFINITY, angle = 0;
        for (int i = 1; i <= SEGMENTS; i++) {
            var b = point(i * STEP);
            double dx = b.x - a.x, dy = b.y - a.y;
            double span = dx * dx + dy * dy;
            double t = span < 1e-8 ? 0 : Math.max(0, Math.min(1, ((x - a.x) * dx + (y - a.y) * dy) / span));
            double distance = Math.hypot(x - a.x - t * dx, y - a.y - t * dy);
            double candidateDepth = a.z + t * (b.z - a.z);
            if (distance < best - .06 || Math.abs(distance - best) <= .06 && candidateDepth > depth) {
                best = distance;
                depth = candidateDepth;
                angle = (i - 1 + t) * STEP;
            }
            a = b;
        }
        return new Hit(this, angle, best);
    }

    static final class Drag {
        private final StudioRing ring;
        private final double det, tx, ty;
        private double x, y, previous, total;

        Drag(Hit hit, double x, double y) {
            ring = hit.ring;
            this.x = x;
            this.y = y;
            var u = ring.u;
            var v = ring.v;
            double determinant = u.x * v.y - u.y * v.x;
            double areaScale = Math.max(u.lengthSquared(), v.lengthSquared());
            det = Math.abs(determinant) > .17 * areaScale ? determinant : 0;
            previous = det == 0 ? hit.angle : angle(x, y);
            double dx = -Math.sin(hit.angle) * u.x + Math.cos(hit.angle) * v.x;
            double dy = -Math.sin(hit.angle) * u.y + Math.cos(hit.angle) * v.y;
            if (Math.hypot(dx, dy) < .3 * Math.sqrt(areaScale)) {
                // A collapsed ellipse has two branches. Freeze the picked branch's tangent.
                double offset = hit.angle + (Math.sin(hit.angle) >= 0 ? -.35 : .35);
                dx = -Math.sin(offset) * u.x + Math.cos(offset) * v.x;
                dy = -Math.sin(offset) * u.y + Math.cos(offset) * v.y;
            }
            double length = Math.hypot(dx, dy);
            double scale = length < 1e-6 ? 0 : 1 / (length * Math.max(length, .55 * Math.sqrt(areaScale)));
            tx = dx * scale;
            ty = dy * scale;
        }

        double move(double nextX, double nextY, boolean precise) {
            double step;
            if (det == 0) step = (nextX - x) * tx + (nextY - y) * ty;
            else {
                step = 0;
                // Angle is undefined close to the pivot. Re-arm on the next stable sample.
                if (Math.hypot(nextX - ring.pivot.x, nextY - ring.pivot.y) < 3) previous = Double.NaN;
                else {
                    double next = angle(nextX, nextY);
                    if (Double.isFinite(previous)) step = Math.IEEEremainder(next - previous, 2 * Math.PI);
                    previous = next;
                }
            }
            x = nextX;
            y = nextY;
            total += precise ? step * .2 : step;
            return total;
        }

        private double angle(double x, double y) {
            x -= ring.pivot.x;
            y -= ring.pivot.y;
            return Math.atan2((ring.u.x * y - ring.u.y * x) / det, (x * ring.v.y - y * ring.v.x) / det);
        }
    }
}
