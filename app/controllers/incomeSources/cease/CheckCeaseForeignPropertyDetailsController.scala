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
import forms.utils.SessionKeys.ceaseForeignPropertyEndDate
import implicits.ImplicitDateFormatter
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Request, Result}
import services.{IncomeSourceDetailsService, UpdateIncomeSourceService}
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
                                                           val updateIncomeSourceservice: UpdateIncomeSourceService,
                                                           val checkCeaseForeignPropertyDetails: CheckCeaseForeignPropertyDetails)
                                                          (implicit val appConfig: FrontendAppConfig,
                                                           implicit val languageUtils: LanguageUtils,
                                                           mcc: MessagesControllerComponents,
                                                           val ec: ExecutionContext,
                                                           val itvcErrorHandler: ItvcErrorHandler,
                                                           val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with ImplicitDateFormatter {

  lazy val homePageCall: Call = controllers.routes.HomeController.show()
  lazy val homePageCallAgent: Call = controllers.routes.HomeController.showAgent

  lazy val backUrl: String = routes.ForeignPropertyEndDateController.show().url
  lazy val backUrlAgent: String = routes.ForeignPropertyEndDateController.showAgent().url

  lazy val changeUrl: String = routes.ForeignPropertyEndDateController.show().url
  lazy val changeUrlAgent: String = routes.ForeignPropertyEndDateController.showAgent().url

  lazy val successCall: Call = controllers.incomeSources.cease.routes.CeaseForeignPropertySuccessController.show()
  lazy val successCallAgent: Call = controllers.incomeSources.cease.routes.CeaseForeignPropertySuccessController.showAgent()

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        isAgent = false,
        backUrl = backUrl,
        changeUrl = changeUrl,
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
              changeUrl = changeUrlAgent,
              homePageCall = homePageCallAgent,
              itvcErrorHandler = itvcErrorHandlerAgent
            )
        }
  }

  def submit(cessationDate: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      handleSubmitRequest(
        successCall = successCall,
        homePageCall = homePageCall,
        cessationDate = cessationDate,
        itvcErrorHandler = itvcErrorHandler
      )
  }

  def submitAgent(cessationDate: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(
              cessationDate = cessationDate,
              successCall = successCallAgent,
              homePageCall = homePageCallAgent,
              itvcErrorHandler = itvcErrorHandlerAgent
            )
        }
  }

  def handleRequest(isAgent: Boolean,
                    backUrl: String,
                    changeUrl: String,
                    homePageCall: Call,
                    itvcErrorHandler: ShowInternalServerError)
                   (implicit user: MtdItUser[_], request: Request[_]): Future[Result] = {

    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(homePageCall))
    } else {
      request.session.get(ceaseForeignPropertyEndDate) match {
        case Some(date) =>
          Future(Ok(checkCeaseForeignPropertyDetails(
            endDate = date,
            backUrl = backUrl,
            isAgent = isAgent,
            changeUrl = changeUrl,
            formattedEndDate = longDate(date.toLocalDate).toLongDate
          )))
        case _ =>
          Logger("application").error(s"[CheckCeaseForeignPropertyDetailsController][handleSubmitRequest]:" +
            s" Could not get ceaseForeignPropertyEndDate from session")
          Future(itvcErrorHandler.showInternalServerError())
      }
    }
  }

  def handleSubmitRequest(homePageCall: Call,
                          successCall: Call,
                          cessationDate: String,
                          itvcErrorHandler: ShowInternalServerError)
                         (implicit user: MtdItUser[_]): Future[Result] = {

    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(homePageCall))
    } else {

      val maybeIncomeSourceId = user.incomeSources.properties
        .find(_.isForeignProperty)
        .flatMap(_.incomeSourceId)

      maybeIncomeSourceId match {
        case Some(id) =>
          updateIncomeSourceservice
            .updateCessationDatev2(user.nino, id, cessationDate).flatMap {
              case Right(_) =>
                Future.successful(Redirect(successCall))
              case _ =>
                Logger("application").error(s"[CheckCeaseForeignPropertyDetailsController][handleSubmitRequest]:" +
                  s" Unsuccessful update response received")
                Future(itvcErrorHandler.showInternalServerError)
          }
        case _ =>
          Logger("application").error(s"[CheckCeaseForeignPropertyDetailsController][handleSubmitRequest]:" +
          s" Failed to retrieve incomeSourceId for foreign property")
          Future(itvcErrorHandler.showInternalServerError)
      }
    }
  }
}
