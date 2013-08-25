package com.lyndir.omicron.api.model;

import com.google.common.collect.ImmutableCollection;
import com.lyndir.omicron.api.util.Maybe;
import javax.annotation.Nonnull;


/**
 * <i>10 07, 2012</i>
 *
 * @author lhunath
 */
public interface ITile {

    @Nonnull
    Maybe<? extends IGameObject> checkContents()
            throws Security.NotAuthenticatedException;

    Coordinate getPosition();

    ILevel getLevel();

    Maybe<Integer> checkResourceQuantity(final ResourceType resourceType)
            throws Security.NotAuthenticatedException;

    @Nonnull
    ITile neighbour(final Coordinate.Side side);

    ImmutableCollection<? extends ITile> neighbours();

    ImmutableCollection<? extends ITile> neighbours(final int distance);

    Maybe<Boolean> checkContains(@Nonnull final IGameObject target)
            throws Security.NotAuthenticatedException;

    /**
     * @return true if this tile is visible to the current player and has no contents.
     */
    boolean checkAccessible()
            throws Security.NotAuthenticatedException;
}
