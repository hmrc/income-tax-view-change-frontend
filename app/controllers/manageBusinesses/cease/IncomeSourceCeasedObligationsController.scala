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

package controllers.manageBusinesses.cease

import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.routes
import enums.IncomeSourceJourney.*
import enums.IncomeSourceJourney.IncomeSourceType.*
import enums.JourneyState.CannotGoBackPage
import enums.JourneyType.{Cease, IncomeSourceJourneyType}
import models.UIJourneySessionData
import models.admin.ReportingFrequencyPage
import models.core.IncomeSourceId
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.CeaseIncomeSourceData
import models.incomeSourceDetails.viewmodels.IncomeSourceCeasedObligationsViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.*
import services.{NextUpdatesService, SessionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyCheckerManageBusinesses
import views.html.manageBusinesses.cease.IncomeSourceCeasedObligationsView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceCeasedObligationsController @Inject()(val authActions: AuthActions,
                                                        val itvcErrorHandler: ItvcErrorHandler,
                                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                        val obligationsView: IncomeSourceCeasedObligationsView,
                                                        val nextUpdatesService: NextUpdatesService,
                                                        val sessionService: SessionService)
                                                       (implicit val appConfig: FrontendAppConfig,
                                                        val mcc: MessagesControllerComponents,
                                                        val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with JourneyCheckerManageBusinesses {

  private def getBusinessName(incomeSourceId: IncomeSourceId)(implicit user: MtdItUser[_]): Option[String] = {
    user.incomeSources.businesses
      .find(m => mkIncomeSourceId(m.incomeSourceId) == incomeSourceId)
      .flatMap(_.tradingName)
  }

  private def getIncomeSourceType(sessionData: UIJourneySessionData, incomeSourceType: IncomeSourceType)
                                 (implicit user: MtdItUser[_]): Option[String] = {
    incomeSourceType match {
      case SelfEmployment =>
        sessionData.ceaseIncomeSourceData.flatMap(_.incomeSourceId)
      case UkProperty =>
        user.incomeSources.properties.filter(_.isUkProperty)
          .map(incomeSource => incomeSource.incomeSourceId).headOption
      case ForeignProperty =>
        user.incomeSources.properties.filter(_.isForeignProperty)
          .map(incomeSource => incomeSource.incomeSourceId).headOption
    }
  }

  private def viewReportingObligationsLink(isAgent: Boolean): String =
    routes.ReportingFrequencyPageController.show(isAgent).url

  private def updateMongoCeased(incomeSourceType: IncomeSourceType)(implicit hc: HeaderCarrier): Future[Boolean] = {
    sessionService.getMongo(IncomeSourceJourneyType(Cease, incomeSourceType)).flatMap {
      case Right(Some(sessionData)) =>
        val oldCeaseIncomeSourceSessionData = sessionData.ceaseIncomeSourceData.getOrElse(
          CeaseIncomeSourceData(
            incomeSourceId = Some(CeaseIncomeSourceData.incomeSourceIdField),
            endDate = None,
            ceaseIncomeSourceDeclare = None,
            journeyIsComplete = None
          )
        )
        val updatedCeaseIncomeSourceSessionData = oldCeaseIncomeSourceSessionData.copy(journeyIsComplete = Some(true))
        val uiJourneySessionData: UIJourneySessionData = sessionData.copy(ceaseIncomeSourceData = Some(updatedCeaseIncomeSourceSessionData))

        sessionService.setMongoData(uiJourneySessionData)

      case _ =>
        Future.failed(new Exception("failed to retrieve session data"))
    }
  }


  def viewAllBusinessLink(isAgent: Boolean): String =
    if (isAgent) {
      controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
    } else {
      controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
    }

  def viewUpcomingUpdatesLink(isAgent: Boolean): String =
    if (isAgent) {
      controllers.routes.NextUpdatesController.showAgent().url
    } else {
      controllers.routes.NextUpdatesController.show().url
    }

  private def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {

    withSessionData(IncomeSourceJourneyType(Cease, incomeSourceType), CannotGoBackPage) { sessionData =>

      updateMongoCeased(incomeSourceType)
      val businessEndDate: Option[LocalDate] = sessionData.ceaseIncomeSourceData.flatMap(_.endDate)

      (getIncomeSourceType(sessionData, incomeSourceType), businessEndDate) match {
        case (Some(incomeSourceId), Some(_)) =>
          val businessName = if (incomeSourceType == SelfEmployment) getBusinessName(IncomeSourceId(incomeSourceId)) else None
          val incomeSourceCeasedObligationsViewModel =
            IncomeSourceCeasedObligationsViewModel(
              incomeSourceDetailsModel = user.incomeSources,
              incomeSourceType = incomeSourceType,
              businessName = businessName,
              isAgent = isAgent
            )
          Future(
            Ok(obligationsView(
              source = incomeSourceCeasedObligationsViewModel,
              viewAllBusinessLink = viewAllBusinessLink(isAgent),
              viewUpcomingUpdatesLink = viewUpcomingUpdatesLink(isAgent),
              reportingObligationsLink = if (isEnabled(ReportingFrequencyPage)) Some(viewReportingObligationsLink(isAgent)) else None
            )))
        case (Some(_), None) =>
          val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
          Logger("application").error(s"${if (isAgent) "[Agent] - ": Unit}cease session data not found for $incomeSourceType")
          Future(errorHandler.showInternalServerError())

        case (None, Some(_)) =>
          val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
          Logger("application").error(s"${if (isAgent) "[Agent] - ": Unit}IncomeSourceId not found for $incomeSourceType")
          Future(errorHandler.showInternalServerError())
        case _ =>
          val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
          Logger("application").error(s"${if (isAgent) "[Agent] - ": Unit}missing incomeSourceId and endDate for $incomeSourceType")
          Future(errorHandler.showInternalServerError())
      }
    }
  }

  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleRequest(isAgent = false, incomeSourceType = incomeSourceType)
  }

  def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit mtdItUser =>
      handleRequest(isAgent = true, incomeSourceType = incomeSourceType)
  }

}
