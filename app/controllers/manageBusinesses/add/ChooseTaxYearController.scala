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
import config.FrontendAppConfig
import enums.IncomeSourceJourney.{IncomeSourceType, ReportingFrequencyPages}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import forms.manageBusinesses.add.ChooseTaxYearForm
import forms.models.ChooseTaxYearFormModel
import models.incomeSourceDetails.{IncomeSourceReportingFrequencySourceData, UIJourneySessionData}
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.manageBusinesses.IncomeSourceRFService
import services.{CalculationListService, DateService, SessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyCheckerManageBusinesses
import views.html.manageBusinesses.add.ChooseTaxYear

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ChooseTaxYearController @Inject()(authActions: AuthActions,
                                        chooseTaxYear: ChooseTaxYear,
                                        val calculationListService: CalculationListService,
                                        val incomeSourceRFService: IncomeSourceRFService,
                                        val form: ChooseTaxYearForm,
                                        val sessionService: SessionService)
                                       (implicit val mcc: MessagesControllerComponents,
                                        val ec: ExecutionContext,
                                        val appConfig: FrontendAppConfig,
                                        val dateService: DateService)
  extends FrontendController(mcc) with I18nSupport with JourneyCheckerManageBusinesses {

  def show(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] =
    authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
      implicit user => {
        handleRequest(isAgent, isChange, incomeSourceType)
      }
  }

  private def handleRequest(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = {
    incomeSourceRFService.redirectChecksForIncomeSourceRF(IncomeSourceJourneyType(Add, incomeSourceType),
      ReportingFrequencyPages, incomeSourceType, dateService.getCurrentTaxYearEnd, isAgent, isChange) { sessionData =>
      val filledOrEmptyForm: Form[ChooseTaxYearFormModel] = {
        if(isChange) {
          val data: ChooseTaxYearFormModel = {
            (displayOptionToChangeForCurrentTy(sessionData), displayOptionToChangeForNextTy(sessionData)) match {
              case (true, true) => ChooseTaxYearFormModel(isCheckedForCurrentTy(sessionData), isCheckedForNextTy(sessionData))
              case (true, false) => ChooseTaxYearFormModel(isCheckedForCurrentTy(sessionData), None)
              case (false, true) => ChooseTaxYearFormModel(None, isCheckedForNextTy(sessionData))
              case _ => ChooseTaxYearFormModel(None, None)
            }
          }
          sessionData.incomeSourceReportingFrequencyData.fold(form())(_ => form().fill(data))
        } else form()
      }

      Future.successful(Ok(chooseTaxYear(
        filledOrEmptyForm,
        isAgent,
        routes.ChooseTaxYearController.submit(isAgent, isChange, incomeSourceType),
        if (displayOptionToChangeForCurrentTy(sessionData)) Some(dateService.getCurrentTaxYear) else None,
        if (displayOptionToChangeForNextTy(sessionData)) Some(dateService.getNextTaxYear) else None,
        incomeSourceType,
        isChange
      )))
    }
  }

  def submit(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] =
    authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
      implicit user => {
        val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
        sessionService.getMongo(journeyType).flatMap {
          case Right(Some(sessionData)) =>
            form().bindFromRequest.fold(
              formWithError => {
                Future.successful(BadRequest(chooseTaxYear(
                  formWithError,
                  isAgent,
                  routes.ChooseTaxYearController.submit(isAgent, isChange, incomeSourceType),
                  if(displayOptionToChangeForCurrentTy(sessionData)) Some(dateService.getCurrentTaxYear) else None,
                  if(displayOptionToChangeForNextTy(sessionData)) Some(dateService.getNextTaxYear) else None,
                  incomeSourceType,
                  isChange
                ))
                )
              },
              form => {
                val updatedSessionData = IncomeSourceReportingFrequencySourceData(
                  displayOptionToChangeForCurrentTy(sessionData), displayOptionToChangeForNextTy(sessionData),
                  form.currentTaxYear.contains(true), form.nextTaxYear.contains(true))
                sessionService.setMongoData(sessionData.copy(incomeSourceReportingFrequencyData = Some(updatedSessionData)))
                Future.successful(Redirect(controllers.manageBusinesses.add.routes.IncomeSourceRFCheckDetailsController.show(isAgent, incomeSourceType)))
              }
            )
          case _ => Future.failed(new Exception(s"failed to retrieve session data for journey ${journeyType.toString}"))
        }.recover {
          case ex =>
            Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
            incomeSourceRFService.errorHandler(isAgent).showInternalServerError()
        }
      }
    }

  private def isCheckedForCurrentTy(sessionData: UIJourneySessionData): Option[Boolean] = {
    Some(displayOptionToChangeForCurrentTy(sessionData) && sessionData.incomeSourceReportingFrequencyData.exists(_.isReportingQuarterlyCurrentYear))
  }

  private def isCheckedForNextTy(sessionData: UIJourneySessionData): Option[Boolean] =
    Some(displayOptionToChangeForNextTy(sessionData) && sessionData.incomeSourceReportingFrequencyData.exists(_.isReportingQuarterlyForNextYear))

  private def displayOptionToChangeForCurrentTy(sessionData: UIJourneySessionData): Boolean =
    sessionData.incomeSourceReportingFrequencyData.exists(_.displayOptionToChangeForCurrentTaxYear)

  private def displayOptionToChangeForNextTy(sessionData: UIJourneySessionData): Boolean =
    sessionData.incomeSourceReportingFrequencyData.exists(_.displayOptionToChangeForNextTaxYear)
}
