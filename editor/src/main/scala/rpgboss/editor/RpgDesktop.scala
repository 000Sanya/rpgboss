package rpgboss.editor

import scala.swing._
import scala.swing.event._
import com.typesafe.scalalogging.slf4j.Logging
import java.lang.Thread.UncaughtExceptionHandler
import javax.swing.UIManager
import javax.imageio.ImageIO

object RpgDesktop
  extends SwingApplication
  with Logging
  with UncaughtExceptionHandler {

  def top() = new MainFrame {
    minimumSize = new Dimension(800, 600)
    title = "rpgboss beta"

    import javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE
    peer.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE)

    val mainPanel = new MainPanel(this)

    contents = mainPanel

    override def closeOperation() = {
      if (mainPanel.askSaveUnchanged()) {
        super.closeOperation()
      }
    }
  }

  def uncaughtException(thread: Thread, ex: Throwable) = {
    logger.error("Uncaught exception")
    ex.printStackTrace()
  }

  def handle(ex: Throwable) = {
    uncaughtException(Thread.currentThread(), ex)
  }

  override def startup(args: Array[String]) = {
    UIManager.setLookAndFeel(
      UIManager.getSystemLookAndFeelClassName());
    
    val iconStream = getClass.getClassLoader.getResourceAsStream("icon.png")
    val icon = ImageIO.read(iconStream)
    
    System.setProperty("sun.awt.exception.handler", "rpgboss.editor.EDTErrorHandler");
    Thread.setDefaultUncaughtExceptionHandler(this);

    // code adapted from SimpleSwingApplication.scala
    val t = top()
    t.peer.setIconImage(icon)
    if (t.size == new Dimension(0, 0)) t.pack()
    t.visible = true
  }
  
  override def main(args: Array[String]) {
    if (args.size >= 2 && args.head == "--player") {
      rpgboss.player.LwjglPlayer.main(args.tail)
    } else {
      super.main(args)
    }
  }
}

class EDTErrorHandler {
  /**
   * This method is invoked by the AWT event dispatch mechanism when an
   * unexpected exception or error is thrown during event dispatching.
   */
  def handle(t: Throwable) {
    t.printStackTrace()
  }
}

