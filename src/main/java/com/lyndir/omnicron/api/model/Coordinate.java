package com.lyndir.omnicron.api.model;

import com.lyndir.lhunath.opal.system.util.MetaObject;


/**
 * <i>10 07, 2012</i>
 *
 * @author lhunath
 */
public class Coordinate extends MetaObject {

    public static final Coordinate ORIGIN = new Coordinate( 0, 0 );

    private final int u;
    private final int v;

    public Coordinate(final int u, final int v) {

        this.u = u;
        this.v = v;
    }

    public int getU() {

        return u;
    }

    public int getV() {

        return v;
    }

    public Coordinate delta(final int du, final int dv, final Size mapSize) {

        return new Coordinate( (mapSize.getWidth() + u + du) % mapSize.getWidth(), (mapSize.getHeight() + v + dv) % mapSize.getHeight() );
    }

    public int distanceTo(final Coordinate other) {

        int du = u - other.getU();
        int dv = v - other.getV();

        return (int) Math.ceil( Math.sqrt( du * du + dv * dv ) );
    }

    public Coordinate getNW(final Size mapSize) {

        return delta( 0, -1, mapSize );
    }

    public Coordinate getNE(final Size mapSize) {

        return delta( 1, -1, mapSize );
    }

    public Coordinate getW(final Size mapSize) {

        return delta( -1, 0, mapSize );
    }

    public Coordinate getE(final Size mapSize) {

        return delta( 1, 0, mapSize );
    }

    public Coordinate getSW(final Size mapSize) {

        return delta( -1, 1, mapSize );
    }

    public Coordinate getSE(final Size mapSize) {

        return delta( 0, 1, mapSize );
    }
}
