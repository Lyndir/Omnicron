/*
 * Copyright 2010, Maarten Billemont
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.lyndir.omicron.cli.view;

import static com.lyndir.lhunath.opal.system.util.ObjectUtils.*;

import com.google.common.base.Optional;
import com.google.common.collect.*;
import com.googlecode.lanterna.input.Key;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.ScreenCharacterStyle;
import com.googlecode.lanterna.terminal.Terminal;
import com.lyndir.lanterna.view.*;
import com.lyndir.omicron.api.controller.GameController;
import com.lyndir.omicron.api.model.*;
import com.lyndir.omicron.api.model.Size;
import com.lyndir.omicron.cli.OmicronCLI;
import java.util.Map;
import javax.annotation.Nonnull;


/**
 * @author lhunath, 2013-07-21
 */
public class MapView extends View {

    private static final Map<LevelType, Terminal.Color>    levelTypeColors    = //
            ImmutableMap.of( LevelType.GROUND, Terminal.Color.GREEN, LevelType.SKY, Terminal.Color.CYAN, LevelType.SPACE,
                             Terminal.Color.BLUE );
    private static final Map<ResourceType, Terminal.Color> resourceTypeColors = //
            ImmutableMap.of( ResourceType.FUEL, Terminal.Color.RED, ResourceType.METALS, Terminal.Color.WHITE, ResourceType.SILICON,
                             Terminal.Color.YELLOW, ResourceType.RARE_ELEMENTS, Terminal.Color.MAGENTA );

    @Nonnull
    private com.lyndir.lanterna.view.Coordinate offset = new com.lyndir.lanterna.view.Coordinate( 0, 0 );
    private LevelType      levelType;
    private Terminal.Color mapColor;
    private String         backgroundPattern;

    public MapView(@Nonnull final LevelType levelType) {
        this.levelType = levelType;
    }

    @Override
    protected void drawForeground(final Screen screen) {
        super.drawForeground( screen );

        Optional<GameController> gameController = OmicronCLI.get().getGameController();
        if (!gameController.isPresent())
            return;

        // Create an empty grid.
        Size levelSize = gameController.get().getGame().getLevel( getLevelType() ).getSize();
        Table<Integer, Integer, Tile> grid = HashBasedTable.create( levelSize.getHeight(), levelSize.getWidth() );

        // Iterate observable tiles and populate the grid.
        for (final Tile tile : OmicronCLI.get().getLocalPlayer().listObservableTiles( OmicronCLI.get().getLocalPlayer() )) {
            int v = tile.getPosition().getV();
            int u = (tile.getPosition().getU() + v / 2) % levelSize.getWidth();
            grid.put( v, u, tile );
        }

        // Draw grid in view.
        Box contentBox = getContentBoxOnScreen();
        com.lyndir.lanterna.view.Size contentSize = contentBox.getSize();
        for (int x = contentBox.getLeft(); x <= contentBox.getRight(); ++x)
            for (int y = contentBox.getTop(); y <= contentBox.getBottom(); ++y) {
                int v = y - contentBox.getTop() + getOffset().getY();
                int u = x - contentBox.getLeft() + getOffset().getX();
                if (!levelSize.isInBounds( new com.lyndir.omicron.api.model.Coordinate( u, v, levelSize ) ))
                    continue;

                Optional<GameObject> contents = Optional.absent();
                Terminal.Color bgColor = getBackgroundColor();

                Tile tile = grid.get( v, u );
                if (tile != null) {
                    contents = tile.getContents();
                    bgColor = levelTypeColors.get( tile.getLevel().getType() );

                    for (final ResourceType resourceType : ResourceType.values())
                        if (tile.getResourceQuantity( resourceType ) > 0)
                            bgColor = resourceTypeColors.get( resourceType );
                }

                screen.putString( x + (y % 2 == 0? 0: 1), y, contents.isPresent()? contents.get().getTypeName().substring( 0, 1 ): " ",
                                  getMapColor(), bgColor, ScreenCharacterStyle.Bold );
            }

        // Draw off-screen warning labels.
        Inset offScreen = new Inset( Math.max( 0, getOffset().getY() ),
                                     Math.max( 0, levelSize.getWidth() - contentSize.getWidth() - getOffset().getX() + 1 ),
                                     Math.max( 0, levelSize.getHeight() - contentSize.getHeight() - getOffset().getY() - 1 ),
                                     Math.max( 0, getOffset().getX() ) );
        int centerX = contentBox.getLeft() + (levelSize.getWidth() - offScreen.getHorizontal()) / 2 - getOffset().getX() + offScreen.getLeft();
        int centerY = contentBox.getTop() + (levelSize.getHeight() - offScreen.getVertical()) / 2 - getOffset().getY() + offScreen.getTop();
        centerX = Math.min( contentBox.getRight() - 3, Math.max( contentBox.getLeft(), centerX ) );
        centerY = Math.min( contentBox.getBottom() - 1, Math.max( contentBox.getTop() + 1, centerY ) );
        if (offScreen.getTop() > 0)
            screen.putString( centerX, contentBox.getTop(), //
                              String.format( "%+d", offScreen.getTop() ), getInfoTextColor(), getInfoBackgroundColor() );
        if (offScreen.getRight() > 0) {
            String label = String.format( "%+d", offScreen.getRight() );
            screen.putString( contentBox.getRight() - label.length(), centerY, //
                              label, getInfoTextColor(), getInfoBackgroundColor() );
        }
        if (offScreen.getBottom() > 0)
            screen.putString( centerX, contentBox.getBottom(), //
                              String.format( "%+d", offScreen.getBottom() ), getInfoTextColor(), getInfoBackgroundColor() );
        if (offScreen.getLeft() > 0)
            screen.putString( contentBox.getLeft(), centerY, //
                              String.format( "%+d", offScreen.getLeft() ), getInfoTextColor(), getInfoBackgroundColor() );
    }

