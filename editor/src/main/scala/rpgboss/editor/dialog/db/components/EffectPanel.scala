package rpgboss.editor.dialog.db.components

import scala.swing._
import scala.swing.event._
import rpgboss.editor.uibase.SwingUtils._
import rpgboss.editor.dialog._
import javax.swing.BorderFactory
import javax.swing.table.AbstractTableModel
import rpgboss.model._
import rpgboss.model.Constants._
import rpgboss.editor.uibase.DesignGridPanel
import rpgboss.editor.uibase.StdDialog
import rpgboss.editor.uibase.NumberSpinner
import scala.Array.canBuildFrom
import scala.Array.fallbackCanBuildFrom
import scala.collection.mutable.ArrayBuffer
import java.awt.event.MouseEvent
import rpgboss.editor.uibase.PopupMenu
import rpgboss.editor.uibase.TableEditor

class EffectPanel(
  owner: Window,
  dbDiag: DatabaseDialog,
  initial: Seq[Effect],
  onUpdate: Seq[Effect] => Unit,
  includeStatEffects: Boolean)
  extends BoxPanel(Orientation.Vertical) {
  
  if (includeStatEffects)
    preferredSize = new Dimension(250, 300)
  else
    preferredSize = new Dimension(250, 200)
  
  import EffectKey._

  def isStatEffect(e: Effect) = {
    val statKeys =
      Set(MhpAdd, MmpAdd, AtkAdd, SpdAdd, MagAdd, ArmAdd, MreAdd).map(_.id)
    statKeys.contains(e.keyId)
  }

  def updateFromModel() = {
    onUpdate(statEffects ++ miscEffects)
  }

  var statEffects: ArrayBuffer[Effect] =
    ArrayBuffer(initial.filter(isStatEffect) : _*)
  val statEffectsPanel = new DesignGridPanel {
    def statSpinner(eKey: EffectKey.Value) = {
      def spinFunc(newValue: Int) = {
        if (newValue == 0) {
          // Remove existing effect.
          statEffects = statEffects.filter(_.keyId != eKey.id)
        } else {
          // Update existing effect, or append a new one.
          statEffects.find(_.keyId == eKey.id) map { effect =>
            effect.v1 = newValue
          } getOrElse {
            statEffects.append(Effect(eKey.id, newValue, 0))
          }
        }
        updateFromModel()
      }

      val initialValue =
        statEffects.find(_.keyId == eKey.id).map(_.v1).getOrElse(0)

      new NumberSpinner(initialValue, -1000, 1000, spinFunc)
    }

    row()
      .grid(lbl("+Max HP")).add(statSpinner(MhpAdd))
      .grid(lbl("+Attack")).add(statSpinner(AtkAdd))
    row()
      .grid(lbl("+Max MP")).add(statSpinner(MmpAdd))
      .grid(lbl("+Speed")).add(statSpinner(SpdAdd))

    row()
      .grid(lbl("+Armor")).add(statSpinner(ArmAdd))
      .grid(lbl("+Magic")).add(statSpinner(MagAdd))

    row()
      .grid(lbl("+Mag. Res.")).add(statSpinner(MreAdd))
      .grid()
  }

  val miscEffects = ArrayBuffer(initial.filter(!isStatEffect(_)) : _*)
  val miscEffectsTable = new TableEditor() {
    def colHeaders = Array("Description", "Key", "Value")
    def getRowStrings(row: Int) = {
      assume(row < miscEffects.size)
      val eff = miscEffects(row)
      val effectKey = EffectKey(eff.keyId)
      Array(effectKey.desc, effectKey.toString,
            effectKey.renderer(eff, dbDiag.model))
    }
    def columnCount: Int = 3
    def modelRowCount: Int = miscEffects.size

    def showEditDialog(row: Int, updateDisplayFunction: () => Unit) = {
      val initialE = miscEffects(row)
      val diag = new EffectDialog(
        owner,
        dbDiag,
        initialE,
        e => {
          miscEffects.update(row, e)
          updateFromModel()
          updateDisplayFunction()
        })
      diag.open()
    }

    def showNewDialog(updateDisplayFunction: () => Unit) = {
      val diag = new EffectDialog(
        owner,
        dbDiag,
        EffectKey.defaultEffect,
        e => {
          miscEffects += e
          updateFromModel()
          updateDisplayFunction()
        })
      diag.open()
    }

    def deleteRow(row: Int, updateDisplayFunction: () => Unit) = {
      miscEffects.remove(row)
      updateDisplayFunction()
    }
  }

  if (includeStatEffects) {
    contents += new BoxPanel(Orientation.Vertical) {
      border = BorderFactory.createTitledBorder("Stat boosts")
      contents += statEffectsPanel
    }
  }

  contents += new ScrollPane {
    border = BorderFactory.createTitledBorder("Other Effects")
    contents = miscEffectsTable
  }
}

