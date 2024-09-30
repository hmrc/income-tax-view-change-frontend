import play.sbt.routes.RoutesKeys
import sbt.*
import sbt.Keys.libraryDependencySchemes
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.DefaultBuildSettings.*
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "income-tax-view-change-frontend"

ThisBuild / majorVersion := 1
ThisBuild / scalaVersion := "2.13.12"

scalacOptions += "-feature"

// Define play settings for clarity
lazy val playSettings: Seq[Setting[_]] = Seq(
  TwirlKeys.templateImports ++= Seq(
    "uk.gov.hmrc.govukfrontend.views.html.components.implicits._",
    "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
    "uk.gov.hmrc.hmrcfrontend.views.html.components.implicits._"
  ),
  RoutesKeys.routesImport := Seq(
    "enums.IncomeSourceJourney._",
    "models.admin._",
    "models.core._"
  )
)


ThisBuild / javaOptions ++= Seq(
  "-Xmx4G",  // Set maximum heap size to 4GB (adjust this value based on your requirements)
  "-Xms2G"   // Set the initial heap size to 2GB (adjust as needed)
)

// Define common scalac options to avoid repetition
lazy val scalacSettings: Seq[String] = Seq(
  "-Wconf:src=routes/.*:s",  // TODO: Remove and clean up parens () for scala 3
  "-Wconf:cat=unused:silent", // Suppress all unused warnings
  "-Wconf:msg=match may not be exhaustive:silent", // Suppress the exhaustive match warning
  "-Wconf:msg=defaultPrefix:silent", // Silence warnings related to defaultPrefix
  "-Xfatal-warnings",
  "-Wconf:cat=lint-multiarg-infix:silent",
  "-Wconf:cat=unused-imports:s,cat=unused-params:s"
)

// Define common test settings to avoid repetition
lazy val testSettings: Seq[Setting[_]] = Seq(
  Test / fork := true,
  Test / javaOptions += "-Dlogger.resource=logback-test.xml",
  testForkedParallel := true
)

lazy val microservice =
  Project(appName, file("."))
    .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
    .disablePlugins(JUnitXmlReportPlugin)
    .settings(
      defaultSettings(),
      playSettings,
      scalaSettings,
      CodeCoverageSettings.settings,
      libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
    )
    .settings(
      scalacOptions ++= scalacSettings, // Group scalac options
      libraryDependencies ++= AppDependencies(),
      retrieveManaged := true,
      fork := false // Don't fork for non-test tasks
    )
    .settings(
      resolvers ++= Seq(
        Resolver.mavenCentral // Use Maven Central instead of deprecated JCenter
      )
    )

lazy val it = project
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings().head)
  .enablePlugins(play.sbt.PlayScala)
  .settings(
    publish / skip := true,
    scalacOptions ++= scalacSettings,
    libraryDependencies ++= AppDependencies.it(),
    testSettings // Reuse common test settings
  )