package rpgboss.player

import com.badlogic.gdx.ApplicationListener
import java.io.File
import rpgboss.model._
import com.badlogic.gdx.graphics._
import com.badlogic.gdx.graphics.g2d._
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.utils.Logger

class MutableMapLoc(var map: Int = -1, var x: Float = 0, var y: Float = 0) {
  def this(other: MapLoc) = this(other.map, other.x, other.y)
  def set(other: MapLoc) = {
    this.map = other.map
    this.x = other.x
    this.y = other.y
  }
}

class Game(gamepath: File) extends ApplicationListener {
  val logger = new Logger("Game", Logger.INFO)
  val fps = new FPSLogger()
  val project = Project.readFromDisk(gamepath).get 
  var map: RpgMap = null
  var mapData: RpgMapData = null
  var camera: OrthographicCamera = null
  var autotiles: Array[Autotile] = null
  var tilesets: Array[Tileset] = null
  var spritesets: Map[String, Spriteset] = null
  var batch: SpriteBatch = null
  
  var atlasTiles: TextureAtlas = null
  var atlasSprites: TextureAtlas = null
  
  // in units of tiles
  var screenW = 20.0
  var screenH = 15.0
  
  // Where in the "second" we are. Varies from 0 to 1.0
  var deltaTime : Float = 0.0f
  
  // camera position and boundaries
  val cameraLoc = new MutableMapLoc()
  var cameraL: Double = 0
  var cameraR: Double = 0
  var cameraT: Double = 0
  var cameraB: Double = 0
  
  // protagonist location
  var characterLoc = new MutableMapLoc()
  
  def setCameraLoc(loc: MapLoc) = {
    cameraLoc.set(loc)
    cameraL = loc.x - screenW/2
    cameraR = loc.x + screenW/2
    cameraT = loc.y - screenH/2
    cameraB = loc.y + screenH/2
    camera.position.x = loc.x
    camera.position.y = loc.y
    camera.update()
  }
  
  override def create() {
    map = RpgMap.readFromDisk(project, project.data.startingLoc.map)
    mapData = map.readMapData().get
    
    camera = new OrthographicCamera()
    camera.setToOrtho(true, screenW.toFloat, screenH.toFloat) // y points down
    
    setCameraLoc(project.data.startingLoc)
    characterLoc.set(project.data.startingLoc)
    
    val packerTiles = new PixmapPacker(1024, 1024, Pixmap.Format.RGBA8888, 0, false)
    
    autotiles = project.data.autotiles.map { name =>
      Autotile.readFromDisk(project, name)
    }
    
    // Pack all the autotiles
    autotiles.map { autotile =>
      val autotilePix = new Pixmap(
          Gdx.files.absolute(autotile.dataFile.getAbsolutePath()))
      
      packerTiles.pack("autotile/%s".format(autotile.name), autotilePix)

      // No need to dispose of pixmaps, I believe, as they get disposed of
      // when the TextureAtlas gets disposed
    }
    
    // Pack all tilesets
    tilesets = map.metadata.tilesets.map(
        name => Tileset.readFromDisk(project, name)).toArray[Tileset]
    
    tilesets.map { tileset =>
      val tilesetPix = new Pixmap(
          Gdx.files.absolute(tileset.dataFile.getAbsolutePath()))
      
      packerTiles.pack("tileset/%s".format(tileset.name), tilesetPix)
    }
    
    logger.info("Packed tilesets and autotiles into %d pages".format(
        packerTiles.getPages().size))
    
    // Generate texture atlas, nearest neighbor with no mipmaps
    atlasTiles = packerTiles.generateTextureAtlas(
        TextureFilter.Nearest, TextureFilter.Nearest, false)
    
    // Generate and pack sprites
    spritesets = Map() ++ Spriteset.list(project).map(
        name => (name, Spriteset.readFromDisk(project, name)))
    val packerSprites = new PixmapPacker(1024, 1024, Pixmap.Format.RGBA8888, 0, false)
    spritesets.foreach { 
      case (name, spriteset) =>
        packerSprites.pack(spriteset.name, 
            new Pixmap(Gdx.files.absolute(spriteset.dataFile.getAbsolutePath()))
        )
    }
    
    logger.info("Packed sprites into %d pages".format(
        packerSprites.getPages().size))
    
    atlasSprites = packerSprites.generateTextureAtlas(
        TextureFilter.Nearest, TextureFilter.Nearest, false)
    
    /*
     * SpriteBatch manages its own matrices. By default, it sets its modelview
     * matrix to the identity, and the projection matrix to an orthographic
     * projection with its lower left corner of the screen at (0, 0) and its
     * upper right corner at (Gdx.graphics.getWidth(), Gdx.graphics.getHeight())
     * 
     * This makes the eye-coordinates the same as the screen-coordinates.
     * 
     * If you'd like to specify your objects in some other space, simply
     * change the projection and modelview (transform) matrices.
     */
    batch = new SpriteBatch() 
  }
  override def render() {
    import Tileset._
    
    // Set delta time
    deltaTime = (deltaTime + Gdx.graphics.getRawDeltaTime()) % 1.0f
    
    // Log fps
    fps.log()
    
    // Clear the context
    Gdx.gl.glClearColor(0, 0, 0, 1)
    Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT)
    
