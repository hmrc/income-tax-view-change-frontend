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
import models.incomeSourceDetails.BusinessAddressModel
import models.incomeSourceDetails.viewmodels.httpparser.GetAddressLookupDetailsHttpParser.{GetAddressLookupDetailsResponse, getAddressLookupDetailsHttpReads}
import models.incomeSourceDetails.viewmodels.httpparser.PostAddressLookupHttpParser
import models.incomeSourceDetails.viewmodels.httpparser.PostAddressLookupHttpParser.{PostAddressLookupResponse, PostAddressLookupSuccessResponse}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.InternalServerException
import views.html.AddBusinessAddress

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class AddBusinessAddressController @Inject()(authenticate: AuthenticationPredicate,
                                             val authorisedFunctions: AuthorisedFunctions,
                                             checkSessionTimeout: SessionTimeoutPredicate,
                                             retrieveNino: NinoPredicate,
                                             val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                             val retrieveBtaNavBar: NavBarPredicate,
                                             val itvcErrorHandler: ItvcErrorHandler,
                                             incomeSourceDetailsService: IncomeSourceDetailsService,
                                             addressLookupConnector: AddressLookupConnector,
                                             addBusinessAddressView: AddBusinessAddress)
                                         (implicit
                                          val appConfig: FrontendAppConfig,
                                          val ec: ExecutionContext,
                                          val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                          mcc: MessagesControllerComponents
                                         )
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  lazy val backURL: String = routes.AddBusinessTradeController.show().url
  lazy val agentBackURL: String = routes.AddBusinessTradeController.showAgent().url
  def show: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(isAgent = false)
      /*val ref = addressLookupConnector.getAddressDetails(user.mtditid)
      Thread.sleep(100)
      Logger("application").info("BEEP" + ref.toString)
      Future{Ok}*/
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
    //val ref = addressLookupConnector.getAddressDetails(user.mtditid)
    //Thread.sleep(100)
    //Logger("application").info("BEEP" + ref.toString)
    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(controllers.routes.HomeController.show()))
    } else {
        addressLookupConnector.initialiseAddressLookup(
          isAgent = false
        )  map {
          case Right(PostAddressLookupSuccessResponse(Some(location))) =>
            Redirect(location)
          case Right(PostAddressLookupSuccessResponse(None)) =>
            throw new InternalServerException(s"[AddBusinessAddressController][handleRequest] - Unexpected response, success, but no location returned")
          case Left(PostAddressLookupHttpParser.UnexpectedStatusFailure(status)) =>
            throw new InternalServerException(s"[AddBusinessAddressController][handleRequest] - Unexpected response, status: $status")
        }


        /*val ref: Future[PostAddressLookupResponse] = addressLookupConnector.initialiseAddressLookup(isAgent)
        val model: Future[GetAddressLookupDetailsResponse] = addressLookupConnector.getAddressDetails(user.mtditid)
        Thread.sleep(100)
        Redirect(ref.value.get.get)
        model.value match {
          case Some(response) => response match {
            case Failure(exception) => BadRequest
            case Success(success) => success match {
              case Left(value) => BadRequest //Status failure
              case Right(value) => value match {
                case Some(value) => {
                  if (!isAgent) Ok(addBusinessAddressView(routes.AddBusinessAddressController.submit(), isAgent, backURL, value))
                  else Ok(addBusinessAddressView(routes.AddBusinessAddressController.agentSubmit(), isAgent, agentBackURL, value))
                }
                case None => Ok //Not found <- This one now
              }
            }
          }
          case None => BadRequest /// <- Future not completed
        }*/
    }

  }

  def submit(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      Future{Ok}
  }

  def agentSubmit(): Action[AnyContent] = Authenticated.async{
    implicit request =>
      implicit user =>
          Future{Ok}
  }
}
