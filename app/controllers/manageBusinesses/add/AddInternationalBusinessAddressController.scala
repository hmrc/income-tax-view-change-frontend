/*
 * Copyright 2026 HM Revenue & Customs
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
import com.google.inject.Inject
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import models.core.Mode
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{AddressLookupService, SessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.IncomeSourcesUtils

import scala.concurrent.{ExecutionContext, Future}

class AddInternationalBusinessAddressController @Inject()(val authActions: AuthActions,
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
  
  def show(isAgent: Boolean, mode: Mode, isTriggeredMigration: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
      handleRequest(isAgent, mode, isTriggeredMigration)(implicitly, itvcErrorHandler)
  }

  private def addressLookupConfirmUrl(id: String): String = {
    s"${appConfig.addressLookupExternalHost}/lookup-address/$id/confirm"
  }

  private def initialiseJourney(isAgent: Boolean, mode: Mode, isTriggeredMigration: Boolean)
                               (implicit user: MtdItUser[_],
                                errorHandler: ShowInternalServerError): Future[Result] = {
    addressLookupService.initialiseAddressJourney(
      isAgent = isAgent,
      mode = mode,
      isTriggeredMigration,
      ukOnly = false
    ) map {
      case Right(Some(location)) =>
        Redirect(location)
      case Right(None) =>
        Logger("application").error("[AddInternationalBusinessAddressController][initialiseJourney] No redirect location returned from connector")
        errorHandler.showInternalServerError()
      case Left(_) =>
        Logger("application").error("[AddInternationalBusinessAddressController][initialiseJourney] Unexpected response")
        errorHandler.showInternalServerError()
    }
  }
  
  def handleRequest(isAgent: Boolean, mode: Mode, isTriggeredMigration: Boolean)(implicit user: MtdItUser[_],
                                                                                 errorHandler: ShowInternalServerError): Future[Result] = {
    withOverseasBusinessFS {
      val journeyType = IncomeSourceJourneyType(Add, SelfEmployment)

      sessionService.getMongo(journeyType).flatMap {
        case Right(Some(sessionData)) =>
          sessionData.addIncomeSourceData.flatMap(_.addressLookupId) match {
            case Some(addressLookupId) =>
              addressLookupService.fetchAddress(Some(addressLookupId)).flatMap {
                case Right(_) =>
                  Future.successful(Redirect(addressLookupConfirmUrl(addressLookupId)))
                case Left(_) =>
                  Logger("application").info(s"[AddInternationalBusinessAddressController][handleRequest] - addressLookupId expired/invalid, starting new ALF journey")
                  initialiseJourney(isAgent = isAgent, mode = mode, isTriggeredMigration)(user, errorHandler)
              }

            case None => initialiseJourney(isAgent = isAgent, mode = mode, isTriggeredMigration)(user, errorHandler)
          }

        case _ => initialiseJourney(isAgent = isAgent, mode = mode, isTriggeredMigration)(user, errorHandler)
      }
    }
  }
}
