import sbt.Keys._
import sbt._
import scalafix.sbt.ScalafixPlugin.autoImport.scalafixDependencies

object BuildSettings {

  lazy val common: Seq[Def.Setting[_]] = Seq(
    scalaVersion := "2.13.8",
    libraryDependencies ++= Seq(
      compilerPlugin(Dependencies.kindProjector),
      compilerPlugin(Dependencies.semanticDb) // necessary for Scalafix
    ),
    ThisBuild / scalafixDependencies ++= Seq(
      Dependencies.scalafixScaluzzi,
      Dependencies.scalafixSortImports
    ),
    scalacOptions ++= Seq(
      "-Yrangepos", // necessary for Scalafix (required by SemanticDB compiler plugin)
      "-Ywarn-unused" // necessary for Scalafix RemoveUnused rule (not present in sbt-tpolecat for 2.13)
    ),
    Test / publishArtifact := false
  )

}
