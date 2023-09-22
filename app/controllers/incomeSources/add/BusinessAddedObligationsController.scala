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

package controllers.incomeSources.add

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.SelfEmployment
import forms.utils.SessionKeys
import forms.utils.SessionKeys.incomeSourceId
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateServiceInterface, IncomeSourceDetailsService, NextUpdatesService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.IncomeSourcesUtils
import views.html.incomeSources.add.IncomeSourceAddedObligations

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessAddedObligationsController @Inject()(authenticate: AuthenticationPredicate,
                                                   val authorisedFunctions: AuthorisedFunctions,
                                                   checkSessionTimeout: SessionTimeoutPredicate,
                                                   retrieveNino: NinoPredicate,
                                                   val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                   val retrieveBtaNavBar: NavBarPredicate,
                                                   val itvcErrorHandler: ItvcErrorHandler,
                                                   incomeSourceDetailsService: IncomeSourceDetailsService,
                                                   val obligationsView: IncomeSourceAddedObligations,
                                                   val sessionService: SessionService,
                                                   nextUpdatesService: NextUpdatesService)
                                                  (implicit val appConfig: FrontendAppConfig,
                                                   implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                   implicit override val mcc: MessagesControllerComponents,
                                                   val ec: ExecutionContext,
                                                   dateService: DateServiceInterface)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with IncomeSourcesUtils {

  def getBusinessNameAndStartDate(incomeSource: Either[Throwable, Option[String]])(implicit user: MtdItUser[_]): Either[Throwable, (String, LocalDate)] = {
    incomeSource match {
      case Right(Some(incomeSourceId)) => {
        for {
          addedBusiness <- user.incomeSources.businesses.find(_.incomeSourceId.equals(incomeSourceId))
          tradeName <- addedBusiness.tradingName
          tradeStartDate <- addedBusiness.tradingStartDate
        } yield Right((tradeName, tradeStartDate))
      }.getOrElse(Left(throw new Exception(s"Failed to get addedBusinessDetails: ${incomeSourceId}")))
      case Right(None) =>
        Left(new Error(s"Failed to extract incomeSource from session"))
      case Left(ex) =>
        Left(ex)
    }
  }

  private def getBackUrl(isAgent: Boolean): String = {
    val baseRoute = if (isAgent) controllers.incomeSources.add.routes.BusinessReportingMethodController.showAgent _ else
      controllers.incomeSources.add.routes.BusinessReportingMethodController.show _
    baseRoute().url
  }

  private def handleRequest(isAgent: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withIncomeSourcesFS {
      val obligationViewModelFuture = for {
        incomeSourceIdMayBe <- sessionService.get(SessionKeys.incomeSourceId)
        res <- Future {
          getBusinessNameAndStartDate(incomeSourceIdMayBe)
        }
        viewModelRes <- res match {
          case Right((businessName, startDate)) =>
            val showPreviousTaxYears: Boolean = startDate.isBefore(dateService.getCurrentTaxYearStart())
            nextUpdatesService.getObligationsViewModel(incomeSourceId, showPreviousTaxYears, Some(businessName)).map(x => Right(x))
          case Left(ex) =>
            Future {
              Left(ex)
            }
        }
      } yield viewModelRes

      obligationViewModelFuture.flatMap(res =>
        res match {
          case Right(obligationViewModel) =>
            val backUrl = getBackUrl(isAgent)
            Future {
              Ok(
                obligationsView(sources = obligationViewModel, backUrl = backUrl, isAgent = isAgent, incomeSourceType = SelfEmployment)
              )
            }
          case Left(ex) =>
            val errorMessage = s"Unable to find incomeSource by id: $incomeSourceId" + ex
            Logger("application").error(errorMessage)
            val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
            Future.successful(errorHandler.showInternalServerError())
        }
      )
    }.recover {
      case exception =>
        val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
        Logger("application").error(s"[BusinessAddedObligationsController][handleRequest] ${exception.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

  private def handleSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    val redirectUrl = if (isAgent) routes.AddIncomeSourceController.showAgent().url else routes.AddIncomeSourceController.show().url
    Future.successful(Redirect(redirectUrl))
  }

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(isAgent = false)
  }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(isAgent = true)
        }
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      handleSubmitRequest(isAgent = false)
  }

  def agentSubmit: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(isAgent = true)
        }
  }
}
