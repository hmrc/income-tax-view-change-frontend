/*
 * Copyright 2024 HM Revenue & Customs
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
import enums.JourneyType.{Opt, OptOutJourney}
import models.UIJourneySessionData
import models.optout.OptOutSessionData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
class OptOutMongoTestJourneyController @Inject()(
                                        sessionService: SessionService,
                                        implicit val mcc: MessagesControllerComponents,
                                        val appConfig: FrontendAppConfig
                                      )(implicit val executionContext: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  def show: Action[AnyContent] = Action.async { implicit request =>
    sessionService.getMongo(Opt(OptOutJourney)).map {
      case Right(Some(sessionData)) =>
        sessionData.optOutSessionData.map(_.selectedOptOutYear) match {
          case Some(intent) => Ok(s"Intent = $intent")
          case None => Ok("No intent found")
        }
      case Right(None) =>
        Ok("No session data found")
      case Left(ex) =>
        Ok(s"Error retrieving session data: ${ex.getMessage}")
    }
  }


  def upsert(keyOpt: Option[String], valueOpt: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      val res = for {
        key <- keyOpt
        value <- valueOpt
      } yield (key, value)
      res match {
        case Some(_) =>
          sessionService.getMongo(Opt(OptOutJourney)).flatMap {
            case Right(Some(sessionDataOption)) =>
                val optOutUIJourneySessionData: UIJourneySessionData = sessionDataOption.copy(optOutSessionData = Some(OptOutSessionData(None, valueOpt)))
                sessionService.setMongoData(optOutUIJourneySessionData).map { _ =>
                  Redirect("/report-quarterly/income-and-expenses/view/test-only/showOptOutSession")
                }
            case Right(None) =>
              val newSessionData = UIJourneySessionData(
                sessionId = hc.sessionId.get.value,
                journeyType = "OPTOUT",
                optOutSessionData = Some(OptOutSessionData(None, valueOpt))
              )
              sessionService.createSession(Opt(OptOutJourney))
              sessionService.setMongoData(newSessionData).map { _ =>
                Redirect("/report-quarterly/income-and-expenses/view/test-only/showOptOutSession")
              }
            case Left(ex) =>
              Future.successful(Ok(s"Unable to add data to session storage ex: ${ex.getMessage}"))
          }
        case None =>
          Future.successful(Ok(s"Unable to add data to session storage"))
      }
  }
}