    // Set the projection matrix to the combined camera matrices
    // This seems to be the only thing that works...
    batch.setProjectionMatrix(camera.combined)
    batch.begin()
    
    // Leftmost, rightmost, topmost, bottom-most tiles to render
    val tileL = math.max(0, cameraL.toInt)
    val tileR = math.min(map.metadata.xSize-1, cameraR.toInt+1)
    val tileT = math.max(0, cameraT.toInt)
    val tileB = math.min(map.metadata.ySize-1, cameraB.toInt+1)
    /*println("Render")
    println(tileL)
    println(tileR)
    println(tileT)
    println(tileB)*/
    for(layerAry <- List(
        mapData.botLayer, mapData.midLayer, mapData.topLayer)) {
      for(tileY <- tileT to tileB) {
        for(tileX <- tileL to tileR) {
          val idx = map.metadata.idx(tileX, tileY)
          val byte1 = layerAry(idx)
          val byte2 = layerAry(idx+1)
          val byte3 = layerAry(idx+2)
          
          if(byte1 < 0) {
            if(byte1 == RpgMap.autotileByte) { // Autotile
              val autotile = autotiles(byte2)
              val region = 
                atlasTiles.findRegion("autotile/%s".format(autotile.name))
              
              val frameIdx = (deltaTime*autotile.frames).toInt
                
              val srcDestPositions = autotile.getHalfTiles(byte3, frameIdx)
              
              srcDestPositions map {
                case ((srcXHt, srcYHt), (dstXHt, dstYHt)) =>
                  batch.draw(
                      region.getTexture(),
                      tileX.toFloat+dstXHt*0.5f,
                      tileY.toFloat+dstYHt*0.5f,
                      0.5f, 0.5f,
                      region.getRegionX() + srcXHt*halftile,
                      region.getRegionY() + srcYHt*halftile,
                      halftile, halftile,
                      false, true
                      )
              }
            }
          } else { // Regular tile
            //println("Draw regular tile")
            val region = 
              atlasTiles.findRegion("tileset/%s".format(tilesets(byte1).name))
            batch.draw(
                region.getTexture(),
                tileX.toFloat, 
                tileY.toFloat,
                1.0f, 1.0f,
                region.getRegionX() + byte2*tilesize, 
                region.getRegionY() + byte3*tilesize,
                tilesize, tilesize,
                false, true)
            
          }
        }
      }
    }
    
    // Draw protagonist
    val protagonistActor = project.data.actors(project.data.startingParty(0))
    
    val region =
      atlasSprites.findRegion(protagonistActor.sprite.spriteset)
    val protagonistSpriteset = spritesets(protagonistActor.sprite.spriteset)
      
    val (srcX, srcY) = protagonistSpriteset.srcTexels(
        protagonistActor.sprite.spriteindex,
        Spriteset.DirectionOffsets.SOUTH,
        Spriteset.Steps.STILL)
    
    val (dstOriginX, dstOriginY, dstWTiles, dstHTiles) = 
      protagonistSpriteset.dstPosition(characterLoc.x, characterLoc.y)
      
    batch.draw(
        region.getTexture(),
        dstOriginX.toFloat, 
        dstOriginY.toFloat,
        dstWTiles, dstHTiles,
        region.getRegionX() + srcX, 
        region.getRegionY() + srcY,
        protagonistSpriteset.tileW, 
        protagonistSpriteset.tileH,
        false, true)
    
    batch.end()
  }
  override def dispose() {
    // Dispose of texture atlas
    atlasTiles.dispose()
  }
  override def pause() {}
  override def resume() {}
  override def resize(x: Int, y: Int) {}
}
