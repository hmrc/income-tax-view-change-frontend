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

package controllers.manageBusinesses.manage

import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.IncomeSourceJourney._
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import enums.{AnnualReportingMethod, CannotGoBackPage, QuarterlyReportingMethod}
import models.core.IncomeSourceId
import models.incomeSourceDetails.TaxYear.getTaxYearModel
import models.admin.OptInOptOutContentUpdateR17
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateService, IncomeSourceDetailsService, NextUpdatesService, SessionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyCheckerManageBusinesses
import views.html.manageBusinesses.manage.ManageObligations

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ManageObligationsController @Inject()(val authActions: AuthActions,
                                            val itvcErrorHandler: ItvcErrorHandler,
                                            val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                            val incomeSourceDetailsService: IncomeSourceDetailsService,
                                            val obligationsView: ManageObligations,
                                            val sessionService: SessionService,
                                            nextUpdatesService: NextUpdatesService,
                                            val dateService: DateService)
                                           (implicit val ec: ExecutionContext,
                                            val mcc: MessagesControllerComponents,
                                            val appConfig: FrontendAppConfig) extends FrontendController(mcc)
  with I18nSupport with JourneyCheckerManageBusinesses {

  private lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  def show(isAgent: Boolean,
           incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withSessionData(IncomeSourceJourneyType(Manage, incomeSourceType), journeyState = CannotGoBackPage) { sessionData =>
        val (reportingMethodOpt, taxYearOpt, incomeSourceIdStringOpt) = (
          sessionData.manageIncomeSourceData.flatMap(_.reportingMethod),
          sessionData.manageIncomeSourceData.flatMap(_.taxYear),
          sessionData.manageIncomeSourceData.flatMap(_.incomeSourceId)
        )

        (incomeSourceType, taxYearOpt, reportingMethodOpt, incomeSourceIdStringOpt) match {
          case (SelfEmployment, Some(taxYear), Some(reportingMethod), incomeSourceIdStringOpt) =>
            val incomeSourceId: Option[IncomeSourceId] = incomeSourceIdStringOpt.map(id => IncomeSourceId(id))
            handleRequest(
              incomeSourceType,
              isAgent,
              s"${taxYear - 1}-$taxYear",
              reportingMethod,
              incomeSourceId
            )
          case (_, Some(taxYear), Some(reportingMethod), _) =>
            handleRequest(
              incomeSourceType,
              isAgent,
              s"${taxYear - 1}-$taxYear",
              reportingMethod,
              None
            )
          case (_, _, _, _) =>
            Logger("application").error(s"Missing session values")
            Future.successful {
              errorHandler(isAgent).showInternalServerError()
            }
        }
      }
  }

  private lazy val successPostUrl = (isAgent: Boolean) => {
    controllers.manageBusinesses.manage.routes.ManageObligationsController.submit(isAgent)
  }

  def handleRequest(incomeSourceType: IncomeSourceType, isAgent: Boolean, taxYear: String, changeTo: String, incomeSourceId: Option[IncomeSourceId])
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    (getTaxYearModel(taxYear), changeTo) match {
      case (Some(years), AnnualReportingMethod.name | QuarterlyReportingMethod.name) =>
        getIncomeSourceId(incomeSourceType, incomeSourceId, isAgent = isAgent) match {
          case Some(incomeSourceId) =>
            val addedBusinessName: String = getBusinessName(incomeSourceType, Some(incomeSourceId))
            nextUpdatesService.getObligationsViewModel(incomeSourceId.value, showPreviousTaxYears = false) map { viewModel =>
              Ok(obligationsView(
                sources = viewModel,
                businessName = addedBusinessName,
                taxYear = years,
                changeTo = changeTo,
                isAgent = isAgent,
                postAction = successPostUrl(isAgent),
                showOptInOptOutContentUpdateR17 = isEnabled(OptInOptOutContentUpdateR17),
                isCurrentTaxYear = dateService.getCurrentTaxYearStart.getYear == years.startYear
              ))
            }
          case None => showError(isAgent, s"Unable to retrieve income source ID for $incomeSourceType")
        }
      case (Some(_), _) =>
        showError(isAgent, s"Invalid changeTo mode provided: -$changeTo-")
      case (None, _) =>
        showError(isAgent, "Invalid tax year provided")
    }
  }

  private def showError(isAgent: Boolean, message: String)(implicit user: MtdItUser[_]): Future[Result] = {
    Logger("application").error(
      s"${if (isAgent) "[Agent]"}$message")
    Future.successful {
      errorHandler(isAgent).showInternalServerError()
    }
  }

  private def getBusinessName(mode: IncomeSourceType, incomeSourceId: Option[IncomeSourceId])(implicit user: MtdItUser[_]): String = {
    (mode, incomeSourceId) match {
      case (SelfEmployment, Some(incomeSourceId)) =>
        val businessDetailsParams = for {
          addedBusiness <- user.incomeSources.businesses.find(businessDetailsModel => businessDetailsModel.incomeSourceId.contains(incomeSourceId.value))
          businessName <- addedBusiness.tradingName
        } yield (addedBusiness, businessName)
        businessDetailsParams match {
          case Some((_, name)) => name
          case None => "Not Found"
        }
      case (UkProperty, _) => "UK property"
      case (ForeignProperty, _) => "Foreign property"
      case _ => "Not Found"
    }
  }

  private def getIncomeSourceId(incomeSourceType: IncomeSourceType, id: Option[IncomeSourceId], isAgent: Boolean)
                       (implicit user: MtdItUser[_]): Option[IncomeSourceId] = {

    incomeSourceType match {
      case SelfEmployment =>
        id.orElse {
          val message = "Missing required income source ID for Self Employment"
          Logger("application").error(message)
          None
        }
      case UkProperty | ForeignProperty =>
        getActiveProperty(incomeSourceType).map(property => IncomeSourceId(property.incomeSourceId))
    }
  }


  def submit(isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async { _ =>
    if(isAgent) Future.successful(Redirect(controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent()))
    else Future.successful(Redirect(controllers.manageBusinesses.routes.ManageYourBusinessesController.show()))
  }
}
