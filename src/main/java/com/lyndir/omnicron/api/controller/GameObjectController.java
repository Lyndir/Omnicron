package com.lyndir.omnicron.api.controller;

import com.google.common.collect.ImmutableList;
import com.lyndir.omnicron.api.model.*;
import org.jetbrains.annotations.NotNull;


public abstract class GameObjectController<O extends GameObject> implements GameObserver {

    private final O gameObject;

    protected GameObjectController(final O gameObject) {

        this.gameObject = gameObject;
    }

    public O getGameObject() {

        return gameObject;
    }

    @Override
    public boolean canObserve(@NotNull final Player currentPlayer, @NotNull final Tile location) {

        return getGameObject().onModuleElse( BaseModule.class, false ).canObserve( currentPlayer, location );
    }

    @NotNull
    @Override
    public Iterable<Tile> listObservableTiles(@NotNull final Player currentPlayer) {

        return getGameObject().onModuleElse( BaseModule.class, ImmutableList.of() ).listObservableTiles( currentPlayer );
    }

    public void newTurn() {

        for (final Module module : getGameObject().listModules())
            module.newTurn();
    }
}
