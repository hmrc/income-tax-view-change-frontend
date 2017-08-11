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

import config.AppConfig
import forms.ExitSurveyForm
import models.ExitSurveyModel
import play.api.Application
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request}
import play.twirl.api.Html
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class ExitSurveyController @Inject()(implicit val applicationConfig: AppConfig,
                                     val messagesApi: MessagesApi
                                    ) extends FrontendController with I18nSupport {

  def view(exitSurveyForm: Form[ExitSurveyModel])(implicit request: Request[_]): Html =
    views.html.exit_survey(
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
        //TODO: Store Survey in Splunk and redirect to 'Thank You'
        //submitSurvey(survey)
        Future.successful(Redirect(routes.HomeController.redirect()))
      }
    )
  }

  //TODO: Uncomment and Implement Splunk Audit
//  def submitSurvey(survey: ExitSurveyModel)(implicit hc: HeaderCarrier): Unit = {
//    val surveyAsMap = surveyFormDataToMap(survey)
//    if (surveyAsMap.nonEmpty)
//      logging.audit(transactionName = "ITSA Survey", detail = surveyAsMap, auditType = Logging.eventTypeSurveyFeedback)
//  }

  private[controllers] final def surveyFormDataToMap(survey: ExitSurveyModel): Map[String, String] = {
    survey.getClass.getDeclaredFields.map {
      field =>
        field.setAccessible(true)
        field.getName -> (field.get(survey) match {
          case Some(x) => x.toString
          case xs: Seq[Any] => xs.mkString(",")
          case x => x.toString
        })
    }.toMap.filter { case (x, y) =>
      // don't keep any fields in the map if they were not answered
      y match {
        case "None" => false
        case _ => true
      }
    }
  }

}
