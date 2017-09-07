/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import audit.AuditingService
import audit.models.ExitSurveyAuditing.ExitSurveyAuditModel
import config.AppConfig
import forms.ExitSurveyForm
import models.ExitSurveyModel
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class ExitSurveyController @Inject()(implicit val applicationConfig: AppConfig,
                                     val messagesApi: MessagesApi,
                                     val auditingService: AuditingService
                                    ) extends FrontendController with I18nSupport {

  def view(exitSurveyForm: Form[ExitSurveyModel])(implicit request: Request[_]): Html =
    views.html.exit_survey.exit_survey(
      surveyForm = exitSurveyForm,
      routes.ExitSurveyController.submit()
    )

  val show: Action[AnyContent] = Action { implicit request =>
    Ok(view(ExitSurveyForm.exitSurveyForm))
  }

  val submit: Action[AnyContent] = Action.async { implicit request =>
    ExitSurveyForm.exitSurveyForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(exitSurveyForm = formWithErrors))),
      survey => {
        submitSurvey(survey)
        Future.successful(Redirect(routes.ThankYouController.show()))
      }
    )
  }

  def submitSurvey(survey: ExitSurveyModel)(implicit hc: HeaderCarrier): Unit =
    auditingService.audit(ExitSurveyAuditModel(survey), controllers.routes.ExitSurveyController.show().url)

}
