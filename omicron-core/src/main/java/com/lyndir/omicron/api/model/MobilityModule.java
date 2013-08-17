package com.lyndir.omicron.api.model;

import static com.lyndir.lhunath.opal.system.util.ObjectUtils.*;
import static com.lyndir.omicron.api.util.PathUtils.*;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.lyndir.lhunath.opal.system.util.*;
import com.lyndir.omicron.api.*;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nonnull;


public class MobilityModule extends Module {

    private final int movementSpeed;
    private final Map<LevelType, Double> movementCost = new EnumMap<>( LevelType.class );
    private final Map<LevelType, Double> levelingCost = new EnumMap<>( LevelType.class );

    private double remainingSpeed;

    protected MobilityModule(final ImmutableResourceCost resourceCost, final int movementSpeed, final Map<LevelType, Double> movementCost,
                             final Map<LevelType, Double> levelingCost) {
        super( resourceCost );

        this.movementSpeed = movementSpeed;
        this.movementCost.putAll( movementCost );
        this.levelingCost.putAll( levelingCost );
    }

    static Builder0 createWithStandardResourceCost() {
        return createWithExtraResourceCost( ResourceCost.immutable() );
    }

    static Builder0 createWithExtraResourceCost(final ImmutableResourceCost resourceCost) {
        return new Builder0( ModuleType.MOBILITY.getStandardCost().add( resourceCost ) );
    }

    public double getRemainingSpeed() {
        return remainingSpeed;
    }

    /**
     * Get the speed cost related to moving around in the given level.
     *
     * @param levelType The level to move around in.
     *
     * @return The speed cost.
     */
    public double costForMovingInLevel(final LevelType levelType) {
        return ifNotNullElse( movementCost.get( levelType ), Double.MAX_VALUE );
    }

    /**
     * Get the speed cost related to leveling from the current level type to the given level type.
     *
     * @param levelType The level to transition to.
     *
     * @return The speed cost.
     */
    public double costForLevelingToLevel(final LevelType levelType) {
        // Level up until we reach the target level.
        double cost = 0;
        LevelType currentLevel = getGameObject().getLocation().getLevel().getType();
        if (levelType == currentLevel)
            return 0;
        do {
            Optional<LevelType> newLevel = currentLevel.up();
            if (!newLevel.isPresent())
                break;

            currentLevel = newLevel.get();

            Double currentLevelCost = levelingCost.get( currentLevel );
            if (currentLevelCost == null)
                // Cannot level to this level.
                return Double.MAX_VALUE;

            cost += currentLevelCost;

            if (currentLevel == levelType)
                return cost;
        }
        while (true);

        // Level down until we reach the target level.
        cost = 0;
        currentLevel = getGameObject().getLocation().getLevel().getType();
        do {
            Optional<LevelType> newLevel = currentLevel.down();
            if (!newLevel.isPresent())
                break;

            currentLevel = newLevel.get();

            Double currentLevelCost = levelingCost.get( currentLevel );
            if (currentLevelCost == null)
                // Cannot level to this level.
                return Double.MAX_VALUE;

            cost += currentLevelCost;

            if (currentLevel == levelType)
                return cost;
        }
        while (true);

        // Unreachable code.
        throw new IllegalArgumentException( "Unsupported level type: " + levelType );
    }

    /**
     * Move the unit to the given level.
     *
     * @param levelType The side of the adjacent tile relative to the current.
     */
    @Authenticated
    public Leveling leveling(final LevelType levelType) {
        if (!getGameObject().isOwnedByCurrentPlayer())
            // Cannot level object that doesn't belong to the current player.
            return new Leveling( 0 );

        Tile currentLocation = getGameObject().getLocation();
        if (levelType == currentLocation.getLevel().getType())
            // Already in the destination level.
            return new Leveling( 0, currentLocation );

        double cost = costForLevelingToLevel( levelType );
        if (cost > remainingSpeed)
            // Cannot move: insufficient speed remaining this turn.
            return new Leveling( cost );

        return new Leveling( cost, getGameObject().getGame().getLevel( levelType ).getTile( currentLocation.getPosition() ).get() );
    }

