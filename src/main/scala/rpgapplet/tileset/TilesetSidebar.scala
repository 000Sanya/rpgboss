package rpgboss.rpgapplet.tileset

import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.message._

import rpgboss.rpgapplet._

import java.awt.image.BufferedImage

class TilesetSidebar(sm: StateMaster)
extends BoxPanel(Orientation.Horizontal)
{
  val thisSidebar = this
  
  def defaultTileCodes = Array(Array(
    (RpgMap.autotileByte, 0.asInstanceOf[Byte], 0.asInstanceOf[Byte])))
  
  // This var must always have at least be 1x1.
  // array of row vectors, so selectedTileCodes(y)(x)
  var selectedTileCodes : Array[Array[(Byte, Byte, Byte)]] = defaultTileCodes
  
  def selectMap(map: RpgMap) = {
    val tilesetsPane = new TabbedPane() {
      tabPlacement(Alignment.Bottom)
      
      val autotileSel = new AutotileSelector(sm.proj, TilesetSidebar.this)
      pages += new TabbedPane.Page("Autotiles", autotileSel)
      
      map.tilesets.zipWithIndex.map({
        case (tsName, i) => 
          val tileset = Tileset.readFromDisk(sm.proj, tsName)
          val tabComponent = tileset.imageOpt.map { img =>
            new ImageTileSelector(img, tXYArray =>
              selectedTileCodes = tXYArray.map(_.map({
                case (xTile, yTile) => 
                  (i.asInstanceOf[Byte], xTile, yTile)
              }))
            )
          } getOrElse new Label("No image")
          
          pages += new TabbedPane.Page(tsName, tabComponent)
      })
      
      // select first Autotile code
      selectedTileCodes = defaultTileCodes
    }
    
    setContent(Some(tilesetsPane))
  }
  
  def setContent(cOpt: Option[Component]) = {
    contents.clear()
    cOpt map { contents += _ }
    revalidate()
  }
}

