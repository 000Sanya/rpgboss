package rpgboss.editor.dialog

import rpgboss.editor.lib._
import scala.swing._
import scala.swing.event._

import rpgboss.model._
import rpgboss.model.resource._

import net.java.dev.designgridlayout._

import java.io._

import rpgboss.lib.FileHelper._

class NewProjectDialog(owner: Window, onSuccess: Project => Any) 
  extends StdDialog(owner, "New Project")
{  
  val rootChooser = Paths.getRootChooserPanel(() => Unit)
  
  val shortnameField = new TextField() {
    columns = 12
  }
  
  val gameTitleField = new TextField() {
    columns = 20
  }
  
  def okFunc() = {
    if(shortnameField.text.isEmpty)
      Dialog.showMessage(shortnameField, "Need a short name.")
    else {    
      val shortname = shortnameField.text
      val p = Project.startingProject(gameTitleField.text,
                                      new File(rootChooser.getRoot, shortname))
      
      val m = RpgMap.defaultInstance(p, RpgMap.generateName(0))
      
      val allSavedOkay = 
        p.writeMetadata() &&
        m.writeMetadata() &&
        m.saveMapData(RpgMap.defaultMapData)
      
      val cl = getClass.getClassLoader
        
      val copiedAllResources = {
        val projRcDir = p.rcDir
        
        val resources = 
          io.Source.fromInputStream(
            cl.getResourceAsStream("defaultrc/enumerated.txt")
          ).getLines().toList
        
        for(resourceName <- resources) {
          
          val target = new File(projRcDir, resourceName)
          
          target.getParentFile.mkdirs()
          
          val fos = new FileOutputStream(target)
          
          val buffer = new Array[Byte](1024*32)
          
          val sourceStr =    
            cl.getResourceAsStream("defaultrc/%s".format(resourceName))
          
          Iterator.continually(sourceStr.read(buffer))
            .takeWhile(_ != -1).foreach(fos.write(buffer, 0, _))
        }
        
        true
      }
      
      if(allSavedOkay && copiedAllResources) {
        onSuccess(p)
        close()
      }
      else 
        Dialog.showMessage(okButton, "File write error", "Error", 
                           Dialog.Message.Error)
    }
  }
  
  
  contents = new DesignGridPanel {
    
    row().grid().add(leftLabel("Directory for all projects:"))
    row().grid().add(rootChooser)
    
    row().grid().add(leftLabel("Project shortname (ex. 'chronotrigger'):"))
    row().grid().add(shortnameField)
    
    row().grid().add(leftLabel("Game title (ex: 'Chrono Trigger'):"))
    row().grid().add(gameTitleField)
    
    addButtons(cancelButton, okButton)
    
    shortnameField.requestFocus()
  }
}