/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.optIn

import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import forms.optIn.ConfirmOptInForm
import models.incomeSourceDetails.TaxYear
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.optIn.OptInService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.optIn.SingleTaxYearWarningView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SingleTaxYearWarningController @Inject()(val view: SingleTaxYearWarningView,
                                               val optInService: OptInService,
                                               val authActions: AuthActions,
                                               val itvcErrorHandler: ItvcErrorHandler,
                                               val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                              (implicit val appConfig: FrontendAppConfig,
                                         mcc: MessagesControllerComponents,
                                         val ec: ExecutionContext)
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport {

  val logger = Logger(getClass)

  private val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  private def withRecover(isAgent: Boolean)(code: => Future[Result])(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    code.recover {
      case ex: Exception =>
        Logger("application").error(s"request failed :: $ex")
        errorHandler(isAgent).showInternalServerError()
    }
  }

  private val submitAction = (isAgent: Boolean) => controllers.optIn.routes.SingleTaxYearWarningController.submit(isAgent)

  private def handleSubmitRequest(isAgent: Boolean, taxYear: TaxYear)(implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    ConfirmOptInForm().bindFromRequest().fold(
      formWithError => {
        Future.successful(BadRequest(view(
          form = formWithError,
          submitAction = submitAction(isAgent),
          isAgent = isAgent,
          taxYear = taxYear
        )))
      },
      {
        case ConfirmOptInForm(Some(true)) =>
          optInService.saveIntent(TaxYear.makeTaxYearWithEndYear(taxYear.endYear))
          val nextPage = controllers.optIn.routes.ConfirmTaxYearController.show(isAgent)
          logger.info(s"redirecting to : $nextPage")
          Future.successful(Redirect(nextPage))
        case ConfirmOptInForm(Some(false)) =>
          val optInCancelledUrl =
            if (isAgent) {
              controllers.routes.HomeController.showAgent.url
            } else {
              controllers.routes.HomeController.show().url
            }
          logger.info(s"redirecting to : $optInCancelledUrl")
          Future.successful(Redirect(optInCancelledUrl))
        case _ =>
          logger.error("bad request")
          Future.successful(errorHandler(isAgent).showInternalServerError())
      })

  }

  def show(isAgent: Boolean = false): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withRecover(isAgent) {
        optInService.availableOptInTaxYear().flatMap {
          case Seq(singleYear) => Future.successful(Ok(view(ConfirmOptInForm(), submitAction(isAgent), isAgent, singleYear)))
          case _ => Future.successful(Redirect(controllers.optIn.routes.ChooseYearController.show(isAgent)))
        }
      }
  }

  def submit(isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit request =>
      withRecover(isAgent) {
          optInService.availableOptInTaxYear().flatMap {
          case Seq(singleYear) => handleSubmitRequest(isAgent, singleYear)
          case _ => Future.successful(Redirect(controllers.optIn.routes.ChooseYearController.show(isAgent)))
        }
      }
  }

}