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
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import exceptions.MissingSessionKey
import forms.utils.SessionKeys.{addUkPropertyAccountingMethod, addUkPropertyStartDate, businessTrade}
import implicits.ImplicitDateFormatter
import models.addIncomeSource.{AddIncomeSourceResponse, PropertyDetails}
import models.checkUKPropertyDetails.CheckUKPropertyDetailsViewModel
import models.incomeSourceDetails.viewmodels.CheckBusinessDetailsViewModel
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.Format.GenericFormat
import play.api.mvc._
import services.{CreateBusinessDetailsService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.play.language.LanguageUtils
import utils.IncomeSourcesUtils.getPropertyDetailsFromSession
import views.html.incomeSources.add.CheckUKPropertyDetails

import java.time.{LocalDate, LocalDateTime}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckUKPropertyDetailsController @Inject()(val checkUKPropertyDetails: CheckUKPropertyDetails,
                                                 val checkSessionTimeout: SessionTimeoutPredicate,
                                                 val authenticate: AuthenticationPredicate,
                                                 val authorisedFunctions: AuthorisedFunctions,
                                                 val retrieveNino: NinoPredicate,
                                                 val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                 val businessDetailsService: CreateBusinessDetailsService,
                                                 val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                 val createBusinessDetailsService: CreateBusinessDetailsService,
                                                 val retrieveBtaNavBar: NavBarPredicate)
                                                (implicit val appConfig: FrontendAppConfig,
                                                 mcc: MessagesControllerComponents,
                                                 val ec: ExecutionContext,
                                                 val itvcErrorHandler: ItvcErrorHandler,
                                                 val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                 val languageUtils: LanguageUtils) extends ClientConfirmedController with I18nSupport with FeatureSwitching with ImplicitDateFormatter {

  def getBackUrl(isAgent: Boolean): String = {
    if (isAgent) controllers.incomeSources.add.routes.UKPropertyAccountingMethod.changeAgent().url else {
      controllers.incomeSources.add.routes.UKPropertyAccountingMethod.change().url
      //TODO: dynamic back url
    }
  }

  def getSubmitUrl(isAgent: Boolean): Call = {
    if (isAgent) controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.submitAgent() else
      controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.submit()
  }

  def getHomePageUrl(isAgent: Boolean): Call = {
    if (isAgent) controllers.routes.HomeController.showAgent else
      controllers.routes.HomeController.show()
  }

  def getUKPropertyAccountingMethodUrl(isAgent: Boolean): Call = {
    if (isAgent) controllers.incomeSources.add.routes.UKPropertyAccountingMethod.showAgent() else
      controllers.incomeSources.add.routes.UKPropertyAccountingMethod.show()
  }

  def getUkPropertyStartDate(user: MtdItUser[_])(implicit messages: Messages): String = {
    val startDate = user.session.get(addUkPropertyStartDate).getOrElse(throw MissingSessionKey(addUkPropertyStartDate))
    val startDateAsLocalDate = LocalDate.from(startDate).toLongDate
    startDateAsLocalDate
  }

  def getUkPropertyAccountingMethod(user: MtdItUser[_])(implicit messages: Messages): String = {
    //    val valueFromSession = user.session.get(addUkPropertyAccountingMethod).getOrElse(throw MissingSessionKey(addUkPropertyAccountingMethod))
    //TODO: uncomment after Tharun's PR
    val valueFromSession = "cash"

    if (valueFromSession.equals("cash")) {
      messages("incomeSources.add.accountingMethod.cash")
    } else {
      messages("incomeSources.add.accountingMethod.accruals")
    }
  }


  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        isAgent = false
      )
  }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true
            )
        }
  }

  def handleRequest(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    val backUrl = getBackUrl(isAgent)
    val postAction = getSubmitUrl(isAgent)
    val homePageRedirectUrl = getHomePageUrl(isAgent)
    val ukPropertyStartDate = getUkPropertyStartDate(user)
    val ukPropertyAccountingMethod = getUkPropertyAccountingMethod(user)
    val viewModel = CheckUKPropertyDetailsViewModel(ukPropertyStartDate, ukPropertyAccountingMethod)

    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(homePageRedirectUrl))
    } else {
      Future.successful(Ok(
        checkUKPropertyDetails(viewModel = viewModel,
          isAgent = isAgent,
          backUrl = backUrl,
          postAction = postAction,
          ukPropertyStartDate = ukPropertyStartDate,
          ukPropertyAccountingMethod = ukPropertyAccountingMethod)).addingToSession(
        addUkPropertyAccountingMethod -> "cash",
        businessTrade -> "uk-property"
      ))
    }
  }

  def submit(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleSubmit(isAgent = false)
  }

  def submitAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleSubmit(isAgent = true)
        }
  }

  def handleSubmit(isAgent: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    val redirectUrl = getUKPropertyAccountingMethodUrl(isAgent)

    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(redirectUrl))
    } else {
      getPropertyDetailsFromSession(user).toOption match {
        case Some(propertyDetails: PropertyDetails) =>
          businessDetailsService.createPropertyDetails(propertyDetails).map {
            case Left(ex) => Logger("application").error(
              s"[CheckUKPropertyDetailsController][handleRequest] - Unable to create income source: ${ex.getMessage}")
              itvcErrorHandler.showInternalServerError()
            case Right(AddIncomeSourceResponse(id)) =>
              Redirect(redirectUrl + s"?id=$id").withNewSession
          }
        case None => Logger("application").error(
          s"[CheckUKPropertyDetailsController][handleSubmit] - Error: Unable to build UK property details on submit")
          Future.successful(itvcErrorHandler.showInternalServerError())
      }
    }
  }

}