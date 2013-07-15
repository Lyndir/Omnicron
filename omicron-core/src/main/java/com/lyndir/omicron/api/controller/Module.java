package com.lyndir.omicron.api.controller;

import com.google.common.base.Preconditions;
import com.lyndir.omicron.api.model.GameObject;


public abstract class Module {

    private GameObject gameObject;

    public void setGameObject(final GameObject gameObject) {

        this.gameObject = gameObject;
    }

    public GameObject getGameObject() {

        return Preconditions.checkNotNull( gameObject, "This module has not yet been initialized by its game object." );
    }

    public abstract void onNewTurn();
}
