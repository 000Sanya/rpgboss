package rpgboss.editor.coop

import java.awt.Color
import scala.swing._
import scala.collection.mutable._

import javax.websocket._
import scala.collection.JavaConversions._

import org.glassfish.tyrus._
import org.glassfish.tyrus.core._

import java.io._
import java.net._
import java.util.concurrent._
import org.glassfish.tyrus.client._
import rpgboss.editor.Internationalized._ 

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import event.{KeyPressed, Key}

object chatArea extends TextArea(rows = 20, columns = 20) {
    editable = false
    background = Color.WHITE
}

class Chat extends MainFrame {
  val la = new Label(getMessage("RPGBoss_Global_Chat"))

  la.foreground = Color.BLUE
  title = getMessage("RPGBoss_Global_Chat")

  minimumSize = new Dimension(800, 300)
  resizable =false 
  centerOnScreen()
  open()

  val textfield:TextField = new TextField("")
  val username_textfield:TextField = new TextField("")

  var messageLatch: CountDownLatch = _

  var SocketSession:Session = null

  new Thread(new Runnable() {
    override def run() {
      try {
        messageLatch = new CountDownLatch(1)
        val cec = ClientEndpointConfig.Builder.create().build()
        val client = ClientManager.createClient()
        client.connectToServer(new Endpoint() {

          override def onOpen(session: Session, config: EndpointConfig) {
            try {
              SocketSession = session
              session.addMessageHandler(new MessageHandler.Whole[String]() {

                override def onMessage(message: String) {
                  println(getMessageColon("Received_Message") + message)
                  chatArea.text += message
                  messageLatch.countDown()
                }
              })
              //session.getBasicRemote.sendText("me<>get-users")
            } catch {
              case e: IOException => e.printStackTrace()
            }
          }
        //}, cec, new URI("ws://localhost:8080/"))
        }, cec, new URI("ws://rpgboss.hendrikweiler.com:8080/"))
        messageLatch.await(100, TimeUnit.SECONDS)
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
  }).start();



  contents = new BoxPanel(Orientation.Vertical) {
    contents += la
    contents += Swing.VStrut(10)
    contents += Swing.Glue
    contents += new ScrollPane(chatArea)
    contents += Swing.VStrut(1)
    contents += new Label(getMessage("Your_Username"))
    contents += username_textfield
    contents += Swing.VStrut(1)
    contents += textfield
    contents += Swing.VStrut(1)
    contents += new BoxPanel(Orientation.Horizontal) {
      contents += Button(getMessage("Send")) { sendText() }
      contents += Swing.HStrut(5)
      contents += Button(getMessage("Hide")) { hideMe() }
      contents += Swing.HStrut(5)
      contents += Button(getMessage("How_Much_Users")) { showUsers() }
    }
    border = Swing.EmptyBorder(10, 10, 10, 10)
  }

  def sendText() = {
    if(SocketSession!=null) {
      val today = Calendar.getInstance().getTime()
      var minuteFormat = new SimpleDateFormat("hh:mm")
      var currentMinuteAsString = minuteFormat.format(today)
      var message = username_textfield.text+"("+currentMinuteAsString+"): "+textfield.text + "\n"

      SocketSession.getBasicRemote.sendText("msg<>"+message)
      chatArea.text += message
      textfield.text = ""
    }
  }

  listenTo(textfield.keys)
  reactions += {
    case KeyPressed(`textfield`, Key.Enter, _, _) =>
      sendText()
  }

  def showUsers() = {
    if(SocketSession!=null) {
      SocketSession.getBasicRemote.sendText("me<>get-users")
    }
  }

  def hideMe() = {
    visible = false
  }

  def show = {
    visible = true
  }

}