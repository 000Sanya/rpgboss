package rpgboss.player

import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.resource._
import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.utils.Logger
import com.badlogic.gdx.graphics._
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d._
import rpgboss.player.entity._
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/**
 * This class renders stuff on the screen.
 *
 * This must be guaranteed to be instantiated after create() on the main
 * ApplicationListener.
 * 
 * This class should only be accessed on the gdx thread
 */
class ScreenLayer(game: MyGame, state: GameState) {
  def project = game.project
  val batch = new SpriteBatch()
  val shapeRenderer = new ShapeRenderer()

  val windowskin = 
    Windowskin.readFromDisk(project, project.data.startup.windowskin)
  val windowskinTexture =
    new Texture(Gdx.files.absolute(windowskin.dataFile.getAbsolutePath()))
  val windowskinRegion = new TextureRegion(windowskinTexture)

  val font = Msgfont.readFromDisk(project, project.data.startup.msgfont)
  var fontbmp: BitmapFont = font.getBitmapFont()
  
  private var lastWindowId: Long = 0
  
  val windows = new collection.mutable.ArrayBuffer[Window]
  
  def getWindowId() : Long = {
    lastWindowId += 1
    lastWindowId
  }

  val screenCamera: OrthographicCamera = new OrthographicCamera()
  screenCamera.setToOrtho(true, 640, 480) // y points down
  screenCamera.update()

  batch.setProjectionMatrix(screenCamera.combined)
  batch.enableBlending()
  batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
  shapeRenderer.setProjectionMatrix(screenCamera.combined)

  def update(delta: Float) = {
    // Update windows
    if (!windows.isEmpty)
      windows.head.update(delta, true)
    if (windows.length > 1)
      windows.tail.foreach(_.update(delta, false))

  }
  
  // Render that's called before the map layer is drawn
  def preMapRender() = {
    batch.begin()
    
    for (i <- PictureSlots.BACKGROUND until PictureSlots.FOREGROUND;
         pic <- state.persistent.pictures(i)) {
      pic.render(batch)
    } 
    
    batch.end()
  }

  def render() = {
    /*
     * We define our screen coordinates to be 640x480.
     * This is the easiest thing possible.
     * 
     * This definitely favors 4:3 aspect ratios, but that's the historic
     * JRPG look, and I'm not sure how to support varying aspect ratios without
     * really complicating the code...
     */

    batch.begin()

    // Render pictures
    for (i <- PictureSlots.FOREGROUND until PictureSlots.TOTAL;
         pic <- state.persistent.pictures(i)) {
      pic.render(batch)
    }

    // Render all windows
    windows.foreach(_.render(batch))

    batch.end()

    // Render transition
    state.curTransition map { transition =>

      // Spritebatch seems to turn off blending after it's done. Turn it on.
      Gdx.gl.glEnable(GL10.GL_BLEND)
      shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

      shapeRenderer.setColor(0, 0, 0, transition.curAlpha)
      shapeRenderer.rect(0, 0, 640, 480)
      shapeRenderer.end()
    }
  }

  def dispose() = {
    batch.dispose()
  }
}
