package com.lyndir.omicron.api.controller;

import com.lyndir.omicron.api.model.*;


public class ObjectController<O extends GameObject> {

    private final O gameObject;

    public ObjectController(final O gameObject) {

        this.gameObject = gameObject;
    }

    protected O getGameObject() {

        return gameObject;
    }

    public void newTurn() {

    }
}
