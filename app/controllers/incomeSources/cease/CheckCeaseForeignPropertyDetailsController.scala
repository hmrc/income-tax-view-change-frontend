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

package controllers.incomeSources.cease

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import exceptions.MissingSessionKey
import forms.utils.SessionKeys.ceaseForeignPropertyEndDate
import implicits.ImplicitDateFormatter
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Request, Result}
import services.IncomeSourceDetailsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.cease.CheckCeaseForeignPropertyDetails

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckCeaseForeignPropertyDetailsController @Inject()(val authenticate: AuthenticationPredicate,
                                                           val authorisedFunctions: FrontendAuthorisedFunctions,
                                                           val checkSessionTimeout: SessionTimeoutPredicate,
                                                           val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                           val retrieveBtaNavBar: NavBarPredicate,
                                                           val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                           val retrieveNino: NinoPredicate,
                                                           val checkCeaseForeignPropertyDetails: CheckCeaseForeignPropertyDetails,
                                                           val customNotFoundErrorView: CustomNotFoundError)
                                                          (implicit val appConfig: FrontendAppConfig,
                                                           implicit val languageUtils: LanguageUtils,
                                                      mcc: MessagesControllerComponents,
                                                      val ec: ExecutionContext,
                                                      val itvcErrorHandler: ItvcErrorHandler,
                                                      val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with ImplicitDateFormatter {

  lazy val backUrl: String = routes.ForeignPropertyEndDateController.show().url
  lazy val backUrlAgent: String = routes.ForeignPropertyEndDateController.showAgent().url

  lazy val homePageCall: Call = controllers.routes.HomeController.show()
  lazy val homePageCallAgent: Call = controllers.routes.HomeController.showAgent

  lazy val postAction: Call = controllers.incomeSources.cease.routes.CheckCeaseForeignPropertyDetailsController.submit()
  lazy val postActionAgent: Call = controllers.incomeSources.cease.routes.CheckCeaseForeignPropertyDetailsController.submitAgent()

  def handleRequest(isAgent: Boolean, backUrl: String, postAction: Call, homePageCall: Call, itvcErrorHandler: ShowInternalServerError)
                   (implicit user: MtdItUser[_], request: Request[_]): Future[Result] = {

    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(homePageCall))
    } else {
      request.session.get(ceaseForeignPropertyEndDate) match {
        case Some(date) =>
          Future(Ok(checkCeaseForeignPropertyDetails(
            isAgent = isAgent,
            backUrl = backUrl,
            postAction = postAction,
            endDate = longDate(date.toLocalDate).toLongDate
          )))
        case _ => throw MissingSessionKey(ceaseForeignPropertyEndDate)
      }
    }
  }

  def show(): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          isAgent = false,
          backUrl = backUrl,
          postAction = postAction,
          homePageCall = homePageCall,
          itvcErrorHandler = itvcErrorHandler
        )
    }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true,
              backUrl = backUrlAgent,
              postAction = postActionAgent,
              homePageCall = homePageCallAgent,
              itvcErrorHandler = itvcErrorHandlerAgent
            )
        }
  }

  def submit(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      Future(Ok)
  }

  def submitAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            Future(Ok)
        }
  }
}
