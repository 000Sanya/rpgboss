package rpgboss.editor.uibase

import com.badlogic.gdx.backends.lwjgl._
import com.badlogic.gdx._
import scala.swing._
import scala.swing.event._
import com.typesafe.scalalogging.slf4j.Logging
import com.badlogic.gdx.utils.Disposable

class GdxPanel(canvasW: Int = 10, canvasH: Int = 10) 
  extends Component 
  with Logging
  with Disposable {
  override lazy val peer = new javax.swing.JComponent with SuperMixin {
    add(gdxCanvas.getCanvas())
  }
  
  lazy val gdxListener = new ApplicationAdapter with Logging {
    override def create() = {
      logger.debug("create()")
    }
    override def dispose() = {
      logger.debug("dispose()")
    }
    override def pause() = {
      logger.debug("pause()")
    }
    override def render() = {
//      logger.debug("render() %d".format(this.hashCode()))
    }
    override def resize(w: Int, h: Int) = {
      logger.debug("resize(%d, %d)".format(w, h))
    }
    override def resume() = {
      logger.debug("resume()")
    }
  }
  
  lazy val gdxCanvas = new LwjglAWTCanvas(gdxListener, false) with Logging{
    override def start() = {
      logger.debug("start()")
      super.start()
    }
    
    override def resize(w: Int, h: Int) = {
      logger.debug("resize(%d, %d)".format(w, h))
      super.resize(w, h)
    }
    
    override def stopped() = {
      logger.debug("stopped()")
      super.stopped()
    }
    
    getCanvas().setSize(canvasW, canvasH)
  }
  
  def dispose() = {
    gdxCanvas.stop()
  }
  
  def getAudio() = gdxCanvas.getAudio()
}