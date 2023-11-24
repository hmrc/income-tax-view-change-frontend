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
import com.google.inject.Singleton
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, JourneyType}
import models.incomeSourceDetails.{AddIncomeSourceData, BusinessAddressModel, UIJourneySessionData}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.{AddressLookupService, IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.IncomeSourcesUtils

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddBusinessAddressController @Inject()(authenticate: AuthenticationPredicate,
                                             val authorisedFunctions: AuthorisedFunctions,
                                             checkSessionTimeout: SessionTimeoutPredicate,
                                             val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                             val retrieveBtaNavBar: NavBarPredicate,
                                             val itvcErrorHandler: ItvcErrorHandler,
                                             incomeSourceDetailsService: IncomeSourceDetailsService,
                                             addressLookupService: AddressLookupService)
                                            (implicit
                                             val appConfig: FrontendAppConfig,
                                             val ec: ExecutionContext,
                                             val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                             mcc: MessagesControllerComponents,
                                             val sessionService: SessionService
                                            )
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with IncomeSourcesUtils{

  def show(isChange: Boolean): Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(isAgent = false, isChange = isChange)
  }

  def showAgent(isChange: Boolean): Action[AnyContent] =
    Authenticated.async {
      implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
            implicit mtdItUser =>
              handleRequest(isAgent = true, isChange = isChange)
          }
    }

  def handleRequest(isAgent: Boolean, isChange: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withIncomeSourcesFSWithSessionCheck(sessionService, JourneyType(Add, SelfEmployment)) {
      addressLookupService.initialiseAddressJourney(
        isAgent = isAgent,
        isChange = isChange
      ) map {
        case Right(Some(location)) =>
          Redirect(location)
        case Right(None) =>
          Logger("application").error(s"[AddBusinessAddressController][handleRequest] - No redirect location returned from connector")
          itvcErrorHandler.showInternalServerError()
        case Left(_) =>
          Logger("application").error(s"[AddBusinessAddressController][handleRequest] - Unexpected response")
          itvcErrorHandler.showInternalServerError()
      }
    }
  }

  def getRedirectUrl(isAgent: Boolean, isChange: Boolean): String = {
    ((isAgent, isChange) match {
      case (false, false) => routes.IncomeSourcesAccountingMethodController.show(SelfEmployment)
      case (false, true) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
      case (true, false) => routes.IncomeSourcesAccountingMethodController.showAgent(SelfEmployment)
      case (true, true) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
    }).url
  }

  private def setUpSession(addressLookUpResult: Either[Throwable, BusinessAddressModel])
                          (implicit request: Request[_]): Future[Boolean] = {
    addressLookUpResult match {
      case Right(value) =>
        val journeyType = JourneyType(Add, SelfEmployment)
        sessionService.getMongo(journeyType.toString).flatMap {
          case Right(Some(sessionData)) =>
            val oldAddIncomeSourceSessionData = sessionData.addIncomeSourceData.getOrElse(AddIncomeSourceData())
            val updatedAddIncomeSourceSessionData = oldAddIncomeSourceSessionData.copy(address = Some(value.address), countryCode = Some("GB"))
            val uiJourneySessionData: UIJourneySessionData = sessionData.copy(addIncomeSourceData = Some(updatedAddIncomeSourceSessionData))

            sessionService.setMongoData(uiJourneySessionData)

          case _ => Future.failed(new Exception(s"failed to retrieve session data for ${journeyType.toString}"))
        }
      case Left(ex) => Future.failed(ex)
    }
  }


  def handleSubmitRequest(isAgent: Boolean, id: Option[String], isChange: Boolean)(implicit user: MtdItUser[_],
                                                                                   ec: ExecutionContext, request: Request[_]): Future[Result] = {
    val redirectUrl = getRedirectUrl(isAgent = isAgent, isChange = isChange)
    val redirect = Redirect(redirectUrl)

    addressLookupService.fetchAddress(id).flatMap(setUpSession(_).flatMap {
      case true => Future.successful(redirect)
      case false => Future.failed(new Exception(s"failed to set session data"))
    })

  }.recover {
    case ex =>
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"[AddBusinessAddressController][fetchAddress] - Unexpected response, status: $ex ")
      errorHandler.showInternalServerError()
  }

  def submit(id: Option[String], isChange: Boolean): Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleSubmitRequest(isAgent = false, id, isChange = isChange)
  }

  def agentSubmit(id: Option[String], isChange: Boolean): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(isAgent = true, id, isChange = isChange)
        }

  }
}
