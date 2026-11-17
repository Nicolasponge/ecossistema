package ecossistema.model;

/** Vetor 2D imutável. Usado para posição, direção e velocidade. */
public record Vec2(double x, double y) {

    public static final Vec2 ZERO = new Vec2(0, 0);

    public Vec2 plus(Vec2 o)    { return new Vec2(x + o.x, y + o.y); }
    public Vec2 minus(Vec2 o)   { return new Vec2(x - o.x, y - o.y); }
    public Vec2 times(double k) { return new Vec2(x * k, y * k); }

    public double length()            { return Math.hypot(x, y); }
    public double distance(Vec2 o)    { return Math.hypot(x - o.x, y - o.y); }

    public Vec2 normalized() {
        double l = length();
        return l < 1e-9 ? ZERO : new Vec2(x / l, y / l);
    }
}
