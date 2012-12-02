import sbt._

import Keys._
import AndroidKeys._
import scala.sys.process.{Process => SysProcess}

object Settings {
  lazy val common = Defaults.defaultSettings ++ Seq (
    version := "0.1",
    scalaVersion := "2.9.2",
    scalacOptions ++= List("-deprecation", "-unchecked", "-Ydependent-method-types"),
    resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    libraryDependencies ++= Seq(
      "com.google.guava" % "guava" % "10.0",
      "net.liftweb" % "lift-json_2.9.1" % "2.4",
      "com.weiglewilczek.slf4s" % "slf4s_2.9.1" % "1.0.7",
      "com.typesafe.akka" % "akka-actor" % "2.0.3",
      "rhino" % "js" % "1.7R2",
      "org.scalatest" %% "scalatest" % "1.6.1" % "test"
    ),
    unmanagedJars in Compile <<= baseDirectory map { base =>
      var baseDirectories = (base / "lib") +++ (base / "lib" / "extensions")
      var jars = baseDirectories ** "*.jar"
      jars.classpath
    },
    updateLibgdxTask
   )

  lazy val playerDesktop = Settings.common ++ Seq (
    fork in Compile := true,
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.0.6"
    ),
    unmanagedJars in Compile <<= baseDirectory map { base =>
      var baseDirectories = (base / "lib") +++ (base / "lib" / "extensions")
      var jars = baseDirectories ** "*.jar"
      jars.classpath
    }
  )

  lazy val playerAndroid = Settings.common ++
    AndroidProject.androidSettings ++
    AndroidMarketPublish.settings ++ Seq (
      platformName in Android := "android-8",
      keyalias in Android := "change-me",
      mainAssetsPath in Android := file("common/src/main/resources"),
      unmanagedJars in Compile <++= baseDirectory map { base =>
        var baseDirectories = 
          (base / "src" / "main" / "libs") +++ 
          (base / "src" / "main" / "libs" / "extensions")
        var jars = baseDirectories ** "*.jar"
        jars.classpath
      }
    )
  
  lazy val editor = Settings.playerDesktop ++ Seq(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-swing" % "2.9.2",
      "org.apache.httpcomponents" % "httpclient" % "4.1.1",
      "net.java.dev.designgridlayout" % "designgridlayout" % "1.8"
    ),
    mainClass in (Compile, run) := Some("rpgboss.editor.RpgDesktop"),
    scalacOptions ++= List("-deprecation", "-unchecked", "-Ydependent-method-types"),
    TaskKey[Unit]("generateEnum") := {  
      SysProcess("python GenerateFileEnum.py", new File("editor/src/main/resources")).run()
      println("Generated file enumeration")
      Unit
    },
    Keys.`compile` <<= (Keys.`compile` in Compile) dependsOn TaskKey[Unit]("generateEnum")
  )

  val updateLibgdx = TaskKey[Unit]("update-gdx", "Updates libgdx")

  val updateLibgdxTask = updateLibgdx <<= streams map { (s: TaskStreams) =>
    import Process._
    import java.io._
    import java.net.URL
    
    // Declare names
    val baseUrl = "http://libgdx.badlogicgames.com/nightlies"
    val gdxName = "libgdx-nightly-latest"

    // Fetch the file.
    s.log.info("Pulling %s" format(gdxName))
    s.log.warn("This may take a few minutes...")
    val zipName = "%s.zip" format(gdxName)
    val zipFile = new java.io.File(zipName)
    val url = new URL("%s/%s" format(baseUrl, zipName))
    IO.download(url, zipFile)

    // Extract jars into their respective lib folders.
    val commonDest = file("common/lib")
    val commonFilter = new ExactFilter("gdx.jar") |
	new ExactFilter("extensions/gdx-freetype.jar") |
	new ExactFilter("extensions/gdx-audio.jar")
    IO.unzip(zipFile, commonDest, commonFilter)

    val desktopDest = file("player-desktop/lib")
    val desktopFilter = new ExactFilter("gdx-natives.jar") |
    new ExactFilter("gdx-backend-lwjgl.jar") |
    new ExactFilter("gdx-backend-lwjgl-natives.jar") |
    new ExactFilter("gdx-tools.jar") |
    new ExactFilter("extensions/gdx-freetype-natives.jar") |
    new ExactFilter("extensions/gdx-audio-natives.jar")
    IO.unzip(zipFile, desktopDest, desktopFilter)

    val androidDest = file("player-android/src/main/libs")
    val androidFilter = new ExactFilter("gdx-backend-android.jar") |
    new ExactFilter("armeabi/libgdx.so") |
    new ExactFilter("armeabi/libandroidgl20.so") |
    new ExactFilter("armeabi/libgdx-freetype.so") |
    new ExactFilter("armeabi/libgdx-audio.so") |
    new ExactFilter("armeabi-v7a/libgdx.so") |
    new ExactFilter("armeabi-v7a/libandroidgl20.so") |
    new ExactFilter("armeabi-v7a/libgdx-freetype.so") |
    new ExactFilter("armeabi-v7a/libgdx-audio.so")
    
    commonFilter
    IO.unzip(zipFile, androidDest, androidFilter)

    // Destroy the file.
    zipFile.delete
    s.log.info("Complete")
  }
}

object LibgdxBuild extends Build {
  val common = Project (
    "common",
    file("common"),
    settings = Settings.common
  )

  lazy val playerDesktop = Project (
    "player-desktop",
    file("player-desktop"),
    settings = Settings.playerDesktop
  ) dependsOn common

  lazy val playerAndroid = Project (
    "player-android",
    file("player-android"),
    settings = Settings.playerAndroid
  ) dependsOn common
  
  lazy val editor = Project (
    "editor",
    file("editor"),
    settings = Settings.editor
  ) dependsOn playerDesktop
}