    @Override
    protected boolean onKey(final Key key) {
        if (key.getKind() == Key.Kind.ArrowUp && key.isCtrlPressed()) {
            setOffset( getOffset().translate( 0, -1 ) );
            return true;
        }
        if (key.getKind() == Key.Kind.ArrowDown && key.isCtrlPressed()) {
            setOffset( getOffset().translate( 0, 1 ) );
            return true;
        }
        if (key.getKind() == Key.Kind.ArrowLeft && key.isCtrlPressed()) {
            setOffset( getOffset().translate( -1, 0 ) );
            return true;
        }
        if (key.getKind() == Key.Kind.ArrowRight && key.isCtrlPressed()) {
            setOffset( getOffset().translate( 1, 0 ) );
            return true;
        }
        if (key.getKind() == Key.Kind.Home && key.isCtrlPressed()) {
            setOffset( new com.lyndir.lanterna.view.Coordinate() );
            return true;
        }

        return false;
    }

    @Override
    public String getBackgroundPattern() {
        return ifNotNullElse( backgroundPattern, getTheme().mapBgPattern() );
    }

    @Override
    public void setBackgroundPattern(final String backgroundPattern) {
        this.backgroundPattern = backgroundPattern;
    }

    public Terminal.Color getMapColor() {
        return ifNotNullElse( mapColor, getTheme().mapFg() );
    }

    public void setMapColor(final Terminal.Color mapColor) {
        this.mapColor = mapColor;
    }

    public LevelType getLevelType() {
        return levelType;
    }

    public void setLevelType(final LevelType levelType) {
        this.levelType = levelType;
    }

    @Nonnull
    public com.lyndir.lanterna.view.Coordinate getOffset() {
        return offset;
    }

    public void setOffset(@Nonnull final com.lyndir.lanterna.view.Coordinate offset) {
        this.offset = offset;
    }
}
