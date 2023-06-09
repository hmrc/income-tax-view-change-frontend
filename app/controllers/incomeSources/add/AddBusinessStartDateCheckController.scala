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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.BusinessStartDateCheckForm
import forms.BusinessStartDateCheckForm.{response, responseNo, responseYes}
import forms.utils.SessionKeys
import forms.utils.SessionKeys.addBusinessStartDate
import implicits.ImplicitDateFormatterImpl
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{DateService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.AddBusinessStartDateCheck

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class AddBusinessStartDateCheckController @Inject()(authenticate: AuthenticationPredicate,
                                                    val authorisedFunctions: AuthorisedFunctions,
                                                    checkSessionTimeout: SessionTimeoutPredicate,
                                                    retrieveNino: NinoPredicate,
                                                    val addBusinessStartDateCheck: AddBusinessStartDateCheck,
                                                    val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                    val retrieveBtaNavBar: NavBarPredicate,
                                                    incomeSourceDetailsService: IncomeSourceDetailsService,
                                                    val customNotFoundErrorView: CustomNotFoundError)
                                                   (implicit val appConfig: FrontendAppConfig,
                                                    implicit val dateFormatter: ImplicitDateFormatterImpl,
                                                    implicit val dateService: DateService,
                                                    implicit val languageUtils: LanguageUtils,
                                                    implicit val itvcErrorHandler: ItvcErrorHandler,
                                                    implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                    implicit override val mcc: MessagesControllerComponents,
                                                    val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  private def getBackUrl(isAgent: Boolean): String = {
    if (isAgent) controllers.incomeSources.add.routes.AddBusinessStartDateController.showAgent().url
    else controllers.incomeSources.add.routes.AddBusinessStartDateController.show().url
  }

  private def getContinueUrl(isAgent: Boolean): String = {
    if (isAgent) controllers.incomeSources.add.routes.AddBusinessTradeController.showAgent().url
    else controllers.incomeSources.add.routes.AddBusinessTradeController.show().url
  }

  private def getPostAction(isAgent: Boolean): Call = {
    if (isAgent) controllers.incomeSources.add.routes.AddBusinessStartDateCheckController.submitAgent()
    else controllers.incomeSources.add.routes.AddBusinessStartDateCheckController.submit()
  }

  def getFormattedBusinessStartDate()(implicit user: MtdItUser[_]): String = {
    val businessStartDateFromSession = getBusinessStartDate()
    val businessStartDateAsLocalDate = LocalDate.parse(businessStartDateFromSession)

    dateFormatter.longDate(businessStartDateAsLocalDate).toLongDate
  }

  def getBusinessStartDate()(implicit user: MtdItUser[_]): String = {
    user.session.get(SessionKeys.addBusinessStartDate).get
  }

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async { implicit user =>
    handleRequest(isAgent = false, itvcErrorHandler)
  }

  def showAgent(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { implicit mtdItUser =>
        handleRequest(isAgent = true, itvcErrorHandlerAgent)
      }
  }

  def handleRequest(isAgent: Boolean, itvcErrorHandler: ShowInternalServerError)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    val incomeSourcesDisabled = isDisabled(IncomeSources)
    val businessStartDateMissing = user.session.get(addBusinessStartDate).isEmpty

    if (incomeSourcesDisabled) {
      Future.successful(Ok(customNotFoundErrorView()))
    } else if (businessStartDateMissing) {
      Future.successful(itvcErrorHandler.showInternalServerError())
    } else {
      displayBusinessStartDateCheck(isAgent)
    }
  }

  def displayBusinessStartDateCheck(isAgent: Boolean)(implicit user: MtdItUser[_], messages: Messages): Future[Result] = {
    val backUrl: String = getBackUrl(isAgent)
    val postAction: Call = getPostAction(isAgent)
    val formattedBusinessStartDate = getFormattedBusinessStartDate()

    Future.successful(Ok(addBusinessStartDateCheck(
      form = BusinessStartDateCheckForm.form,
      postAction = postAction,
      backUrl = backUrl,
      isAgent = isAgent,
      businessStartDate = formattedBusinessStartDate,
      btaNavPartial = user.btaNavPartial
    )(messages, user)))
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      val backUrl = getBackUrl(false)
      val continueUrl = getContinueUrl(false)
      val postAction = getPostAction(false)
      val businessStartDate = getBusinessStartDate()
      val businessStartDateAsLocalDate = LocalDate.parse(businessStartDate)
      val formattedBusinessStartDate = getFormattedBusinessStartDate()

      BusinessStartDateCheckForm.form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(addBusinessStartDateCheck(
            form = formWithErrors,
            postAction = postAction,
            backUrl = backUrl,
            isAgent = false,
            businessStartDate = formattedBusinessStartDate,
            btaNavPartial = request.btaNavPartial
          ))),
        _.toFormMap(response).headOption match {
          case Some(selection) if selection.contains(responseNo) =>
            Future.successful(Redirect(backUrl).removingFromSession(SessionKeys.addBusinessStartDate))
          case Some(selection) if selection.contains(responseYes) =>
            val businessAccountingPeriodEndDate = dateService.getAccountingPeriodEndDate(businessStartDateAsLocalDate)
            Future.successful(Redirect(continueUrl)
              .addingToSession(SessionKeys.addBusinessAccountingPeriodStartDate -> businessStartDate,
                SessionKeys.addBusinessAccountingPeriodEndDate -> businessAccountingPeriodEndDate))
          case None => Future.successful(itvcErrorHandler.showInternalServerError())
        }
      ).recoverWith {
        case ex => Logger("application").error(s"[AddBusinessStartDateCheckController][submit]: ${ex.getMessage}")
          Future.successful(itvcErrorHandler.showInternalServerError())
      }
  }

  def submitAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            val backUrl = getBackUrl(true)
            val continueUrl = getContinueUrl(true)
            val postAction = getPostAction(true)
            val businessStartDate = getBusinessStartDate()
            val businessStartDateAsLocalDate = LocalDate.parse(businessStartDate)
            val formattedBusinessStartDate = getFormattedBusinessStartDate()

            BusinessStartDateCheckForm.form.bindFromRequest().fold(
              formWithErrors => {
                Future.successful(BadRequest(addBusinessStartDateCheck(
                  form = formWithErrors,
                  postAction = postAction,
                  backUrl = backUrl,
                  isAgent = true,
                  businessStartDate = formattedBusinessStartDate,
                  btaNavPartial = mtdItUser.btaNavPartial
                )))
              },
              _.toFormMap(response).headOption match {
                case Some(selection) if selection.contains(responseNo) =>
                  Future.successful(Redirect(backUrl).removingFromSession(SessionKeys.addBusinessStartDate))
                case Some(selection) if selection.contains(responseYes) =>
                  val businessAccountingPeriodEndDate = dateService.getAccountingPeriodEndDate(businessStartDateAsLocalDate)
                  Future.successful(Redirect(continueUrl)
                    .addingToSession(SessionKeys.addBusinessAccountingPeriodStartDate -> businessStartDate,
                      SessionKeys.addBusinessAccountingPeriodEndDate -> businessAccountingPeriodEndDate))
                case None => Future.successful(itvcErrorHandler.showInternalServerError())
              }
            )
        }.recoverWith {
          case ex => Logger("application").error(s"[Agent][AddBusinessStartDateCheckController][submitAgent]: ${ex.getMessage}")
            Future.successful(itvcErrorHandler.showInternalServerError())
        }
  }

}

