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
import connectors.AddressLookupConnector
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.utils.SessionKeys
import models.incomeSourceDetails.BusinessAddressModel
import models.incomeSourceDetails.viewmodels.httpparser.GetAddressLookupDetailsHttpParser.{GetAddressLookupDetailsResponse, InvalidJson, getAddressLookupDetailsHttpReads}
import models.incomeSourceDetails.viewmodels.httpparser.{GetAddressLookupDetailsHttpParser, PostAddressLookupHttpParser}
import models.incomeSourceDetails.viewmodels.httpparser.PostAddressLookupHttpParser.{PostAddressLookupResponse, PostAddressLookupSuccessResponse}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

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
                                             addressLookupConnector: AddressLookupConnector)
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
      Future.successful(Redirect(controllers.routes.HomeController.show()))
    } else {
        addressLookupConnector.initialiseAddressLookup(
          isAgent = isAgent
        ) map {
          case Right(PostAddressLookupSuccessResponse(Some(location))) =>
            Redirect(location)
          case Right(PostAddressLookupSuccessResponse(None)) =>
            throw new InternalServerException(s"[AddBusinessAddressController][handleRequest] - Unexpected response, success, but no location returned")
          case Left(PostAddressLookupHttpParser.UnexpectedStatusFailure(status)) =>
            throw new InternalServerException(s"[AddBusinessAddressController][handleRequest] - Unexpected response, status: $status")
        }
    }
  }

  def submit(id: Option[String]): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      id match {
        case Some(value) => fetchAddress(value)
          fetchAddress(value) map { value =>
            Redirect(routes.AddBusinessAccountingMethodController.show().url).addingToSession(
              SessionKeys.addBusinessAddressLine1 -> value.address.lines.head,
              SessionKeys.addBusinessAddressLine2 -> {if (value.address.lines.isDefinedAt(1)) value.address.lines(1) else ""},
              SessionKeys.addBusinessAddressLine3 -> {if (value.address.lines.isDefinedAt(2)) value.address.lines(2) else ""},
              SessionKeys.addBusinessAddressLine4 -> {if (value.address.lines.isDefinedAt(3)) value.address.lines(3) else ""},
              SessionKeys.addBusinessPostalCode -> value.address.postcode.getOrElse(""),// check what postcode null value should be
              SessionKeys.addBusinessCountryCode -> "GB"
            )
          }
        case None => throw new InternalServerException(s"[AddressLookupRoutingController][fetchAddress] - Id not returned from address service")
      }
  }

  def agentSubmit(id: Option[String]): Action[AnyContent] = Authenticated.async{
    implicit request =>
      implicit user =>
        id match {
          case Some(value) => val address: Future[BusinessAddressModel] = fetchAddress(value)
            address map { value =>
              Redirect(routes.AddBusinessAccountingMethodController.showAgent().url).addingToSession(
                SessionKeys.addBusinessAddressLine1 -> value.address.lines.head,
                SessionKeys.addBusinessAddressLine2 -> {if (value.address.lines.isDefinedAt(1)) value.address.lines(1) else ""},
                SessionKeys.addBusinessAddressLine3 -> {if (value.address.lines.isDefinedAt(2)) value.address.lines(2) else ""},
                SessionKeys.addBusinessAddressLine4 -> {if (value.address.lines.isDefinedAt(3)) value.address.lines(3) else ""},
                SessionKeys.addBusinessPostalCode -> value.address.postcode.getOrElse(""), // check what postcode null value should be
                SessionKeys.addBusinessCountryCode -> "GB"
              )
            }
          case None => throw new InternalServerException(s"[AddressLookupRoutingController][fetchAddress] - Id not returned from address service")
        }

  }

  def fetchAddress(id: String)(implicit hc: HeaderCarrier): Future[BusinessAddressModel] = {
        addressLookupConnector.getAddressDetails(id) map {
          case Right(Some(addressDetails)) => addressDetails
          case Right(None) =>
            throw new InternalServerException(s"[AddressLookupRoutingController][fetchAddress] - No address details found with id: $id")
          case Left(InvalidJson) =>
            throw new InternalServerException(s"[AddressLookupRoutingController][fetchAddress] - Invalid json response")
          case Left(GetAddressLookupDetailsHttpParser.UnexpectedStatusFailure(status)) =>
            throw new InternalServerException(s"[AddressLookupRoutingController][fetchAddress] - Unexpected response, status: $status")
        }
  }
}
