import play.sbt.PlayImport.{caffeine, ws}
import sbt.*

object AppDependencies {

  val bootstrapPlayVersion = "8.1.0"
  val playPartialsVersion = "9.1.0"
  val playFrontendHMRCVersion = "8.5.0"
  val catsVersion = "2.8.0"

  val scalaTestPlusVersion = "7.0.0"
  val pegdownVersion = "1.6.0"
  val jsoupVersion = "1.15.4"
  val mockitoVersion = "5.8.0"
  val scalaMockVersion = "5.2.0"
  val wiremockVersion = "3.0.0-beta-7"
  val hmrcMongoVersion = "1.6.0"
  val playVersion = "play-30"


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

  def test(scope: String = "test"): Seq[ModuleID] =
    Seq(
      "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
      "org.scalamock" %% "scalamock" % scalaMockVersion % scope,
      "org.pegdown" % "pegdown" % pegdownVersion % scope,
      "org.jsoup" % "jsoup" % jsoupVersion % scope,
      "org.mockito" % "mockito-core" % mockitoVersion % scope,
      "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion % scope,
      "org.scalacheck" %% "scalacheck" % "1.17.0" % scope,
      "org.scalatestplus" %% "scalacheck-1-15" % "3.2.11.0" % scope,
      "uk.gov.hmrc" %% s"bootstrap-test-$playVersion" % bootstrapPlayVersion % "test",
      caffeine,
      "uk.gov.hmrc" %% s"crypto-json-$playVersion" % "7.6.0"
    )

  def it(scope: String = "test"): Seq[ModuleID] =
    Seq(
      "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
      "org.scalamock" %% "scalamock" % scalaMockVersion % scope,
      "org.pegdown" % "pegdown" % pegdownVersion % scope,
      "org.jsoup" % "jsoup" % jsoupVersion % scope,
      "org.mockito" % "mockito-core" % mockitoVersion % scope,
      "com.github.tomakehurst" % "wiremock" % wiremockVersion % scope,
      "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion % scope,
      caffeine
    )

  lazy val appDependencies: Seq[ModuleID] = compile ++ test() ++ it()

  def apply(): Seq[ModuleID] = appDependencies

}
