package rpgboss.editor

import rpgboss.editor.lib._
import rpgboss.editor.tileset._

import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.message._

class ProjectPanel(mainP: MainPanel, sm: StateMaster)
  extends SplitPane(Orientation.Vertical) with SelectsMap
{  
  val tileSelector = new TabbedTileSelector(sm)
  val mapSelector = new MapSelector(sm, this)
  val mapView = new MapView(sm, tileSelector)
  
  val projMenu = new PopupMenu {
    contents += new MenuItem(mainP.actionNew)
    contents += new MenuItem(mainP.actionOpen)
    contents += new MenuItem(mainP.actionSave)
	}
  
  val menuAndSelector = new BoxPanel(Orientation.Vertical) {
    contents += new BoxPanel(Orientation.Horizontal) {
      contents += new Button {
        val btn = this
        action = Action("Project \u25BC") {
          projMenu.show(btn, 0, btn.bounds.height)
        }
      }
    }
    contents += tileSelector
  }
  
  topComponent =
    new SplitPane(Orientation.Horizontal, menuAndSelector, mapSelector)
  bottomComponent = mapView
  enabled = false
  
  def selectMap(mapOpt: Option[RpgMap]) = {
    List(sm, tileSelector, mapView).map(_.selectMap(mapOpt))
  }
  
  // select most recent or first map if not empty
  selectMap({
    if(!sm.maps.isEmpty) {
      val idToLoad =
        if(sm.maps.contains(sm.proj.recentMapId))
          sm.proj.recentMapId
        else
          sm.maps.keys.min
      
      sm.maps.get(idToLoad).map(_.mapMeta)
    }
    else None
  })
  
  mainP.revalidate()
}

