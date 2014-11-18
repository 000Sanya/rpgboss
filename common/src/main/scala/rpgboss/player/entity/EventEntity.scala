package rpgboss.player.entity

import rpgboss.model.event._
import rpgboss.player._
import rpgboss.model.SpriteSpec
import scala.concurrent.Promise
import scala.collection.mutable.Subscriber
import scala.collection.script.Message
import rpgboss.lib.Utils

case class EventScriptInterface(mapName: String, id: Int)

class EventEntity(game: RpgGame, mapName: String, mapEvent: RpgEvent)
  extends Entity(game, mapEvent.x, mapEvent.y) {

  def id = mapEvent.id

  val states = if (mapEvent.isInstance) {
    val eventClass = game.project.data.enums.eventClasses(mapEvent.eventClassId)
    val eventClassStates = eventClass.states

    val bindCmds = mapEvent.params map {
      case (key, parameter) =>
        val p = Utils.deepCopy(parameter)
        p.localVariable = key
        SetLocalVariable(p)
    }

    // Bind local variables.
    eventClassStates.map(s => s.copy(cmds = bindCmds.toArray ++ s.cmds))
  } else {
    mapEvent.states
  }

  private var curThread: Option[ScriptThread] = None

  var evtStateIdx = 0

  def getScriptInterface() = EventScriptInterface(mapName, id)

  def evtState = states(evtStateIdx)

  def height: Int = {
    for (i <- evtStateIdx to 1 by -1) {
      if (!states(i).sameAppearanceAsPrevState)
        return states(i).height
    }
    return states.head.height
  }

  val persistentListener =
    new Subscriber[PersistentStateUpdate, PersistentState#Pub] {
    def notify(pub: PersistentState#Pub, evt: PersistentStateUpdate) =
      evt match {
        case EventStateChange((mapName, id), _) => updateState()
        case _ => Unit
      }
    game.persistent.subscribe(this)
  }

  def updateState(): Unit = {
    evtStateIdx = game.persistent.getEventState(mapName, mapEvent.id)
    for (i <- evtStateIdx to 1 by -1) {
      if (!states(i).sameAppearanceAsPrevState)
        return setSprite(states(i).sprite)
    }
    return setSprite(states.head.sprite)
  }
  updateState()

  // Returns None if it's already running.
  def activate(activatorsDirection: Int =
                   SpriteSpec.Directions.NONE): Option[ScriptThread] = {
    import SpriteSpec._

    if (curThread.isDefined)
      return None

    val origDir = dir
    if (activatorsDirection != Directions.NONE)
      dir = Directions.opposite(activatorsDirection)

    val origState = evtStateIdx

    val startingMovesEnqueued = movesEnqueued

    curThread = Some(ScriptThread.fromEventEntity(
      game,
      game.mapScreen,
      game.mapScreen.scriptInterface,
      this,
      "%s/%d".format(mapEvent.name, evtStateIdx),
      evtState,
      evtStateIdx,
      onFinish = Some(() => {
        val movedDuringScript = movesEnqueued != startingMovesEnqueued
        if (activatorsDirection!= Directions.NONE && !movedDuringScript &&
            origState == evtStateIdx)
          dir = origDir
        curThread = None
      })))
    curThread.get.run()

    return curThread
  }

  override def eventTouchCallback(touchedNpcs: Iterable[EventEntity]) = {
    val activatedEvts =
      touchedNpcs.filter(e =>
        e.evtState.trigger == EventTrigger.EVENTTOUCH.id ||
          e.evtState.trigger == EventTrigger.ANYTOUCH.id)

    activatedEvts.foreach(_.activate(dir))
  }
}