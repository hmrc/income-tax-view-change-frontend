package utils

import helpers.ComponentSpecBase
import play.api.Environment
import play.api.i18n.MessagesApi

import scala.io.Source

class WelshMessagesSpec extends ComponentSpecBase {

  private val env: Environment = app.injector.instanceOf[Environment]
  private val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  private def loadMessagesFile(path: String): Map[String, String] = {
    env.resourceAsStream(path)
      .map(Source.fromInputStream)
      .map(_.getLines().toList)
      .map { lines =>
        lines
          .filterNot(_.trim.startsWith("#"))
          .filter(_.contains("="))
          .map { line =>
            val Array(key, value) = line.split("=", 2)
            key.trim -> value.trim
          }.toMap
      }
      .getOrElse(Map.empty)
  }

  private val englishMessages = loadMessagesFile("messages")
  private val welshMessages   = loadMessagesFile("messages.cy")

  "all English messages should have a Welsh translation" in {
    val missingWelshKeys = englishMessages.keySet.diff(welshMessages.keySet)

    if (missingWelshKeys.nonEmpty) {
      val failureText = missingWelshKeys.foldLeft(
        s"There are ${missingWelshKeys.size} missing Welsh translations:"
      ) {
        case (out, key) => out + s"\n$key: ${englishMessages(key)}"
      }
      fail(failureText)
    }
  }

  "all Welsh messages should have an English translation" in {
    val missingEnglishKeys = welshMessages.keySet.diff(englishMessages.keySet)

    if (missingEnglishKeys.nonEmpty) {
      val failureText = missingEnglishKeys.foldLeft(
        s"There are ${missingEnglishKeys.size} missing English translations:"
      ) {
        case (out, key) => out + s"\n$key: ${welshMessages(key)}"
      }
      fail(failureText)
    }
  }
}
