import sbt._
import play.sbt.PlayImport._
import play.core.PlayVersion
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

object FrontendBuild extends Build with MicroService {

  val appName = "income-tax-view-change-frontend"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()


  val frontendBootstrapVersion  = "0.12.0"
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
    "uk.gov.hmrc" %% "bootstrap-play-25" % frontendBootstrapVersion,
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

}