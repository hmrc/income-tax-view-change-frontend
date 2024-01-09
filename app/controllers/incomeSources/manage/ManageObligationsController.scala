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

package controllers.incomeSources.manage

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import enums.IncomeSourceJourney._
import enums.JourneyType.{JourneyType, Manage}
import enums.{AnnualReportingMethod, QuarterlyReportingMethod}
import models.core.IncomeSourceId
import models.incomeSourceDetails.ManageIncomeSourceData
import models.incomeSourceDetails.TaxYear.getTaxYearModel
import play.api.Logger
import play.api.mvc._
import services.{IncomeSourceDetailsService, NextUpdatesService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthenticatorPredicate, IncomeSourcesUtils}
import views.html.incomeSources.manage.ManageObligations

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ManageObligationsController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                            implicit val itvcErrorHandler: ItvcErrorHandler,
                                            implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                            val incomeSourceDetailsService: IncomeSourceDetailsService,
                                            val obligationsView: ManageObligations,
                                            val sessionService: SessionService,
                                            nextUpdatesService: NextUpdatesService,
                                            val auth: AuthenticatorPredicate)
                                           (implicit val ec: ExecutionContext,
                                            implicit override val mcc: MessagesControllerComponents,
                                            val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching with IncomeSourcesUtils {

  private lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  def showSelfEmployment(changeTo: String, taxYear: String): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      withIncomeSourcesFS {
        sessionService.getMongoKey(ManageIncomeSourceData.incomeSourceIdField, JourneyType(Manage, SelfEmployment)).flatMap {
          case Right(incomeSourceIdMaybe) =>
            val incomeSourceIdOption: Option[IncomeSourceId] = incomeSourceIdMaybe.map(id => IncomeSourceId(id))
            handleRequest(
              incomeSourceType = SelfEmployment,
              isAgent = false,
              taxYear,
              changeTo,
              incomeSourceIdOption
            )
          case Left(ex) =>
            Logger("application").error(s"[ManageObligationsController][showSelfEmployment]: ${ex.getMessage}")
            Future.successful {
              errorHandler(false).showInternalServerError()
            }
        }
      }
  }

  def showAgentSelfEmployment(changeTo: String, taxYear: String): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit user =>
      withIncomeSourcesFS {
        sessionService.getMongoKey(ManageIncomeSourceData.incomeSourceIdField, JourneyType(Manage, SelfEmployment)).flatMap {
          case Right(incomeSourceIdMaybe) =>
            val incomeSourceIdOption: Option[IncomeSourceId] = incomeSourceIdMaybe.map(id => IncomeSourceId(id))
            handleRequest(
              incomeSourceType = SelfEmployment,
              isAgent = true,
              taxYear,
              changeTo,
              incomeSourceIdOption
            )
          case Left(ex) =>
            Logger("application").error(s"[ManageObligationsController][showSelfEmployment]: ${ex.getMessage} - ${ex.getCause}")
            Future.successful {
              errorHandler(true).showInternalServerError()
            }
        }
      }
  }

  def showUKProperty(changeTo: String, taxYear: String): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      handleRequest(
        incomeSourceType = UkProperty,
        isAgent = false,
        taxYear,
        changeTo,
        None
      )
  }

  def showAgentUKProperty(changeTo: String, taxYear: String): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleRequest(
        incomeSourceType = UkProperty,
        isAgent = true,
        taxYear,
        changeTo,
        None
      )
  }

  def showForeignProperty(changeTo: String, taxYear: String): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      handleRequest(
        incomeSourceType = ForeignProperty,
        isAgent = false,
        taxYear,
        changeTo,
        None
      )
  }

  def showAgentForeignProperty(changeTo: String, taxYear: String): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleRequest(
        incomeSourceType = ForeignProperty,
        isAgent = true,
        taxYear,
        changeTo,
        None
      )
  }

  private lazy val successPostUrl = (isAgent: Boolean) => {
    if (isAgent) controllers.incomeSources.manage.routes.ManageObligationsController.agentSubmit()
    else controllers.incomeSources.manage.routes.ManageObligationsController.submit()
  }

  def handleRequest(incomeSourceType: IncomeSourceType, isAgent: Boolean, taxYear: String, changeTo: String, incomeSourceId: Option[IncomeSourceId])
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    withIncomeSourcesFS {
      (getTaxYearModel(taxYear), changeTo) match {
        case (Some(years), AnnualReportingMethod.name | QuarterlyReportingMethod.name) =>
          getIncomeSourceId(incomeSourceType, incomeSourceId, isAgent = isAgent) match {
            case Some(incomeSourceId) =>
              val addedBusinessName: String = getBusinessName(incomeSourceType, Some(incomeSourceId))
              nextUpdatesService.getObligationsViewModel(incomeSourceId.value, showPreviousTaxYears = false) map { viewModel =>
                Ok(obligationsView(viewModel, addedBusinessName, years, changeTo, isAgent, successPostUrl(isAgent)))
              }
            case None => showError(isAgent, s"Unable to retrieve income source ID for $incomeSourceType")
          }
        case (Some(_), _) =>
          showError(isAgent, s"Invalid changeTo mode provided: -$changeTo-")
        case (None, _) =>
          showError(isAgent, "Invalid tax year provided")
      }
    }
  }

  def showError(isAgent: Boolean, message: String)(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    Logger("application").error(
      s"${if (isAgent) "[Agent]"}[ManageObligationsController][handleRequest] - $message")
    Future.successful {
      errorHandler(isAgent).showInternalServerError()
    }
  }

  def getBusinessName(mode: IncomeSourceType, incomeSourceId: Option[IncomeSourceId])(implicit user: MtdItUser[_]): String = {
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

  def getIncomeSourceId(incomeSourceType: IncomeSourceType, id: Option[IncomeSourceId], isAgent: Boolean)
                       (implicit user: MtdItUser[_]): Option[IncomeSourceId] = {

    incomeSourceType match {
      case SelfEmployment =>
        id.orElse {
          val message = "[ManageObligationsController][getIncomeSourceId] Missing required income source ID for Self Employment"
          Logger("application").error(message)
          None
        }
      case UkProperty | ForeignProperty =>
        getActiveProperty(incomeSourceType).map(property => IncomeSourceId(property.incomeSourceId))
    }
  }


  def submit: Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit request =>
      Future.successful(Redirect(controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(false)))
  }

  def agentSubmit: Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      Future.successful(Redirect(controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(true)))
  }
}
