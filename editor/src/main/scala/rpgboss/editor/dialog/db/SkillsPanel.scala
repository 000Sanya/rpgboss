package rpgboss.editor.dialog.db

import rpgboss.editor._
import rpgboss.editor.uibase._
import rpgboss.editor.dialog.db.components._
import rpgboss.editor.uibase.SwingUtils._
import scala.swing._
import scala.swing.event._
import rpgboss.editor.dialog._
import rpgboss.model._
import rpgboss.model.Constants._
import net.java.dev.designgridlayout._
import rpgboss.editor.resourceselector._

class SkillsPanel(
  owner: Window,
  sm: StateMaster,
  val dbDiag: DatabaseDialog)
  extends RightPaneArrayDatabasePanel(
    owner,
    "Skills",
    dbDiag.model.enums.skills) {
  def panelName = "Skills"
  def newDefaultInstance() = Skill()

  def editPaneForItem(idx: Int, model: Skill) = {
    new BoxPanel(Orientation.Horizontal) {
      contents += new DesignGridPanel {
        val fName = textField(
          model.name,
          v => {
            model.name = v
            refreshModel()
          })

        val fScope = enumIdCombo(Scope)(model.scopeId, model.scopeId = _)

        val fCost = new NumberSpinner(model.cost, 0, 100, model.cost = _)

        val fAnimationId = indexedCombo(
          dbDiag.model.enums.animations,
          model.animationId,
          model.animationId = _)

        row().grid(lbl("Name:")).add(fName)
        row().grid(lbl("Scope:")).add(fScope)
        row().grid(lbl("Skill point cost:")).add(fCost)
        row().grid(lbl("Animation:")).add(fAnimationId)
      }

      contents += new BoxPanel(Orientation.Vertical) {
        val effectPanel = new EffectPanel(owner, dbDiag, model.effects,
                                          model.effects = _, false)
        val damagePanel =
          new DamageArrayPanel(dbDiag, model.damages, model.damages = _)

        contents += effectPanel
        contents += damagePanel
      }
    }

  }

  override def onListDataUpdate() = {
    dbDiag.model.enums.skills = dataAsArray
  }
}