    /**
     * Move the unit to an adjacent tile.
     *
     * @param target The side of the adjacent tile relative to the current.
     */
    @Authenticated
    public Movement movement(final Tile target) {
        if (!getGameObject().isOwnedByCurrentPlayer())
            // Cannot move object that doesn't belong to the current player.
            return new Movement( 0 );

        Leveling leveling = leveling( target.getLevel().getType() );
        if (!leveling.isPossible())
            // Cannot move because we can't level to the target's level.
            return new Movement( leveling.getCost() );

        // Initialize cost calculation.
        Tile currentLocation = leveling.getTarget();
        final double stepCost = costForMovingInLevel( currentLocation.getLevel().getType() );

        // Initialize path finding data functions.
        PredicateNN<Tile> foundFunction = new PredicateNN<Tile>() {
            @Override
            public boolean apply(@Nonnull final Tile input) {
                return ObjectUtils.isEqual( input, target );
            }
        };
        NNFunctionNN<Step<Tile>, Double> costFunction = new NNFunctionNN<Step<Tile>, Double>() {
            @Nonnull
            @Override
            public Double apply(@Nonnull final Step<Tile> input) {
                if (!input.getTo().checkAccessible())
                    return Double.MAX_VALUE;

                return stepCost;
            }
        };
        NNFunctionNN<Tile, Iterable<Tile>> neighboursFunction = new NNFunctionNN<Tile, Iterable<Tile>>() {
            @Nonnull
            @Override
            public Iterable<Tile> apply(@Nonnull final Tile input) {
                return input.neighbours();
            }
        };

        // Find the path!
        Optional<Path<Tile>> path = find( currentLocation, foundFunction, costFunction, remainingSpeed, neighboursFunction );
        return new Movement( leveling.getCost() + (path.isPresent()? path.get().getCost(): 0), leveling, path );
    }

    @Override
    protected void onReset() {
        remainingSpeed = movementSpeed;
    }

    @Override
    protected void onNewTurn() {
    }

    @Override
    public ModuleType<?> getType() {
        return ModuleType.MOBILITY;
    }

    public class Leveling {

        private final double         cost;
        private final Optional<Tile> target;

        Leveling(final double cost) {
            this.cost = cost;
            target = Optional.absent();
        }

        Leveling(final double cost, final Tile target) {
            this.cost = cost;
            this.target = Optional.of( target );
        }

        public boolean isPossible() {
            return target.isPresent();
        }

        /**
         * The cost for executing the leveling.  If not possible, the cost is either zero if unknown or the cost for the action that
         * exceeded the module's remaining speed (not the cost of getting to the target).
         *
         * @return An amount of speed.
         */
        public double getCost() {
            return cost;
        }

        /**
         * @return The target tile after leveling.
         *
         * @throws IllegalStateException if the leveling is not possible ({@link #isPossible()} returns {@code false})
         */
        Tile getTarget() {
            return target.get();
        }

        @Authenticated
        public void execute() {
            Preconditions.checkState( isPossible(), "Cannot execute: it is not possible." );
            Preconditions.checkState( cost <= remainingSpeed, "Cannot execute: not enough remaining speed." );
            // TODO: No target.isAccessible check: Most units that level cannot see other levels before they go there.
            // TODO: Should we disallow leveling until you can see the level above you like we do with movement and the tile you move to?
            Change.From<Tile> locationChange = Change.from( getGameObject().getLocation() );
            ChangeDbl.From remainingSpeedChange = ChangeDbl.from( remainingSpeed );

            // Execute the leveling.
            getGameObject().getController().setLocation( target.get() );
            remainingSpeed -= cost;

            getGameObject().getGame()
                    .getController()
                    .fireFor( new PredicateNN<Player>() {
                        @Override
                        public boolean apply(@Nonnull final Player input) {
                            return input.canObserve( getGameObject().getLocation() );
                        }
                    } )
                    .onMobilityLeveled( MobilityModule.this, locationChange.to( getGameObject().getLocation() ),
                                        remainingSpeedChange.to( remainingSpeed ) );
        }
    }


