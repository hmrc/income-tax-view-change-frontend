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
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.ForeignProperty
import forms.utils.SessionKeys.ceaseForeignPropertyEndDate
import implicits.ImplicitDateFormatter
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{IncomeSourceDetailsService, UpdateIncomeSourceService}
import uk.gov.hmrc.play.language.LanguageUtils
import utils.IncomeSourcesUtils
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
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with ImplicitDateFormatter with IncomeSourcesUtils {

  lazy val backUrl: String = routes.BusinessEndDateController.show(None, ForeignProperty.key).url
  lazy val backUrlAgent: String = routes.BusinessEndDateController.showAgent(None, ForeignProperty.key).url

  lazy val changeUrl: String = routes.BusinessEndDateController.show(None, ForeignProperty.key).url
  lazy val changeUrlAgent: String = routes.BusinessEndDateController.showAgent(None, ForeignProperty.key).url

  lazy val successCall: Call = controllers.incomeSources.cease.routes.ForeignPropertyCeasedObligationsController.show()
  lazy val successCallAgent: Call = controllers.incomeSources.cease.routes.ForeignPropertyCeasedObligationsController.showAgent()

  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        isAgent = false,
        backUrl = backUrl,
        changeUrl = changeUrl,
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
              itvcErrorHandler = itvcErrorHandlerAgent
            )
        }
  }

  def submit(cessationDate: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      handleSubmitRequest(
        successCall = successCall,
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
              itvcErrorHandler = itvcErrorHandlerAgent
            )
        }
  }

  def handleRequest(isAgent: Boolean,
                    backUrl: String,
                    changeUrl: String,
                    itvcErrorHandler: ShowInternalServerError)
                   (implicit user: MtdItUser[_], request: Request[_]): Future[Result] = {

    withIncomeSourcesFS {
      request.session.get(ceaseForeignPropertyEndDate) match {
        case Some(date) =>
          Future.successful(Ok(checkCeaseForeignPropertyDetails(
            endDate = date.toLocalDate,
            backUrl = backUrl,
            isAgent = isAgent,
            changeUrl = changeUrl
          )))
        case _ =>
          Logger("application").error(s"[CheckCeaseForeignPropertyDetailsController][handleSubmitRequest]:" +
            s" Could not get ceaseForeignPropertyEndDate from session")
          Future.successful(itvcErrorHandler.showInternalServerError())
      }
    }
  }

  def handleSubmitRequest(successCall: Call,
                          cessationDate: String,
                          itvcErrorHandler: ShowInternalServerError)
                         (implicit user: MtdItUser[_]): Future[Result] = {

    withIncomeSourcesFS {
      val foreignPropertyIncomeSources = user.incomeSources.properties.filter(_.isForeignProperty)

      if (foreignPropertyIncomeSources.isEmpty) {
        Logger("application").error(s"[CheckCeaseForeignPropertyDetailsController][handleSubmitRequest]:" +
          s" Failed to retrieve incomeSourceId for foreign property")
        Future(itvcErrorHandler.showInternalServerError())
      } else {
        val incomeSourceId = foreignPropertyIncomeSources.head.incomeSourceId

        updateIncomeSourceservice
          .updateCessationDatev2(user.nino, incomeSourceId, cessationDate).flatMap {
          case Right(_) =>
            Future.successful(Redirect(successCall))
          case _ =>
            Logger("application").error(s"[CheckCeaseForeignPropertyDetailsController][handleSubmitRequest]:" +
              s" Unsuccessful update response received")
            Future(itvcErrorHandler.showInternalServerError())
        }
      }
    }
  }
}
