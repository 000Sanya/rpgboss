package rpgboss.editor.dialog.db
import rpgboss.model.ProjectData
import rpgboss.editor.dialog.DatabaseDialog
import rpgboss.editor.lib.RightPaneArrayEditingPanel
import scala.swing._

trait DatabasePanel {
  def panelName: String
  def dbDiag: DatabaseDialog
  
  def activate(): Unit = {}
}

abstract class RightPaneArrayDatabasePanel[T](
    owner: Window,
    label: String,
    initialAry: Array[T])(implicit m: Manifest[T])
  extends RightPaneArrayEditingPanel[T](owner, label, initialAry)(m)
  with DatabasePanel 
{
  override def activate(): Unit = {
    listView.selectIndices(listView.selection.indices.head)
  }
}