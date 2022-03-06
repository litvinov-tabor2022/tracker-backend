Global / onChangedBuildSource := ReloadOnSourceChanges
ThisBuild / turbo := true
ThisBuild / organization := "cz.jenda"

lazy val commonSettings = BuildSettings.common ++ Seq(
  libraryDependencies ++= Seq(
    Dependencies.logbackClassic,
    Dependencies.scalaTest % Test
  ),
  Test / publishArtifact := false
)

lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.circeGenericExtras,
      Dependencies.circeLiteral,
      Dependencies.circeParser,
      Dependencies.http4sCirce,
      Dependencies.mqttClient,
      Dependencies.mysql,
      Dependencies.log4cats,
      Dependencies.sstBundleMonixHttp4sBlaze,
      Dependencies.sstDoobieHikariPureConfig,
      Dependencies.sstDoobieHikari,
      Dependencies.sstFlywayPureConfig,
      Dependencies.sstJvm
    ),
    name := "tracker-backend"
  )

addCommandAlias("check", "; scalafmtSbtCheck; scalafmtCheckAll; compile:scalafix --check; test:scalafix --check; test")
addCommandAlias("fix", "; compile:scalafix; test:scalafix; scalafmtSbt; scalafmtAll")
