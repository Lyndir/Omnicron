package com.lyndir.omicron.cli.command;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterators;
import com.lyndir.omicron.api.model.*;
import com.lyndir.omicron.cli.OmicronCLI;
import java.util.Iterator;


/**
 * <i>10 16, 2012</i>
 *
 * @author lhunath
 */
@CommandGroup(parent = AddCommand.class, name = "game", abbr = "g", desc = "Add things to an Omicron game that is being built.")
public class AddGameCommand extends Command {

    public AddGameCommand(final OmicronCLI omicron) {
        super( omicron );
    }

    @SubCommand(abbr = "p", desc = "The players that will compete in this game.")
    public void player(final Iterator<String> tokens) {

        if (getOmicron().getLocalPlayer() != null) {
            err( "There is already a local player: %s", getOmicron().getLocalPlayer().getName() );
            return;
        }

        Game.Builder gameBuilder = getOmicron().getBuilders().getGameBuilder();
        if (gameBuilder == null) {
            err( "No game build to add game properties to.  Begin with the 'build' command." );
            return;
        }

        String value = Iterators.getOnlyElement( tokens, null );
        if (value == null) {
            err( "Missing definition of player to add.  Syntax: name,primary color,secondary color" );
            return;
        }

        Iterator<String> playerValueIt = Splitter.on( ',' ).limit( 3 ).split( value ).iterator();
        String playerName = playerValueIt.next();
        String playerPrimaryColor = playerValueIt.next();
        String playerSecondaryColor = Iterators.getOnlyElement( playerValueIt );

        getOmicron().setLocalPlayer( new Player( gameBuilder.nextPlayerID(), getOmicron().getLocalKey(), playerName, Color.of( playerPrimaryColor ),
                                             Color.of( playerSecondaryColor ) ) );
        gameBuilder.getPlayers().add( getOmicron().getLocalPlayer() );
        inf( "Added player to game: %s", getOmicron().getLocalPlayer() );
    }
}
