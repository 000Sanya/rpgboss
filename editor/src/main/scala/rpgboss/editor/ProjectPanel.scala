package rpgboss.editor

import rpgboss.editor.lib._
import rpgboss.editor.tileset._

import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.model.resource._

class ProjectPanel(val mainP: MainPanel, sm: StateMaster)
  extends SplitPane(Orientation.Vertical) with SelectsMap
{  
  val tileSelector = new TabbedTileSelector(sm)
  val mapSelector = new MapSelector(sm, this)
  val mapView = new MapView(this, sm, tileSelector)
  
  val projMenu = new PopupMenu {
    contents += new MenuItem(mainP.actionNew)
    contents += new MenuItem(mainP.actionOpen)
    contents += new MenuItem(mainP.actionSave)
	}
  
  val menuAndSelector = new BoxPanel(Orientation.Vertical) {
    import rpgboss.editor.dialog._
    
    contents += new BoxPanel(Orientation.Horizontal) {
      contents += new Button {
        val btn = this
        action = Action("RPG \u25BC") {
          projMenu.show(btn, 0, btn.bounds.height)
        }
      }
      contents += new Button(Action("DB...") {
        val d = new DatabaseDialog(mainP.topWin, sm)
        d.open()
      })
      contents += new Button(Action("Resources...") {
        val d = new ResourcesDialog(mainP.topWin, sm)
        d.open()
      })
    }
    contents += tileSelector
  }
  
  topComponent =
    new SplitPane(Orientation.Horizontal, menuAndSelector, mapSelector)
  bottomComponent = mapView
  enabled = false
  
  def selectMap(mapOpt: Option[RpgMap]) = {
    List(tileSelector, mapView).map(_.selectMap(mapOpt))
  }
  
  // select most recent or first map if not empty
  selectMap({
    val mapStates = sm.getMapStates
    if(!mapStates.isEmpty) {
      val idToLoad =
        if(mapStates.contains(sm.getProj.data.recentMapName))
          sm.getProj.data.recentMapName
        else
          mapStates.keys.min
      
      mapStates.get(idToLoad).map(_.map)
    }
    else None
  })
  
  mainP.revalidate()
}

