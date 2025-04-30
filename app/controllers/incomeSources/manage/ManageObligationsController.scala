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
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.IncomeSourceJourney._
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import enums.{AnnualReportingMethod, QuarterlyReportingMethod}
import exceptions.MissingSessionKey
import models.core.IncomeSourceId
import models.incomeSourceDetails.ManageIncomeSourceData
import models.incomeSourceDetails.TaxYear.getTaxYearModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{NextUpdatesService, SessionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.IncomeSourcesUtils
import views.html.incomeSources.manage.ManageObligations

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ManageObligationsController @Inject()(val authActions: AuthActions,
                                            val itvcErrorHandler: ItvcErrorHandler,
                                            val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                            val obligationsView: ManageObligations,
                                            val sessionService: SessionService,
                                            nextUpdatesService: NextUpdatesService)
                                           (implicit val ec: ExecutionContext,
                                            val mcc: MessagesControllerComponents,
                                            val appConfig: FrontendAppConfig) extends FrontendController(mcc)
  with I18nSupport with IncomeSourcesUtils {

  private lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  def show(changeTo: String, taxYear: String, incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      withIncomeSourcesFS {
        getOptIncomeSourceId(incomeSourceType).flatMap { incomeSourceIdOption =>
          handleRequest(
            incomeSourceType = incomeSourceType,
            isAgent = false,
            taxYear,
            changeTo,
            incomeSourceIdOption
          )
        }.recoverWith {
          case ex => Logger("application").error(s"${ex.getMessage}")
            Future.successful {
              errorHandler(false).showInternalServerError()
            }
        }
      }
  }

  def showAgent(changeTo: String,
                taxYear: String,
                incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit user =>
      withIncomeSourcesFS {
        getOptIncomeSourceId(incomeSourceType).flatMap { incomeSourceIdOption =>
          handleRequest(
            incomeSourceType = incomeSourceType,
            isAgent = true,
            taxYear,
            changeTo,
            incomeSourceIdOption
          )
        }.recoverWith {
          case ex => Logger("application").error(s"${ex.getMessage}")
            Future.successful {
              errorHandler(true).showInternalServerError()
            }
        }
      }
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

  def showError(isAgent: Boolean, message: String)(implicit user: MtdItUser[_]): Future[Result] = {
    Logger("application").error(
      s"${if (isAgent) "[Agent]"}$message")
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
          val message = "Missing required income source ID for Self Employment"
          Logger("application").error(message)
          None
        }
      case UkProperty | ForeignProperty =>
        getActiveProperty(incomeSourceType).map(property => IncomeSourceId(property.incomeSourceId))
    }
  }

  private def getOptIncomeSourceId(incomeSourceType: IncomeSourceType)
                                  (implicit hc: HeaderCarrier): Future[Option[IncomeSourceId]] = {
    if(incomeSourceType == SelfEmployment) {
      sessionService.getMongoKey(ManageIncomeSourceData.incomeSourceIdField, IncomeSourceJourneyType(Manage, incomeSourceType)).flatMap {
        case Right(incomeSourceIdMaybe) => Future.successful(incomeSourceIdMaybe.map(id => IncomeSourceId(id)))
        case Left(_) => Future.failed(MissingSessionKey(ManageIncomeSourceData.incomeSourceIdField))
      }
    } else {
      Future.successful(None)
    }
  }


  def submit: Action[AnyContent] = authActions.asMTDIndividual.async { _ =>
      Future.successful(Redirect(controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(false)))
  }

  def agentSubmit: Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {_ =>
      Future.successful(Redirect(controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(true)))
  }
}
