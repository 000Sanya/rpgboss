rpgboss-editor
==============

RPG game editor. Runs on Windows, Linux, and Mac. Authored games eventually will run on mobile platforms also. Based on libgdx.

Licensed under AGPL3.

Building
--------

Below directions assume a POSIX environment. It should still work on Windows (I've done it), but you may need to modify the directions slightly.

Pre-requisites:

+  git
+  sbt - http://www.scala-sbt.org/
+  Java

Build instructions:

1. Clone this repo and enter it.

```
git clone https://github.com/tommycli/rpgboss.git
cd rpgboss
```

2. Run sbt. Once you are in the sbt prompt, run these commands to pull in extra dependencies:

```
sbt
> update-gdx
> update-libs
```

3. You should still be in the sbt prompt. Switch to the 'editor' subproject, compile, and package it.

```
> project editor
> compile
> package
```

4. You can run the editor from the sbt prompt also.

```
> run
```

Packaging into binaries
-----------------------

1. Install launch4j.

2. Enter the repository and run the package shell script.

```
cd rpgboss
./package/package.sh
```

3. Find your binaries in:

```
$ ls package/target/
rpgboss-editor.exe  rpgboss-editor-linux.tar.gz
```

Project structure
-----------------

+ **Bottom line up front** - Starting *rpgboss.editor.RpgDesktop* as the 'main' class will launch the editor, which will allow you to access the test player.

+ **common** - Contains the models, the game player, and automated tests. 
  
  The models are defined in rpgboss.model and its subpackages.

  The player is defined in rpgboss.player and its subpackages. The player is based on libgdx. Please look at the [libgdx wiki tutorial](https://github.com/libgdx/libgdx/wiki/A-simple-game) to understand the implementation structure.
  
  rpgboss.player.MyGame contains the ApplicationListener, which contains all the player logic. It doesn't have an explicit main-game-loop inside, but is called by libgdx's loop.
  
  The desktop player has a 'main' class is at rpgboss.player.LwjglPlayer. It requires the path to the game project directory as its first argument to run the game.

+ **editor** - Contains the Swing based editor. Contains no game logic, just UI to edit the model defined in the *common* package. The 'main' class is rpgboss.editor.RpgDesktop.

Material
--------

### Autotiles, Spritesets, Tilesets

http://www.tekepon.net/fsm/
http://www.tekepon.net/fsm/modules/refmap/index.php?mode=rules

### Battlers

http://opengameart.org/content/sideview-pixel-art-rpg-enemy-sprites under creative commons.

### Iconsets

420__Pixel_Art__Icons_for_RPG_by_Ails.png:
http://ails.deviantart.com/art/420-Pixel-Art-Icons-for-RPG-129892453

### Faces

### Window Skin

http://rpgmakertimes.agilityhoster.com/2011/02/final-fantasy-i-xpvx-windowskin/

### Picture

LordSpirit.jpg:
http://www.rpgrevolution.com/forums/index.php?autocom=gallery&req=si&img=3701

defaultrc_battleback/crownlesswish_rrr.jpg
http://www.rpgrevolution.com/forums/index.php?autocom=gallery&req=si&img=3769
http://crownlesswish.deviantart.com/

### Sounds

Generated using bfxr at:
http://www.bfxr.net/

### Material Rules

Autotiles are individual files in Rpg Maker VX format.
Spritesets are in Rpg Maker VX format.
Tilesets only constrained to be comprised of 32x32 square tiles.
Iconsets can be any size, but you will need to specify the tile-size when you import it.

Windowskins are in the Rpg Maker XP format.

