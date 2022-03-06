import sbt._

object Dependencies {

  val doobie = "org.tpolecat" %% "doobie-postgres" % Versions.doobie
  val kindProjector = "org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full
  val logbackClassic = "ch.qos.logback" % "logback-classic" % "1.2.10"
  val semanticDb = "org.scalameta" % "semanticdb-scalac_2.13.8" % "4.5.0"
  val scalaTest = "org.scalatest" %% "scalatest" % "3.2.9"
  val scalafixScaluzzi = "com.github.vovapolu" %% "scaluzzi" % "0.1.20"
  val scalafixSortImports = "com.nequissimus" %% "sort-imports" % "0.6.1"
  val sstBundleMonixHttp4sBlaze = "com.avast" %% "sst-bundle-monix-http4s-blaze" % Versions.sst
  val sstDoobieHikari = "com.avast" %% "sst-doobie-hikari" % Versions.sst
  val sstDoobieHikariPureConfig = "com.avast" %% "sst-doobie-hikari-pureconfig" % Versions.sst
  val sstFlywayPureConfig = "com.avast" %% "sst-flyway-pureconfig" % Versions.sst
  val sstHttp4sClientBlazePureConfig = "com.avast" %% "sst-http4s-client-blaze-pureconfig" % Versions.sst
  val sstHttp4sClientMonixCatcap = "com.avast" %% "sst-http4s-client-monix-catnap" % Versions.sst
  val sstJvm = "com.avast" %% "sst-jvm" % Versions.sst
  val sstMonixCatnapPureConfig = "com.avast" %% "sst-monix-catnap-pureconfig" % Versions.sst

  object Versions {
    val sst = "0.15.6"
    val silencer = "1.7.1"
    val doobie = "0.13.4"
    val testContainers = "0.38.6"
  }

}
