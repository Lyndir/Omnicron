package com.lyndir.omicron.api.model;

import static com.lyndir.lhunath.opal.system.util.ObjectUtils.*;

import com.google.common.base.*;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.lyndir.lhunath.opal.system.util.*;
import com.lyndir.omicron.api.*;
import com.lyndir.omicron.api.util.Maybe;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * <i>10 07, 2012</i>
 *
 * @author lhunath
 */
@ObjectMeta(useFor = { })
public class Tile extends MetaObject {

    @Nullable
    private       GameObject contents;
    @ObjectMeta(useFor = ObjectMeta.For.all)
    private final Coordinate position;
    @ObjectMeta(useFor = ObjectMeta.For.all)
    private final Level      level;
    @ObjectMeta(useFor = ObjectMeta.For.all)
    private final Map<ResourceType, Integer> resourceQuantities = new EnumMap<>( ResourceType.class );

    Tile(final Coordinate position, final Level level) {
        this.position = position;
        this.level = level;
    }

    Tile(final int u, final int v, final Level level) {
        this( new Coordinate( u, v, level.getSize() ), level );
    }

    @Nonnull
    Optional<GameObject> getContents() {
        return Optional.fromNullable( contents );
    }

    @Nonnull
    @Authenticated
    public Maybe<GameObject> checkContents() {
        if (!Security.getCurrentPlayer().canObserve( this )) {
            return Maybe.unknown();
        }
        return Maybe.fromNullable( contents );
    }

    void setContents(@Nullable final GameObject contents) {
        if (contents != null)
            Preconditions.checkState( this.contents == null, "Cannot put object on tile that is not empty: %s", this );
        Change.From<GameObject> contentsChange = Change.from( this.contents );

        this.contents = contents;

        getLevel().getGame().getController().fireFor( new PredicateNN<Player>() {
            @Override
            public boolean apply(@Nonnull final Player input) {
                return input.canObserve( Tile.this );
            }
        } ).onTileContents( this, contentsChange.to( this.contents ) );
    }

    public Coordinate getPosition() {
        return position;
    }

    public Level getLevel() {
        return level;
    }

    void setResourceQuantity(final ResourceType resourceType, final int resourceQuantity) {
        Preconditions.checkArgument( resourceQuantity >= 0, "Resource quantity cannot be less than zero: %s", resourceQuantity );
        ChangeInt.From quantityChange;
        if (resourceQuantity > 0)
            quantityChange = ChangeInt.from( resourceQuantities.put( resourceType, resourceQuantity ) );
        else
            quantityChange = ChangeInt.from( resourceQuantities.remove( resourceType ) );

        getLevel().getGame().getController().fireFor( new PredicateNN<Player>() {
            @Override
            public boolean apply(@Nonnull final Player input) {
                return input.canObserve( Tile.this );
            }
        } ).onTileResources( this, resourceType, quantityChange.to( resourceQuantity ) );
    }

    void addResourceQuantity(final ResourceType resourceType, final int resourceQuantity) {
        setResourceQuantity( resourceType, ifNotNullElse( resourceQuantities.get( resourceType ), 0 ) + resourceQuantity );
    }

    Optional<Integer> getResourceQuantity(final ResourceType resourceType) {
        return Optional.fromNullable( resourceQuantities.get( resourceType ) );
    }

    @Authenticated
    public Maybe<Integer> checkResourceQuantity(final ResourceType resourceType) {
        if (!Security.getCurrentPlayer().canObserve( this ))
            return Maybe.unknown();

        return Maybe.fromNullable( resourceQuantities.get( resourceType ) );
    }

    @Nonnull
    public Tile neighbour(final Coordinate.Side side) {
        return level.getTile( getPosition().neighbour( side ) ).get();
    }

    public ImmutableCollection<Tile> neighbours() {
        ImmutableList.Builder<Tile> neighbours = ImmutableList.builder();
        for (final Coordinate.Side side : Coordinate.Side.values())
            neighbours.add( neighbour( side ) );

        return neighbours.build();
    }

    public ImmutableCollection<Tile> neighbours(final int distance) {
        ImmutableList.Builder<Tile> neighbours = ImmutableList.builder();
        // FIXME: Not correct.
        for (int du = -distance; du <= distance; ++du)
            for (int dv = Math.max( -distance, -du - distance ); dv <= Math.min( distance, -du + distance ); ++dv)
                neighbours.add( level.getTile( getPosition().delta( du, dv ) ).get() );

        return neighbours.build();
    }

    @Authenticated
    public Maybe<Boolean> checkContains(@Nonnull final GameObject target) {
        Maybe<GameObject> contents = checkContents();
        if (contents.presence() == Maybe.Presence.ABSENT)
            return Maybe.of( false );
        if (contents.presence() == Maybe.Presence.UNKNOWN)
            return Maybe.unknown();

        return Maybe.of( ObjectUtils.isEqual( contents.get(), target ) );
    }

    @Override
    public int hashCode() {
        return Objects.hashCode( position, level );
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Tile))
            return false;

        Tile o = (Tile) obj;
        return ObjectUtils.isEqual( position, o.position ) && ObjectUtils.isEqual( level, o.level );
    }

    /**
     * @return true if this tile has no contents.
     */
    boolean isAccessible() {
        return !getContents().isPresent();
    }

    /**
     * @return true if this tile is visible to the current player and has no contents.
     */
    @Authenticated
    public boolean checkAccessible() {
        return checkContents().presence() == Maybe.Presence.ABSENT;
    }
}
