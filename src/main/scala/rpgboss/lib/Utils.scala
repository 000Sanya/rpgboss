package rpgboss.lib 
import java.io._
import scala.io.Source
  
class FileHelper(file : File) {
  import FileHelper._
  
  def write(text : String) : Unit = {
    val fw = new FileWriter(file)
    try{ fw.write(text) }
    finally{ fw.close }
  }

  def readAsString : Option[String] = {
    if(file.isFile && file.canRead)
      Some(Source.fromFile(file).mkString)
    else
      None
  }
  
  def deleteAll() : Boolean = {
    def deleteFile(dfile : File) : Boolean = {
      val subFilesGone = 
        if(dfile.isDirectory) 
          dfile.listFiles.foldLeft(true)( _ && deleteFile(_) )
        else 
          true
      
      subFilesGone && dfile.delete
    }
    
    deleteFile(file)
  }

  def getBytes = {

    val is = new FileInputStream(file)

    val length = file.length()

    val bytes = new Array[Byte](length.asInstanceOf[Int])

    is.read(bytes)

    is.close()
    bytes
  }

  def writeBytes(bytes: Array[Byte]) = {
    val fos = new FileOutputStream(file)
    fos.write(bytes)
    fos.close()
  }
  
  def copyTo(dest: File) = {
    dest.createNewFile()
    val srcChan = (new FileInputStream(file)).getChannel
    val desChan = (new FileOutputStream(dest)).getChannel
    desChan.transferFrom(srcChan, 0, srcChan.size)
    srcChan.close
    desChan.close
  }
  
  // return value: if we can write to resultant directory
  def makeWriteableDir() : Boolean = {
    if(file.exists) {
      if(file.isDirectory)
        return file.canWrite
      else {
        // It's an ordinary file. Delete and recreate
        file.delete() && file.mkdirs() && file.canWrite
      }
    } else {
      file.mkdirs() && file.canWrite
    }
  }
  
  // True if it exists and is writeable, OR if we make a new one
  def canWriteToFile() : Boolean = 
    file.canWrite || file.createNewFile()
  
  // creates parent directory and file if necessary. ensure writable
  def prepareWrite(writeFunc : (FileOutputStream) => Boolean) : Boolean = 
  {
    if(file.getParentFile().makeWriteableDir() && file.canWriteToFile())
      writeFunc(new FileOutputStream(file))
    else
      false
  }
}

object FileHelper {
  implicit def file2helper(file : File) : FileHelper = new FileHelper(file)
}
