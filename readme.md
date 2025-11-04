# TeamIV Maze Game

This project is built using **LibGDX**

---

## Folder Structure and Running

The main code for the game is stored in `Game/core/src/main/java/ENG1/teamIV/`

The entry point for the program (for desktop) is `Game/lwjgl3/src/main/java/ENG1/teamIV/lwjgl3/Lwjgl3Launcher.java` 

The assets are stored in `Game/assets`

## Core Components

### `Entity`
The `Entity` class represents all interactive objects in the game world

- Each entity contains both a **Sprite** and a **Rectangle**, to keep visuals and collisions aligned
- Entities can be drawn (`visible = true`) and can participate in collision detection (`collidable = true`)
- The position, movement, and collision resolution are all handled internally

#### Responsibilities
- Track position (`setPos`, `getPos`)
- Handle collisions (`overlaps`, `collide`)
- Manage sprite drawing (`draw`)
- Store attributes like speed, visibility, and collision state

#### Usage Example
```java
Entity wall = new Entity("wall.png", 32, new Vector2(100, 100));
wall.collidable = true;
```

### `Event`

The `Event` class extends `Entity` and represents gameplay triggers such as objectives, doors, or tasks

Each event can depend on other events, forming a dependency chain using the blockedBy array property. An event cannot execute until all its prerequisite events in blockedBy are complete. This allows multi-stage events such as collecting a key to open a door

Writing code in the `onStart()`, `onUpdate()` and `onFinish()` methods is how the event's behaviour is defined. To define the behaviour of an event, these methods should be **overridden** on instantiaion using **anonymous classes** 

The `Event` class stores static counter variables for each of the event types. When a task is completed, the corresponding counter should be incremented.

#### Lifecycle

1. `tryEvent()` checks whether the event can run (`isExecutable()`).

2. If all dependencies are complete, it calls `onStart()`.

3. After `onStart()` has been called, `onUpdate()` will be called every frame

4. Once the event's completion conditions have been met, `onFinish()` will be called

5. The event is complete

### `onStart()`

This is the code that will execute at the start of an event. It should deal with instantiating any objects that the event will need and potentially prompting the user.

Many simple events will only have code in this method and will have no need to override `onUpdate()` or `onFinish()`.

By default, it is empty.

### `onUpdate()`

This method will be called every frame **AFTER** `onStart()` has been called. It should handle any continous behaviour (such as checking conditions) that the event should have.

By default, this method simply sets `complete = true` which will trigger `onFinish()` to be called on the next frame, ending the event.

When this method is overridden, there should be at least one branch which sets `complete = true` in order to progress the event to completion.

### `onFinish()`

This method will be called once the event is completed, the frame after `onUpdate()` has set `complete = true`.

It should handle any cleaning up the event may need, such as disposing of objects, despawning entities, undrawing UI elements, etc.

The static counter variables for each event type should be incremented here. Keep in mind, multi-stage *scenarios* should only increment their respective counter on the **last** `Event` of their chain, rather than for every `Event` object that the *scenario* consists of.

By default, this method is empty.

#### Usage Example – Adding a New Event

```java
Event pickupKey = new Event() {
    @Override
    public void onStart() {
        // Logic when the player collects the key
        System.out.println("Key collected!");
    }
};

Event openDoor = new Event(new Array<>(new Event[]{pickupKey}), 32, new Vector2(200, 200)) {
    @Override
    public void onStart() {
        // Logic when the player opens the door
        System.out.println("Door opened!");
    }
};
```

In this example, the `Event` `openDoor` is blocked by its **prerequisite** `pickupKey`. Meaning the `openDoor` event can only be triggered if the `pickupKey` event has been completed

#### Event Resets

All entities come with a `reset()` method for resetting them when the game is restarted. Depending on the code in the `onStart()`, `onUpdate()` and `onFinish()` methods, the actions needed to reset an event may vary. Therefore, any extra actions that must be taken to reset an event can be defined by overriding this method on instantiation, just like with `onStart()`, `onUpdate()` and `onFinish()`
At minimum, the `reset()` method should call `super.reset()` and set `complete = false` and `started = false`. These behaviours are defined by default and most events wont need to override this method

### Timer

