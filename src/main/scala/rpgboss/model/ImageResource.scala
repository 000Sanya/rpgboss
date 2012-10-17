package rpgboss.model

import rpgboss.lib._
import rpgboss.message.ModelSerialization._
import rpgboss.lib.FileHelper._

import java.io._
import java.awt.image._
import javax.imageio._

trait ImageResource[T] extends Resource[T] {
  def imgFile = new File(rcTypeDir, "%s.png".format(name))
  
  lazy val imageOpt = Option(ImageIO.read(imgFile))
}

object ImageResource {
  lazy val errorTile = { 
    val errImgStream = getClass.getClassLoader.getResourceAsStream("error.png")
    ImageIO.read(errImgStream)
  }
}
