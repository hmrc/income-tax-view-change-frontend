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
import forms.utils.SessionKeys
import models.incomeSourceDetails.BusinessAddressModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.{AddressLookupService, IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
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
                                             mcc: MessagesControllerComponents,
                                             val sessionService: SessionService
                                            )
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def show(isChange: Boolean): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
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
    if (isDisabled(IncomeSources)) {
      Future.successful(if (isAgent) Redirect(controllers.routes.HomeController.showAgent) else Redirect(controllers.routes.HomeController.show()))
    } else {
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
    if (isChange) {
      if (isAgent) routes.CheckBusinessDetailsController.showAgent().url else routes.CheckBusinessDetailsController.show().url
    } else {
      if (isAgent) routes.IncomeSourcesAccountingMethodController.showAgent(incomeSourceType = SelfEmployment.key).url
      else routes.IncomeSourcesAccountingMethodController.show(incomeSourceType = SelfEmployment.key).url
    }
  }

  private def setUpSession(addressLookUpResult: Either[Throwable, BusinessAddressModel], redirect: Result)(implicit request: Request[_]): Future[Either[Throwable, Result]] = {
    // multiple key / value pair update example
    addressLookUpResult match {
      case Right(value) =>
        val kvPairs : Map[String, String] = Map(
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
          SessionKeys.addBusinessPostalCode -> {
            value.address.postcode.getOrElse("")
          },
          SessionKeys.addBusinessCountryCode -> "GB"
        )
        kvPairs.foldLeft[ Future[Either[Throwable, Result]] ]( Future{ Right(redirect) }) { (acc, kv) =>
          val e = for {
            a <- acc
          } yield a match {
            case Right(r) =>
              sessionService.set(kv._1, kv._2, r)
            case Left(ex) =>
              Future{ Left(ex) }
          }
          e.flatten
        }
      case Left(ex) =>
        Future.successful(Left(ex))
    }
  }


  def handleSubmitRequest(isAgent: Boolean, id: Option[String], isChange: Boolean)(implicit user: MtdItUser[_],
                                                                                   ec: ExecutionContext, request: Request[_]): Future[Result] = {
    val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    val redirectUrl = getRedirectUrl(isAgent = isAgent, isChange = isChange)
    val redirect = Redirect(redirectUrl)
    for {
      addressLookUpResult <- addressLookupService.fetchAddress(id)
      sessionResult <- setUpSession(addressLookUpResult, redirect)(request)
    } yield sessionResult match {
      case Right(updateResult) =>
        updateResult
      case Left(ex) =>
        Logger("application").error(s"[AddBusinessAddressController][fetchAddress] - Unexpected response, status: $ex ")
        errorHandler.showInternalServerError()
    }
  }

  def submit(id: Option[String], isChange: Boolean): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
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
