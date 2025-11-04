package ENG1.teamIV;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/*
 * This file was part of the original LibGDX source code and has been modified by the ENG1.teamIV team
 */

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    FitViewport viewport;
    SpriteBatch spriteBatch;

    Texture backgroundTexture;
    Texture menuBgTexture;
    Texture controlsTexture;
    Texture notesTexture;

    Array<Entity> wallEntities;
    
    BitmapFont XsmallFont;
    BitmapFont smallFont;
    BitmapFont mediumFont;
    BitmapFont largeFont;
    Timer timer;
    Music music;
    Sound dropSound;
    Sound notifSound;
    Sound successSound;

    int score;

    // This string is displayed in the menu and is used to relay information to the user
    // It should be kept short in order to fit in the space allocated
    // Updating this will overwrite the previous message
    // Any area of code, i.e. an event, can write to this variable to display a message
    // E.g. If the player tries to open a locked door, this message could be set to "Pick up the key to open the door!"
    String menuMsg;

    // Player
    Entity playerEntity;

    // Events
    Array<Event> events;
    ObjectMap<String, Entity> eventEntities;    // Entities related to events should be added and removed as required
    Stage eventOverlay;        // Used for rendering event graphics

    // Game states
    boolean freeze;     // Whether to freeze gameplay (for pause and game end)
    boolean gameEnd;    // Whether the game is finished
    boolean win;        // Whether the game end is due to a win or loss
    boolean paused;     // Whether the game is paused
    // freeze will simply stop gameplay, while pause will draw the pause screen. Both should be true when paused

    @Override
    public void resize(int width, int height){
        viewport.update(width, height, true);
    }

    @Override
    public void create(){
        // Set up game states to start paused
        paused = true;
        freeze = true;
        gameEnd = false;
        win = false;

        // Basics setup
        viewport = new FitViewport(AppConstants.worldWidth, AppConstants.worldHeight);
        spriteBatch = new SpriteBatch();
        eventOverlay = new Stage(viewport);
        Gdx.input.setInputProcessor(eventOverlay);

        score = 0;
        
        // Fonts
        XsmallFont = new BitmapFont();
        XsmallFont.getData().setScale(0.4f);
        smallFont = new BitmapFont();
        smallFont.getData().setScale(0.7f);
        mediumFont = new BitmapFont();
        mediumFont.getData().setScale(2f);
        largeFont = new BitmapFont();
        largeFont.getData().setScale(7f);

        backgroundTexture = new Texture(AppConstants.BACKGROUND_TEX);
        menuBgTexture = new Texture(AppConstants.MENU_BG_TEX);
        controlsTexture = new Texture(AppConstants.CONTROLS_TEX);
        notesTexture = new Texture(AppConstants.NOTES_TEX);

        menuMsg = "";

        // Player setup
        Vector2 playerStartPos = AppConstants.playerStartPos;
        playerEntity = new Entity(AppConstants.PLAYER_TEX, 0.7f * AppConstants.cellSize, playerStartPos);
        playerEntity.setSpeed(AppConstants.playerSpeedDefault);
        playerEntity.collidable = true;

        // Map setup
        try{
            wallEntities = Utilities.loadMap(AppConstants.MAP_FP, AppConstants.MAP_TEXTURES_FP);
        }
        catch(Exception e){
            System.err.println("Exception while loading map: " + e);
            wallEntities = new Array<>();
        }
        
        timer = new Timer(AppConstants.TIMER_LIMIT_DEFAULT, AppConstants.TIMER_STEP_DEFAULT);
        music = Gdx.audio.newMusic(Gdx.files.internal(AppConstants.MUSIC_FP));
        music.setLooping(true);
        music.setVolume(0.1f);
        music.play();
        dropSound = Gdx.audio.newSound(Gdx.files.internal(AppConstants.DROP_SOUND_FP));
        notifSound = Gdx.audio.newSound(Gdx.files.internal(AppConstants.NOTIF_SOUND_FP));
        successSound = Gdx.audio.newSound(Gdx.files.internal(AppConstants.SUCCESS_SOUND_FP));

        // Events setup
        eventEntities = new ObjectMap<>();
        events = new Array<>();

        // Define events here
        
        // 0. Game Win
        // Doesnt count as an "event" as defined in the brief but the Event system is used to detect a win condition
        Vector2 endPos = new Vector2(AppConstants.mapWidth - AppConstants.cellSize, AppConstants.mapHeight - AppConstants.cellSize);

        Event gameWin0 = new Event("gameWin0", new Array<>(), AppConstants.mapWidth, playerStartPos){
            @Override
            void onStart(){
                // Spawn end cell
                Entity endCell = new Entity(AppConstants.END_CELL_TEX, AppConstants.cellSize, endPos);
                eventEntities.put("endCell", endCell);
                menuMsg = "Collect your notes and escape uni!";
            }
        };
        events.add(gameWin0);

        Event gameWin1 = new Event("gameWin1", new Array<>(new Event[]{gameWin0}), 0.3f * AppConstants.cellSize, endPos){
            @Override
            void onStart(){
                menuMsg = "You escaped!";
                // Win game
                freeze = true;
                gameEnd = true;
                win = true;
            }
        };
        Utilities.centreOnCell(gameWin1);
        gameWin1.setStartPos();
        events.add(gameWin1);

        // 1. Key Event
        Vector2 doorPos = new Vector2(380, 370);
        Vector2 keyPos = new Vector2(60, 260);
        
        // Event init
        Event getKey0 = new Event("getKey0", new Array<>(), AppConstants.mapWidth, playerStartPos){
            @Override
            void onStart(){        
                // Create a door to block the path
                Entity door = new Entity(AppConstants.DOOR_TEX, AppConstants.cellSize, doorPos);
                door.collidable = true;
                eventEntities.put("door", door);
            }
        };
        events.add(getKey0);

        // Event trigger
        Event getKey1 = new Event("getKey1", new Array<>(new Event[]{getKey0}), 1.1f * AppConstants.cellSize, doorPos){
            @Override
            void onStart(){
                // Spawn a key
                Entity key = new Entity(AppConstants.KEY_TEX, 0.8f * AppConstants.cellSize, keyPos);
                Utilities.centreOnCell(key);
                key.setStartPos();
                eventEntities.put("key", key);

                dropSound.play();
                menuMsg = "Pick up the key to open the door!";
            }
        };
        Utilities.centreOnCell(getKey1);
        getKey1.setStartPos();
        events.add(getKey1);

        // Pick up the key
        Event getKey2 = new Event("getKey2", new Array<>(new Event[]{getKey1}), 0.7f * AppConstants.cellSize, keyPos){
            @Override
            void onStart(){
                // Despawn the key
                eventEntities.remove("key");
                dropSound.play();
                menuMsg = "Picked up key. Open the door!";
            }
        };
        Utilities.centreOnCell(getKey2);
        getKey2.setStartPos();
        events.add(getKey2);

        // Open the door
        Event getKey3 = new Event("getKey3", new Array<>(new Event[]{getKey2}), 1.1f * AppConstants.cellSize, doorPos){
            @Override
            void onStart(){
                // Despawn the door
                eventEntities.remove("door");
                menuMsg = "Door opened!";
            }

            @Override
            void onFinish(){
                Event.incrementBadEventCounter();
            }
        };
        Utilities.centreOnCell(getKey3);
        getKey3.setStartPos();
        events.add(getKey3);

        // 2. TrioAuthentication Code
        Event trio0 = new Event("trio0", new Array<>(), 10f * AppConstants.cellSize, new Vector2(10f * AppConstants.cellSize, 200)){
            private TextField codeField;
            private Image bgImage;
            private Texture bgTex;
            private TextureRegion region;
            private String trioCode = AppConstants.trioCode;
            private float playerSpeed;
            private Texture notifTex;
            private Image notifImage;
            private Label notifLabel;
            private Skin skin = new Skin(Gdx.files.internal("uiskin.json"));
            private float notifSpeed = AppConstants.playerSpeedDefault * 2f;
            private float notifLabelOffset;
            private boolean notifSoundPlayed = false;

            @Override
            void onStart(){
                // PLayer movement speed may be altered from the default
                // Therefore, we want to store the speed so we can reset it when the event is done
                playerSpeed = playerEntity.getSpeed();
                playerEntity.setSpeed(0f);      // Disable player movement

                // Create popup
                bgTex = new Texture(AppConstants.TRIO_BG_TEX);
                region = new TextureRegion(bgTex);
                bgImage = new Image(region);
                
                // Centre BG
                float width = 150f;
                float height = 200f;
                float x = (AppConstants.mapWidth - width) / 2f;
                float y = (AppConstants.mapHeight - height) / 2f;
                
                bgImage.setPosition(x, y);
                bgImage.setSize(width, height);
                
                // centre text field in popup
                codeField = new TextField("", skin);
                float padding = 10f;
                codeField.setWidth(width - (2f * padding));
                codeField.setPosition(x + padding, y + padding);

                // Create notification
                notifTex = new Texture(AppConstants.TRIO_NOTIF_TEX);
                TextureRegion notifRegion = new TextureRegion(notifTex);
                notifImage = new Image(notifRegion);

                // Set position above map
                height = 50f;
                y = AppConstants.mapHeight + (height * 5f);
                notifImage.setPosition(x, y);
                notifImage.setSize(width, height);

                // Create the text in the notif and set its position to the centre of the notif image
                notifLabel = new Label("Trio: " + trioCode, skin);
                float labelHeight = notifLabel.getHeight();
                notifLabelOffset = (height - labelHeight) / 2f;
                float notifLabelPadding = 2f;
                notifLabel.setPosition(x + notifLabelPadding, y + notifLabelOffset);

                eventOverlay.addActor(bgImage);
                eventOverlay.addActor(codeField);
                eventOverlay.addActor(notifImage);
                eventOverlay.addActor(notifLabel);

                menuMsg = "Wait for your authentication code!";
            }

            @Override
            void onUpdate(){
                // Move the notif down into the screen
                float notifStopPos = AppConstants.mapHeight - notifImage.getHeight();
                if(notifImage.getY() > notifStopPos){
                    // If the notif is still above the screen, move it down
                    float y = notifImage.getY();
                    float displacement = Gdx.graphics.getDeltaTime() * notifSpeed;
                    y = Math.max(y - displacement, notifStopPos);   // Clamp to lowest position
                    notifImage.setY(y);
                    // Move the label down too
                    notifLabel.setY(y + notifLabelOffset);
                }
                else{
                    if(!notifSoundPlayed){
                        notifSound.play();
                        notifSoundPlayed = true;
                        menuMsg = "Enter the authentication code!";
                    }
                }

                // Process code
                String enteredCode = codeField.getText();
                if(Gdx.input.isKeyJustPressed(Keys.ENTER)){
                    if(enteredCode != null && enteredCode.equals(trioCode)){
                        // If the player enters the correct code, then finish the event
                        complete = true;
                        menuMsg = "Code Accepted!";
                    }
                    else{
                        // Otherwise, apply time penalty
                        timer.tick(30);
                        menuMsg = "Wrong code! -30 secs";
                    }
                }
            }

            @Override
            void onFinish(){
                // Remove actors from overlay
                eventOverlay.getRoot().removeActor(bgImage);
                eventOverlay.getRoot().removeActor(codeField);
                eventOverlay.getRoot().removeActor(notifImage);
                eventOverlay.getRoot().removeActor(notifLabel);
                
                // Dispose of assets
                bgTex.dispose();
                notifTex.dispose();

                // Reset player movement
                playerEntity.setSpeed(playerSpeed);

                Event.incrementHiddenEventCounter();
            }

            @Override
            public void reset() {
                super.reset();
                complete = false;
                started = false;

                notifSoundPlayed = false;
            }
        };
        Utilities.centreOnCell(trio0);
        trio0.setStartPos();
        events.add(trio0);

        // 3. Collect notes
        Vector2 note0Pos = new Vector2(47f * AppConstants.cellSize, 20f * AppConstants.cellSize);
        Vector2 note1Pos = new Vector2(15f * AppConstants.cellSize, 14f * AppConstants.cellSize);
        Vector2 note2Pos = new Vector2(40f * AppConstants.cellSize, 30f * AppConstants.cellSize);
        float noteHeight = 0.8f * AppConstants.cellSize;
        float noteWidth = 0.6f * AppConstants.cellSize;
        int pointBonus = 10;

        // Spawn notes
        Event notes0 = new Event("notes0", notesTexture, new Array<>(), noteWidth, noteHeight, note0Pos){
            @Override
            void onStart(){
                visible = false;    // make the note disappear
                score += pointBonus;
                menuMsg = "Note collected. +" + Integer.toString(pointBonus) + " pts!";
                successSound.play();
            }

            @Override
            public void reset(){
                visible = true;
                complete = false;
                started = false;
            }
        };
        Utilities.centreOnCell(notes0);
        notes0.setStartPos();
        events.add(notes0);

        Event notes1 = new Event("notes1", notesTexture, new Array<>(), noteWidth, noteHeight, note1Pos){
            @Override
            void onStart(){
                visible = false;    // make the note disappear
                score += pointBonus;
                menuMsg = "Note collected. +" + Integer.toString(pointBonus) + " pts!";
                successSound.play();
            }

            @Override
            public void reset(){
                visible = true;
                complete = false;
                started = false;
            }
        };
        Utilities.centreOnCell(notes1);
        notes1.setStartPos();
        events.add(notes1);

        Event notes2 = new Event("notes2", notesTexture, new Array<>(), noteWidth, noteHeight, note2Pos){
            @Override
            void onStart(){
                visible = false;            // make the note disappear
                score += pointBonus;
                menuMsg = "Note collected. +" + Integer.toString(pointBonus) + " pts!";
                successSound.play();
            }

            @Override
            public void reset(){
                visible = true;
                complete = false;
                started = false;
            }
        };
        Utilities.centreOnCell(notes2);
        notes2.setStartPos();
        events.add(notes2);

        // Event that spans the whole map that will trigger once all notes are collected. Used to increment event counter only when all notes are collected
        Event notes3 = new Event("notes3", new Array<>(new Event[]{notes0, notes1, notes2}), AppConstants.mapWidth, AppConstants.mapHeight, new Vector2()){
            @Override
            void onFinish(){
                Event.incrementGoodEventCounter();
            }
        };
        events.add(notes3);
    }

    @Override
    public void render(){
        input();
        logic();
        draw();
    }

    private void input(){
        // Some checks must still be done while frozen
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)){
            if(gameEnd){
                // Restart game
                restart();
            }
            else{
                // Toggle pause
                paused = !paused;
                // freeze may be changed due to non-pause events such as a game end
                // So freeze and pause may become out of sync
                // Therefore, freeze must be set to the value of paused rather than simply toggled
                freeze = paused;
            }
        }

        if(freeze) return;      // Anything after this will not run during pause/game end

        Vector2 movementDirection = new Vector2();
        Vector2 playerPos = playerEntity.getPos();
        float delta = Gdx.graphics.getDeltaTime();

        // Get directional movement from arrow keys
        if(Gdx.input.isKeyPressed(Input.Keys.RIGHT)){
            movementDirection.x += 1;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.LEFT)){
            movementDirection.x -= 1;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.UP)){
            movementDirection.y += 1;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.DOWN)){
            movementDirection.y -= 1;
        }

        movementDirection.nor();    // Normalise so diagonal movement is not faster than orthogonal
        playerPos.add(movementDirection.scl(delta * playerEntity.getSpeed()));
        playerEntity.setPos(playerPos);
    }

    private void logic(){
        if(freeze){
            music.pause();
            return;
        }
        else{
            music.play();
        }
        // Anything after this will not run while frozen

        float delta = Gdx.graphics.getDeltaTime();

        // Collisions
        for(Entity wallEntity : wallEntities){
            playerEntity.setPos(playerEntity.collide(wallEntity));
        }
        for(Entity eventEntity : eventEntities.values()){
            playerEntity.setPos(playerEntity.collide(eventEntity));
        }

        Vector2 playerPos = playerEntity.getPos();
        // Clamp the player position to the world borders
        playerPos.x = MathUtils.clamp(playerPos.x, 0, AppConstants.mapWidth - playerEntity.getWidth());
        playerPos.y = MathUtils.clamp(playerPos.y, 0, AppConstants.mapHeight - playerEntity.getHeight());

        // Set player position before event interactions
        playerEntity.setPos(playerPos);

        // Check event triggers
        for(Event e : events){
            if(playerEntity.overlaps(e)) e.tryEvent();
        }
        
        // Make end cell flash
        Entity endCell = eventEntities.get("endCell");
        if(endCell != null){
            float phase = timer.getRealTime() % 2;  // 2 second cycle
            if(phase < 1.25f){
                // cell is visible for 1.25 seconds
                endCell.visible = true;
            }
            else{
                // cell is off for 0.75 seconds. 
                endCell.visible = false;
            }
        }

        timer.tick(delta);
        playerEntity.updateOldPos();   // Player position should not change after this line

        // Check game-over
        if(timer.isFinished()){
            menuMsg = "Times up...";
            freeze = true;
            gameEnd = true;
            win = false;
            music.stop();
        }
    }

    private void draw(){
        ScreenUtils.clear(Color.BLACK);
        viewport.apply();

        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();

        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
        spriteBatch.begin();

        spriteBatch.draw(backgroundTexture, 0, 0, worldWidth, worldHeight);

        playerEntity.draw(spriteBatch);

        // Draw maze walls
        for(Entity wallEntity : wallEntities){
            wallEntity.draw(spriteBatch);
        }

        // Draw events
        for(Event e : events){
            e.draw(spriteBatch);
        }

        // Draw extra entities for events
        for(Entity e : eventEntities.values()){
            e.draw(spriteBatch);
        }

        eventOverlay.act(Gdx.graphics.getDeltaTime());
        eventOverlay.draw();

        // Menu should be on top of anything maze related
        spriteBatch.draw(menuBgTexture, AppConstants.mapWidth, 0, AppConstants.worldWidth - AppConstants.mapWidth, AppConstants.worldHeight);

        // Draw controls texture
        float controlsBuffer = AppConstants.cellSize / 2f;
        float controlsX = AppConstants.mapWidth + controlsBuffer;
        float controlsY = controlsBuffer;
        float controlsWidth = AppConstants.worldWidth - AppConstants.mapWidth - (2 * controlsBuffer);
        float controlsHeight = 1.4f * controlsWidth;
        spriteBatch.draw(controlsTexture, controlsX, controlsY, controlsWidth, controlsHeight);

        // Draw timer text
        float menuWidth = AppConstants.worldWidth - AppConstants.mapWidth;
        float timerTextX = AppConstants.mapWidth;
        float timerTextY = AppConstants.mapHeight - (2 * AppConstants.cellSize);
        LayoutPos timerTextLP = Utilities.writeText(spriteBatch, mediumFont, timer.toString(), new Vector2(timerTextX, timerTextY), menuWidth, Color.WHITE);
        GlyphLayout timerTextLayout = timerTextLP.glyphLayout;

        // Display the menuMsg
        float buffer = AppConstants.cellSize;
        float menuMsgMaxWidth = menuWidth - buffer;  // Give a little buffer around the message
        float menuMsgX = AppConstants.mapWidth + (buffer / 2f);
        float menuMsgY = timerTextY - timerTextLayout.height - (4 * AppConstants.cellSize);
        LayoutPos menuMsgLP = Utilities.writeText(spriteBatch, smallFont, menuMsg, new Vector2(menuMsgX, menuMsgY), menuMsgMaxWidth, Color.WHITE);
        
        // Draw completed event counters
        float counterBufferY = AppConstants.cellSize * 3;
        float countersY = controlsY + controlsHeight + timerTextLayout.height + counterBufferY;      // We add timerTextLayout.height because it is the same size font as the counters and they draw from a top left origin
        // Draw the middle counter first. Simply centre it on the whole menu width
        float badCounterX = AppConstants.mapWidth;
        LayoutPos badCounterLP = Utilities.writeText(spriteBatch, mediumFont, Integer.toString(Event.getBadEventCounter()), new Vector2(badCounterX, countersY), menuWidth, Color.RED);
        // The left and right counters can now use half the menu width to determine the window to centre in
        float counterWindowWidth = menuWidth / 2f;
        LayoutPos goodCounterLP = Utilities.writeText(spriteBatch, mediumFont, Integer.toString(Event.getGoodEventCounter()), new Vector2(badCounterX, countersY), counterWindowWidth, Color.GREEN);
        LayoutPos hiddenCounterLP = Utilities.writeText(spriteBatch, mediumFont, Integer.toString(Event.getHiddenEventCounter()), new Vector2(badCounterX + counterWindowWidth, countersY), counterWindowWidth, Color.ORANGE);

        // Draw score
        float scoreBuffer = AppConstants.cellSize;
        float scoreTextX = AppConstants.mapWidth + scoreBuffer;
        float scoreTextY = badCounterLP.pos.y + (AppConstants.cellSize * 3f) + timerTextLayout.height;      // We add timerTextLayout.height because it is the same size font as the counters and they draw from a top left origin
        // Draw "Score: " in small text
        LayoutPos scoreTextLP = Utilities.writeText(spriteBatch, smallFont, "Score:", new Vector2(scoreTextX, scoreTextY), Color.WHITE);
        // Draw the score value in larger text, to the right of "Score: "
        float scoreValueX = scoreTextLP.pos.x + scoreTextLP.glyphLayout.width;
        // Create a vertical window larger than the size of a medium font and it will centre on the same line as the small font
        float scoreValueY = scoreTextLP.pos.y + scoreTextLP.glyphLayout.height; // One small character above the start of the text
        float scoreValueHeightWindow = scoreTextLP.glyphLayout.height * 3f;  // One for the character above the line, one on the line, and one below the line
        // Get the horizontal window
        float scoreValueWidthWindow = AppConstants.worldWidth - scoreTextLP.pos.x - scoreTextLP.glyphLayout.width - scoreBuffer;
        LayoutPos scoreValueLP = Utilities.writeText(spriteBatch, mediumFont, Integer.toString(score + timer.toScore()), new Vector2(scoreValueX, scoreValueY), scoreValueWidthWindow, scoreValueHeightWindow, Color.WHITE);
        // Draw score calculation explanation
        float scoreCalcY = scoreValueLP.pos.y - scoreValueLP.glyphLayout.height - AppConstants.cellSize;      // Draw it below the previous line
        String scoreCalculation = "Score = Remaining Time / Total Time + Event Bonuses";
        float scoreCalculationWindowWidth = AppConstants.worldWidth - AppConstants.mapWidth;
        LayoutPos scoreCalcLP = Utilities.writeText(spriteBatch, XsmallFont, scoreCalculation, new Vector2(AppConstants.mapWidth, scoreCalcY), scoreCalculationWindowWidth, Color.WHITE);

        // Draw pause screen
        if(paused){
            overlay("PAUSED", "Press ESC to Resume");
        }
        if(gameEnd){
            if(win){
                overlay("YOU WIN!", "Press ESC to Restart");
            }
            else{
                overlay("GAME OVER", "Press ESC to Restart");
            }
        }

        spriteBatch.end();
    }

    /**
     * Write an overlay on the screen with a main message in large text above a secondary message in smaller text
     * 
     * @param mainMsg The main message to be shown in large text
     * @param minorMsg The secondary message to be shown below the main message in smaller text
     */
    private void overlay(String mainMsg, String minorMsg){
        // Main message
        LayoutPos mainLayout = Utilities.writeText(spriteBatch, largeFont, mainMsg, new Vector2(0, AppConstants.worldHeight), AppConstants.worldWidth, AppConstants.worldHeight, Color.RED);

        // Minor message
        float offsetY = mainLayout.pos.y - mainLayout.glyphLayout.height - AppConstants.cellSize;
        LayoutPos minorLayout = Utilities.writeText(spriteBatch, mediumFont, minorMsg, new Vector2(0, offsetY), AppConstants.worldWidth, Color.RED);
    }

    @Override
    public void pause(){
        // Pause the game
        freeze = true;
        paused = true;
    }

    @Override
    public void resume(){
    }

    public void restart(){
        // Reset game states
        gameEnd = false;
        freeze = false;
        win = false;
        paused = false;

        // Reset player
        playerEntity.reset();
        playerEntity.collidable = true;
        playerEntity.setSpeed(AppConstants.playerSpeedDefault);

        // Reset events
        for(Event e : events){
            e.reset();
        }
        // Reset eventEntities
        eventEntities.clear();

        // Reset timer
        timer.reset();

        score = 0;

        menuMsg = "";

        Event.resetEventCounters();

        music.play();
    }
}