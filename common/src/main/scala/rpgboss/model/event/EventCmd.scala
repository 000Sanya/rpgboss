package rpgboss.model.event

import rpgboss.model._
import rpgboss.player._
import org.json4s.TypeHints
import org.json4s.ShortTypeHints
import EventCmd._
import EventJavascript._
import rpgboss.player.entity.WindowText
import scala.collection.mutable.ArrayBuffer
import rpgboss.lib.Utils
import rpgboss.lib.ArrayUtils
import rpgboss.lib.Layout

trait EventCmd extends HasScriptConstants {
  def sections: Array[CodeSection]

  /**
   *  Returns a copy of this EventCmd with a new set of inner commands placed
   *  in section |commandListI|.
   */
  def copyWithNewInnerCmds(commandListI: Int,
    newInnerCmds: Array[EventCmd]): EventCmd = {
    throw new NotImplementedError
  }

  val toJs: Array[String] =
    sections.flatMap(_.toJs)

  def getParameters(): List[EventParameter[_]] = Nil
}

object EventCmd {
  case class CommandList(cmds: Array[EventCmd],
    indent: Int) extends CodeSection {
    def toJs = cmds
      .map(_.toJs) // Convert to JS
      .flatten
      .map(("  " * indent) + _) // Indent by 2 spaces
  }

  // Used to deserialize legacy names.
  case object EventRenameHints extends TypeHints {
    val hints: List[Class[_]] = Nil
    def hintFor(clazz: Class[_]) = sys.error("No hints provided")
    def classFor(hint: String) = hint match {
      case "SetEvtState" => Some(classOf[SetEventState])
      case _ => None
    }
  }

  val hints = ShortTypeHints(List(
    classOf[AddRemoveItem],
    classOf[AddRemoveGold],
    classOf[GetChoice],
    classOf[HidePicture],
    classOf[IncrementEventState],
    classOf[LockPlayerMovement],
    classOf[ModifyParty],
    classOf[MoveEvent],
    classOf[OpenStore],
    classOf[PlayMusic],
    classOf[PlaySound],
    classOf[RunJs],
    classOf[SetEventState],
    classOf[SetGlobalInt],
    classOf[SetLocalInt],
    classOf[ShowText],
    classOf[ShowPicture],
    classOf[StartBattle],
    classOf[StopMusic],
    classOf[Teleport])) + EventRenameHints
}

case class AddRemoveItem(
  var add: Boolean = true,
  itemId: IntParameter = IntParameter(),
  quantity: IntParameter = IntParameter(1))
  extends EventCmd {
  def qtyDelta =
    RawJs(if (add) quantity.rawJs.exp else "%s * -1".format(quantity.rawJs.exp))
  def sections = singleCall("game.addRemoveItem", itemId, qtyDelta)

  override def getParameters() = List(itemId, quantity)
}

case class AddRemoveGold(
  var add: Boolean = true,
  quantity: IntParameter = IntParameter())
  extends EventCmd {
  def qtyDelta =
    RawJs(if (add) quantity.rawJs.exp else "%s * -1".format(quantity.rawJs.exp))
  def sections = singleCall("game.addRemoveGold", qtyDelta)

  override def getParameters() = List(quantity)
}

/**
 * @param   innerCmds   Has one more element than choices to account for the
 *                      default case.
 */
case class GetChoice(var question: Array[String] = Array(),
                     var choices: Array[String] = Array("Yes", "No"),
                     var allowCancel: Boolean = false,
                     var innerCmds: Array[Array[EventCmd]] =
                       Array(Array(), Array(), Array())) extends EventCmd {
  def sections = {
    val buf = new ArrayBuffer[CodeSection]()

    def caseSections(caseLabel: String, code: Array[EventCmd]) = Array(
      PlainLines(Array("  %s:".format(caseLabel))),
      CommandList(code, 2),
      PlainLines(Array("    break;")))

    buf += PlainLines(Array(
      RawJs("switch(game.getChoice(%s, %s, %b)) {".format(
          EventJavascript.toJs(question),
          EventJavascript.toJs(choices), allowCancel)).exp))

    for (i <- 0 until choices.size) {
      caseSections("case %d".format(i), innerCmds(i)).foreach(buf += _)
    }

    caseSections("default", innerCmds.last).foreach(buf += _)

    buf += PlainLines(Array(RawJs("}").exp))

    buf.toArray
  }

  override def copyWithNewInnerCmds(commandListI: Int,
    newInnerCmds: Array[EventCmd]): EventCmd = {
    val newArray = ArrayUtils.normalizedAry(
        innerCmds, choices.size + 1, choices.size + 1, () => Array[EventCmd]())
    newArray.update(commandListI, newInnerCmds)
    copy(innerCmds = newArray)
  }
}

case class HidePicture(
    slot: IntParameter = IntParameter(PictureSlots.ABOVE_MAP)) extends EventCmd {
  def sections = singleCall("game.hidePicture", slot)
}

case class IncrementEventState() extends EventCmd {
  def sections = singleCall("game.incrementEventState", RawJs("event.id()"))
}

case class LockPlayerMovement(body: Array[EventCmd]) extends EventCmd {
  // TODO: Clean this up. Probably need a better JS DSL.
  def sections = Array(
    PlainLines(Array(
      jsStatement("game.setInt", PLAYER_MOVEMENT_LOCKS,
        RawJs(jsCall("game.getInt", PLAYER_MOVEMENT_LOCKS).exp + " + 1")),
      RawJs("try {").exp)),
    CommandList(body, 1),
    PlainLines(Array(
      RawJs("} finally {").exp,
      jsStatement("  game.setInt", PLAYER_MOVEMENT_LOCKS,
        RawJs(jsCall("game.getInt", PLAYER_MOVEMENT_LOCKS).exp + " - 1")),
      RawJs("}").exp)))

  override def copyWithNewInnerCmds(commandListI: Int,
    newInnerCmds: Array[EventCmd]): EventCmd = {
    assert(commandListI == 0)
    copy(body = newInnerCmds)
  }
}

