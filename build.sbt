
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.SbtAutoBuildPlugin
import play.core.PlayVersion
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

import sbt.Tests.{Group, SubProcess}

val appName = "income-tax-view-change-frontend"

val bootstrapPlayVersion      = "1.7.0"
val govTemplateVersion        = "5.52.0-play-26"
val playPartialsVersion       = "6.10.0-play-26"
val authClientVersion         = "2.22.0-play-26"
val playUiVersion             = "8.15.0-play-26"
val playLanguageVersion       = "4.2.0-play-26"
val catsVersion = "0.9.0"

val scalaTestPlusVersion      = "3.1.3"
val hmrcTestVersion           = "3.9.0-play-26"
val scalatestVersion          = "3.0.8"
val pegdownVersion            = "1.6.0"
val jsoupVersion              = "1.11.3"
val mockitoVersion            = "2.27.0"
val scalaMockVersion          = "3.5.0"
val wiremockVersion           = "2.26.1"


val compile = Seq(
  ws,
  "uk.gov.hmrc" %% "bootstrap-play-26" % bootstrapPlayVersion,
  "uk.gov.hmrc" %% "govuk-template" % govTemplateVersion,
  "uk.gov.hmrc" %% "play-ui" % playUiVersion,
  "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
  "uk.gov.hmrc" %% "auth-client" % authClientVersion,
  "uk.gov.hmrc" %% "play-language" % playLanguageVersion,
  "org.typelevel" %% "cats" % catsVersion,
  "uk.gov.hmrc" %% "logback-json-logger" % "4.8.0",
  "com.typesafe.play" %% "play-json-joda" % "2.6.10"
)

def test(scope: String = "test,it"): Seq[ModuleID] = Seq(
  "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
  "uk.gov.hmrc" %% "bootstrap-play-26" % "1.7.0" % Test classifier "tests",
  "org.scalatest" %% "scalatest" % scalatestVersion % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
  "org.scalamock" %% "scalamock-scalatest-support" % scalaMockVersion % scope,
  "org.pegdown" % "pegdown" % pegdownVersion % scope,
  "org.jsoup" % "jsoup" % jsoupVersion % scope,
  "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
  "org.mockito" % "mockito-core" % mockitoVersion % scope,
  "com.github.tomakehurst" % "wiremock-jre8" % wiremockVersion % scope
)

lazy val appDependencies: Seq[ModuleID] = compile ++ test()

lazy val plugins: Seq[Plugins] = Seq.empty
lazy val playSettings: Seq[Setting[_]] = Seq.empty

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;models/.data/..*;" +
      "filters.*;.handlers.*;components.*;.*BuildInfo.*;.*FrontendAuditConnector.*;.*Routes.*;views.html.*;appConfig.*;" +
      "controllers.feedback.*;app.*;prod.*;appConfig.*;com.*;testOnly.*;\"",
    ScoverageKeys.coverageMinimum := 90,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(playSettings : _*)
  .settings(scalaSettings: _*)
  .settings(scalaVersion := "2.12.12")
  .settings(publishingSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(majorVersion := 1)
  .settings(
    Keys.fork in Test := true,
    javaOptions in Test += "-Dlogger.resource=logback-test.xml"
  )
  .settings(
    libraryDependencies ++= appDependencies,
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    parallelExecution in IntegrationTest := false)
  .settings(resolvers ++= Seq(
    Resolver.bintrayRepo("hmrc", "releases"),
    Resolver.jcenterRepo
  ))
