package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.lib._
import rpgboss.editor.lib.SwingUtils._
import scala.swing._
import scala.swing.event._

import rpgboss.editor.dialog._

import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.model.resource._

import net.java.dev.designgridlayout._

class CharactersPanel(
    owner: Window, 
    sm: StateMaster, 
    val dbDiag: DatabaseDialog) 
  extends RightPaneArrayEditingPanel(
      owner, 
      "Characters", 
      dbDiag.model.characters)
  with DatabasePanel
{
  def panelName = "Characters"
  def newDefaultInstance() = new Character()
  def label(character: Character) = character.defaultName
  
  def editPaneForItem(idx: Int, initial: Character) = {
    var model = initial
      
    def updateModel(newModel: Character) = {
      model = newModel
      updatePreserveSelection(idx, model)
    }
    
    val leftPane = new DesignGridPanel {
      val fName = textField(
          model.defaultName, 
          v => updateModel(model.copy(defaultName = v)))
      val fSubtitle = textField(
          model.subtitle, 
          v => updateModel(model.copy(subtitle = v)))
      val fDescription = textField(
          model.description, 
          v => updateModel(model.copy(description = v)))
      
      val fSprite = new SpriteBox(
          owner,
          sm,
          model.sprite,
          (newSprite) => {
            updateModel(model.copy(sprite = newSprite))
          })
      
      def numParamEdit(
          initial: Int, 
          mutateF: (Int) => Unit, 
          min: Int = 0, 
          max: Int = 100) = {
        new NumberSpinner(initial, 0, 100, onUpdate = mutateF)
      }
      
      val fInitLevel = numParamEdit(
          model.initLevel, 
          v => updateModel(model.copy(initLevel = v)),
          MINLEVEL,
          MAXLEVEL)
        
      val fMaxLevel = numParamEdit(
          model.maxLevel, 
          v => updateModel(model.copy(maxLevel = v)),
          MINLEVEL,
          MAXLEVEL)
      
      row().grid(leftLabel("Default name:")).add(fName)
      
      row().grid(leftLabel("Subtitle:")).add(fSubtitle)
      
      row().grid(leftLabel("Description:")).add(fDescription)
      
      row().grid(leftLabel("Sprite:")).add(fSprite)
      
      row()
        .grid(leftLabel("Initial level:")).add(fInitLevel)
        .grid(leftLabel("Max level:")).add(fMaxLevel)
    }
    
    val rightPane = new CharProgressionPanel(model.progressions, p => {
      updateModel(model.copy(progressions = p))
    })
    
    new BoxPanel(Orientation.Horizontal) {
      contents += leftPane
      contents += rightPane
    }
  }
  
  override def onListDataUpdate() = {
    logger.info("Characters data updated")
    dbDiag.model = dbDiag.model.copy(
        characters = array
    )
  }
}