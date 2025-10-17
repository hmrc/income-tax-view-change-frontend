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

package controllers.manageBusinesses.add

import auth.MtdItUser
import auth.authV2.AuthActions
import com.google.inject.Singleton
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import models.UIJourneySessionData
import models.core.{IncomeSourceId, Mode, NormalMode}
import models.admin.AccountingMethodJourney
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.{AddIncomeSourceData, BusinessAddressModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{AddressLookupService, SessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.IncomeSourcesUtils

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddBusinessAddressController @Inject()(val authActions: AuthActions,
                                             addressLookupService: AddressLookupService)
                                            (implicit
                                             val appConfig: FrontendAppConfig,
                                             val ec: ExecutionContext,
                                             val itvcErrorHandler: ItvcErrorHandler,
                                             val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                             mcc: MessagesControllerComponents,
                                             val sessionService: SessionService
                                            )
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport with IncomeSourcesUtils {

  def show(mode: Mode): Action[AnyContent] = authActions.asMTDIndividual.async { implicit user =>
    handleRequest(isAgent = false, mode = mode)(implicitly, itvcErrorHandler)
  }

  def showAgent(mode: Mode): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit mtdItUser =>
      handleRequest(isAgent = true, mode = mode)(implicitly, itvcErrorHandlerAgent)
  }

  def handleRequest(isAgent: Boolean, mode: Mode)
                   (implicit user: MtdItUser[_],
                    errorHandler: ShowInternalServerError): Future[Result] = {
    addressLookupService.initialiseAddressJourney(
      isAgent = isAgent,
      mode = mode
    ) map {
      case Right(Some(location)) =>
        Redirect(location)
      case Right(None) =>
        Logger("application").error("No redirect location returned from connector")
        errorHandler.showInternalServerError()
      case Left(_) =>
        Logger("application").error("Unexpected response")
        errorHandler.showInternalServerError()
    }
  }

  def getRedirectUrl(isAgent: Boolean, mode: Mode)(implicit user: MtdItUser[_]): String = {
    ((isEnabled(AccountingMethodJourney), isAgent, mode) match {
      case (true, _, NormalMode) => routes.IncomeSourcesAccountingMethodController.show(SelfEmployment, isAgent)
      case (_, false, _) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
      case (_, true, _) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
    }).url
  }

  private def setUpSession(addressLookUpResult: Either[Throwable, BusinessAddressModel])
                          (implicit request: Request[_]): Future[Boolean] = {
    addressLookUpResult match {
      case Right(value) =>
        val journeyType = IncomeSourceJourneyType(Add, SelfEmployment)
        sessionService.getMongo(journeyType).flatMap {
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


  def handleSubmitRequest(isAgent: Boolean, id: Option[IncomeSourceId], mode: Mode)
                         (implicit user: MtdItUser[_], errorHandler: ShowInternalServerError): Future[Result] = {
    val redirectUrl = getRedirectUrl(isAgent = isAgent, mode = mode)
    val redirect = Redirect(redirectUrl)

    addressLookupService.fetchAddress(id).flatMap(setUpSession(_).flatMap {
      case true => Future.successful(redirect)
      case false => Future.failed(new Exception("failed to set session data"))
    })

  }.recover {
    case ex =>
      Logger("application")
        .error(s"Unexpected response, status: - ${ex.getMessage} - ${ex.getCause} ")
      errorHandler.showInternalServerError()
  }

  def submit(id: Option[String], mode: Mode): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user => {
        val incomeSourceIdMaybe = id.map(mkIncomeSourceId)
        handleSubmitRequest(isAgent = false, incomeSourceIdMaybe, mode = mode)(implicitly, itvcErrorHandler)
      }
  }

  def agentSubmit(id: Option[String], mode: Mode): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit mtdItUser =>
      val incomeSourceIdMaybe = id.map(mkIncomeSourceId)
      handleSubmitRequest(isAgent = true, incomeSourceIdMaybe, mode = mode)(implicitly, itvcErrorHandlerAgent)
  }
}
