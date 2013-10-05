package rpgboss.player

import org.mozilla.javascript.{ Context, ScriptableObject, Scriptable }
import rpgboss.model.resource.Script
import scala.concurrent.ops.spawn
import rpgboss.model.MapLoc
import java.lang.Thread.UncaughtExceptionHandler
import rpgboss.model.event.RpgEvent
import rpgboss.model.Constants._
import rpgboss.player.entity.EventEntity
import scala.concurrent.Promise
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * Thread used to run a javascript script...
 *
 * @param   game                  MyGame instance
 * @param   scriptName            Name of script used for debugging and logging
 * @param   scriptBody            Body of script. Should be broken into lines.
 * @param   fnToRun               Javascript to run after scriptBody is done.
 * @param   onFinishSyncCallback  Function to run on gdx thread after script end
 *
 */
class ScriptThread(
  game: MyGame,
  scriptName: String,
  scriptBody: String,
  fnToRun: String = "",
  onFinish: Option[() => Unit] = None,
  onFinishOnScriptThread: Boolean = false)
  extends UncaughtExceptionHandler {
  def initScope(jsScope: ScriptableObject): Any = {
    val jsInterface = game.state

    ScriptableObject.putProperty(jsScope, "game",
      Context.javaToJS(jsInterface, jsScope))
    ScriptableObject.putProperty(jsScope, "project",
      Context.javaToJS(game.project, jsScope))

    ScriptableObject.putProperty(jsScope, "out",
      Context.javaToJS(System.out, jsScope))

    // Some models to be imported
    ScriptableObject.putProperty(jsScope, "MapLoc",
      Context.javaToJS(MapLoc, jsScope))
    ScriptableObject.putProperty(jsScope, "Transitions",
      Context.javaToJS(Transitions, jsScope))
    
    ScriptableObject.putProperty(jsScope, "None",
      Context.javaToJS(None, jsScope))    
  }

  val runnable = new Runnable() {
    override def run() = {
      Thread.setDefaultUncaughtExceptionHandler(ScriptThread.this)

      val jsContext = Context.enter()
      val jsScope = jsContext.initStandardObjects()

      initScope(jsScope)

      val globalScript = Script.readFromDisk(game.project, "globals.js")
      
      jsContext.evaluateString(
        jsScope,
        globalScript.getAsString,
        globalScript.name,
        1, null)
      
      jsContext.evaluateString(
        jsScope,
        scriptBody,
        scriptName,
        1, null)

      if (!fnToRun.isEmpty) {
        jsContext.evaluateString(
          jsScope,
          fnToRun,
          fnToRun,
          1, null)
      }

      onFinish.map { f =>
        if (onFinishOnScriptThread) {
          f()
        } else {
          game.syncRun {
            f()
          }
        }
      }
      
      finishPromise.success(0)
    }
  }

  private val finishPromise = Promise[Int]()
  val thread = new Thread(runnable)

  def run() = {
    thread.start()
  }

  def isFinished = finishPromise.isCompleted

  def awaitFinish() = {
    Await.result(finishPromise.future, Duration.Inf)
  }

  def uncaughtException(thread: Thread, ex: Throwable) = {
    ex match {
      case e: org.mozilla.javascript.EcmaError => {
        System.err.println(e.getErrorMessage())
        System.err.println("%s:%d".format(e.sourceName(), e.lineNumber()))
      }
      case e => e.printStackTrace()
    }
  }
}

object ScriptThread {
  def fromFile(
    game: MyGame,
    scriptName: String,
    fnToRun: String = "",
    onFinish: Option[() => Unit] = None,
    onFinishOnScriptThread: Boolean = false) = {
    val script = Script.readFromDisk(game.project, scriptName)
    new ScriptThread(
      game,
      script.name,
      script.getAsString,
      fnToRun,
      onFinish,
      onFinishOnScriptThread)
  }

  def fromEventEntity(
    game: MyGame,
    entity: EventEntity,
    state: Int,
    onFinish: Option[() => Unit] = None,
    onFinishOnScriptThread: Boolean = false) = {
    val scriptName = "%s/%d".format(entity.mapEvent.name, state)
    val scriptBody = 
      entity.mapEvent.states(state).cmds.flatMap(_.toJs()).mkString("\n");
    new ScriptThread(
      game,
      scriptName,
      scriptBody,
      "",
      onFinish,
      onFinishOnScriptThread) {
      override def initScope(jsScope: ScriptableObject) = {
        super.initScope(jsScope)

        // Bind 'event' to the EventEntity so that we can control its movement
        ScriptableObject.putProperty(jsScope, "event",
          Context.javaToJS(entity, jsScope))
      }
    }
  }

}