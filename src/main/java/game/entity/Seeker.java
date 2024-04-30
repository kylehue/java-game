package game.entity;

import game.Game;
import game.colliders.Collider;
import game.colliders.CollisionResolvers;
import game.map.Layer;
import game.map.Material;
import game.map.PathFinder;
import game.utils.IntervalMap;
import game.utils.Vector;
import utils.Async;

import java.util.ArrayList;

public abstract class Seeker extends Entity {
    private ArrayList<Vector> pathToSeek = new ArrayList<>();
    private float angleToPathToSeek = 0;
    private final Vector positionToSeek = new Vector();
    private boolean isPathClear = false;
    private final IntervalMap intervalMap = new IntervalMap();
    private boolean isFacingOnLeftSide = false;
    
    private enum Interval {
        UPDATE_PATH,
        UPDATE_IS_PATH_CLEAR
    }
    
    public Seeker() {
        intervalMap.registerIntervalFor(
            Interval.UPDATE_PATH,
            150
        );
        intervalMap.registerIntervalFor(
            Interval.UPDATE_IS_PATH_CLEAR,
            100
        );
    }
    
    /**
     * Checks if the straight line from this to player
     * has no obstacles.
     */
    private boolean _isPathClear() {
        for (Layer layer : Game.world.getMap().getLayers()) {
            for (Material material : layer.getMaterials()) {
                Collider obstacle = material.getCollider();
                if (obstacle == null) continue;
                
                Vector intersectionPoint = CollisionResolvers.getLineToColliderIntersectionPoint(
                    position,
                    positionToSeek,
                    obstacle
                );
                
                if (intersectionPoint != null) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    protected boolean isPathClear() {
        return isPathClear;
    }
    
    protected abstract void handleSeek(float angle);
    
    protected void seek(Vector positionToSeek) {
        this.positionToSeek.set(positionToSeek);
        
        this.maybeUpdateIfPathIsClear();
        this.maybeUpdatePathToSeek();
        this.maybeUpdateAngleToPathToSeek();
        
        // Use straightforward angle if path is clear
        if (isPathClear) {
            isFacingOnLeftSide = Math.abs(angleToPathToSeek) > (Math.PI / 2);
            handleSeek(angleToPathToSeek);
        }
        
        // Use pathfinder if path has obstacles
        if (!isPathClear && pathToSeek.size() > 1) {
            Vector step = pathToSeek.get(Math.max(0, pathToSeek.size() - 2));
            float angle = position.getAngle(step);
            isFacingOnLeftSide = Math.abs(angle) > (Math.PI / 2);
            handleSeek(angle);
        }
    }
    
    protected boolean isFacingOnLeftSide() {
        return isFacingOnLeftSide;
    }
    
    private void maybeUpdateAngleToPathToSeek() {
        if (!isPathClear) return;
        angleToPathToSeek = position.getAngle(positionToSeek);
    }
    
    private void maybeUpdateIfPathIsClear() {
        if (intervalMap.isIntervalOverFor(Interval.UPDATE_IS_PATH_CLEAR)) {
            Async.queue1.submit(() -> {
                this.isPathClear = this._isPathClear();
            });
            
            intervalMap.resetIntervalFor(Interval.UPDATE_IS_PATH_CLEAR);
        }
    }
    
    private void maybeUpdatePathToSeek() {
        if (isPathClear) return;
        
        if (intervalMap.isIntervalOverFor(Interval.UPDATE_PATH)) {
            Async.queue1.submit(() -> {
                PathFinder pathFinder = Game.world.getPathFinder();
                pathToSeek = pathFinder.requestPath(
                    position,
                    positionToSeek
                );
            });
            
            intervalMap.resetIntervalFor(Interval.UPDATE_PATH);
        }
    }
}
