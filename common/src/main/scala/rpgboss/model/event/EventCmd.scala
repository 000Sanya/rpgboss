package rpgboss.model.event

import EventCmd._
import rpgboss.model._

trait EventCmd {
  def toJs(): List[String]
  override def toString = {
    val js = toJs()

    if (js.isEmpty) {
      ">>> "
    } else {
      val lines = (">>> " + js.head) :: js.tail.map("... " + _)

      lines.mkString("\n")
    }
  }
}

object EventCmd {
  def types = List(
    classOf[EndOfScript],
    classOf[ShowText],
    classOf[Teleport],
    classOf[SetEvtState],
    classOf[MoveEvent])

  def aryToJs(a: Array[String]) = a.map(strToJs(_)).mkString("[", ", ", "]")
  def strToJs(s: String) = """"%s"""".format(s.replaceAll("\"", "\\\\\""))
}

case class EndOfScript() extends EventCmd {
  def toJs() = Nil
}

case class ShowText(lines: Array[String] = Array()) extends EventCmd {
  def toJs() = List("showText(" + aryToJs(lines) + ");")
}

case class Teleport(loc: MapLoc, transition: Int) extends EventCmd {
  def toJs() = List("""teleport("%s", %f, %f, %d);""".format(
    loc.map, loc.x, loc.y, transition))
}

case class SetEvtState(state: Int = 0) extends EventCmd {
  def toJs() =
    List("""game.setEvtState(%s, %d);""".format("event.idx()", state))
}

case class MoveEvent(
  var entitySpec: EntitySpec = EntitySpec(),
  var dx: Float = 0f,
  var dy: Float = 0f,
  var affixDirection: Boolean = false,
  var async: Boolean = false) extends EventCmd {
  def toJs() = {
    val getEntityCmd = entitySpec match {
      case EntitySpec(which, _) if which == WhichEntity.PLAYER.id =>
        """var _entity = game.getPlayerEntity();"""
      case EntitySpec(which, _) if which == WhichEntity.THIS_EVENT.id =>
        """var _entity = game.getEventEntity(%s);""".format("event.id()")
      case EntitySpec(which, eventIdx) if which == WhichEntity.OTHER_EVENT.id =>
        """var _entity = game.getEventEntity(%s);""".format(eventIdx)
    }
    
    val moveCmd = """game.moveEntity(_entity, %f, %f, %b, %b);""".format(
      dx, dy, affixDirection, async)

    List(getEntityCmd, moveCmd)
  }
}