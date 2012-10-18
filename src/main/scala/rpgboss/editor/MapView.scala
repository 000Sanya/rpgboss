package rpgboss.editor

import rpgboss.lib._
import rpgboss.cache._
import rpgboss.model._
import rpgboss.model.resource._
import rpgboss.editor.tileset._
import rpgboss.editor.lib._
import rpgboss.editor.lib.GraphicsUtils._
import com.weiglewilczek.slf4s.Logging
import scala.math._
import scala.swing._
import scala.swing.event._
import javax.imageio._
import java.awt.{BasicStroke, AlphaComposite, Color}
import java.awt.geom.Line2D
import java.awt.event.MouseEvent
import rpgboss.model.event.RpgEvent
import rpgboss.editor.dialog.EventDialog
import com.google.common.cache._
import java.awt.image.BufferedImage

class MapView(
    projectPanel: ProjectPanel, 
    sm: StateMaster, 
    tileSelector: TabbedTileSelector)
extends BoxPanel(Orientation.Vertical) with SelectsMap with Logging
{
  //--- VARIABLES ---//
  var viewStateOpt : Option[MapViewState] = None
  var curTilesize = Tileset.tilesize
  
  //--- EVT IMG CACHE ---//
  val evtImgCache = CacheBuilder.newBuilder()
    .concurrencyLevel(1)
    .softValues()
    .maximumSize(50)
    .expireAfterWrite(10, java.util.concurrent.TimeUnit.MINUTES)
    .build(new CacheLoader[SpriteSpec, BufferedImage] {
      def load(spriteSpec: SpriteSpec) = {
        val spriteset = Spriteset.readFromDisk(sm.getProj, spriteSpec.spriteset)
        val srcImg = spriteset.srcTileImg(spriteSpec)
        
        val dstSz = Tileset.tilesize-4-1
        
        val dstImg = new BufferedImage(
            dstSz, dstSz, BufferedImage.TYPE_4BYTE_ABGR) 
        
        val g = dstImg.getGraphics()
        
        val sx1 = (srcImg.getWidth()-dstSz)/2
        val sy1 = 10
        g.drawImage(srcImg,
            0, 0,
            dstSz-1, dstSz-1,
            sx1, sy1,
            sx1+dstSz-1, sy1+dstSz-1,
            null)
        
        dstImg
      }
    })
  
  //--- CONVENIENCE DEFS ---//
  def selectedLayer = MapLayers.selected
  
  //--- BUTTONS ---//
  def scaleButton(title: String, invScale: Int) = new RadioButton() { 
    action = Action(title) {
      curTilesize = Tileset.tilesize / invScale
      resizeRevalidateRepaint()
    }
  }
  
  val s11Btn = scaleButton("1/1", 1)
  val s12Btn = scaleButton("1/2", 2)
  val s14Btn = scaleButton("1/4", 4)
  
  //--- WIDGETS --//
  val toolbar = new BoxPanel(Orientation.Horizontal) {
    minimumSize = new Dimension(200, 32)
    def addBtnsAsGrp(btns: List[RadioButton]) = {
      val grp = new ButtonGroup(btns : _*)
      grp.select(btns.head)
      
      contents ++= btns
    }
    
    def enumButtons[T](enum: ListedEnum[T]) = 
      enum.valueList.map { eVal =>
        new RadioButton() {
          action = Action(eVal.toString) { 
            enum.selected = eVal 
            resizeRevalidateRepaint()
          }
        }
      }
    
    addBtnsAsGrp(enumButtons(MapLayers))
    addBtnsAsGrp(enumButtons(MapViewTools))
    addBtnsAsGrp(List(s11Btn, s12Btn, s14Btn))
    
    contents += Swing.HGlue
  }
  
  val canvasPanel = new Panel() {
    var cursorSquare: TileRect = TileRect.empty
    var eventCursor: TileRect = TileRect.empty
    var selectedEvtIdx: Option[Int] = None
    
    background = Color.WHITE
    
    override def paintComponent(g: Graphics2D) =
    {
      super.paintComponent(g)
      
      viewStateOpt.map(vs => {
        
        val bounds = g.getClipBounds
        val tilesize = curTilesize
        
        val minX = bounds.getMinX/tilesize
        val maxX = bounds.getMaxX/tilesize
        val minY = bounds.getMinY/tilesize
        val maxY = bounds.getMaxY/tilesize
        
        val minXTile = minX.toInt
        val minYTile = minY.toInt
        
        val maxXTile = min(vs.mapMeta.xSize-1, maxX.toInt)
        val maxYTile = min(vs.mapMeta.ySize-1, maxY.toInt)
        
        logger.info("Paint Tiles: x: [%d,%d], y: [%d,%d]".format(
          minXTile, maxXTile, minYTile, maxYTile))
          
        // draw tiles
        import MapLayers._
        enumDrawOrder(vs.nextMapData).map {
          case(curLayer, layerAry) => 
            if(selectedLayer != Evt && selectedLayer != curLayer)
              g.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, 0.5f))
            else
              g.setComposite(AlphaComposite.SrcOver)
            
            for(xTile <- minXTile to maxXTile; yTile <- minYTile to maxYTile) {
              if(layerAry(yTile)(xTile*RpgMap.bytesPerTile) != -1) {
                val tileImg = 
                  vs.tilecache.getTileImage(layerAry, xTile, yTile, 0)
                
                g.drawImage(tileImg, 
                            xTile*tilesize, yTile*tilesize,
                            (xTile+1)*tilesize, (yTile+1)*tilesize,
                            0, 0, Tileset.tilesize, Tileset.tilesize,
                            null)
              }
            }   
        }
        
        // draw start loc
        val startingLoc = sm.getProj.data.startingLoc
        if(startingLoc.map == vs.mapName &&
           startingLoc.x >= minXTile && startingLoc.x <= maxXTile &&
           startingLoc.y >= minYTile && startingLoc.y <= maxYTile) {
          g.setComposite(AlphaComposite.SrcOver)
          g.drawImage(MapView.startLocTile,
            (startingLoc.x*tilesize).toInt-MapView.startLocTile.getWidth()/2, 
            (startingLoc.y*tilesize).toInt-MapView.startLocTile.getHeight()/2, 
            null, null) 
        }
        
        // draw all other events
        val evtFill = new Color(0.5f, 0.5f, 0.5f, 0.5f)
        if(selectedLayer != Evt)
          g.setComposite(AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER, 0.5f))
        else
          g.setComposite(AlphaComposite.SrcOver)
        g.setStroke(new BasicStroke())
        vs.nextMapData.events.foreach( rpgevt =>
          // If within view frame
          if( rpgevt.x > minX && rpgevt.x < maxX &&
              rpgevt.y > minY && rpgevt.y < maxY) {
            g.setColor(Color.WHITE)
            g.drawRect(
                ((rpgevt.x-0.5)*tilesize+2).toInt,
                ((rpgevt.y-0.5)*tilesize+2).toInt,
                tilesize-4-1,
                tilesize-4-1)
            
            val dx1 = ((rpgevt.x-0.5)*tilesize+2).toInt+1
            val dy1 = ((rpgevt.y-0.5)*tilesize+2).toInt+1
            rpgevt.states.head.sprite.map { spriteSpec =>
              g.drawImage(
                  evtImgCache.get(spriteSpec), 
                  dx1, dy1, evtFill, null)
            } getOrElse {
              g.setColor(evtFill)
              g.fillRect(dx1, dy1, Tileset.tilesize-4-1, Tileset.tilesize-4-1)
            }
            
          }
        )
        
        if(selectedLayer == Evt) {
          // draw grid if on evt layer
          g.setComposite(AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER, 0.5f))
          g.setStroke(new BasicStroke(2.0f))
          g.setColor(new Color(0f, 0f, 0f, 0.5f))
          for(xTile <- minXTile to maxXTile+1) {
            g.draw(new Line2D.Double(xTile*tilesize, minYTile*tilesize,
                                     xTile*tilesize, (maxYTile+1)*tilesize))
          }
          for(yTile <- minYTile to maxYTile+1) {
            g.draw(new Line2D.Double(minXTile*tilesize, yTile*tilesize,
                                     (maxXTile+1)*tilesize, yTile*tilesize))
          }
          
          // draw selection square
          eventCursor.optionallyDrawSelRect(g, tilesize, tilesize)
          
        } else {        
          // otherwise draw selection square
          cursorSquare.optionallyDrawSelRect(g, curTilesize, curTilesize)
        }
      })
    }
  }
  
  //--- ADDING WIDGETS ---//
  contents += toolbar
  contents += new ScrollPane(canvasPanel) {
    horizontalScrollBarPolicy = ScrollPane.BarPolicy.Always
    verticalScrollBarPolicy = ScrollPane.BarPolicy.Always
  }
  
  //--- MISC FUNCTIONS ---//
  def toTileCoords(p: Point) = 
    (p.getX.toInt/curTilesize, p.getY.toInt/curTilesize) 
  
  def resizeRevalidateRepaint() = {
    canvasPanel.preferredSize = viewStateOpt.map { vs =>
      new Dimension(vs.mapMeta.xSize*curTilesize,
                    vs.mapMeta.ySize*curTilesize)
    } getOrElse {
      new Dimension(0,0)
    }
    
    canvasPanel.revalidate()
    canvasPanel.repaint()
  }
  
  def selectMap(mapOpt: Option[RpgMap]) = {
    viewStateOpt = mapOpt map { mapMeta =>
      val tc = new TileCache(sm.getProj, sm.getAutotiles, mapMeta)
      new MapViewState(sm, mapMeta.name, tc)
    }
      
    resizeRevalidateRepaint()
  }
  
  // Updates cursor square, and queues up any appropriate repaints
  def updateCursorSq(visible: Boolean, x: Int = 0, y: Int = 0) = 
  {
    val oldSq = canvasPanel.cursorSquare
    canvasPanel.cursorSquare = if(visible) {
      val tCodes = tileSelector.selectedTileCodes 
      assert(tCodes.length > 0 && tCodes(0).length > 0, "Selected tiles empty")
      TileRect(x, y, tCodes(0).length, tCodes.length)
    } else TileRect.empty
    
    // if updated, redraw. otherwise, don't redraw
    if(oldSq != canvasPanel.cursorSquare) {
      repaintRegion(oldSq)
      repaintRegion(canvasPanel.cursorSquare)
    }
  }
  
  //--- EVENT POPUP MENU ---//
  import MapLayers._
  def editEvent() = viewStateOpt map { vs =>
    val isNewEvent = canvasPanel.selectedEvtIdx.isEmpty
    
    /**
    * Brings up dialog to create new or edit event at the selected event tile
    */
    vs.begin()
      
    val event = canvasPanel.selectedEvtIdx.map { idx =>
      vs.nextMapData.events(idx)
    } getOrElse {
      vs.nextMapData = vs.nextMapData.copy(
          lastGeneratedEventId = vs.nextMapData.lastGeneratedEventId + 1)
      // Need the +0.5f to offset into center of selected tile 
      RpgEvent.blank(
          vs.nextMapData.lastGeneratedEventId, 
          canvasPanel.eventCursor.x1+0.5f, 
          canvasPanel.eventCursor.y1+0.5f)
    }
    
    val dialog = new EventDialog(
        projectPanel.mainP.topWin, 
        sm.getProj,
        event,
        onOk = { e: RpgEvent =>
          if(isNewEvent) 
            vs.nextMapData = vs.nextMapData.copy(
                events = vs.nextMapData.events ++ Array(e))
          else
            vs.nextMapData.events.update(canvasPanel.selectedEvtIdx.get, e)
          
          vs.commit()
          repaintRegion(TileRect(e.x.toInt, e.y.toInt))
        },
        onCancel = { e: RpgEvent =>
          vs.abort()
        })
    
    dialog.open()
  }
  
  def deleteEvent() = viewStateOpt map { vs =>
    canvasPanel.selectedEvtIdx map { evtIdx =>
      vs.begin()
      // Deletion of events is expensive, but hopefully, not too common
      val oldEvents = vs.nextMapData.events
      val deletedEvt = oldEvents(evtIdx)
      val newEvents = new Array[RpgEvent](oldEvents.size-1)
      // Copy over every item except the deleted item
      for(i <- 0 until evtIdx) {
        newEvents.update(i, oldEvents(i))
      }
      for(i <- evtIdx+1 until oldEvents.size) {
        newEvents.update(i-1, oldEvents(i))
      }
      vs.nextMapData = vs.nextMapData.copy(events = newEvents)
      vs.commit()

      // Repaint deleted event region
      repaintRegion(canvasPanel.eventCursor)
      // Delete the cached selected event id
      canvasPanel.selectedEvtIdx = None
    }
  }
  
  def evtPopupMenu(): Option[PopupMenu] = viewStateOpt map { vs =>
    val isNewEvent = canvasPanel.selectedEvtIdx.isEmpty
    val newEditText = if(isNewEvent) "New event" else "Edit event"
    
    val menu = new PopupMenu {
      contents += new MenuItem(Action(newEditText) { editEvent() })
      contents += new MenuItem(Action("Delete") { deleteEvent() })
    }
    
    menu
  }
  
  //--- REACTIONS ---//
  listenTo(canvasPanel.mouse.clicks, canvasPanel.mouse.moves)
  
  def repaintRegion(r1: TileRect) =
    canvasPanel.repaint(r1.rect(curTilesize, curTilesize))
  
  reactions += {
    /**
     * Three reactions in the case that the selectedLayer is not the Evt layer
     */
    case MouseMoved(`canvasPanel`, p, _) if selectedLayer != Evt => {
      val (tileX, tileY) = toTileCoords(p)
      updateCursorSq(true, tileX, tileY)
    }
    case MouseExited(`canvasPanel`, _, _) if selectedLayer != Evt =>
      updateCursorSq(false)
    case MousePressed(`canvasPanel`, point, _, _, _) if selectedLayer != Evt => 
      viewStateOpt map { vs => 
        vs.begin()
        
        val tCodes = tileSelector.selectedTileCodes
        val tool = MapViewTools.selected
        val (x1, y1) = toTileCoords(point)
                
        updateCursorSq(tool.selectionSqOnDrag, x1, y1)
        repaintRegion(MapViewTools.selected.onMousePressed(vs, tCodes, x1, y1))
        
        var (xLastDrag, yLastDrag) = (-1, -1) // init to impossible value
          
        lazy val temporaryReactions : PartialFunction[Event, Unit] = { 
          case MouseDragged(`canvasPanel`, point, _) => {
            val (x2, y2) = toTileCoords(point)
            
            // only redo action if dragged to a different square
            if( (x2, y2) != (xLastDrag, yLastDrag) ) {
              updateCursorSq(tool.selectionSqOnDrag, x2, y2)
              repaintRegion(MapViewTools.selected.onMouseDragged(
                  vs, tCodes, x1, y1, x2, y2))
              
              xLastDrag = x2
              yLastDrag = y2
            }
          }
          case MouseReleased(`canvasPanel`, point, _, _, _) => {
            val (x2, y2) = toTileCoords(point)
            vs.commit()
            
            updateCursorSq(true, x2, y2)
            
            reactions -= temporaryReactions
          }
        }
        
        reactions += temporaryReactions
      }
    case e: MousePressed if e.source == canvasPanel && selectedLayer == Evt =>
      logger.info("MousePressed on EvtLayer")
      viewStateOpt map { vs => 
        val (x1, y1) = toTileCoords(e.point)
        
        val oldEvtCursor = canvasPanel.eventCursor
        canvasPanel.eventCursor = TileRect(x1, y1)
        
        // Updated the selected event idx
        val existingEventIdx = 
          vs.nextMapData.events.indexWhere(e => 
            e.x.toInt == canvasPanel.eventCursor.x1 && 
            e.y.toInt == canvasPanel.eventCursor.y1)
      
        if(existingEventIdx == -1) {
          canvasPanel.selectedEvtIdx = None
        } else {
          canvasPanel.selectedEvtIdx = Some(existingEventIdx)
        }
        
        repaintRegion(oldEvtCursor)
        repaintRegion(canvasPanel.eventCursor)
        
        e.peer.getButton() match {
          // Popup menu
          case MouseEvent.BUTTON3 =>
            evtPopupMenu().map { _.show(canvasPanel, e.point.x, e.point.y) }
          
          // Code for the drag moving of events
          case MouseEvent.BUTTON1 if canvasPanel.selectedEvtIdx.isDefined =>
            val evtIdx = canvasPanel.selectedEvtIdx.get
            var (xLastDrag, yLastDrag) = (-1, -1) // init to impossible value
            
            vs.begin()
            
            lazy val temporaryReactions : PartialFunction[Event, Unit] = { 
              case MouseDragged(`canvasPanel`, point, _) => {
                val (x2, y2) = toTileCoords(point)
                
                // only redo action if dragged to a different square
                if( (x2, y2) != (xLastDrag, yLastDrag) ) {
                  val oldCursor = canvasPanel.eventCursor
                  canvasPanel.eventCursor = TileRect(x2, y2)
                  repaintRegion(oldCursor)
                  repaintRegion(canvasPanel.eventCursor)
                  val evt = vs.nextMapData.events(evtIdx)
                  vs.nextMapData.events.update(evtIdx, 
                      evt.copy(x = x2+0.5f, y = y2+0.5f))
                                    
                  xLastDrag = x2
                  yLastDrag = y2
                }
              }
              case MouseReleased(`canvasPanel`, point, _, _, _) => {
                val (x2, y2) = toTileCoords(point)
                vs.commit()
                reactions -= temporaryReactions
              }
            }
            
            reactions += temporaryReactions
          case _ => None
        }
      }
    case e: MouseClicked if e.source == canvasPanel && selectedLayer == Evt =>
      logger.info("MouseClicked on EvtLayer")
      if(e.clicks == 2) {
        editEvent()
      }
  }
}

object MapView {
  lazy val startLocTile = ImageIO.read(
    getClass.getClassLoader.getResourceAsStream("player_play.png"))
}
