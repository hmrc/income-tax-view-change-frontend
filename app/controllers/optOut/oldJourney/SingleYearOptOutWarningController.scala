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

package controllers.optOut.oldJourney

import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import forms.optOut.ConfirmOptOutSingleTaxYearForm
import models.incomeSourceDetails.TaxYear
import models.optout.OptOutOneYearViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.optout.OptOutService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.reportingObligations.ReportingObligationsUtils
import views.html.optOut.oldJourney.SingleYearOptOutWarningView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SingleYearOptOutWarningController @Inject()(auth: AuthActions,
                                                  view: SingleYearOptOutWarningView,
                                                  optOutService: OptOutService)
                                                 (implicit val appConfig: FrontendAppConfig,
                                                  val ec: ExecutionContext,
                                                  val itvcErrorHandler: ItvcErrorHandler,
                                                  val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                  val mcc: MessagesControllerComponents)
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport with ReportingObligationsUtils {

  val logger = Logger(getClass)


  private val submitAction = (isAgent: Boolean) => controllers.optOut.oldJourney.routes.SingleYearOptOutWarningController.submit(isAgent)
  private val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
  private val nextUpdatesUrl = (isAgent: Boolean) =>
    if (isAgent) controllers.routes.NextUpdatesController.showAgent() else controllers.routes.NextUpdatesController.show()

  def show(isAgent: Boolean): Action[AnyContent] = withAuth(isAgent) { implicit user =>
    withOptOutFS {
      withRecover(isAgent)(handleRequest(isAgent))
    }
  }

  def submit(isAgent: Boolean): Action[AnyContent] = withAuth(isAgent) { implicit user =>
    withOptOutFS {
      withRecover(isAgent)(handleSubmitRequest(isAgent))
    }
  }

  private def withAuth(isAgent: Boolean)(code: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
    val userAuthAction = if (isAgent) auth.asMTDAgentWithConfirmedClient else auth.asMTDIndividual
    userAuthAction.async(user => code(user))
  }

  private def handleRequest(isAgent: Boolean)(implicit mtdItUser: MtdItUser[_]): Future[Result] =
    withOptOutQualifiedTaxYear(isAgent) { taxYear =>
      Ok(view(
        taxYear = taxYear,
        form = ConfirmOptOutSingleTaxYearForm(taxYear),
        submitAction = submitAction(isAgent),
        isAgent = isAgent,
        backUrl = nextUpdatesUrl(isAgent).url)
      )
    }

  private def handleSubmitRequest(isAgent: Boolean)(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    withOptOutQualifiedTaxYear(isAgent) {
      taxYear =>
        ConfirmOptOutSingleTaxYearForm(taxYear).bindFromRequest().fold(
          formWithError => {
            BadRequest(view(
              taxYear = taxYear,
              form = formWithError,
              submitAction = submitAction(isAgent),
              backUrl = nextUpdatesUrl(isAgent).url,
              isAgent = isAgent
            ))
          },
          {
            case ConfirmOptOutSingleTaxYearForm(Some(true), _) =>
              val nextPage = controllers.optOut.oldJourney.routes.ConfirmOptOutController.show(isAgent)
              logger.info(s"redirecting to : $nextPage")
              Redirect(nextPage)
            case ConfirmOptOutSingleTaxYearForm(Some(false), _) =>
              val optOutCancelledUrl =
                if (isAgent) {
                  controllers.optOut.oldJourney.routes.OptOutCancelledController.showAgent().url
                } else {
                  controllers.optOut.oldJourney.routes.OptOutCancelledController.show().url
                }
              logger.info(s"redirecting to : $optOutCancelledUrl")
              Redirect(optOutCancelledUrl)
            case _ =>
              logger.error("bad request")
              errorHandler(isAgent).showInternalServerError()
          })

    }
  }

  private def withRecover(isAgent: Boolean)(code: => Future[Result])(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    code.recover {
      case ex: Exception =>
        logger.error(s"request failed :: $ex")
        errorHandler(isAgent).showInternalServerError()
    }
  }

  private def withOptOutQualifiedTaxYear(isAgent: Boolean)(code: TaxYear => Result)
                                        (implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    optOutService.recallNextUpdatesPageOptOutViewModel().map {
      case Some(OptOutOneYearViewModel(taxYear, _)) => code(taxYear)
      case _ =>
        logger.error("No qualified tax year available for opt out")
        errorHandler(isAgent).showInternalServerError()
    }

  }
}