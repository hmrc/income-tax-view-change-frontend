import play.sbt.routes.RoutesKeys
import sbt.*
import sbt.Keys.*
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "income-tax-view-change-frontend"

val bootstrapPlayVersion = "8.1.0"
val playPartialsVersion = "9.1.0"
val playFrontendHMRCVersion = "8.5.0"
val catsVersion = "2.12.0"

val scalaTestPlusVersion = "7.0.1"
val pegdownVersion = "1.6.0"
val jsoupVersion = "1.15.4"
val mockitoVersion = "5.8.0"
val scalaMockVersion = "5.2.0"
val wiremockVersion = "3.0.0-beta-7"
val hmrcMongoVersion = "1.6.0"
val currentScalaVersion = "2.13.12"
val playVersion = "play-30"
val cryptoJsonVersion = "7.6.0"

val compileDependencies =
  Seq(
    ws,
    "uk.gov.hmrc" %% s"bootstrap-frontend-$playVersion" % bootstrapPlayVersion,
    "uk.gov.hmrc" %% s"play-partials-$playVersion" % playPartialsVersion,
    "org.typelevel" %% "cats-core" % catsVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion" % hmrcMongoVersion,
    "uk.gov.hmrc" %% s"play-frontend-hmrc-$playVersion" % playFrontendHMRCVersion,
    "uk.gov.hmrc" %% s"crypto-json-$playVersion" % cryptoJsonVersion,
    "org.jsoup" % "jsoup" % jsoupVersion
  )

val commonTestDependencies =
  Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion,
    "org.scalamock" %% "scalamock" % scalaMockVersion,
    "org.pegdown" % "pegdown" % pegdownVersion,
    "org.jsoup" % "jsoup" % jsoupVersion,
    "org.mockito" % "mockito-core" % mockitoVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion
  )

def testDependencies(scope: String = "test"): Seq[ModuleID] =
  commonTestDependencies.map(_ % scope) ++ Seq(
    "org.scalacheck" %% "scalacheck" % "1.18.0" % scope,
    "org.scalatestplus" %% "scalacheck-1-15" % "3.2.11.0" % scope,
    "uk.gov.hmrc" %% s"bootstrap-test-$playVersion" % bootstrapPlayVersion % scope,
    caffeine,
    "uk.gov.hmrc" %% s"crypto-json-$playVersion" % cryptoJsonVersion % scope
  )

def itDependencies(scope: String = "test"): Seq[ModuleID] =
  commonTestDependencies.map(_ % scope) ++ Seq(
    "com.github.tomakehurst" % "wiremock" % wiremockVersion % scope,
    caffeine
  )

lazy val appDependencies: Seq[ModuleID] = compileDependencies ++ testDependencies()
lazy val appDependenciesIt: Seq[ModuleID] = itDependencies()

lazy val baseSettings = Seq(
  scalaVersion := currentScalaVersion,
  majorVersion := 1,
  scalacOptions ++= Seq(
    "-feature",
    "-Wconf:src=routes/.*:s",
    "-Wconf:cat=deprecation:s",
    "-Wconf:cat=lint-multiarg-infix:silent",
    "-Xfatal-warnings",
    "-Wunused:imports",
    "-Wconf:cat=unused-imports:s",
    "-Wconf:cat=unused-params:s",
    "-nowarn"
  ),
  ScoverageKeys.coverageExcludedPackages := "<empty>;controllers\\..*Reverse.*;models/.data/..*;" +
    "filters.*;.handlers.*;components.*;.*BuildInfo.*;.*standardError*.*;.*Routes.*;views.html.*;appConfig.*;" +
    "controllers.feedback.*;app.*;prod.*;appConfig.*;com.*;testOnlyDoNotUseInAppConf.*;testOnly.*;\"",
  ScoverageKeys.coverageMinimumStmtTotal := 90,
  ScoverageKeys.coverageFailOnMinimum := false,
  ScoverageKeys.coverageHighlighting := true,
  publish / skip := true,
  testForkedParallel := true,
  Test / Keys.fork := true,
  Test / javaOptions += "-Dlogger.resource=logback-test.xml"
)

lazy val microservice =
  Project(appName, file("."))
    .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
    .disablePlugins(JUnitXmlReportPlugin)
    .settings(baseSettings)
    .settings(
      scalaSettings,
      defaultSettings(),
      libraryDependencies ++= appDependencies,
      retrieveManaged := true,
      TwirlKeys.templateImports ++= Seq(
        "uk.gov.hmrc.govukfrontend.views.html.components.implicits._",
        "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
        "uk.gov.hmrc.hmrcfrontend.views.html.components.implicits._"
      ),
      RoutesKeys.routesImport := Seq("enums.IncomeSourceJourney._", "models.admin._", "models.core._"),
      resolvers ++= Seq(Resolver.jcenterRepo)
    )

lazy val it = project
  .dependsOn(microservice % "test->test")
  .enablePlugins(play.sbt.PlayScala)
  .settings(baseSettings)
  .settings(
    libraryDependencies ++= appDependenciesIt
  )
