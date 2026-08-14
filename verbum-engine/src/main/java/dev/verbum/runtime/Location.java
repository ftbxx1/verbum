package dev.verbum.runtime;

/** A 3D point in a Minecraft world. */
public record Location(String world, double x, double y, double z) {

    public static Location at(String world, double x, double y, double z) {
        return new Location(world, x, y, z);
    }

    /** Distance in the horizontal plane, ignoring height. */
    public double distanceHorizontal(Location o) {
        double dx = x - o.x;
        double dz = z - o.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    @Override
    public String toString() {
        return world + " (" + format(x) + ", " + format(y) + ", " + format(z) + ")";
    }

    private static String format(double v) {
        if (v == Math.rint(v)) return String.valueOf((long) v);
        return String.valueOf(v);
    }
}
