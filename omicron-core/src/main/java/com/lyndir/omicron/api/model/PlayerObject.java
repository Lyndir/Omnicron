package com.lyndir.omicron.api.model;

import com.lyndir.omicron.api.controller.Module;
import org.jetbrains.annotations.NotNull;


/**
 * <i>10 15, 2012</i>
 *
 * @author lhunath
 */
public abstract class PlayerObject extends GameObject {

    private final Player owner;

    protected PlayerObject(final String typeName, final Player owner, final Tile location, final Module... modules) {

        super( typeName, owner.nextObjectID(), location, modules );

        this.owner = owner;
    }

    @NotNull
    @Override
    public final Player getPlayer() {

        return owner;
    }
}
