package com.lyndir.omnicron.cli;

import com.google.common.base.Joiner;
import com.google.common.collect.*;
import com.lyndir.omnicron.api.model.*;
import java.util.*;


/**
 * <i>10 07, 2012</i>
 *
 * @author lhunath
 */
@CommandGroup(name = "print", abbr = "p", desc = "Print various information on the current state of the omnicron game.")
public class PrintCommand extends Command {

    private static final List<Class<? extends Level>>           levelIndexes    = ImmutableList.of( GroundLevel.class, SkyLevel.class,
                                                                                                    SpaceLevel.class );
    private static final Map<Class<? extends Level>, Character> levelCharacters = ImmutableMap.of( GroundLevel.class, '_', //
                                                                                                   SkyLevel.class, '~', //
                                                                                                   SpaceLevel.class, '^' );

    @SubCommand(abbr = "f", desc = "A view of all observable tiles.")
    public void field(final OmnicronCLI omnicron, final Iterator<String> tokens) {

        // Create an empty grid.
        Size maxSize = null;
        for (final Level level : omnicron.getGameController().listLevels())
            maxSize = Size.max( maxSize, level.getLevelSize() );
        assert maxSize != null;
        Table<Integer, Integer, StringBuilder> grid = HashBasedTable.create( maxSize.getHeight(), maxSize.getWidth() );
        for (int u = 0; u < maxSize.getWidth(); ++u)
            for (int v = 0; v < maxSize.getHeight(); ++v)
                grid.put( v, u, new StringBuilder( "   " ) );

        // Iterate observable tiles and populate the grid.
        for (final Tile tile : omnicron.getLocalPlayer().listObservableTiles( omnicron.getLocalPlayer() )) {
            GameObject contents = tile.getContents();
            char contentsChar;
            if (contents == null)
                contentsChar = levelCharacters.get( tile.getLevel().getClass() );
            else
                contentsChar = contents.getTypeName().charAt( 0 );

            int levelIndex = levelIndexes.indexOf( tile.getLevel().getClass() );
            int v = tile.getPosition().getV();
            int u = (tile.getPosition().getU() + v / 2) % maxSize.getWidth();
            grid.get( v, u ).setCharAt( levelIndex, contentsChar );
        }

        for (int v = 0; v < maxSize.getHeight(); ++v) {
            Map<Integer, StringBuilder> row = new TreeMap<>( Ordering.natural() );
            row.putAll( grid.row( v ) );
            inf( "%s|%s|", v % 2 == 0? "": "  ", Joiner.on( ' ' ).join( row.values() ) );
        }
    }
}
