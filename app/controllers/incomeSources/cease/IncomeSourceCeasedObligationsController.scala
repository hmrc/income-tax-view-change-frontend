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

package controllers.incomeSources.cease

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Cease, JourneyType}
import exceptions.MissingSessionKey
import models.core.IncomeSourceId
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.{CeaseIncomeSourceData, UIJourneySessionData}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateServiceInterface, IncomeSourceDetailsService, NextUpdatesService, SessionDataService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyChecker}
import views.html.incomeSources.cease.IncomeSourceCeasedObligations

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceCeasedObligationsController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                                        val itvcErrorHandler: ItvcErrorHandler,
                                                        val obligationsView: IncomeSourceCeasedObligations,
                                                        val nextUpdatesService: NextUpdatesService,
                                                        val sessionService: SessionService,
                                                        val auth: AuthenticatorPredicate,
                                                        val sessionDataService: SessionDataService)
                                                       (implicit val appConfig: FrontendAppConfig,
                                                        implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                        implicit override val mcc: MessagesControllerComponents,
                                                        val ec: ExecutionContext,
                                                        dateService: DateServiceInterface)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with IncomeSourcesUtils with JourneyChecker {

  private def getBusinessName(incomeSourceId: IncomeSourceId)(implicit user: MtdItUser[_]): Option[String] = {
    user.incomeSources.businesses
      .find(m => mkIncomeSourceId(m.incomeSourceId) == incomeSourceId)
      .flatMap(_.tradingName)
  }

  private def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withIncomeSourcesFS {
      updateMongoCeased(incomeSourceType)
      val incomeSourceDetails: Future[(Either[Throwable, Option[String]], IncomeSourceType)] = incomeSourceType match {
        case SelfEmployment =>
          sessionService.getMongoKeyTyped[String](CeaseIncomeSourceData.incomeSourceIdField, JourneyType(Cease, SelfEmployment)).map((_, SelfEmployment))
        case UkProperty =>
          Future.successful(
            (user.incomeSources.properties
              .filter(_.isUkProperty)
              .map(incomeSource => incomeSource.incomeSourceId).headOption match {
              case Some(incomeSourceId) =>
                Right(Some(incomeSourceId))
              case None =>
                Left(new Error("IncomeSourceId not found for UK property"))
            }, UkProperty)
          )
        case ForeignProperty =>
          Future.successful(
            (user.incomeSources.properties
              .filter(_.isForeignProperty)
              .map(incomeSource => incomeSource.incomeSourceId).headOption match {
              case Some(incomeSourceId) =>
                Right(Some(incomeSourceId))
              case None =>
                Left(new Error("IncomeSourceId not found for Foreign Property"))
            }, ForeignProperty)
          )
      }

      incomeSourceDetails.flatMap {
        case (Right(Some(incomeSourceIdStr)), incomeSourceType) =>
          val incomeSourceId = mkIncomeSourceId(incomeSourceIdStr)
          val businessName = if (incomeSourceType == SelfEmployment) getBusinessName(incomeSourceId) else None
          nextUpdatesService.getObligationsViewModel(incomeSourceId.value, showPreviousTaxYears = false).map { viewModel =>
            Ok(obligationsView(
              sources = viewModel,
              businessName = businessName,
              isAgent = isAgent,
              incomeSourceType = incomeSourceType))
          }
        case incomeSourceD@(Right(None), _) =>
          Logger("application").error(s"${if (isAgent) "[Agent]"}${incomeSourceD._1}- =${incomeSourceD._2}=")
          Future.failed(MissingSessionKey(CeaseIncomeSourceData.incomeSourceIdField))
        case (Left(exception), _) => Future.failed(exception)
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
