
import play.core.PlayVersion
import play.sbt.routes.RoutesKeys
import sbt.*
import uk.gov.hmrc.DefaultBuildSettings.*
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "income-tax-view-change-frontend"

val bootstrapPlayVersion = "5.21.0" // "7.11.0" in the next iteration / this causing number of unit tests to fail
val playPartialsVersion = "8.3.0-play-28"
val playFrontendHMRCVersion = "7.0.0-play-28"
val playLanguageVersion = "5.2.0-play-28"
val catsVersion = "2.8.0"

val scalaTestPlusVersion = "5.0.0"
val pegdownVersion = "1.6.0"
val jsoupVersion = "1.11.3"
val mockitoVersion = "3.12.4"
val scalaMockVersion = "5.2.0"
val wiremockVersion = "2.26.3"
val hmrcMongoVersion = "0.73.0"
val currentScalaVersion = "2.13.8"

scalacOptions += "-feature"

val compile = Seq(
  ws,
  "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % bootstrapPlayVersion,
  "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
  "org.typelevel" %% "cats-core" % catsVersion,
  "com.typesafe.play" %% "play-json-joda" % "2.9.3",
  "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28" % hmrcMongoVersion,
  "uk.gov.hmrc" %% "play-frontend-hmrc" % playFrontendHMRCVersion
)

def test(scope: String = "test"): Seq[ModuleID] = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
  "org.scalamock" %% "scalamock" % scalaMockVersion % scope,

  "org.pegdown" % "pegdown" % pegdownVersion % scope,
  "org.jsoup" % "jsoup" % jsoupVersion % scope,
  "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
  "org.mockito" % "mockito-core" % mockitoVersion % scope,
  "com.github.tomakehurst" % "wiremock-jre8" % wiremockVersion % scope,
  "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-28" % hmrcMongoVersion % scope,
  "org.scalacheck" %% "scalacheck" % "1.17.0" % scope,
  "uk.gov.hmrc" %% "bootstrap-test-play-28"  % bootstrapPlayVersion % "test",
  caffeine
)

def it(scope: String = "it"): Seq[ModuleID] = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
  "org.scalamock" %% "scalamock" % scalaMockVersion % scope,
  "org.pegdown" % "pegdown" % pegdownVersion % scope,
  "org.jsoup" % "jsoup" % jsoupVersion % scope,
  "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
  "org.mockito" % "mockito-core" % mockitoVersion % scope,
  "com.github.tomakehurst" % "wiremock-jre8" % wiremockVersion % scope,
  "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-28" % hmrcMongoVersion % scope,
  caffeine
)

lazy val appDependencies: Seq[ModuleID] = compile ++ test() ++ it()

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
    Test / javaOptions += "-Dlogger.resource=logback-test.xml"
  )
  .settings(
    libraryDependencies ++= appDependencies,
    retrieveManaged := true
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    Test / Keys.fork := true,
    scalaVersion := currentScalaVersion,
    scalacOptions += "-Wconf:src=routes/.*:s",
    Test / javaOptions += "-Dlogger.resource=logback-test.xml")
  .configs(IntegrationTest)
  .settings(
    IntegrationTest / Keys.fork := false,
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory) (base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest / parallelExecution := false,
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components.implicits._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components.implicits._"
    ),
    RoutesKeys.routesImport := Seq("enums.IncomeSourceJourney._", "models.incomeSourceDetails.LatencyYear._"),
    scalacOptions += "-Wconf:cat=unused-imports:s,cat=unused-params:s"
  )
  .settings(resolvers ++= Seq(
    Resolver.jcenterRepo
  ))
