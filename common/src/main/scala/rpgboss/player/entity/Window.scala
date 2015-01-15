package rpgboss.player.entity

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.Color
import java.awt._
import java.awt.image._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.Promise
import rpgboss.lib._
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.player._
import com.badlogic.gdx.utils.Disposable
import rpgboss.lib.GdxUtils.syncRun

object Window {
  val Opening = 0
  val Open = 1
  val Closing = 2
  val Closed = 3

  val Left = 0
  val Center = 1
  val Right = 2

  def maxWidth(lines: Array[String], fontbmp: BitmapFont, xPadding: Int) = {
    val linesWidth =
      if (lines.isEmpty) 0 else lines.map(fontbmp.getBounds(_).width).max
    linesWidth + 2 * xPadding
  }
}

/**
 * This class is created and may only be accessed on the main GDX thread, with
 * the exception of the getScriptInterface(), which may only be used from
 * a different, scripting thread.
 */
class Window(
  manager: WindowManager,
  inputs: InputMultiplexer,
  layout: Layout, invisible: Boolean = false)
  extends InputHandler with ThreadChecked with Disposable {

  def openCloseTime: Double = 0.25
  def initialState = if (openCloseTime > 0) Window.Opening else Window.Open

  private var _state = initialState
  // Determines when states expire. In seconds.
  protected var stateAge = 0.0

  private lazy val rect = {
    layout.getRect(100, 100, manager.screenW, manager.screenH)
  }

  protected def getRectFromLines(
      lines: Array[String], linesShown: Int, xPadding: Int) = {
    val autoW = Window.maxWidth(lines, manager.fontbmp, xPadding)
    val displayedLines = if (linesShown > 0) linesShown else lines.length
    val autoH = WindowText.DefaultLineHeight * displayedLines
    layout.getRect(autoW, autoH, manager.screenW, manager.screenH)
  }

  if (inputs != null)
    inputs.prepend(this)

  if (manager != null)
    manager.addWindow(this)

  /**
   * Accessed on multiple threads.
   */
  def state = synchronized {
    _state
  }

  def skin = manager.windowskin
  def skinRegion = manager.windowskinRegion

  private def changeState(newState: Int) = synchronized {
    assertOnBoundThread()
    _state = newState
    stateAge = 0.0
  }

  def update(delta: Float) = {
    assertOnBoundThread()
    stateAge += delta
    // change state of "expired" opening or closing animations
    if (stateAge >= openCloseTime) {
      state match {
        case Window.Opening => changeState(Window.Open)
        case Window.Open =>
        case Window.Closing => {
          changeState(Window.Closed)
        }
        case _ => Unit
      }
    }
  }

  def render(b: SpriteBatch): Unit = {
    assertOnBoundThread()

    if (invisible)
      return

    state match {
        case Window.Open => {
        skin.draw(b, skinRegion, rect.left, rect.top, rect.w, rect.h)
      }
      case Window.Opening => {
        val hVisible =
          math.max(32 + (stateAge / openCloseTime * (rect.h - 32)).toInt, 32)

        skin.draw(b, skinRegion, rect.left, rect.top, rect.w, hVisible)
      }
      case Window.Closing => {
        val hVisible = math.max(
          rect.h - (stateAge / openCloseTime * (rect.h - 32)).toInt, 32)

        skin.draw(b, skinRegion, rect.left, rect.top, rect.w, hVisible)
      }
      case _ => Unit
    }
  }

  def dispose() = {

  }

  def startClosing(): Unit = {
    assertOnBoundThread()

    // This method may be called multiple times, but the subsequent calls after
    // the first should be ignored.
    if (state != Window.Opening && state != Window.Open) {
      return
    }

    changeState(Window.Closing)

    // We allow scripts to continue as soon as the window is closing to provide
    // a snappier game.
    closePromise.success(0)
  }

  class WindowScriptInterface {
    def getState() = {
      assertOnDifferentThread()
      state
    }

    def close() = {
      assertOnDifferentThread()
      GdxUtils.syncRun {
        startClosing()
      }
      awaitClose()
    }

    def awaitClose() = {
      assertOnDifferentThread()
      Await.result(closePromise.future, Duration.Inf)
    }
  }

  lazy val scriptInterface = new WindowScriptInterface

  def removeFromWindowManagerAndInputs() = {
    assertOnBoundThread()
    assert(state == Window.Closed)

    if (inputs != null)
      inputs.remove(this)

    if (manager != null)
      manager.removeWindow(this)
  }

  // This is used to either convey a choice, or simply that the window
  // has been closed
  private val closePromise = Promise[Int]()
}

