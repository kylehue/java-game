package scenes.game;

import colliders.Collider;
import colliders.ColliderWorld;
import entity.Bullet;
import entity.Entity;
import entity.Zombie;
import entity.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import main.GameApplication;
import map.CityMap;
import map.Map;
import utils.Camera;
import utils.GameUtils;
import utils.Quadtree;

import java.util.ArrayList;
import java.util.HashMap;

public class World {
    private final GameApplication gameApplication;
    private final ArrayList<Zombie> zombies = new ArrayList<>();
    private final ArrayList<Bullet> bullets = new ArrayList<>();
    private final Camera camera;
    private final Map map = new CityMap();
    private final Quadtree<Collider> quadtree;
    private final ColliderWorld colliderWorld = new ColliderWorld();
    private Player player;
    
    /* For debugging */
    public static final HashMap<String, DebugRenderCallback> debugRender = new HashMap<>();
    public interface DebugRenderCallback {
        void call(GraphicsContext ctx);
    }
    
    public World(GameApplication gameApplication) {
        this.gameApplication = gameApplication;
        float quadtreeBoundsOffset = 100; // for map bounds colliders
        this.quadtree = new Quadtree<>(
            new Quadtree.Bounds(
                -map.getTotalWidth() / 2 - map.getTileWidth() / 2 - quadtreeBoundsOffset,
                -map.getTotalHeight() / 2 - map.getTileHeight() / 2 - quadtreeBoundsOffset,
                map.getTotalWidth() + quadtreeBoundsOffset * 2,
                map.getTotalHeight() + quadtreeBoundsOffset * 2
            ),
            12,
            15
        );
        this.colliderWorld.setQuadtree(this.quadtree);
        this.camera = new Camera(gameApplication.getGameScene().getGraphicsContext());
        this.map.setRenderTileViewportOffset(2, 2);
        this.map.initializeColliders(this.colliderWorld);
    }
    
    public void setup() {
        this.player = new Player(gameApplication);
        for (int i = 0; i < 400; i++) {
            Zombie enemy = new Zombie(gameApplication);
            enemy.getCollider().getPosition().set(
                GameUtils.random(-map.getTotalWidth() / 2, map.getTotalWidth() / 2),
                GameUtils.random(-map.getTotalHeight() / 2, map.getTotalHeight() / 2)
            );
            zombies.add(enemy);
        }
    }
    
    public void render(GraphicsContext ctx) {
        float renderDistanceOffset = 50;
        
        this.camera.begin();
        map.render(ctx);
        
        // render entities according to y position
        ArrayList<Entity> entities = new ArrayList<>();
        entities.add(player);
        entities.addAll(zombies);
        
        // exclude entities that are not in viewport
        for (int i = entities.size() - 1; i >= 0; i--) {
            Entity entity = entities.get(i);
            boolean isInViewport = this.camera.isInViewport(
                entity.getPosition(),
                renderDistanceOffset
            );
            if (!isInViewport) {
                entities.remove(i);
            }
        }
        
        // TODO: project requirements application: apply insertion sort
        entities.sort((a, b) -> {
            float ay = a.getPosition().getY();
            float by = b.getPosition().getY();
            if (ay < by) return -1;
            else if (ay > by) return 1;
            return 0;
        });
        
        for (Entity entity : entities) {
            entity.render(ctx);
        }
        
        for (Bullet bullet : bullets) {
            boolean isInViewport = this.camera.isInViewport(
                bullet.getPosition(),
                renderDistanceOffset
            );
            if (isInViewport) {
                bullet.render(ctx);
            }
        }
        
        // this.renderMeta(ctx);
        
        if (!debugRender.isEmpty()) {
            debugRender.forEach((key, run) -> {
                run.call(ctx);
            });
            debugRender.clear();
        }
        
        this.camera.end();
        
        this.renderFPS(ctx);
    }
    
    private void renderFPS(GraphicsContext ctx) {
        ctx.beginPath();
        ctx.setFill(Paint.valueOf("#00FF00"));
        ctx.setFont(Font.font(null, FontWeight.BOLD, 24));
        ctx.fillText(
            String.valueOf(gameApplication.getGameScene().getGameLoop().getFPS()), 15,
            30,
            100
        );
        ctx.closePath();
    }
    
    public void renderMeta(GraphicsContext ctx) {
        this.quadtree.render(ctx);
        for (Collider collider : colliderWorld.getColliders()) {
            collider.render(ctx);
        }
    }
    
    public void update(float deltaTime) {
        this.quadtree.clear();
        this.handleBulletDisposal();
        this.map.putCollidersInQuadtree(this.quadtree);
        
        player.update(deltaTime);
        
        for (Zombie zombie : zombies) {
            zombie.update(deltaTime);
        }
        
        for (Bullet bullet : bullets) {
            bullet.update(deltaTime);
        }
        
        this.camera.moveTo(player.getPosition());
        this.camera.zoomTo(400);
        colliderWorld.update(deltaTime);
        
        this.map.setViewport(
            camera.getViewport().getTop(),
            camera.getViewport().getBottom(),
            camera.getViewport().getLeft(),
            camera.getViewport().getRight()
        );
    }
    
    public Quadtree<Collider> getQuadtree() {
        return quadtree;
    }
    
    public ColliderWorld getColliderWorld() {
        return colliderWorld;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public ArrayList<Bullet> getBullets() {
        return bullets;
    }
    
    public Bullet spawnBullet(float x, float y, float angle) {
        Bullet bullet = new Bullet(this.gameApplication, x, y, angle);
        this.bullets.add(bullet);
        return bullet;
    }
    
    private void handleBulletDisposal() {
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            // dispose bullets when reached max distance
            float distance = bullet.getPosition().getDistanceFrom(bullet.getInitialPosition());
            if (distance > bullet.getMaxDistance()) {
                bullets.remove(i);
            }
        }
    }
    
    public Camera getCamera() {
        return camera;
    }
}
