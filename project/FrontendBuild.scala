import sbt._
import play.sbt.PlayImport._
import play.core.PlayVersion
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

object FrontendBuild extends Build with MicroService {

  val appName = "income-tax-view-change-frontend"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()


  val frontendBootstrapVersion  = "7.26.0"
  val playPartialsVersion       = "5.4.0"
  val playAuthVersion           = "1.2.0"
  val playConfigVersion         = "4.3.0"
  val logbackVersion            = "3.1.0"
  val govTemplateVersion        = "5.2.0"
  val playHealthVersion         = "2.1.0"
  val playUiVersion             = "7.4.0"

  val scalaTestPlusVersion      = "2.0.0"
  val hmrcTestVesrion           = "2.3.0"
  val scalatestVersion          = "3.0.0"
  val pegdownVersion            = "1.6.0"
  val jsoupVersion              = "1.10.2"
  val mockitoVersion            = "2.7.17"
  val scalaMockVersion          = "3.5.0"
  val wiremockVersion           = "2.5.1"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion,
    "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
    "uk.gov.hmrc" %% "play-auth" % playAuthVersion,
    "uk.gov.hmrc" %% "play-config" % playConfigVersion,
    "uk.gov.hmrc" %% "logback-json-logger" % logbackVersion,
    "uk.gov.hmrc" %% "govuk-template" % govTemplateVersion,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-ui" % playUiVersion
  )

  def test(scope: String = "test,it") = Seq(
    "uk.gov.hmrc" %% "hmrctest" % hmrcTestVesrion % scope,
    "org.scalatest" %% "scalatest" % scalatestVersion % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
    "org.scalamock" %% "scalamock-scalatest-support" % scalaMockVersion % scope,
    "org.pegdown" % "pegdown" % pegdownVersion % scope,
    "org.jsoup" % "jsoup" % jsoupVersion % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.mockito" % "mockito-core" % mockitoVersion % scope,
    "com.github.tomakehurst" % "wiremock" % wiremockVersion % scope
  )

}