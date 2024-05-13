/*
 * Copyright 2023 HM Revenue & Customs
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

package testOnly.controllers

import config.FrontendAppConfig
import play.api.i18n.I18nSupport
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.Future

class SessionStorageController @Inject()
                              (implicit mcc: MessagesControllerComponents,
                               val appConfig: FrontendAppConfig)
  extends FrontendController(mcc)  with I18nSupport {


  val show: Action[AnyContent] = Action.async { implicit request =>
    // we really don't need to show these
    val filterOutKeys = Seq("sessionId", "authToken", "csrfToken", "origin")
    val sessionDataStr: String = request.session
      .data
      .filter( kv => !filterOutKeys.contains(kv._1))
      .mkString("\n")
    Future.successful(Ok(sessionDataStr))
  }

  def upsert(keyOpt: Option[String], valueOpt: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      val res = for {
        key <- keyOpt
        value <- valueOpt
      } yield (key, value)
      res match {
        case Some((k, v)) =>
          Future.successful(
            Redirect("/report-quarterly/income-and-expenses/view/test-only/showSession")
              .withSession(request.session + (k -> v))
          )
        case None =>
          Future.successful(Ok("Unable to add data to session storage"))
    }

  }

}
