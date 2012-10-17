package rpgboss.model

import rpgboss.lib._
import rpgboss.lib.FileHelper._

import net.liftweb.json.Serialization

import java.io._

case class RpgEvent()

/*
 * This class has mutable members.
 * 
 * See RpgMap object for an explanation of the data format.
 */
case class RpgMapData(botLayer: Array[Array[Byte]],
                      midLayer: Array[Array[Byte]],
                      topLayer: Array[Array[Byte]],
                      events: Array[RpgEvent])
{
  import RpgMapData._
  def drawOrder = List(botLayer, midLayer, topLayer)
  
  def toIntermediate = 
    RpgMapDataIntermediate(botLayer.map(_.map(_.toInt)),
                           midLayer.map(_.map(_.toInt)),
                           topLayer.map(_.map(_.toInt)),
                           events)
  
  def writeToFile(p: Project, name: String) =
    RpgMapData.dataFile(p, name).useWriter { writer =>
      implicit val formats = net.liftweb.json.DefaultFormats
      Serialization.writePretty(this.toIntermediate, writer) != null
    } getOrElse false
}

// Actually jsonable case class
case class RpgMapDataIntermediate(botLayer: Array[Array[Int]],
                                  midLayer: Array[Array[Int]],
                                  topLayer: Array[Array[Int]],
                                  events: Array[RpgEvent])

case object RpgMapData {
  def dataFile(p: Project, name: String) = 
    new File(p.mapsDir, "%s.mapdata.json".format(name))
  
  def readFromDisk(proj: Project, name: String) : Option[RpgMapData] = {
    implicit val formats = net.liftweb.json.DefaultFormats
    dataFile(proj, name).getReader().map { reader => 
      val intermediate = Serialization.read[RpgMapDataIntermediate](reader)
      RpgMapData(intermediate.botLayer.map(_.map(_.toByte)),
                 intermediate.midLayer.map(_.map(_.toByte)),
                 intermediate.topLayer.map(_.map(_.toByte)),
                 intermediate.events)
    }
  }
}
