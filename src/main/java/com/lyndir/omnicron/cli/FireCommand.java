package com.lyndir.omnicron.cli;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterators;
import com.lyndir.lhunath.opal.system.util.ConversionUtils;
import com.lyndir.omnicron.api.controller.WeaponModule;
import com.lyndir.omnicron.api.model.*;
import java.util.Iterator;


/**
 * <i>10 07, 2012</i>
 *
 * @author lhunath
 */
@CommandGroup(name = "fire", abbr = "f", desc = "Fire weapons at a target.")
public class FireCommand extends Command {

    @Override
    public void evaluate(final OmnicronCLI omnicron, final Iterator<String> tokens) {

        String objectIDArgument = Iterators.getNext( tokens, null );
        if (objectIDArgument == null) {
            err( "Missing objectID.  Syntax: objectID dU dV [level]" );
            return;
        }
        if ("help".equals( objectIDArgument )) {
            inf( "Usage: objectID dU dV [level]" );
            inf( "    objectID: The ID of the object to fire with (see list objects)." );
            inf( "          dU: The delta from the current position's u to the target u." );
            inf( "          dV: The delta from the current position's v to the target v." );
            inf( "       level: The level into which to target the weapon (optional, default=current)." );
            return;
        }

        String duArgument = Iterators.getNext( tokens, null );
        if (duArgument == null) {
            err( "Missing dU.  Syntax: objectID dU dV [level]" );
            return;
        }
        String dvArgument = Iterators.getNext( tokens, null );
        if (dvArgument == null) {
            err( "Missing dV.  Syntax: objectID dU dV [level]" );
            return;
        }

        int objectId = ConversionUtils.toIntegerNN( objectIDArgument );
        int du = ConversionUtils.toIntegerNN( duArgument );
        int dv = ConversionUtils.toIntegerNN( dvArgument );

        // Find the game object for the given ID.
        Optional<GameObject> optionalObject = omnicron.getLocalPlayer().getController().getObject( omnicron.getLocalPlayer(), objectId );
        if (!optionalObject.isPresent()) {
            err( "No observable object with ID: %s", objectId );
            return;
        }
        GameObject gameObject = optionalObject.get();

        final String levelArgument = Iterators.getNext( tokens, gameObject.getLocation().getLevel().getName() );
        Optional<Level> level = FluentIterable.from( omnicron.getGameController().listLevels() ).firstMatch( new Predicate<Level>() {
            @Override
            public boolean apply(final Level input) {

                return input.getName().equalsIgnoreCase( levelArgument );
            }
        } );
        if (!level.isPresent()) {
            err( "No such level in this game: %s", levelArgument );
            return;
        }

        // Check to see if it's mobile by finding its mobility module.
        Optional<WeaponModule> optionalWeapon = gameObject.getModule( WeaponModule.class );
        if (!optionalWeapon.isPresent()) {
            err( "Object has no weapons: %s", gameObject );
            return;
        }
        WeaponModule weaponModule = optionalWeapon.get();

        // Find the target tile.
        Coordinate targetCoordinate = gameObject.getLocation().getPosition().delta( du, dv );
        Optional<Tile> target = level.get().getTile( targetCoordinate );
        if (!(target.isPresent())) {
            err( "No tile in level: %s, at position: %s", level.get().getName(), targetCoordinate );
            return;
        }

        // Fire at the target.
        weaponModule.fireAt( omnicron.getLocalPlayer(), target.get() );
        Optional<GameObject> targetContents = target.get().getContents();
        inf( "Fired at: %s", targetContents.isPresent()? targetContents.get(): target );
    }
}
