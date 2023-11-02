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
import enums.JourneyType.{Add, JourneyType}
import exceptions.MissingSessionKey
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
                                               val businessDetailsService: CreateBusinessDetailsService)
                                              (implicit val ec: ExecutionContext,
                                               implicit override val mcc: MessagesControllerComponents,
                                               val appConfig: FrontendAppConfig,
                                               implicit val sessionService: SessionService,
                                               implicit val itvcErrorHandler: ItvcErrorHandler,
                                               implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler) extends ClientConfirmedController
  with IncomeSourcesUtils with FeatureSwitching {

  lazy val businessAddressUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.show(isChange = false).url
  lazy val agentBusinessAddressUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.showAgent(isChange = false).url

  lazy val incomeSourcesAccountingMethodUrl: String = controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.show(SelfEmployment).url
  lazy val agentIncomeSourcesAccountingMethodUrl: String = controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.showAgent(SelfEmployment).url


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
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = withIncomeSourcesFS {

    val backUrl: String = if (isAgent) getAgentBackURL(user.headers.get(REFERER)) else getBackURL(user.headers.get(REFERER))
    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.submitAgent(SelfEmployment) else
      controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.submit(SelfEmployment)


    getBusinessDetailsFromSession(user, ec).map {
      viewModel =>
        Ok(checkBusinessDetails(
          viewModel = viewModel,
          postAction = postAction,
          isAgent = isAgent,
          backUrl = backUrl
        ))
    }.recover {
      case ex: Throwable =>
        val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
        Logger("application").error(
          s"${if (isAgent) "[Agent]"}[CheckBusinessDetailsController][handleRequest] - Error: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }

  }


  def handleSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {
    val (redirect, errorHandler) = {
      if (isAgent)
        (routes.BusinessReportingMethodController.showAgent _, routes.IncomeSourceNotAddedController.showAgent(SelfEmployment).url)
      else
        (routes.BusinessReportingMethodController.show _, routes.IncomeSourceNotAddedController.show(SelfEmployment).url)
    }
    getBusinessDetailsFromSession(user, ec).flatMap {
      viewModel => {
        businessDetailsService.createBusinessDetails(viewModel).flatMap {
          case Right(CreateIncomeSourceResponse(id)) =>
            sessionService.deleteMongoData(JourneyType(Add, SelfEmployment))
            Future.successful(Redirect(redirect(id).url))

          case Left(ex) => Future.failed(ex)
        }
      }
    }.recover {
      case ex: Exception =>
        Logger("application").error(s"[AddIncomeSourceController][handleRequest]${ex.getMessage}")
        Redirect(errorHandler)
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

  private def getBusinessDetailsFromSession(implicit user: MtdItUser[_], ec: ExecutionContext): Future[CheckBusinessDetailsViewModel] = {
    val userActiveBusinesses: List[BusinessDetailsModel] = user.incomeSources.businesses.filterNot(_.isCeased)
    val skipAccountingMethod: Boolean = userActiveBusinesses.isEmpty
    val errorTracePrefix = "[CheckBusinessDetailsController][getBusinessDetailsFromSession]:"
    sessionService.getMongo(JourneyType(Add, SelfEmployment).toString).map {
      case Right(Some(uiJourneySessionData)) =>
        uiJourneySessionData.addIncomeSourceData match {
          case Some(addIncomeSourceData) =>

            val address = addIncomeSourceData.address.getOrElse(throw MissingSessionKey(s"$errorTracePrefix address"))
            CheckBusinessDetailsViewModel(
              businessName = addIncomeSourceData.businessName,
              businessStartDate = addIncomeSourceData.dateStarted,
              accountingPeriodEndDate = addIncomeSourceData.accountingPeriodEndDate
                .getOrElse(throw MissingSessionKey(s"$errorTracePrefix accountingPeriodEndDate")),
              businessTrade = addIncomeSourceData.businessTrade
                .getOrElse(throw MissingSessionKey(s"$errorTracePrefix businessTrade")),
              businessAddressLine1 = address.lines.headOption
                .getOrElse(throw MissingSessionKey(s"$errorTracePrefix businessAddressLine1")),
              businessAddressLine2 = address.lines.lift(1),
              businessAddressLine3 = address.lines.lift(2),
              businessAddressLine4 = address.lines.lift(3),
              businessPostalCode = address.postcode,
              businessCountryCode = addIncomeSourceData.countryCode,
              incomeSourcesAccountingMethod = addIncomeSourceData.incomeSourcesAccountingMethod,
              cashOrAccrualsFlag = addIncomeSourceData.incomeSourcesAccountingMethod
                .getOrElse(throw MissingSessionKey(s"$errorTracePrefix incomeSourcesAccountingMethod")),
              skippedAccountingMethod = skipAccountingMethod
            )

          case None => throw new Exception(s"$errorTracePrefix failed to retrieve addIncomeSourceData")
        }
      case _ => throw new Exception(s"$errorTracePrefix failed to retrieve uiJourneySessionData ")
    }

  }

}