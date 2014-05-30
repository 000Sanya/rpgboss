package rpgboss.editor.dialog.db.components

import javax.swing.BorderFactory
import rpgboss.editor.uibase._
import rpgboss.editor.dialog._
import rpgboss.lib._
import rpgboss.model._
import scala.collection.mutable.ArrayBuffer
import scala.swing._

class LearnedSkillPanel(
  owner: Window,
  dbDiag: DatabaseDialog,
  initial: Array[LearnedSkill],
  onUpdate: Array[LearnedSkill] => Unit)
  extends BoxPanel(Orientation.Vertical) {

  preferredSize = new Dimension(150, 200)

  val learnedSkills = ArrayBuffer(initial : _*)

  val tableEditor = new TableEditor() {
    def title = "Learned Skills"
    def colHeaders = Array("Level", "Skill")
    def getRowStrings(row: Int) = {
      assume(row < learnedSkills.size)
      val learnedSkill = learnedSkills(row)
      val skill = dbDiag.model.enums.skills(learnedSkill.skillId)
      Array("Level %d".format(learnedSkill.level),
            StringUtils.standardIdxFormat(learnedSkill.skillId, skill.name))
    }
    def columnCount: Int = 2
    def modelRowCount: Int = learnedSkills.size

    def showEditDialog(row: Int, updateDisplayFunction: () => Unit) = {
      val initialE = learnedSkills(row)
      val diag = new LearnedSkillDialog(
        owner,
        dbDiag,
        initialE,
        v => {
          learnedSkills.update(row, v)
          updateDisplayFunction()
          onUpdate(learnedSkills.toArray)
        })
      diag.open()
    }

    def showNewDialog(updateDisplayFunction: () => Unit) = {
      val diag = new LearnedSkillDialog(
        owner,
        dbDiag,
        LearnedSkill(1, 0),
        v => {
          learnedSkills += v
          updateDisplayFunction()
          onUpdate(learnedSkills.toArray)
        })
      diag.open()
    }

    def deleteRow(row: Int, updateDisplayFunction: () => Unit) = {
      learnedSkills.remove(row)
      updateDisplayFunction()
      onUpdate(learnedSkills.toArray)
    }
  }

  contents += tableEditor
}

class LearnedSkillDialog(
  owner: Window,
  dbDiag: DatabaseDialog,
  initial: LearnedSkill,
  onOk: LearnedSkill => Unit)
  extends StdDialog(owner, "Learned Skill") {

  import SwingUtils._

  val model = initial

  val fLevel = new NumberSpinner(model.level, 1, 100, model.level = _)
  val fSkill =
    indexedCombo(dbDiag.model.enums.skills, model.skillId, model.skillId = _)

  contents = new DesignGridPanel {
    row().grid(lbl("Level:")).add(fLevel)
    row().grid(lbl("Skill:")).add(fSkill)

    addButtons(cancelBtn, okBtn)
  }

  def okFunc() = {
    onOk(model)
    close()
  }
}