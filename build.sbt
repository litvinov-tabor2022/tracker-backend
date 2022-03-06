import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.dockerUpdateLatest

Global / onChangedBuildSource := ReloadOnSourceChanges
ThisBuild / turbo := true
ThisBuild / organization := "cz.jenda"

val installBashCommands = Seq(
  Cmd("USER", "root"),
  Cmd("RUN", "apk", "add", "--update", "bash", "&&", "rm", "-rf", "/var/cache/apk/*"),
  Cmd("USER", "daemon")
)

lazy val containerSettings = Seq(
  dockerBaseImage := "adoptopenjdk/openjdk11:alpine-jre",
  dockerUpdateLatest := true,
  dockerExposedPorts := Seq(8080),
  dockerEntrypoint := Seq("bin/tracker-backend", "-Dconfig.file=/application.conf"),
  Docker / packageName := "tracker-server"
)

lazy val commonSettings = BuildSettings.common ++ Seq(
  version := sys.props.getOrElse("version", "1.0-SNAPSHOT"),
  libraryDependencies ++= Seq(
    Dependencies.logbackClassic,
    Dependencies.scalaTest % Test
  ),
  Test / publishArtifact := false
)

lazy val root = project
  .in(file("."))
  .settings(commonSettings)
  .settings(containerSettings)
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
    name := "tracker-backend",
    Compile / mainClass := Some("cz.jenda.tracker.Main"),
    dockerCommands ++= installBashCommands
  )
  .enablePlugins(JavaAppPackaging, UniversalPlugin, DockerPlugin)

addCommandAlias("check", "; scalafmtSbtCheck; scalafmtCheckAll; compile:scalafix --check; test:scalafix --check; test")
addCommandAlias("fix", "; compile:scalafix; test:scalafix; scalafmtSbt; scalafmtAll")
