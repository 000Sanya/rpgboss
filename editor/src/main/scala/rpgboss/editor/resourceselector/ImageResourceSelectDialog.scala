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
import rpgboss.editor.imageset.selector.SpriteSelector

abstract class SpriteSelectDialog(
  owner: Window,
  sm: StateMaster,
  initial: Option[SpriteSpec])
  extends ResourceSelectDialog(owner, sm, initial, true, Spriteset) {
  import Spriteset._

  def specToResourceName(spec: SpriteSpec): String = spec.name
  def newRcNameToSpec(name: String, prevSpec: Option[SpriteSpec]) = {
    val (dir, step) = prevSpec.map { spec =>
      (spec.dir, spec.step)
    } getOrElse ((SpriteSpec.Directions.SOUTH, SpriteSpec.Steps.STILL))

    val idx = prevSpec.map { spec =>
      val newSpriteset = sm.assetCache.getSpriteset(name)
      val prevIdx = spec.spriteIndex

      // ensure that the prevIdx is applicable to the new spriteset
      if (prevIdx < newSpriteset.nSprites) prevIdx else 0
    } getOrElse (0)

    SpriteSpec(name, idx, dir, step)
  }

  def rightPaneDim = new Dimension(384, 384)

  override def rightPaneFor(
    selection: SpriteSpec,
    updateSelectionF: SpriteSpec => Unit) = {
    val spriteset = sm.assetCache.getSpriteset(selection.name)

    new SpriteSelector(spriteset, selection, updateSelectionF)
  }
}

abstract class BattlerSelectDialog(
  owner: Window,
  sm: StateMaster,
  initial: Option[BattlerSpec])
  extends ResourceSelectDialog(owner, sm, initial, true, Battler) {

  def specToResourceName(spec: BattlerSpec): String = spec.name
  def newRcNameToSpec(name: String, prevSpec: Option[BattlerSpec]) =
    prevSpec.map(_.copy(name = name)).getOrElse(BattlerSpec(name))

  override def rightPaneFor(
    selection: BattlerSpec,
    updateSelectionF: BattlerSpec => Unit) = {
    new BattlerSelector(sm.getProj, selection, updateSelectionF)
  }
}

class BattlerSelector(
  proj: Project,
  initial: BattlerSpec,
  selectFunction: BattlerSpec => Any)
  extends DesignGridPanel with DisposableComponent {

  def updateModel(spec: BattlerSpec) = {
    fPreview.setImage(BattlerField.getImage(spec, proj))
    selectFunction(spec)
  }

  val fPreview = new Panel {
    private var _img: BufferedImage = null

    def setImage(img: BufferedImage) = {
      _img = img
      repaint()
    }

    preferredSize = new Dimension(240, 240)

    override def paintComponent(g: Graphics2D) = {
      super.paintComponent(g)
      if (_img != null) g.drawImage(_img, 0, 0, null)
    }
  }

  val fScale = new FloatSpinner(
    initial.scale,
    0.1f,
    10.0f,
    v => updateModel(initial.copy(scale = v)),
    0.1f)

  updateModel(initial)

  import SwingUtils._
  row().grid(leftLabel("Preview:")).add(fPreview)
  row().grid(leftLabel("Scale:")).add(fScale)
}