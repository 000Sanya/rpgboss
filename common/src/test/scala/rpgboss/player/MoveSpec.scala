package rpgboss.player

import com.google.common.io.Files
import java.awt._
import java.awt.image._
import java.io.File
import javax.imageio.ImageIO
import rpgboss._
import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.resource._
import rpgboss.player._
import rpgboss.player.entity.EntityMove

class MoveSpec extends UnitSpec {
  def paint(array: Array[Array[Byte]], x: Int, y: Int, bytes: Array[Byte]) = {
    array.length should be > (0)
    array.head.length should be > (0)
    x should be >= (0)
    x should be < array.head.length
    y should be >= (0)
    y should be < array.length
    bytes.length should equal(RpgMap.bytesPerTile)
    
    for (i <- 0 until RpgMap.bytesPerTile) {
      array(y)(x * RpgMap.bytesPerTile + i) = bytes(i)
    }
  }
  
  def paintPassable(array: Array[Array[Byte]], x: Int, y: Int) = {
    paint(array, x, y, Array(RpgMap.autotileByte, 16, 0))
  }
  
  def fixture(startingX: Float, startingY: Float) = {
    val tempDir = Files.createTempDir()
    val projectOption = rpgboss.util.ProjectCreator.create("test", tempDir)
    projectOption should be ('isDefined)
    
    val proj = projectOption.get
    
    // Set starting loc
    proj.data.startup.startingLoc = 
      MapLoc(RpgMap.generateName(1), startingX, startingY)
    proj.writeMetadata() should be (true)
    
    // Create a fake map
    val mapName = RpgMap.generateName(proj.data.lastCreatedMapId)
    val map = RpgMap.readFromDisk(proj, mapName)
    val mapData = map.readMapData().get
    
    for (x <- 0 to 20)
      for (y <- 0 to 20)
        paintPassable(mapData.botLayer, x, y)
    
    map.saveMapData(mapData) should equal(true)
    
    new {
      val projectDirectory = tempDir
      val project = proj
    }
  }
  
  "Move" should "should work in cardinal directions" in {
    val f = fixture(5.5f, 5.5f)
    
    val game = new TestGame(f.projectDirectory) {
      def runTest() = {
        state.setPlayerLoc(project.data.startup.startingLoc);
        val player = state.getPlayerEntity()
        val move = EntityMove(4.0f, 0)
        player.enqueueMove(move)
        move.awaitDone()
      }
    }
    
    TestPlayer.launch(game)
    
    game.awaitFinish() should equal(true)
  }
}