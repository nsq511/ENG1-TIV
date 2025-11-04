package ENG1.teamIV;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Combines a Sprite and a Rectangle so translation operations act on both 
 */
public class Entity {
    private Sprite sprite;
    private Rectangle rectangle;
    private float speed;
    private Vector2 oldPos;     // Position of Entity in previous frame
    public boolean visible;     // Whether the sprite should be rendered
    public boolean collidable;  // Whether the rectangle should collide
    private Vector2 startPos;   // The position the entity was instantiated at

    /**
     * Create a transparent square entity with matching Sprite and Rectangle
     */
    public Entity(){
        this(new Texture(AppConstants.TRANSPARENT_TEX), 1, new Vector2());
    }
    /**
     * Create a square entity with a matching Sprite and Rectangle
     * 
     * @param spriteTexture The filepath to the texture for the Sprite
     * @param size The width and height of the Sprite and Rectangle
     * @param pos The world position of the entity
     */
    public Entity(String spriteTexture, float size, Vector2 pos){
        this(new Texture(spriteTexture), size, size, size, size, pos);
    }
    /**
     * Create a square entity with a matching Sprite and Rectangle
     * 
     * @param spriteTexture The Texture for the Sprite
     * @param size The width and height of the Sprite and Rectangle
     * @param pos The world position of the entity
     */
    public Entity(Texture spriteTexture, float size, Vector2 pos){
        this(spriteTexture, size, size, size, size, pos);
    }
    /**
     * Create an entity with differing Sprite and Rectangle sizes
     * 
     * @param spriteTexture The filepath to the texture for the Sprite
     * @param spriteWidth The width of the Sprite
     * @param spriteHeight The heigh of the Sprite
     * @param rectWidth The width of the Rectangle
     * @param rectHeight The height of the Rectangle
     * @param pos The world position of the entity
     */
    public Entity(String spriteTexture, float spriteWidth, float spriteHeight, float rectWidth, float rectHeight, Vector2 pos){
        this(new Texture(spriteTexture), spriteWidth, spriteHeight, rectWidth, rectHeight, pos);
    }
    /**
     * Create an entity with differing Sprite and Rectangle sizes
     * 
     * @param spriteTexture The filepath to the texture for the Sprite
     * @param spriteWidth The width of the Sprite
     * @param spriteHeight The heigh of the Sprite
     * @param rectWidth The width of the Rectangle
     * @param rectHeight The height of the Rectangle
     * @param pos The world position of the entity
     */
    public Entity(Texture spriteTexture, float spriteWidth, float spriteHeight, float rectWidth, float rectHeight, Vector2 pos){
        sprite = new Sprite(spriteTexture);
        sprite.setSize(spriteWidth, spriteHeight);

        rectangle = new Rectangle();
        rectangle.setSize(rectWidth, rectHeight);

        speed = 0f;
        visible = true;
        collidable = false;

        startPos = pos.cpy();
        oldPos = pos.cpy();
        setPos(pos);
    }

    /**
     * Sets the position of the Entity.
     * Position refers to the sprite and not the rectangle.
     * The sprite and rectangle centres will be algined.
     * 
     * @param pos The world position to set the entity to
     */
    public void setPos(Vector2 pos){
        setPos(pos.x, pos.y);
    }
    /**
     * Sets the position of the Entity.
     * Position refers to the sprite and not the rectangle.
     * The sprite and rectangle centres will be algined.
     * 
     * @param x The world X co-ordinate to set the entity to
     * @param y The world Y co-ordinate to set the entity to
     */
    public void setPos(float x, float y){
        sprite.setPosition(x, y);

        // As the rectangle could be a different size to the sprite, we must account for the offset in order to keep the centres aligned
        float Xoffset = (sprite.getWidth() - rectangle.getWidth()) / 2f;
        float Yoffset = (sprite.getHeight() - rectangle.getHeight()) / 2f;
        rectangle.setPosition(x + Xoffset, y + Yoffset);
    }

    /**
     * Updates the previous position of the Entity.
     * Should be called once every frame.
     */
    public void updateOldPos(){
        oldPos = getPos();
    }

    /**
     * Gets the displacement vector from the previous frame
     * 
     * @return The displacement vector
     */
    public Vector2 displacement(){
        return getPos().sub(oldPos);
    }

    /**
     * Get the position of the Entity.
     * Position refers to the sprite and not the rectangle.
     * 
     * @return The position of the Entities anchor
     */
    public Vector2 getPos(){
        return new Vector2(sprite.getX(), sprite.getY());
    }

    /**
     * Draws the sprite
     * 
     * @param batch The SpriteBatch to draw to
     */
    public void draw(Batch batch){
        if(visible) sprite.draw(batch);
    }

    /**
     * Checks whether entities are colliding
     * 
     * @param other The other entity in the collisison
     * @return Whether the entities overlap
     */
    public boolean overlaps(Entity other){
        return rectangle.overlaps(other.rectangle);
    }

    /**
     * Finds the Minimum Translation Vector to separate two overlapping entities.
     * Used in collision resolution.
     * 
     * @param other The other entity in the collision
     * @return The Minimum Translation Vector that will push the this entity out. null if no collision
     */
    private Vector2 getMTV(Entity other){
        if(!collidable || !other.collidable || !rectangle.overlaps(other.rectangle)) return null;

        // Subtract the rightmost left-edge of the rectangles from the leftmost right-edge of the rectangles
        // This gives the size of overlap
        float overlapX = Math.min(rectangle.x + rectangle.width, other.rectangle.x + other.rectangle.width) - Math.max(rectangle.x, other.rectangle.x);
        // Similar for Y axis
        float overlapY = Math.min(rectangle.y + rectangle.height, other.rectangle.y + other.rectangle.height) - Math.max(rectangle.y, other.rectangle.y);

        // Move in the direction of minumum overlap
        if(overlapX < overlapY){
            // Push along the X axis

            // If this entity's centre is to the right of the other entity's centre then move to the right
            if(rectangle.x + rectangle.width / 2f > other.rectangle.x + other.rectangle.width / 2f){
                return new Vector2(overlapX, 0);
            }
            else{
                return new Vector2(-overlapX, 0);
            }
        }
        else{
            // Push along the Y axis

            // If this entity's centre is above the other entity's centre then move to the up
            if(rectangle.y + rectangle.height / 2f > other.rectangle.y + other.rectangle.height / 2f){
                return new Vector2(0, overlapY);
            }
            else{
                return new Vector2(0, -overlapY);
            }
        }
    }

    /**
     * Resolves a collision between entity and other by moving this entity out of the overlap
     * using the MTV.
     * 
     * @param other The other entity in the collision
     * @return The new position of the entity
     */
    public Vector2 collide(Entity other){
        Vector2 mtv = getMTV(other);
        if(mtv == null) return getPos();     // No collision

        return getPos().add(mtv);
    }

    public float getSpeed(){
        return speed;
    }

    public void setSpeed(float newSpeed){
        if (newSpeed < 0){
            throw new IllegalArgumentException("Speed must not be negative");
        }
        speed = newSpeed;
    }

    public float getWidth(){
        return sprite.getWidth();
    }

    public float getHeight(){
        return sprite.getHeight();
    }

    /**
     * Sets the Entity's current position as the position it will move back to on reset.
     * 
     * Used for when the entity should return to a position other than the position it was 
     * instantiated at on reset, such as being adjusted to sit in the centre of a cell
     */
    public void setStartPos(){
        startPos = getPos();
    }

    public void reset(){
        // Set the entity to its original position
        setPos(startPos);
        collidable = false;
        visible = true;
    }
}
