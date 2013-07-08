package com.lyndir.omnicron.api.controller;

import com.google.common.base.*;
import com.google.common.collect.*;
import com.lyndir.lhunath.opal.system.logging.Logger;
import com.lyndir.omnicron.api.model.*;
import com.lyndir.omnicron.api.view.PlayerGameInfo;
import java.util.*;


public class GameController {

    private final Game game;

    public GameController(final Game game) {

        this.game = game;

        newTurn();
    }

    public PlayerGameInfo getPlayerGameInfo(final GameObserver observer, final Player player) {

        if (hasDiscovered( observer, player.getController() ))
            return PlayerGameInfo.discovered( player, player.getScore() );

        return PlayerGameInfo.undiscovered( player );
    }

    private boolean hasDiscovered(final GameObserver observer, final GameObserver target) {

        return FluentIterable.from( getObservedTiles( observer ) ).anyMatch( new Predicate<Tile>() {
            @Override
            public boolean apply(final Tile input) {

                return input.contains( target );
            }
        } );
    }

    private Iterable<Tile> getObservedTiles(final GameObserver observer) {

        return FluentIterable.from( game.getGround().getTiles().values() ).filter( new Predicate<Tile>() {
            @Override
            public boolean apply(final Tile input) {

                return observer.canObserve( observer.getPlayer(), input );
            }
        } );
    }

    public Collection<PlayerGameInfo> listPlayerGameInfo(final GameObserver observer) {

        return Collections2.transform( game.getPlayers(), new Function<Player, PlayerGameInfo>() {
            @Override
            public PlayerGameInfo apply(final Player input) {

                return getPlayerGameInfo( observer, input );
            }
        } );
    }

    public Iterable<Player> listPlayers() {

        return game.getPlayers();
    }

    public void setReady(final Player currentPlayer) {

        game.getReadyPlayers().add( currentPlayer );

        if (game.getReadyPlayers().containsAll( game.getPlayers() )) {
            newTurn();
            game.getReadyPlayers().clear();
        }
    }

    private void newTurn() {

        for (final Player player : game.getPlayers())
            player.getController().newTurn();
    }
}
