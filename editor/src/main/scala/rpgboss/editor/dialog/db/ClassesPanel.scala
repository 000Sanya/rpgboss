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

class ClassesPanel(
    owner: Window, 
    sm: StateMaster, 
    val dbDiag: DatabaseDialog) 
  extends RightPaneArrayDatabasePanel(
      owner, 
      "Classes", 
      dbDiag.model.classes)
  with DatabasePanel
{
  def panelName = "Classes"
  def newDefaultInstance() = new CharClass()
  def label(charClass: CharClass) = charClass.name
  
  def editPaneForItem(idx: Int, model: CharClass) = {
    val fName = textField(
        model.name, 
        v => {
          model.name = v
          refreshModel()
        })
    
    val fEffects = 
      new EffectPanel(owner, dbDiag, model.effects, model.effects = _)
    
    val fCanEquip = new StringArrayMultiselectPanel(
        owner, 
        "Can equip",
        dbDiag.model.equipSubtypes,
        model.canUseEquipSubtypes,
        model.canUseEquipSubtypes = _)
    
    val mainFields = new DesignGridPanel {
      
      row().grid(leftLabel("Name:")).add(fName)
      
    }
    
    new BoxPanel(Orientation.Horizontal) {
      contents += mainFields
      contents += fCanEquip
      contents += fEffects
    }
  }
  
  override def onListDataUpdate() = {
    logger.info("Classes updated")
    dbDiag.model = dbDiag.model.copy(
        classes = array
    )
  }
}