package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.uibase._
import rpgboss.editor.dialog.db.components._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing._
import scala.swing.event._
import rpgboss.editor.dialog._
import rpgboss.lib._
import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.resource._
import net.java.dev.designgridlayout._
import rpgboss.editor.resourceselector._
import java.awt.image.BufferedImage
import rpgboss.player.BattleState
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import rpgboss.model.battle.Battle
import com.badlogic.gdx.graphics.g2d.TextureAtlas

object EncounterFieldGdxPanel {
  val width = 640
  val height = 320
}

class EncounterFieldGdxPanel(project: Project, initial: Encounter) 
  extends GdxPanel(EncounterFieldGdxPanel.width, 
                   EncounterFieldGdxPanel.height) {
  var battleState: BattleState = null
  var atlasSprites: TextureAtlas = null
  
  override lazy val gdxListener = new ApplicationAdapter {
    def updateBattleState(encounter: Encounter) = {
      assume(battleState != null)
      
      // TODO: See if this dummy battle constructor has a better home
      val battle = new Battle(
        project.data,
        project.data.startup.startingParty,
        project.data.enums.characters.map(v => 1),
        project.data.enums.characters.map(v => 1),
        project.data.enums.characters.map(v => 1),
        project.data.enums.characters.map(v => Seq()),
        project.data.enums.characters.map(v => Seq()),
        project.data.enums.characters.map(v => 0),
        encounter,
        Nil)
      
      if (battleState.battleActive)
        battleState.endBattle()
      
      battleState.startBattle(
        battle, Some("defaultrc_battleback/crownlesswish_rrr.jpg"))
    }
    
    override def create() = {
      atlasSprites = GdxUtils.generateSpritesTextureAtlas(
        Spriteset.list(project).map(Spriteset.readFromDisk(project, _)))
      battleState = new BattleState(project, EncounterFieldGdxPanel.width,
                                    EncounterFieldGdxPanel.height)
      updateBattleState(initial)
    }
    
    override def render() = {
      battleState.update(Gdx.graphics.getDeltaTime())
      battleState.render(atlasSprites)
    }
  }

  def updateBattleState(encounter: Encounter) = GdxUtils.asyncRun {
    gdxListener.updateBattleState(encounter)
  }
}

class EncountersPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends RightPaneArrayDatabasePanel(
    owner,
    "Encounters",
    dbDiag.model.enums.encounters) {
  def battlefieldWidth: Int = 480
  def battlefieldHeight: Int = 320
  
  def panelName = "Encounters"
  def newDefaultInstance() = Encounter()
  def label(e: Encounter) = e.name

  def editPaneForItem(idx: Int, model: Encounter) = {
    def autoArrangeModel(): Unit = {
      if (model.units.isEmpty)
        return
      
      val unitCount = model.units.length
      
      // Divide the battlefield into rectangles, max 2 in y, any number in x.
      val xRects = (unitCount + 1) / 2
      val xRectSize = battlefieldWidth / xRects
      val yRectSize = battlefieldHeight / 2
      
      // Place units onto grid
      val newUnits = for (i <- 0 until unitCount) {
        model.units(i).x = (i / 2) * xRectSize + (xRectSize / 2)
        model.units(i).y = (i % 2) * yRectSize + (yRectSize / 2)
      }
      
      // If there's an odd number, pull the 'last' unit into the center
      if (unitCount % 2 == 1) {
        model.units.last.y = battlefieldHeight / 2
      }
    }
    
    val fDisplay = new EncounterFieldGdxPanel(sm.getProj, model)
    
    val fEnemySelector = new ArrayListView(dbDiag.model.enums.enemies) {
      override def label(a: Enemy) = a.name
    }
    fEnemySelector.selectIndices(0)
    
    val btnAdd = new Button(Action("<= Add") {
      if (!fEnemySelector.selection.indices.isEmpty) {
        val unit = EncounterUnit(fEnemySelector.selection.indices.head, 0, 0)
        model.units = model.units ++ Seq(unit)
        autoArrangeModel()
        fDisplay.updateBattleState(model)
      }
    })
    
    val btnRemove = new Button(Action("=> Remove") {
      if (!model.units.isEmpty) {
        model.units = model.units.dropRight(1)
        autoArrangeModel()
        fDisplay.updateBattleState(model)
      }
    })
    
    new BoxPanel(Orientation.Horizontal) {
      contents += fDisplay
      contents += new BoxPanel(Orientation.Vertical) {
        contents += btnAdd
        contents += btnRemove
      }
      contents += new BoxPanel(Orientation.Vertical) {
        contents += leftLabel("Enemies:")
        contents += new ScrollPane(fEnemySelector) {
          preferredSize = new Dimension(200, 320)
        }
      }
    }
  }

  override def onListDataUpdate() = {
    dbDiag.model.enums.encounters = arrayBuffer
  }
}