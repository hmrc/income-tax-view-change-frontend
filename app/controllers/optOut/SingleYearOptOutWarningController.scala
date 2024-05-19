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

package controllers.optOut

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import forms.optOut.ConfirmOptOutSingleTaxYearForm
import models.incomeSourceDetails.TaxYear
import models.optOut.OptOutOneYearViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.optout.OptOutPropositionService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.AuthenticatorPredicate
import views.html.optOut.SingleYearOptOutWarning

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SingleYearOptOutWarningController @Inject()(auth: AuthenticatorPredicate,
                                                  view: SingleYearOptOutWarning,
                                                  optOutService: OptOutPropositionService)
                                                 (implicit val appConfig: FrontendAppConfig,
                                                       val ec: ExecutionContext,
                                                       val authorisedFunctions: AuthorisedFunctions,
                                                       val itvcErrorHandler: ItvcErrorHandler,
                                                       val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                       override val mcc: MessagesControllerComponents)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {


  private val submitAction = (isAgent: Boolean) => controllers.optOut.routes.SingleYearOptOutWarningController.submit(isAgent)
  private val homePage = (isAgent: Boolean) => if (isAgent) controllers.routes.HomeController.showAgent else controllers.routes.HomeController.show()
  private val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
  private val backUrl = (isAgent: Boolean) => if (isAgent) controllers.routes.NextUpdatesController.showAgent else controllers.routes.NextUpdatesController.show()

  def show(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user => withRecover(isAgent)(handleRequest(isAgent))
  }

  def submit(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user => withRecover(isAgent)(handleSubmitRequest(isAgent))
  }

  private def handleRequest(isAgent: Boolean)(implicit mtdItUser: MtdItUser[_]): Future[Result] =
    withOptOutQualifiedTaxYear(isAgent) {
      taxYear =>

        Ok(view(
          taxYear = taxYear,
          form = ConfirmOptOutSingleTaxYearForm(taxYear),
          submitAction = submitAction(isAgent),
          isAgent = isAgent,
          backUrl = backUrl(isAgent).url))

    }

  private def handleSubmitRequest(isAgent: Boolean)(implicit mtdItUser: MtdItUser[_]): Future[Result] =
    withOptOutQualifiedTaxYear(isAgent) {
      taxYear =>

        ConfirmOptOutSingleTaxYearForm(taxYear).bindFromRequest().fold(
          formWithError => {
            BadRequest(view(
              taxYear = taxYear,
              form = formWithError,
              submitAction = submitAction(isAgent),
              backUrl = backUrl(isAgent).url,
              isAgent = isAgent
            ))
          },
          {
            case ConfirmOptOutSingleTaxYearForm(Some(true), _) =>
              val nextPage = if (isAgent)
                controllers.optOut.routes.ConfirmOptOutController.showAgent()
              else controllers.optOut.routes.ConfirmOptOutController.show()

              Logger("application").info(s"redirecting to : $nextPage")
              Redirect(nextPage)
            case ConfirmOptOutSingleTaxYearForm(Some(false), _) =>
              Logger("application").info(s"redirecting to : ${homePage(isAgent)}")
              Redirect(homePage(isAgent))
            case _ =>
              Logger("application").error("bad request")
              errorHandler(isAgent).showInternalServerError()
          })

    }

  private def withRecover(isAgent: Boolean)(code: => Future[Result])(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    code.recover {
      case ex: Exception =>
        Logger("application").error(s"request failed :: $ex")
        errorHandler(isAgent).showInternalServerError()
    }
  }

  private def withOptOutQualifiedTaxYear(isAgent: Boolean)(code: TaxYear => Result)
                                        (implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    optOutService.nextUpdatesPageOneYearOptOutViewModel().map {
      case Some(OptOutOneYearViewModel(taxYear, _)) => code(taxYear)
      case None =>
        Logger("application").error("No qualified tax year available for opt out")
        errorHandler(isAgent).showInternalServerError()
    }

  }


}
