package com.lyndir.omicron.cli.command;

import com.lyndir.omicron.api.model.Game;
import com.lyndir.omicron.cli.OmicronCLI;
import java.util.Iterator;


/**
 * <i>10 07, 2012</i>
 *
 * @author lhunath
 */
@CommandGroup(name = "create", abbr = "c", desc = "Create a previously built game object.")
public class CreateCommand extends Command {

    public CreateCommand(final OmicronCLI omicron) {
        super( omicron );
    }

    @SubCommand(abbr = "g", desc = "Create a new game of Omicron")
    public void game(final Iterator<String> tokens) {

        Game.Builder gameBuilder = getOmicron().getBuilders().getGameBuilder();
        if (gameBuilder == null) {
            err( "No game has been built yet.  Begin with the 'build' command." );
            return;
        }
        if (gameBuilder.getPlayers().isEmpty()) {
            err( "No local player has been added yet.  Add players with the 'add' command." );
            return;
        }

        Game game = gameBuilder.build();
        getOmicron().setGameController( game.getController() );
        getOmicron().getBuilders().setGameBuilder( null );
        game.getController().start();
        inf( "Created game: %s", game );
    }
}