The `Timer` class is a simple countdown mechanism. It can advance either by time or by player movement (`tick(Vector2)`).

This is used as the main time limit for the game

### Utilities

A collection of static helper methods 

1. `loadMap()` parses a text file into wall entities
2. `centreOnCell()` An entity's position is anchored at the bottom left corner of the Sprite. This means entities smaller than a cell will not appeared centred in the cell. This method will centre an entity in whichever cell its position co-ordinates are in
3. `wrapText()` Ensures UI text fits within a designated width
4. `writeText()` Writes text to the screen

### Map Loading

To make map design easily changeable, the map can be defined in a text file which will then be read and parsed by `Utilities.loadMap()` to create wall `Entities` accordingly. By default, this file is `assets/map.txt`

The map file defines walls using user-defined characters, representing different textures. And defines empty, non-wall spaces with a ` ` character.
The mapping of characters to textures can be defined in `assets/map_textures.txt`

The dimensions of the map should match the number of cells in the world map. This is defined by the constants `AppConstants.mapWidth`, `AppConstants.mapHeight` and `AppConstants.cellSize`. E.g. If the mapWidth is 150 and the cellSize is 10, then the map is 15 cells wide so the text file should have 15 characters in each row

#### Defining Wall Textures

Within the `assets/map_textures.txt` file, the mapping of characters to texture files can be defined.

Each line is one mapping of a *symbol* to a *texture filepath*
1. Symbols **MUST** be *one character* long
2. A symbol can be any text character other than ` ` or `=`
3. Filepaths are relative to the `assets/` folder
4. The filepath is the path to an image file that the texture will be made from 
5. The symbol and filepath must be separated by a `=` symbol

E.g.
```
# = wall1.png
A = wall2.png
B = wall3.png
```
This file defines 3 symbols that each correspond to textures. In the `map.txt` file, these symbols can be used to represent a wall with the specified texture.

---

### Game States

There are four main booleans defining the state the game is in. These can be used to control the state of the app:
1. `gameEnd` Will end the game. This is `false` when the game is being played and `true` when the player has either won or lost
2. `win` Whether the player has won or lost. This is `false` while the game is being played (while `gameEnd == flase`). When the game is finished (`gameEnd == true`) this should be `true` if the player met the win condition or `false` if not
3. `freeze` Will stop all logical processing. Not exactly the same as `paused`. `freeze` should be set to `true` when all logic must stop, i.e. when the game is paused (`paused == true`) or at game-end (`gameEnd == true`)
4. `paused` Pauses the game. This will freeze the game (automatically set `freeze == true`) and overlay a pause screen

The following is a table of the possible combinations of game states:

| gameEnd | win | freeze | paused |    Explanation    |
| :-----: | :-: | :----: | :----: | :---------------: |
|    F    |  F  |   F    |   F    | Unpaused gameplay |
|    F    |  F  |   T    |   T    |  Paused gameplay  |
|    T    |  F  |   T    |   F    | Game over - loss  |
|    T    |  T  |   T    |   F    |  Game over - win  |

### `menuMsg`

The `Main` class has a `String menuMsg` which holds a string that will be displayed on the menu every frame

This string may be changed by any area of code to set a message to display to the user. This is particularly useful to set inside the `Event.execute()` method override when you want to give the user direction on what the event requires them to do

## License

This project’s source code is released under **The Unlicense**,  
making it fully public domain and free of all copyright restrictions.

You are free to:
- Use, modify, and distribute this software for any purpose
- Create commercial or non-commercial derivative works
- Omit attribution (though it’s appreciated)

```
This is free and unencumbered software released into the public domain.

Anyone is free to copy, modify, publish, use, compile, sell, or
distribute this software, either in source code form or as a compiled
binary, for any purpose, commercial or non-commercial, and by any
means.

In jurisdictions that recognize copyright laws, the author or authors
of this software dedicate any and all copyright interest in the
software to the public domain. We make this dedication for the benefit
of the public at large and to the detriment of our heirs and
successors. We intend this dedication to be an overt act of
relinquishment in perpetuity of all present and future rights.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
```

> Note: This project depends on [LibGDX](https://libgdx.com),  
> which is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).  
> This does not affect the permissive status of this code.