case class ModifyParty(add: Boolean = true, characterId: Int = 0)
  extends EventCmd {
  def sections = singleCall("game.modifyParty", add, characterId)
}

case class MoveEvent(
  var entitySpec: EntitySpec = EntitySpec(),
  var dx: Float = 0f,
  var dy: Float = 0f,
  var affixDirection: Boolean = false,
  var async: Boolean = false) extends EventCmd {
  def sections = entitySpec match {
    case EntitySpec(which, _, _) if which == WhichEntity.PLAYER.id =>
      singleCall("game.movePlayer", dx, dy, affixDirection, async)
    case EntitySpec(which, _, _) if which == WhichEntity.THIS_EVENT.id =>
      singleCall(
        "game.moveEvent", RawJs("event.id()"), dx, dy, affixDirection,
        async)
    case EntitySpec(which, _, eventIdx)
    if which == WhichEntity.EVENT_ON_MAP.id =>
      singleCall(
        "game.moveEvent", entitySpec.eventId, dx, dy, affixDirection, async)
  }
}

case class OpenStore(
  itemIdsSold: IntArrayParameter = IntArrayParameter(),
  buyPriceMultiplier: FloatParameter = FloatParameter(1.0f),
  sellPriceMultiplier: FloatParameter = FloatParameter(0.5f)) extends EventCmd {
  def sections = singleCall("game.openStore", itemIdsSold, buyPriceMultiplier,
      sellPriceMultiplier)

  override def getParameters() =
    List(itemIdsSold, buyPriceMultiplier, sellPriceMultiplier)
}

case class PlaySound(var spec: SoundSpec = SoundSpec()) extends EventCmd {
  def sections =
    singleCall("game.playSound", spec.sound, spec.volume, spec.pitch)
}

case class PlayMusic(
  slot: IntParameter = IntParameter(0),
  var spec: SoundSpec = SoundSpec(),
  var loop: Boolean = true,
  var fadeDuration: Float = 0.5f) extends EventCmd {
  def sections =
    singleCall("game.playMusic", slot, spec.sound, spec.volume, loop,
        fadeDuration)
}

case class RunJs(scriptBody: String = "") extends EventCmd {
  def sections = Array(PlainLines(Array(scriptBody.split("\n"): _*)))
}

case class SetEventState(
  var entitySpec: EntitySpec = EntitySpec(),
  var state: Int = 0) extends EventCmd {
  def sections = {
    val (mapName, eventId) = WhichEntity(entitySpec.whichEntityId) match {
      case WhichEntity.THIS_EVENT =>
        (RawJs("event.mapName()"), RawJs("event.id()"))
      case WhichEntity.EVENT_ON_MAP =>
        (RawJs("event.mapName()"), entitySpec.eventId)
      case WhichEntity.EVENT_ON_OTHER_MAP =>
        (entitySpec.mapName, entitySpec.eventId)
    }

    singleCall("game.setEventState", mapName, eventId, state)
  }
}

case class SetGlobalInt(
    var key: String = "globalVariableName",
    var operatorId: Int = OperatorType.default.id,
    value1: IntParameter = IntParameter(),
    value2: IntParameter = IntParameter()) extends EventCmd {
  def sections = {
    val operator = OperatorType(operatorId)
    val operatorString = operator.jsString

    if (operatorString.isEmpty)
      singleCall("game.setInt", key, value1)
    else
      singleCall("game.setInt", key,
          applyOperator(value1.rawJs, operatorString, value2.rawJs))
  }
}

case class SetLocalInt(variableName: String,
                       value: EventParameter[_]) extends EventCmd {
  def sections = Array(PlainLines(
      Array("var %s = %s;".format(variableName, value.rawJs.exp))))
}

case class ShowText(var lines: Array[String] = Array()) extends EventCmd {
  def processedLines = lines.map { l =>
    // The local variables need to be processed here rather than in the
    // WindowText class, because only the Javascript has access to the local
    // variables.
    val l1 = EventJavascript.toJs(l)
    val l2 = WindowText.javascriptCtrl.replaceAllIn(
        l1, rMatch => {
          """" + %s + """".format(rMatch.group(1))
        })

    RawJs(l2)
  }

  def sections = singleCall("game.showText", processedLines)
}

case class ShowPicture(
    slot: IntParameter = IntParameter(PictureSlots.ABOVE_MAP),
    var picture: String = "",
    layout: Layout = Layout.defaultForPictures) extends EventCmd {
  def sections =
    singleCall("game.showPicture", slot, picture, RawJs(layout.toJs()))
}

case class StartBattle(
    var encounterId: Int = 0,
    var battleMusic: Option[SoundSpec] = None,
    var battleBackground: String = "")
  extends EventCmd {
  def sections = battleMusic.map { music =>
    singleCall("game.startBattle", encounterId, battleBackground,
        music.sound, music.volume)
  }.getOrElse {
    singleCall("game.startBattle", encounterId, battleBackground, "", 1.0f)
  }
}

case class StopMusic(
    slot: IntParameter = IntParameter(0),
    var fadeDuration: Float = 0.5f) extends EventCmd {
  def sections = singleCall("game.stopMusic", slot, fadeDuration)
}

case class Teleport(loc: MapLoc, transitionId: Int) extends EventCmd {
  def sections =
    singleCall("game.teleport", loc.map, loc.x, loc.y, transitionId)
}
