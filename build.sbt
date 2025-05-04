
import play.sbt.routes.RoutesKeys
import sbt.*
import sbt.Keys.libraryDependencySchemes
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.DefaultBuildSettings.*
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "income-tax-view-change-frontend"

val bootstrapPlayVersion = "9.7.0"
val playPartialsVersion = "9.1.0"
val playFrontendHMRCVersion = "11.11.0"
val catsVersion = "2.12.0"
val scalaTestPlusVersion = "7.0.1"
val pegdownVersion = "1.6.0"
val jsoupVersion = "1.18.1"
val mockitoVersion = "5.11.0"
val scalaMockVersion = "5.2.0"
val wiremockVersion = "3.0.0-beta-7"
val hmrcMongoVersion = "2.4.0"
val currentScalaVersion = "2.13.16"
val playVersion = "play-30"

scalacOptions ++= Seq(
  "-feature",
  "-Wconf:src=target/.*:silent")

val compile = Seq(
  ws,
  "uk.gov.hmrc" %% s"bootstrap-frontend-$playVersion" % bootstrapPlayVersion,
  "uk.gov.hmrc" %% s"play-partials-$playVersion" % playPartialsVersion,
  "org.typelevel" %% "cats-core" % catsVersion,
  "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVersion" % hmrcMongoVersion,
  "uk.gov.hmrc" %% s"play-frontend-hmrc-$playVersion" % playFrontendHMRCVersion,
  "uk.gov.hmrc" %% s"crypto-json-$playVersion" % "8.1.0",
  "org.jsoup" % "jsoup" % jsoupVersion,
)

def test(scope: String = "test"): Seq[ModuleID] = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
  "org.scalamock" %% "scalamock" % scalaMockVersion % scope,
  "org.pegdown" % "pegdown" % pegdownVersion % scope,
  "org.jsoup" % "jsoup" % jsoupVersion % scope,
  "org.mockito" % "mockito-core" % mockitoVersion % scope,
  "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion % scope,
  "org.scalacheck" %% "scalacheck" % "1.18.1" % scope,
  "org.scalatestplus" %% "scalacheck-1-15" % "3.2.11.0" % scope,
  "uk.gov.hmrc" %% s"bootstrap-test-$playVersion" % bootstrapPlayVersion % "test",
  caffeine,
  "uk.gov.hmrc" %% s"crypto-json-$playVersion" % "8.1.0"
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
    ScoverageKeys.coverageMinimumStmtTotal := 90.0,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(playSettings *)
  .settings(scalaSettings *)
  .settings(scalaVersion := currentScalaVersion)
  .settings(scoverageSettings *)
  .settings(defaultSettings() *)
  .settings(majorVersion := 1)
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
    Test / javaOptions += "-Dlogger.resource=logback-test.xml")
  .settings(
    Keys.fork := false,
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components.implicits._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components.implicits._"
    ),
    RoutesKeys.routesImport := Seq("enums.IncomeSourceJourney._", "models.admin._", "models.core._"),
  )
  .settings(resolvers ++= Seq(
    Resolver.jcenterRepo
  ))
  .settings(semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision)

lazy val it = project
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings().head)
  .enablePlugins(play.sbt.PlayScala)
  .settings(
    publish / skip := true
  )
  .settings(scalaVersion := currentScalaVersion)
  .settings(majorVersion := 1)
  .settings(
    testForkedParallel := true
  )
  .settings(
    libraryDependencies ++= appDependenciesIt
  )
