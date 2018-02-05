import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.SbtAutoBuildPlugin
import play.core.PlayVersion
import sbt.Tests.{Group, SubProcess}

val appName = "income-tax-view-change-frontend"

val bootstrapPlayVersion      = "1.0.0"
val govTemplateVersion        = "5.14.0"
val playPartialsVersion       = "6.1.0"
val authClientVersion         = "2.3.0"
val playUiVersion             = "7.8.0"

val scalaTestPlusVersion      = "2.0.0"
val hmrcTestVersion           = "3.0.0"
val scalatestVersion          = "3.0.0"
val pegdownVersion            = "1.6.0"
val jsoupVersion              = "1.10.2"
val mockitoVersion            = "2.7.17"
val scalaMockVersion          = "3.5.0"
val wiremockVersion           = "2.5.1"

val compile = Seq(
  ws,
  "uk.gov.hmrc" %% "bootstrap-play-25" % bootstrapPlayVersion,
  "uk.gov.hmrc" %% "govuk-template" % govTemplateVersion,
  "uk.gov.hmrc" %% "play-ui" % playUiVersion,
  "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
  "uk.gov.hmrc" %% "auth-client" % authClientVersion
)

def test(scope: String = "test,it") = Seq(
  "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
  "org.scalatest" %% "scalatest" % scalatestVersion % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
  "org.scalamock" %% "scalamock-scalatest-support" % scalaMockVersion % scope,
  "org.pegdown" % "pegdown" % pegdownVersion % scope,
  "org.jsoup" % "jsoup" % jsoupVersion % scope,
  "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
  "org.mockito" % "mockito-core" % mockitoVersion % scope,
  "com.github.tomakehurst" % "wiremock" % wiremockVersion % scope
)

lazy val appDependencies: Seq[ModuleID] = compile ++ test()

lazy val plugins : Seq[Plugins] = Seq.empty
lazy val playSettings : Seq[Setting[_]] = Seq.empty

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;models/.data/..*;" +
      "filters.*;.handlers.*;components.*;.*BuildInfo.*;.*FrontendAuditConnector.*;.*Routes.*;views.html.*;config.*;" +
      "controllers.feedback.*;app.*;prod.*;config.*;com.*;testOnly.*;\"",
    ScoverageKeys.coverageMinimum := 90,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}

def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] =
  tests map {
    test => new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name, "-Dlogger.resource=logback-test.xml"))))
  }


lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala,SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin) ++ plugins : _*)
  .settings(playSettings : _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    Keys.fork in Test := true,
    javaOptions in Test += "-Dlogger.resource=logback-test.xml",
    scalaVersion := "2.11.11"
  )
  .settings(
    libraryDependencies ++= appDependencies,
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    routesGenerator := InjectedRoutesGenerator
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest <<= (baseDirectory in IntegrationTest)(base => Seq(base / "it")),
    addTestReportOption(IntegrationTest, "int-test-reports"),
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    parallelExecution in IntegrationTest := false)
  .settings(resolvers ++= Seq(
    Resolver.bintrayRepo("hmrc", "releases"),
    Resolver.jcenterRepo
  ))

