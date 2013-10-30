package rpgboss.editor.resourceselector

import scala.swing._
import scala.swing.event._
import java.awt.Color
import rpgboss.editor.uibase._
import rpgboss.model._
import rpgboss.model.resource._
import java.awt.image.BufferedImage
import rpgboss.editor.StateMaster
import rpgboss.editor.resourceselector._
import java.awt.Dimension
import java.awt.Graphics2D
import rpgboss.model.resource.Tileset.tilesize

abstract class ImageResourceField[SpecT](
  owner: Window, 
  sm: StateMaster, 
  initial: Option[SpecT], 
  onUpdate: (Option[SpecT]) => Any)
  extends Component {
  
  private var model: Option[SpecT] = None
  private var image: Option[BufferedImage] = None

  def getValue = model
  
  // Implementing classes provide an image representation for the spec
  def getImage(s: SpecT): BufferedImage
  def componentW: Int
  def componentH: Int
  def getSelectDialog(): Dialog
  
  /**
   * Updates the model and the representative image used to draw the component
   */
  def updateModel(s: Option[SpecT]): Unit = {
    model = s
    image = s.map(getImage _)
    onUpdate(s)
    repaint()
  }

  updateModel(initial)

  preferredSize = new Dimension(componentW, componentH)
  maximumSize = new Dimension(componentW * 2, componentH * 2)

  override def paintComponent(g: Graphics2D) = {
    super.paintComponent(g)
    g.setColor(Color.WHITE)
    g.fillRect(0, 0, peer.getWidth(), peer.getHeight())

    // Draw the image centered if it exists
    image map { img =>
      val dstX = (componentW - img.getWidth()) / 2
      val dstY = (componentH - img.getHeight()) / 2
      g.drawImage(img, dstX, dstY, null)
    }
  }

  listenTo(mouse.clicks)
  reactions += {
    case e: MouseClicked => getSelectDialog().open()
  }
}

class SpriteField(
  owner: Window, 
  sm: StateMaster, 
  initial: Option[SpriteSpec], 
  onUpdate: (Option[SpriteSpec]) => Any)
  extends ImageResourceField(owner, sm, initial, onUpdate) {
  
  def getImage(s: SpriteSpec): BufferedImage = {
    val spriteset = Spriteset.readFromDisk(sm.getProj, s.name)
    spriteset.srcTileImg(s)
  }
  
  def componentW = tilesize * 2
  def componentH = tilesize * 3
  
  def getSelectDialog() =
    new SpriteSelectDialog(owner, sm, getValue) {
      def onSuccess(result: Option[SpriteSpec]) =
        updateModel(result)
    }
}

class BattlerField(
  owner: Window, 
  sm: StateMaster, 
  initial: Option[BattlerSpec], 
  onUpdate: (Option[BattlerSpec]) => Any)
  extends ImageResourceField(owner, sm, initial, onUpdate) {
  
  def getImage(m: BattlerSpec): BufferedImage =
    Battler.readFromDisk(sm.getProj, m.name).img
  
  def componentW = 128
  def componentH = 128
  
  def getSelectDialog() =
    new BattlerSelectDialog(owner, sm, getValue) {
      def onSuccess(result: Option[BattlerSpec]) =
        updateModel(result)
    }
}