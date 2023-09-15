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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.SelfEmployment
import exceptions.MissingSessionKey
import forms.utils.SessionKeys
import models.createIncomeSource.CreateIncomeSourceResponse
import models.incomeSourceDetails.viewmodels.CheckBusinessDetailsViewModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.Logger
import play.api.mvc._
import services.{CreateBusinessDetailsService, IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.IncomeSourcesUtils
import views.html.incomeSources.add.CheckBusinessDetails

import java.net.URI
import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckBusinessDetailsController @Inject()(val checkBusinessDetails: CheckBusinessDetails,
                                               val checkSessionTimeout: SessionTimeoutPredicate,
                                               val authenticate: AuthenticationPredicate,
                                               val authorisedFunctions: AuthorisedFunctions,
                                               val retrieveNino: NinoPredicate,
                                               val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                               val incomeSourceDetailsService: IncomeSourceDetailsService,
                                               val retrieveBtaNavBar: NavBarPredicate,
                                               val businessDetailsService: CreateBusinessDetailsService,
                                               val sessionService: SessionService)
                                              (implicit val ec: ExecutionContext,
                                               implicit override val mcc: MessagesControllerComponents,
                                               val appConfig: FrontendAppConfig,
                                               implicit val itvcErrorHandler: ItvcErrorHandler,
                                               implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler) extends ClientConfirmedController
  with IncomeSourcesUtils with FeatureSwitching {

  lazy val businessAddressUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.show(isChange = false).url
  lazy val agentBusinessAddressUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.showAgent(isChange = false).url

  lazy val incomeSourcesAccountingMethodUrl: String = controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.show(SelfEmployment.key).url
  lazy val agentIncomeSourcesAccountingMethodUrl: String = controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.showAgent(SelfEmployment.key).url


  private def getBackURL(referer: Option[String]): String = {
    referer.map(URI.create(_).getPath) match {
      case Some(url) if url.equals(incomeSourcesAccountingMethodUrl) => incomeSourcesAccountingMethodUrl
      case _ => businessAddressUrl
    }
  }

  private def getAgentBackURL(referer: Option[String]): String = {
    referer.map(URI.create(_).getPath) match {
      case Some(`agentIncomeSourcesAccountingMethodUrl`) => agentIncomeSourcesAccountingMethodUrl
      case _ => agentBusinessAddressUrl
    }
  }

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = false
      )
  }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              sources = mtdItUser.incomeSources,
              isAgent = true
            )
        }
  }

  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    val backUrl: String = if (isAgent) getAgentBackURL(user.headers.get(REFERER)) else getBackURL(user.headers.get(REFERER))
    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.CheckBusinessDetailsController.submitAgent() else
      controllers.incomeSources.add.routes.CheckBusinessDetailsController.submit()

    withIncomeSourcesFS {
      getBusinessDetailsFromSession(user, ec).flatMap {
        case Right(viewModel) =>
          Future.successful(Ok(checkBusinessDetails(
            viewModel = viewModel,
            postAction = postAction,
            isAgent = isAgent,
            backUrl = backUrl
          )))
        case Left(ex) => Future.failed(ex)
      }.recover {
        case ex: Throwable =>
          if (isAgent) {
            Logger("application").error(
              s"[Agent][CheckBusinessDetailsController][handleRequest] - Error: ${ex.getMessage}")
            itvcErrorHandlerAgent.showInternalServerError()
          } else {
            Logger("application").error(
              s"[CheckBusinessDetailsController][handleRequest] - Error: ${ex.getMessage}")
            itvcErrorHandler.showInternalServerError()
          }
      }
    }
  }


  def handleSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    withIncomeSourcesFS {

      val (redirect, errorHandler) = {
        if (isAgent)
          (routes.BusinessReportingMethodController.showAgent _, routes.IncomeSourceNotAddedController.showAgent(incomeSourceType = SelfEmployment.key).url)
        else
          (routes.BusinessReportingMethodController.show _, routes.IncomeSourceNotAddedController.show(incomeSourceType = SelfEmployment.key).url)
      }
      getBusinessDetailsFromSession(user, ec).flatMap {
        case Right(viewModel) =>
          businessDetailsService.createBusinessDetails(viewModel).flatMap {
            case Right(CreateIncomeSourceResponse(id)) =>
              newWithIncomeSourcesRemovedFromSession(Redirect(redirect(id).url), sessionService, Redirect(errorHandler))
            case Left(ex) => Future.failed(ex)
          }
        case Left(ex) => Future.failed(ex)
      }.recover {
        case ex: Throwable =>
          Logger("application").error(
            s"[CheckBusinessDetailsController][handleRequest] - Error while processing request: ${ex.getMessage}")
          newWithIncomeSourcesRemovedFromSession(Redirect(errorHandler), sessionService, Redirect(errorHandler))
          Redirect(errorHandler)
      }
    }
  }

  def submit(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleSubmitRequest(isAgent = false)
  }

  def submitAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(isAgent = true)
        }
  }

  private def getBusinessDetailsFromSession(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Either[Throwable, CheckBusinessDetailsViewModel]] = {
    val userActiveBusinesses: List[BusinessDetailsModel] = user.incomeSources.businesses.filterNot(_.isCeased)
    val skipAccountingMethod: Boolean = userActiveBusinesses.isEmpty

    val result = for {
      businessName <- sessionService.get(SessionKeys.businessName)
      businessStartDate <- sessionService.get(SessionKeys.businessStartDate)
      businessTrade <- sessionService.get(SessionKeys.businessTrade)
      businessAddressLine1 <- sessionService.get(SessionKeys.addBusinessAddressLine1)
      businessAccountingMethod <- sessionService.get(SessionKeys.addIncomeSourcesAccountingMethod)
      accountingPeriodEndDate <- sessionService.get(SessionKeys.addBusinessAccountingPeriodEndDate)
      businessAddressLine2 <- sessionService.get(SessionKeys.addBusinessAddressLine2)
      businessAddressLine3 <- sessionService.get(SessionKeys.addBusinessAddressLine3)
      businessAddressLine4 <- sessionService.get(SessionKeys.addBusinessAddressLine4)
      businessPostalCode <- sessionService.get(SessionKeys.addBusinessPostalCode)
      businessCountryCode <- sessionService.get(SessionKeys.addBusinessCountryCode)
      incomeSourcesAccountingMethod <- sessionService.get(SessionKeys.addIncomeSourcesAccountingMethod)
    } yield (businessName, businessStartDate, businessTrade, businessAddressLine1,
      businessAccountingMethod, accountingPeriodEndDate, businessAddressLine2,
      businessAddressLine3, businessAddressLine4, businessPostalCode, businessCountryCode,
      incomeSourcesAccountingMethod)

    result.flatMap {
      case (Right(businessName), Right(businessStartDate), Right(Some(businessTrade)),
      Right(Some(businessAddressLine1)), Right(Some(businessAccountingMethod)), Right(Some(accountingPeriodEndDate)),
      Right(businessAddressLine2), Right(businessAddressLine3), Right(businessAddressLine4), Right(businessPostalCode),
      Right(businessCountryCode), Right(incomeSourcesAccountingMethod)) =>
        Future.successful(Right(CheckBusinessDetailsViewModel(
          businessName = businessName,
          businessStartDate = businessStartDate.map(LocalDate.parse(_)),
          accountingPeriodEndDate = LocalDate.parse(accountingPeriodEndDate),
          businessTrade = businessTrade,
          businessAddressLine1 = businessAddressLine1,
          businessAddressLine2 = businessAddressLine2,
          businessAddressLine3 = businessAddressLine3,
          businessAddressLine4 = businessAddressLine4,
          businessPostalCode = businessPostalCode,
          businessCountryCode = businessCountryCode,
          incomeSourcesAccountingMethod = incomeSourcesAccountingMethod,
          cashOrAccrualsFlag = businessAccountingMethod,
          skippedAccountingMethod = skipAccountingMethod
        )))
      case ex => Future.successful(Left(MissingSessionKey("[IncomeSourcesUtils][getBusinessDetailsFromSession]" + ex)))
    }
  }

}