class DamageTextWindow(
  persistent: PersistentState,
  manager: WindowManager,
  damageString: String,
  initialX: Float, initialY: Float,
  delayTime: Float)
  // TODO: We pass 'null' as inputs here because we don't want to accept input.
  // Window has zeros for x, y, w, and h because the window itself is invisible.
  extends Window(manager, null, Layout.empty, invisible = true) {

  private val expiryTime = 0.8
  private val yDisplacement = -25.0

  private var age = -delayTime

  val textImage = new WindowText(
    persistent,
    initialText = Array(damageString),
    rect = Rect(initialX, initialY, 20, 20),
    fontbmp = manager.fontbmp,
    justification = Window.Center)

  override def update(delta: Float): Unit = {
    super.update(delta)

    if (state != Window.Open)
      return

    age += delta;

    if (age < 0)
      return

    textImage.updatePosition(
        initialX,
        ((age / expiryTime * yDisplacement) + initialY).toFloat)

    textImage.update(delta)

    if (age > expiryTime) {
      startClosing()
    }
  }

  override def render(b: SpriteBatch): Unit = {
    if (age < 0)
      return

    super.render(b)
    textImage.render(b)
  }
}

case class PrintingTextWindowOptions(
  timePerChar: Float = 0.02f,
  linesPerBlock: Int = 4,
  justification: Int = Window.Left,
  stayOpenTime: Float = 0,
  showArrow: Boolean = false)

class PrintingTextWindow(
  persistent: PersistentState,
  manager: WindowManager,
  inputs: InputMultiplexer,
  initialLines: Array[String] = Array(),
  layout: Layout,
  options: PrintingTextWindowOptions = PrintingTextWindowOptions())
  extends Window(manager, inputs, layout) {
  val xpad = 24
  val ypad = 24

  val rect = getRectFromLines(initialLines, options.linesPerBlock, xpad)
  val textImage = new PrintingWindowText(
    persistent,
    initialLines,
    rect.copy(w = rect.w - 2 * xpad, h = rect.h - 2 * ypad),
    skin,
    skinRegion,
    manager.fontbmp,
    options)

  override def keyDown(key: Int): Unit = {
    import MyKeys._
    if (state == Window.Closing || state == Window.Closed)
      return

    if (key == OK) {
      if (textImage.allTextPrinted)
        startClosing()
      else if(textImage.wholeBlockPrinted)
        textImage.advanceBlock()
      else
        textImage.speedThrough()
    }
  }

  override def update(delta: Float) = {
    super.update(delta)
    textImage.update(delta)

    if (options.stayOpenTime > 0.0 && state == Window.Open &&
        stateAge >= options.stayOpenTime) {
      startClosing()
    }
  }

  override def render(b: SpriteBatch) = {
    super.render(b)
    state match {
      case Window.Open => {
        textImage.render(b)
      }
      case Window.Opening => {
        textImage.render(b)
      }
      case _ => {
      }
    }
  }

  def updateLines(lines: Array[String]) = {
    assertOnBoundThread()
    textImage.updateText(lines)
  }

  class PrintingTextWindowScriptInterface extends WindowScriptInterface {
    def updateLines(lines: Array[String]) = syncRun {
      PrintingTextWindow.this.updateLines(lines)
    }
  }

  override lazy val scriptInterface = new PrintingTextWindowScriptInterface
}
