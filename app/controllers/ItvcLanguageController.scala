/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import config.FrontendAppConfig

import javax.inject.{Inject, Singleton}
import play.api.i18n.Lang
import play.api.mvc.{Action, AnyContent, Headers, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.play.language.{LanguageController, LanguageUtils}

import scala.concurrent.Future

@Singleton
class ItvcLanguageController @Inject()(mcc: MessagesControllerComponents,
                                       appConfig: FrontendAppConfig,
                                       languageUtils: LanguageUtils) extends LanguageController(languageUtils, mcc) {


  override def fallbackURL: String = controllers.routes.HomeController.home().url

  val english: Lang = Lang("en")
  val welsh: Lang = Lang("cy")


  override def languageMap: Map[String, Lang] = Map("en" -> english, "cy" -> welsh)

  private def switchLang(fragment: Option[String], lang: String)(implicit request: Request[AnyContent]): Future[Result] = {
    val frag = if (fragment.isDefined) s"#${fragment.get}" else ""
    if (request.headers.get("Referer").isDefined) {
      val currentReferer = request.headers.get("Referer").get
      switchToLanguage(lang)(request.withHeaders(Headers("Referer" -> s"$currentReferer$frag")))
    } else switchToLanguage(lang)(request)
  }

  def switchToEnglish(fragment: Option[String]): Action[AnyContent] = Action.async {
    request: Request[AnyContent] =>
      switchLang(fragment, "en")(request)
  }

  def switchToWelsh(fragment: Option[String]): Action[AnyContent] = Action.async {
    request: Request[AnyContent] =>
      switchLang(fragment, "cy")(request)
  }
}