class EffectDialog(
  owner: Window,
  dbDiag: DatabaseDialog,
  initial: Effect,
  onOk: Effect => Unit)
  extends StdDialog(owner, "Edit Effect") {
  case class EffectControls(
    key: EffectKey.Val,
    btn: AbstractButton,
    control: Component,
    getVal: () => Effect,
    setVal: (Effect) => Unit)

  var model = initial
  var controls = Nil
  var selectedControls: EffectControls = null

  def selectKey(key: EffectKey.Val) = {
    selectedControls = effectsMap.get(key.id).get

    btnGroup.select(selectedControls.btn)

    effectsAll.foreach { eControls =>
      eControls.control.enabled = false
    }

    selectedControls.control.enabled = true

    model = selectedControls.getVal()
  }

  private def newRadioForKey(key: EffectKey.Val) = new RadioButton() {
    action = Action(key.desc) {
      selectKey(key)
    }
  }

  def onCurControlChange() = {
    model = selectedControls.getVal()
  }

  def nilEffect(key: EffectKey.Val): EffectControls = {
    EffectControls(
      key,
      newRadioForKey(key),
      new BoxPanel(Orientation.Vertical),
      () => Effect(key.id, 0, 0),
      e => Unit)
  }

  def intEffect(key: EffectKey.Val): EffectControls = {
    val spinner = new NumberSpinner(
      0,
      MINEFFECTARG,
      MAXEFFECTARG,
      onUpdate = v => onCurControlChange())

    val control = new BoxPanel(Orientation.Horizontal) {
      contents += spinner
      contents += new Label("p") {
        preferredSize = new Dimension(15, 15)
      }

      override def enabled_=(b: Boolean) = {
        super.enabled_=(b)
        spinner.enabled = b
      }
    }

    EffectControls(
      key,
      newRadioForKey(key),
      control,
      () => Effect(key.id, spinner.getValue, 0),
      e => spinner.setValue(e.v1))
  }

  def percentEffect(key: EffectKey.Val): EffectControls = {
    val spinner = new NumberSpinner(
      0,
      -100,
      100,
      onUpdate = v => onCurControlChange())

    val control = new BoxPanel(Orientation.Horizontal) {
      contents += spinner
      contents += new Label("%") {
        preferredSize = new Dimension(15, 15)
      }

      override def enabled_=(b: Boolean) = {
        super.enabled_=(b)
        spinner.enabled = b
      }
    }

    EffectControls(
      key,
      newRadioForKey(key),
      control,
      () => Effect(key.id, spinner.getValue, 0),
      e => spinner.setValue(e.v1))
  }

  def choiceEffect[T <: HasName](key: EffectKey.Val,
                                 choices: Seq[T]): EffectControls = {
    val combo = indexedCombo(choices, 0, i => onCurControlChange())

    EffectControls(
      key,
      newRadioForKey(key),
      combo,
      () => Effect(key.id, combo.selection.index, 0),
      e => combo.selection.index = e.v1)
  }

  def choiceWithValueEffect[T <% HasName](
    key: EffectKey.Val,
    choices: Seq[T],
    initial: Int,
    min: Int,
    max: Int,
    label: String): EffectControls = {
    val combo = indexedCombo(choices, 0, i => onCurControlChange())
    val spinner = new NumberSpinner(
      initial,
      min,
      max,
      onUpdate = v => onCurControlChange()) {
      maximumSize = new Dimension(90, Int.MaxValue)
    }

    val control = new BoxPanel(Orientation.Horizontal) {
      contents += combo
      contents += new BoxPanel(Orientation.Horizontal) {
        contents += spinner
        contents += new Label(label) {
          preferredSize = new Dimension(15, 15)
        }
      }

      override def enabled_=(b: Boolean) = {
        super.enabled_=(b)
        combo.enabled = b
        spinner.enabled = b
      }
    }

    EffectControls(
      key,
      newRadioForKey(key),
      control,
      () => Effect(key.id, combo.selection.index, spinner.getValue),
      e => {
        combo.selection.index = e.v1
        spinner.setValue(e.v2)
      })
  }
  
  def choicePercentEffect[T <: HasName](key: EffectKey.Val,
                                        choices: Seq[T]): EffectControls = {
    choiceWithValueEffect(key, choices, 100, 0, 100, "%")
  }
  
  def choicePointsEffect[T <% HasName](key: EffectKey.Val,
                                       choices: Seq[T]): EffectControls = {
    choiceWithValueEffect(key, choices, 0, MINEFFECTARG, MAXEFFECTARG, "p")
  }
  
  import EffectKey._

  val effectsStatus = Array(
    intEffect(RecoverHpAdd),
    percentEffect(RecoverHpMul),
    intEffect(RecoverMpAdd),
    percentEffect(RecoverMpMul),
    choicePercentEffect(AddStatusEffect, dbDiag.model.enums.statusEffects),
    choicePercentEffect(RemoveStatusEffect, dbDiag.model.enums.statusEffects))

  val effectsStats = Array(
    intEffect(MhpAdd),
    intEffect(MmpAdd),
    intEffect(SpdAdd),
    intEffect(AtkAdd),
    intEffect(MagAdd),
    intEffect(ArmAdd),
    intEffect(MreAdd),
    choicePointsEffect(ElementResist, dbDiag.model.enums.elements))

  val effectsOther = Array(
    nilEffect(EscapeBattle),
    choiceEffect(UseSkill, dbDiag.model.enums.skills),
    choiceEffect(LearnSkill, dbDiag.model.enums.skills))

  val effectsAll = effectsStatus ++ effectsStats ++ effectsOther
  val effectsMap = Map(effectsAll.map(x => x.key.id -> x): _*)

  val btnGroup = new ButtonGroup(effectsAll.map(_.btn): _*)

  // Does initialization of dialog
  {
    effectsMap.get(initial.keyId) map { ctrlGrp =>
      selectKey(ctrlGrp.key)
      ctrlGrp.setVal(initial)
    }
  }

  class ControlPage(label: String, val controls: Seq[EffectControls]) {
    val panel = new DesignGridPanel {
      controls.foreach { eControls =>
        row()
          .grid().add(eControls.btn)
          .grid().add(eControls.control)
      }
    }
    val tabPage = new TabbedPane.Page(label, panel)
  }

  val ctlPages = Array(
    new ControlPage("Status", effectsStatus),
    new ControlPage("Stats", effectsStats),
    new ControlPage("Other", effectsOther))

  contents = new DesignGridPanel {
    val tabPane = new TabbedPane {
      pages ++= ctlPages.map(_.tabPage)
    }

    row().grid().add(tabPane)

    addButtons(cancelBtn, okBtn)
  }

  def okFunc() = {
    onOk(model)
    close()
  }
}