    public class Movement {

        private final double               cost;
        private final Leveling             leveling;
        private final Optional<Path<Tile>> path;

        Movement(final double cost) {
            this.cost = cost;
            leveling = null;
            path = Optional.absent();
        }

        Movement(final double cost, @Nonnull final Leveling leveling, final Optional<Path<Tile>> path) {
            this.cost = cost;
            this.leveling = leveling;
            this.path = path;
        }

        /**
         * The cost for executing the movement.  If not possible, the cost is either zero if unknown or the cost for the action that
         * exceeded the module's remaining speed (not the cost of getting to the target).
         *
         * @return An amount of speed.
         */
        public double getCost() {
            return cost;
        }

        /**
         * @return The target tile after leveling.
         *
         * @throws IllegalStateException if the leveling is not possible ({@link #isPossible()} returns {@code false})
         */
        Path<Tile> getPath() {
            return path.get();
        }

        public boolean isPossible() {
            return path.isPresent();
        }

        @Authenticated
        public void execute() {
            Preconditions.checkState( isPossible(), "Cannot execute: it is not possible." );
            Preconditions.checkState( cost <= remainingSpeed, "Cannot execute: not enough remaining speed." );
            Change.From<Tile> locationChange = Change.from( getGameObject().getLocation() );
            ChangeDbl.From remainingSpeedChange = ChangeDbl.from( remainingSpeed );

            // Check that the path can still be walked.
            Path<Tile> tracePath = path.get();
            do {
                Preconditions.checkState( tracePath.getTarget().checkAccessible() || //
                                          ObjectUtils.isEqual( tracePath.getTarget(), getGameObject().getLocation() ),
                                          "Cannot execute: path no longer accessible." );

                Optional<Path<Tile>> parent = tracePath.getParent();
                if (!parent.isPresent())
                    break;

                tracePath = parent.get();
            }
            while (true);

            // Execute the leveling.
            leveling.execute();

            // Execute the path.
            getGameObject().getController().setLocation( path.get().getTarget() );
            remainingSpeed -= path.get().getCost();

            getGameObject().getGame()
                    .getController()
                    .fireFor( new PredicateNN<Player>() {
                        @Override
                        public boolean apply(@Nonnull final Player input) {
                            return input.canObserve( getGameObject().getLocation() );
                        }
                    } )
                    .onMobilityMoved( MobilityModule.this, locationChange.to( getGameObject().getLocation() ),
                                      remainingSpeedChange.to( remainingSpeed ) );
        }
    }


    @SuppressWarnings({ "ParameterHidesMemberVariable", "InnerClassFieldHidesOuterClassField" })
    static class Builder0 {

        private final ImmutableResourceCost resourceCost;

        private Builder0(final ImmutableResourceCost resourceCost) {
            this.resourceCost = resourceCost;
        }

        Builder1 movementSpeed(final int movementSpeed) {
            return new Builder1( movementSpeed );
        }

        class Builder1 {

            private final int movementSpeed;

            private Builder1(final int movementSpeed) {
                this.movementSpeed = movementSpeed;
            }

            Builder2 movementCost(final Map<LevelType, Double> movementCost) {
                return new Builder2( movementCost );
            }

            class Builder2 {

                private final Map<LevelType, Double> movementCost;

                private Builder2(final Map<LevelType, Double> movementCost) {
                    this.movementCost = movementCost;
                }

                MobilityModule levelingCost(final Map<LevelType, Double> levelingCost) {
                    return new MobilityModule( resourceCost, movementSpeed, movementCost, levelingCost );
                }
            }
        }
    }
}
