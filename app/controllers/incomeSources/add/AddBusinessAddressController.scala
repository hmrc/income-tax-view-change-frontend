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
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, JourneyType}
import models.core.IncomeSourceId
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.{AddIncomeSourceData, BusinessAddressModel, UIJourneySessionData}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{AddressLookupService, IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{AuthenticatorPredicate, IncomeSourcesUtils}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddBusinessAddressController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                             val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                             val itvcErrorHandler: ItvcErrorHandler,
                                             addressLookupService: AddressLookupService,
                                             auth: AuthenticatorPredicate)
                                            (implicit
                                             val appConfig: FrontendAppConfig,
                                             val ec: ExecutionContext,
                                             val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                             mcc: MessagesControllerComponents,
                                             val sessionService: SessionService
                                            )
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with IncomeSourcesUtils {

  def show(isAgent: Boolean, isChange: Boolean): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        handleRequest(isAgent, isChange)
  }

  def submit(id: Option[String], isAgent: Boolean, isChange: Boolean): Action[AnyContent] =
    auth.authenticatedAction(isAgent = false) {
      implicit user =>
        handleSubmitRequest(id.map(mkIncomeSourceId), isAgent, isChange)
  }


  def handleRequest(isAgent: Boolean, isChange: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withIncomeSourcesFS {
      addressLookupService.initialiseAddressJourney(
        isAgent = isAgent,
        isChange = isChange
      ) map {
        case Right(Some(location)) =>
          Redirect(location)
        case Right(None) =>
          Logger("application").error("[AddBusinessAddressController][handleRequest] - No redirect location returned from connector")
          itvcErrorHandler.showInternalServerError()
        case Left(_) =>
          Logger("application").error("[AddBusinessAddressController][handleRequest] - Unexpected response")
          itvcErrorHandler.showInternalServerError()
      }
    }
  }

  def handleSubmitRequest(id: Option[IncomeSourceId], isAgent: Boolean, isChange: Boolean)
                         (implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {

    addressLookupService.fetchAddress(id).flatMap(setUpSession(_).flatMap {
      case true => Future.successful(Redirect(redirectUrl(isAgent, isChange)))
      case false => Future.failed(new Exception("failed to set session data"))
    })

  }.recover {
    case ex =>
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application")
        .error(s"[AddBusinessAddressController][handleSubmitRequest] - Unexpected response, status: - ${ex.getMessage} - ${ex.getCause} ")
      errorHandler.showInternalServerError()
  }

  private def setUpSession(addressLookUpResult: Either[Throwable, BusinessAddressModel])
                          (implicit request: Request[_]): Future[Boolean] = {
    addressLookUpResult match {
      case Right(value) =>
        sessionService.getMongo(JourneyType(Add, SelfEmployment)).flatMap {
          case Right(Some(sessionData)) =>
            val oldAddIncomeSourceSessionData = sessionData.addIncomeSourceData.getOrElse(AddIncomeSourceData())
            val updatedAddIncomeSourceSessionData = oldAddIncomeSourceSessionData.copy(address = Some(value.address), countryCode = Some("GB"))
            val uiJourneySessionData: UIJourneySessionData = sessionData.copy(addIncomeSourceData = Some(updatedAddIncomeSourceSessionData))

            sessionService.setMongoData(uiJourneySessionData)

          case _ => Future.failed(new Exception(s"failed to retrieve session data for ${JourneyType(Add, SelfEmployment).toString}"))
        }
      case Left(ex) => Future.failed(ex)
    }
  }

  private lazy val redirectUrl: (Boolean, Boolean) => String = (isAgent, isChange) => {
    if (isChange)   routes.IncomeSourceCheckDetailsController       .show(isAgent, SelfEmployment)
    else            routes.IncomeSourcesAccountingMethodController  .show(isAgent, isChange, SelfEmployment)
  }.url
}
