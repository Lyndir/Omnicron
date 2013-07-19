package com.lyndir.omicron.api.model;

import static com.lyndir.lhunath.opal.system.util.ObjectUtils.*;

import com.google.common.base.*;
import com.google.common.collect.ImmutableList;
import com.lyndir.lhunath.opal.system.util.*;
import java.util.EnumMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


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

    public Tile(final Coordinate position, final Level level) {

        this.position = position;
        this.level = level;

        level.putTile( position, this );
        level.getTile( position.neighbour( Coordinate.Side.NW ) ).get();
        level.getTile( position.neighbour( Coordinate.Side.NE ) ).get();
        level.getTile( position.neighbour( Coordinate.Side.W ) ).get();
        level.getTile( position.neighbour( Coordinate.Side.E ) ).get();
        level.getTile( position.neighbour( Coordinate.Side.SW ) ).get();
        level.getTile( position.neighbour( Coordinate.Side.SE ) ).get();
    }

    public Tile(final int u, final int v, final Level level) {

        this( new Coordinate( u, v, level.getSize() ), level );
    }

    @NotNull
    public Optional<GameObject> getContents() {

        return Optional.fromNullable( contents );
    }

    public void setContents(@Nullable final GameObject contents) {

        if (contents != null)
            Preconditions.checkState( !getContents().isPresent(), "Cannot put object on tile that is not empty: %s, holds: %s", //
                                      this, getContents().orNull() );

        this.contents = contents;
    }

    public Coordinate getPosition() {

        return position;
    }

    public Level getLevel() {

        return level;
    }

    public void setResourceQuantity(final ResourceType resourceType, final int resourceQuantity) {

        Preconditions.checkArgument( resourceQuantity >= 0, "Resource quantity cannot be less than zero: %s", resourceQuantity );
        resourceQuantities.put( resourceType, resourceQuantity );
    }

    public void addResourceQuantity(final ResourceType resourceType, final int resourceQuantity) {

        setResourceQuantity( resourceType, getResourceQuantity( resourceType ) + resourceQuantity );
    }

    public int getResourceQuantity(final ResourceType resourceType) {

        return ifNotNullElse( resourceQuantities.get( resourceType ), 0 );
    }

    @NotNull
    public Tile neighbour(final Coordinate.Side side) {

        return level.getTile( getPosition().neighbour( side ) ).get();
    }

    public Iterable<Tile> neighbours() {

        ImmutableList.Builder<Tile> neighbours = ImmutableList.builder();
        for (final Coordinate.Side side : Coordinate.Side.values())
            neighbours.add( neighbour( side ) );

        return neighbours.build();
    }

    public boolean contains(@NotNull final GameObserver target) {

        if (contents == null)
            return false;

        if (ObjectUtils.isEqual( contents, target ))
            return true;

        Player player = contents.getPlayer();
        if (player != null)
            return ObjectUtils.isEqual( player.getController(), target );

        return false;
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

    public boolean isAccessible() {

        return !getContents().isPresent();
    }
}
