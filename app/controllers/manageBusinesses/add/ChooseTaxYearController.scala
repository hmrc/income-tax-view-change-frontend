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
import enums.IncomeSourceJourney.IncomeSourceType
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import enums.ReportingFrequencyPages
import forms.manageBusinesses.add.ChooseTaxYearForm
import forms.models.ChooseTaxYearFormModel
import models.admin.OptInOptOutContentUpdateR17
import models.incomeSourceDetails.{IncomeSourceReportingFrequencySourceData, UIJourneySessionData}
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.manageBusinesses.IncomeSourceRFService
import services.{CalculationListService, DateService, SessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyCheckerManageBusinesses
import views.html.manageBusinesses.add.ChooseTaxYearView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ChooseTaxYearController @Inject()(authActions: AuthActions,
                                        chooseTaxYearView: ChooseTaxYearView,
                                        val calculationListService: CalculationListService,
                                        val incomeSourceRFService: IncomeSourceRFService,
                                        val form: ChooseTaxYearForm,
                                        val sessionService: SessionService)
                                       (implicit val mcc: MessagesControllerComponents,
                                        val ec: ExecutionContext,
                                        val appConfig: FrontendAppConfig,
                                        val dateService: DateService)
  extends FrontendController(mcc) with I18nSupport with JourneyCheckerManageBusinesses {

  private def isCheckedForCurrentTy(sessionData: UIJourneySessionData): Option[Boolean] = {
    Some(displayOptionToChangeForCurrentTy(sessionData) && sessionData.incomeSourceReportingFrequencyData.exists(_.isReportingQuarterlyCurrentYear))
  }

  private def isCheckedForNextTy(sessionData: UIJourneySessionData): Option[Boolean] =
    Some(displayOptionToChangeForNextTy(sessionData) && sessionData.incomeSourceReportingFrequencyData.exists(_.isReportingQuarterlyForNextYear))

  private def displayOptionToChangeForCurrentTy(sessionData: UIJourneySessionData): Boolean =
    sessionData.incomeSourceReportingFrequencyData.exists(_.displayOptionToChangeForCurrentTaxYear)

  private def displayOptionToChangeForNextTy(sessionData: UIJourneySessionData): Boolean =
    sessionData.incomeSourceReportingFrequencyData.exists(_.displayOptionToChangeForNextTaxYear)

  def show(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] =
    authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
      implicit user => {
        handleRequest(isAgent, isChange, incomeSourceType)
      }
    }

  private def handleRequest(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = {
    incomeSourceRFService.redirectChecksForIncomeSourceRF(IncomeSourceJourneyType(Add, incomeSourceType),
      ReportingFrequencyPages, incomeSourceType, dateService.getCurrentTaxYearEnd, isAgent, isChange) { sessionData =>

      val chooseTaxYearForm = form(isEnabled(OptInOptOutContentUpdateR17))

      val filledOrEmptyForm: Form[ChooseTaxYearFormModel] = {
        if (isChange) {
          val data: ChooseTaxYearFormModel = {
            (displayOptionToChangeForCurrentTy(sessionData), displayOptionToChangeForNextTy(sessionData)) match {
              case (true, true) => ChooseTaxYearFormModel(isCheckedForCurrentTy(sessionData), isCheckedForNextTy(sessionData))
              case (true, false) => ChooseTaxYearFormModel(isCheckedForCurrentTy(sessionData), None)
              case (false, true) => ChooseTaxYearFormModel(None, isCheckedForNextTy(sessionData))
              case _ => ChooseTaxYearFormModel(None, None)
            }
          }
          sessionData.incomeSourceReportingFrequencyData.fold(chooseTaxYearForm)(_ => chooseTaxYearForm.fill(data))
        } else {
          chooseTaxYearForm
        }
      }

      Future.successful(
        Ok(
          chooseTaxYearView(
            form = filledOrEmptyForm,
            isAgent = isAgent,
            postAction = routes.ChooseTaxYearController.submit(isAgent, isChange, incomeSourceType),
            currentTaxYear = if (displayOptionToChangeForCurrentTy(sessionData)) Some(dateService.getCurrentTaxYear) else None,
            nextTaxYear = if (displayOptionToChangeForNextTy(sessionData)) Some(dateService.getNextTaxYear) else None,
            incomeSourceType = incomeSourceType,
            isChange = isChange,
            isOptInOptOutContentUpdateR17 = isEnabled(OptInOptOutContentUpdateR17)
          )))
    }
  }

  def submit(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] =
    authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
      implicit user => {
        val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
        sessionService.getMongo(journeyType).flatMap {
          case Right(Some(sessionData)) =>
            val chooseTaxYearForm = form(isEnabled(OptInOptOutContentUpdateR17))
            chooseTaxYearForm
              .bindFromRequest()
              .fold(
                formWithError => {
                  Future.successful(BadRequest(
                    chooseTaxYearView(
                      form = formWithError,
                      isAgent = isAgent,
                      postAction = routes.ChooseTaxYearController.submit(isAgent, isChange, incomeSourceType),
                      currentTaxYear = if (displayOptionToChangeForCurrentTy(sessionData)) Some(dateService.getCurrentTaxYear) else None,
                      nextTaxYear = if (displayOptionToChangeForNextTy(sessionData)) Some(dateService.getNextTaxYear) else None,
                      incomeSourceType = incomeSourceType,
                      isChange = isChange,
                      isOptInOptOutContentUpdateR17 = isEnabled(OptInOptOutContentUpdateR17)
                    ))
                  )
                },
                form => {
                  val updatedSessionData =
                    IncomeSourceReportingFrequencySourceData(
                      displayOptionToChangeForCurrentTaxYear = displayOptionToChangeForCurrentTy(sessionData),
                      displayOptionToChangeForNextTaxYear = displayOptionToChangeForNextTy(sessionData),
                      isReportingQuarterlyCurrentYear = form.currentTaxYear.contains(true),
                      isReportingQuarterlyForNextYear = form.nextTaxYear.contains(true)
                    )
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
}
