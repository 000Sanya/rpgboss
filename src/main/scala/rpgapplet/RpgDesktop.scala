package rpgboss.rpgapplet

import scala.swing._
import scala.swing.event._


object RpgDesktop extends SimpleSwingApplication {
  
  def top = new MainFrame {
    minimumSize = new Dimension(800, 600)
    title = "RPG Desktop version"
    contents = new MainPanel("tester", 0, "tester/rcs/tileset/testar")
  }
}

