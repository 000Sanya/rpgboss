//seq(ProguardPlugin.proguardSettings :_*)

name := "rpgboss-editor"

version := "0.1"

organization := "rpgboss"

scalaVersion := "2.9.0-1"

fork in run := true

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-swing" % "2.9.0-1",
  "org.apache.httpcomponents" % "httpclient" % "4.1.1",
  "org.apache.sanselan" % "sanselan" % "0.97-incubator",
  "net.java.dev.designgridlayout" % "designgridlayout" % "1.8",
  "com.google.guava" % "guava" % "10.0",
  "net.liftweb" %% "lift-json" % "2.4-M4",
  "net.iharder" % "base64" % "2.3.8"
)

mainClass in (Compile, run) := Some("rpgboss.editor.RpgDesktop")

scalacOptions ++= List("-deprecation", "-Xexperimental", "-unchecked")

//proguardOptions ++= List(
//  "-dontshrink",
//  "-keep class rpgboss.editor.RpgApplet",
//  """-keepclasseswithmembers public class * {
//       public static void main(java.lang.String[]);
//  }""")
