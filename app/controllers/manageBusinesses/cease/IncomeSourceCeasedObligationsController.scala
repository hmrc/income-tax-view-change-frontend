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
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import enums.IncomeSourceJourney._
import enums.JourneyType.{Cease, JourneyType}
import models.core.IncomeSourceId
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.viewmodels.IncomeSourceCeasedObligationsViewModel
import models.incomeSourceDetails.{CeaseIncomeSourceData, UIJourneySessionData}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateServiceInterface, NextUpdatesService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyCheckerManageBusinesses}
import views.html.manageBusinesses.cease.IncomeSourceCeasedObligations

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceCeasedObligationsController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                                        val itvcErrorHandler: ItvcErrorHandler,
                                                        val obligationsView: IncomeSourceCeasedObligations,
                                                        val nextUpdatesService: NextUpdatesService,
                                                        val sessionService: SessionService,
                                                        val auth: AuthenticatorPredicate)
                                                       (implicit val appConfig: FrontendAppConfig,
                                                        implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                        implicit override val mcc: MessagesControllerComponents,
                                                        val ec: ExecutionContext,
                                                        dateService: DateServiceInterface)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with IncomeSourcesUtils with JourneyCheckerManageBusinesses {

  private def getBusinessName(incomeSourceId: IncomeSourceId)(implicit user: MtdItUser[_]): Option[String] = {
    user.incomeSources.businesses
      .find(m => mkIncomeSourceId(m.incomeSourceId) == incomeSourceId)
      .flatMap(_.tradingName)
  }

  private def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withIncomeSourcesFS {
      withSessionData(JourneyType(Cease, incomeSourceType), CannotGoBackPage) { sessionData =>

        updateMongoCeased(incomeSourceType)

        val incomeSourceId: Option[String] = incomeSourceType match {
          case SelfEmployment => sessionData.ceaseIncomeSourceData.flatMap(_.incomeSourceId)
          case UkProperty => user.incomeSources.properties.filter(_.isUkProperty)
            .map(incomeSource => incomeSource.incomeSourceId).headOption
          case ForeignProperty => user.incomeSources.properties.filter(_.isForeignProperty)
            .map(incomeSource => incomeSource.incomeSourceId).headOption
        }

        val businessEndDate: Option[LocalDate] = sessionData.ceaseIncomeSourceData.flatMap(_.endDate)

        (incomeSourceId, businessEndDate) match {
          case (Some(incomeSourceId), Some(endDate)) =>
            val businessName = if (incomeSourceType == SelfEmployment) getBusinessName(IncomeSourceId(incomeSourceId)) else None
            nextUpdatesService.getObligationsViewModel(incomeSourceId, showPreviousTaxYears = false).map {
              obligationsViewModel =>
                val incomeSourceCeasedObligationsViewModel = IncomeSourceCeasedObligationsViewModel(obligationsViewModel,
                  incomeSourceType,
                  businessName,
                  endDate,
                  isAgent)(dateService)

                lazy val viewAllBusinessLink = if (isAgent) controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url else controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
                lazy val viewUpcomingUpdatesLink = if (isAgent) controllers.routes.NextUpdatesController.showAgent.url else controllers.routes.NextUpdatesController.show().url

                Ok(obligationsView(incomeSourceCeasedObligationsViewModel, viewAllBusinessLink, viewUpcomingUpdatesLink))
            }
          case (Some(_), None) => Future.failed(new Error(s"cease session data not found for $incomeSourceType"))
          case (None, Some(_)) => Future.failed(new Error(s"IncomeSourceId not found for $incomeSourceType"))
          case _ => Future.failed(new Error(s"missing incomeSourceId and endDate for $incomeSourceType"))
        }
      }
    }
  }.recover {
    case ex: Exception =>
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"${if (isAgent) "[Agent]"}${ex.getMessage} - ${ex.getCause}")
      errorHandler.showInternalServerError()
  }

  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      handleRequest(isAgent = false, incomeSourceType = incomeSourceType)
  }

  def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleRequest(isAgent = true, incomeSourceType = incomeSourceType)
  }

  private def updateMongoCeased(incomeSourceType: IncomeSourceType)(implicit hc: HeaderCarrier): Future[Boolean] = {
    sessionService.getMongo(JourneyType(Cease, incomeSourceType).toString).flatMap {
      case Right(Some(sessionData)) =>
        val oldCeaseIncomeSourceSessionData = sessionData.ceaseIncomeSourceData.getOrElse(CeaseIncomeSourceData(incomeSourceId = Some(CeaseIncomeSourceData.incomeSourceIdField), endDate = None, ceaseIncomeSourceDeclare = None, journeyIsComplete = None))
        val updatedCeaseIncomeSourceSessionData = oldCeaseIncomeSourceSessionData.copy(journeyIsComplete = Some(true))
        val uiJourneySessionData: UIJourneySessionData = sessionData.copy(ceaseIncomeSourceData = Some(updatedCeaseIncomeSourceSessionData))

        sessionService.setMongoData(uiJourneySessionData)

      case _ => Future.failed(new Exception("failed to retrieve session data"))
    }
  }
}
