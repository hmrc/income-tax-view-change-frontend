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

package controllers

import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.utils.SessionKeys
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{AddressLookupService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddBusinessAddressController @Inject()(authenticate: AuthenticationPredicate,
                                             val authorisedFunctions: AuthorisedFunctions,
                                             checkSessionTimeout: SessionTimeoutPredicate,
                                             retrieveNino: NinoPredicate,
                                             val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                             val retrieveBtaNavBar: NavBarPredicate,
                                             val itvcErrorHandler: ItvcErrorHandler,
                                             incomeSourceDetailsService: IncomeSourceDetailsService,
                                             addressLookupService: AddressLookupService)
                                         (implicit
                                          val appConfig: FrontendAppConfig,
                                          val ec: ExecutionContext,
                                          val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                          mcc: MessagesControllerComponents
                                         )
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def show: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(isAgent = false)
  }

  def showAgent(): Action[AnyContent] =
    Authenticated.async {
      implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
            implicit mtdItUser =>
              handleRequest(isAgent = true)
          }
    }

  def handleRequest(isAgent: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    if (isDisabled(IncomeSources)) {
      Future.successful(if (isAgent) Redirect(controllers.routes.HomeController.showAgent) else Redirect(controllers.routes.HomeController.show()))
    } else {
        addressLookupService.initialiseAddressJourney(
          isAgent = isAgent
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

  def submit(id: Option[String]): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      val res = addressLookupService.fetchAddress(id)
      res map {
        case Right(value) =>
          Redirect(controllers.incomeSources.add.routes.BusinessAccountingMethodController.show().url).addingToSession(
            SessionKeys.addBusinessAddressLine1 -> {
              if (value.address.lines.isDefinedAt(0)) value.address.lines.head else ""
            },
            SessionKeys.addBusinessAddressLine2 -> {
              if (value.address.lines.isDefinedAt(1)) value.address.lines(1) else ""
            },
            SessionKeys.addBusinessAddressLine3 -> {
              if (value.address.lines.isDefinedAt(2)) value.address.lines(2) else ""
            },
            SessionKeys.addBusinessAddressLine4 -> {
              if (value.address.lines.isDefinedAt(3)) value.address.lines(3) else ""
            },
            SessionKeys.addBusinessPostalCode -> value.address.postcode.getOrElse(""), // check what postcode null value should be
            SessionKeys.addBusinessCountryCode -> "GB"
          )
        case Left(value) =>
          Logger("application").error(s"[AddBusinessAddressController][fetchAddress] - Unexpected response, status: $value")
          itvcErrorHandlerAgent.showInternalServerError()
      }
  }

  def agentSubmit(id: Option[String]): Action[AnyContent] = Authenticated.async{
      implicit request =>
        implicit user =>
          val res = addressLookupService.fetchAddress(id)
           res map {
            case Right(value) =>
              Redirect(controllers.incomeSources.add.routes.BusinessAccountingMethodController.showAgent().url).addingToSession(
                SessionKeys.addBusinessAddressLine1 -> value.address.lines.head,
                SessionKeys.addBusinessAddressLine2 -> {
                  if (value.address.lines.isDefinedAt(1)) value.address.lines(1) else ""
                },
                SessionKeys.addBusinessAddressLine3 -> {
                  if (value.address.lines.isDefinedAt(2)) value.address.lines(2) else ""
                },
                SessionKeys.addBusinessAddressLine4 -> {
                  if (value.address.lines.isDefinedAt(3)) value.address.lines(3) else ""
                },
                SessionKeys.addBusinessPostalCode -> value.address.postcode.getOrElse(""), // check what postcode null value should be
                SessionKeys.addBusinessCountryCode -> "GB"
              )
            case Left(value) =>
              Logger("application").error(s"[AddBusinessAddressController][fetchAddress] - Unexpected response, $value")
              itvcErrorHandlerAgent.showInternalServerError()
          }
  }
}
