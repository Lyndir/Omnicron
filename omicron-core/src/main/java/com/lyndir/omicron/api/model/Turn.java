package com.lyndir.omicron.api.model;

import com.lyndir.lhunath.opal.system.util.MetaObject;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class Turn extends MetaObject {

    @Nullable
    private final Turn previousTurn;
    private final int  number;

    Turn() {
        previousTurn = null;
        number = 0;
    }

    Turn(@Nonnull final Turn previousTurn) {
        this.previousTurn = previousTurn;
        number = previousTurn.getNumber() + 1;
    }

    @Nullable
    public Turn getPreviousTurn() {
        return previousTurn;
    }

    public int getNumber() {
        return number;
    }
}
