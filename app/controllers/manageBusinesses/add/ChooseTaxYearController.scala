/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.manageBusinesses.add

import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.IncomeSourceJourney.IncomeSourceType
import enums.JourneyType.{IncomeSourceJourneyType, ManageReportingFrequency}
import forms.manageBusinesses.add.ChooseTaxYearForm
import models.incomeSourceDetails.IncomeSourceReportingFrequencySourceData
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{DateService, SessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.IncomeSourcesUtils
import views.html.manageBusinesses.add.ChooseTaxYear

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ChooseTaxYearController @Inject()(authActions: AuthActions,
                                        chooseTaxYear: ChooseTaxYear,
                                        dateService: DateService,
                                        sessionService: SessionService,
                                        val itvcErrorHandler: ItvcErrorHandler,
                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                       (implicit val mcc: MessagesControllerComponents,
                                        val ec: ExecutionContext,
                                        val appConfig: FrontendAppConfig)
  extends FrontendController(mcc) with I18nSupport with IncomeSourcesUtils {

  private def errorHandler(isAgent: Boolean) = if (isAgent) {
    itvcErrorHandlerAgent
  } else {
    itvcErrorHandler
  }

  def show(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] =
    authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
      implicit user => {
        handleRequest(isAgent, incomeSourceType)
      }
  }

  private def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = {
    withNewIncomeSourcesFS {
      Future.successful(Ok(chooseTaxYear(
        ChooseTaxYearForm(),
        isAgent,
        routes.ChooseTaxYearController.submit(isAgent, incomeSourceType),
        dateService.getCurrentTaxYear,
        dateService.getCurrentTaxYear.nextYear,
        incomeSourceType))
      )
    }
  }.recover {
    case ex =>
      Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
      errorHandler(isAgent).showInternalServerError()
  }

  def submit(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] =
    authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
      implicit user => {
        ChooseTaxYearForm().bindFromRequest.fold(
          formWithError => {
            Future.successful(BadRequest(chooseTaxYear(
              formWithError,
              isAgent,
              routes.ChooseTaxYearController.submit(isAgent, incomeSourceType),
              dateService.getCurrentTaxYear,
              dateService.getCurrentTaxYear.nextYear))
            )
          },
          form => {
            val journeyType = IncomeSourceJourneyType(ManageReportingFrequency, incomeSourceType)

            sessionService.getMongo(journeyType).flatMap {
              case Right(Some(sessionData)) =>
                val updatedSessionData = IncomeSourceReportingFrequencySourceData(form.currentTaxYear, form.nextTaxYear)

                sessionService.setMongoData(sessionData.copy(incomeSourceReportingFrequencyData = Some(updatedSessionData)))
                Future.successful(Ok(chooseTaxYear(
                  ChooseTaxYearForm(),
                  isAgent,
                  routes.ChooseTaxYearController.submit(isAgent, incomeSourceType),
                  dateService.getCurrentTaxYear,
                  dateService.getCurrentTaxYear.nextYear)))

              case _ => Future.failed(new Exception(s"failed to retrieve session data for journey ${journeyType.toString}"))
            }
          }
        )
      }.recover {
        case ex =>
          Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
          errorHandler(isAgent).showInternalServerError()
      }
    }
}
