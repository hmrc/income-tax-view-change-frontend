
import play.sbt.routes.RoutesKeys
import sbt.*
import sbt.Keys.libraryDependencySchemes
import uk.gov.hmrc.DefaultBuildSettings.*
import uk.gov.hmrc.{DefaultBuildSettings, SbtAutoBuildPlugin}
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "income-tax-view-change-frontend"

val bootstrapPlayVersion = "8.1.0"
val playPartialsVersion = "9.1.0"
val playFrontendHMRCVersion = "8.1.0"
val catsVersion = "2.8.0"

val scalaTestPlusVersion = "7.0.0"
val pegdownVersion = "1.6.0"
val jsoupVersion = "1.15.4"
val mockitoVersion = "5.8.0"
val scalaMockVersion = "5.2.0"
val wiremockVersion = "3.0.0-beta-7"
val hmrcMongoVersion = "1.6.0"
val currentScalaVersion = "2.13.12"
val playVersion = "play-30"

scalacOptions += "-feature"

val compile = Seq(
  ws,
  "uk.gov.hmrc" %% s"bootstrap-frontend-$playVersion" % bootstrapPlayVersion,
  "uk.gov.hmrc" %% s"play-partials-$playVersion" % playPartialsVersion,
  "org.typelevel" %% "cats-core" % catsVersion,
  "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion" % hmrcMongoVersion,
  "uk.gov.hmrc" %% s"play-frontend-hmrc-$playVersion" % playFrontendHMRCVersion,
  "uk.gov.hmrc" %% s"crypto-json-$playVersion" % "7.6.0",
  "org.jsoup" % "jsoup" % jsoupVersion,
)

def test(scope: String = "test"): Seq[ModuleID] = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
  "org.scalamock" %% "scalamock" % scalaMockVersion % scope,
  "org.pegdown" % "pegdown" % pegdownVersion % scope,
  "org.jsoup" % "jsoup" % jsoupVersion % scope,
  "org.mockito" % "mockito-core" % mockitoVersion % scope,
  "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion % scope,
  "org.scalacheck" %% "scalacheck" % "1.17.0" % scope,
  "uk.gov.hmrc" %% s"bootstrap-test-$playVersion"  % bootstrapPlayVersion % "test",
  caffeine,
  "uk.gov.hmrc" %% s"crypto-json-$playVersion" % "7.6.0"
)

def it(scope: String = "test"): Seq[ModuleID] = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
  "org.scalamock" %% "scalamock" % scalaMockVersion % scope,
  "org.pegdown" % "pegdown" % pegdownVersion % scope,
  "org.jsoup" % "jsoup" % jsoupVersion % scope,
  "org.mockito" % "mockito-core" % mockitoVersion % scope,
  "com.github.tomakehurst" % "wiremock" % wiremockVersion % scope,
  "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion % scope,
  caffeine
)

lazy val appDependencies: Seq[ModuleID] = compile ++ test()
lazy val appDependenciesIt: Seq[ModuleID] = it()

lazy val plugins: Seq[Plugins] = Seq.empty
lazy val playSettings: Seq[Setting[_]] = Seq.empty

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;controllers\\..*Reverse.*;models/.data/..*;" +
      "filters.*;.handlers.*;components.*;.*BuildInfo.*;.*standardError*.*;.*Routes.*;views.html.*;appConfig.*;" +
      "controllers.feedback.*;app.*;prod.*;appConfig.*;com.*;testOnlyDoNotUseInAppConf.*;testOnly.*;\"",
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(scalaVersion := currentScalaVersion)
  .settings(scoverageSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(majorVersion := 1)
  .settings(scalacOptions += "-Wconf:cat=lint-multiarg-infix:silent")
  .settings(scalacOptions += "-Xfatal-warnings")
  .settings(
    Test / Keys.fork := true,
    Test / javaOptions += "-Dlogger.resource=logback-test.xml",
    libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
  )
  .settings(
    libraryDependencies ++= appDependencies,
    retrieveManaged := true
  )
  .settings(
    Test / Keys.fork := true,
    scalaVersion := currentScalaVersion,
    scalacOptions += "-Wconf:src=routes/.*:s",
    Test / javaOptions += "-Dlogger.resource=logback-test.xml")
  .settings(
    Keys.fork := false,
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components.implicits._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components.implicits._"
    ),
    RoutesKeys.routesImport := Seq("enums.IncomeSourceJourney._"),
    scalacOptions += "-Wconf:cat=unused-imports:s,cat=unused-params:s"
  )
  .settings(resolvers ++= Seq(
    Resolver.jcenterRepo
  ))

lazy val it = project
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings.head)
  .enablePlugins(play.sbt.PlayScala)
  .settings(
    publish / skip := true
  )
  .settings(scalaVersion := currentScalaVersion)
  .settings(majorVersion := 1)
  .settings(scalacOptions += "-Xfatal-warnings")
  .settings(
    testForkedParallel := true
  )
  .settings(
    libraryDependencies ++= appDependenciesIt
  )
