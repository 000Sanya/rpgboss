package rpgboss.editor.dialog.cmd

import scala.swing._
import rpgboss.model.event._
import rpgboss.editor.lib.SwingUtils._
import rpgboss.editor.dialog.StdDialog
import rpgboss.editor.lib.DesignGridPanel

class ShowTextCmdDialog(
    owner: Window, 
    initial: ShowText, 
    successF: (ShowText) => Any) 
  extends StdDialog (owner, "Show text")
{
  
  val textEdit = new TextArea(initial.lines.mkString("\n"))
  
  def okFunc() = {
    successF(ShowText(textEdit.text.split("\n")))
    close()
  }
  
  contents = new DesignGridPanel {
    row().grid().add(leftLabel("Text:"))
    row().grid().add(textEdit)
    
    addButtons(cancelBtn, okBtn)
  }
  
}