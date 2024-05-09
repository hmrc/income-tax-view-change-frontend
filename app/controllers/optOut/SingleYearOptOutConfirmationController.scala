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
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.AuthenticatorPredicate
import views.html.optOut.ConfirmSingleYearOptOut

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SingleYearOptOutConfirmationController @Inject()(auth: AuthenticatorPredicate,
                                                       view: ConfirmSingleYearOptOut)
                                                      (implicit val appConfig: FrontendAppConfig,
                                                       val ec: ExecutionContext,
                                                       val authorisedFunctions: AuthorisedFunctions,
                                                       val itvcErrorHandler: ItvcErrorHandler,
                                                       val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                       override val mcc: MessagesControllerComponents)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  private val submitAction = (isAgent: Boolean) => controllers.optOut.routes.SingleYearOptOutConfirmationController.submit(isAgent)
  private val backUrl = ""
  private val previousPage = (isAgent: Boolean) => if (isAgent) controllers.routes.NextUpdatesController.showAgent else controllers.routes.NextUpdatesController.show()
  private val nextPage = controllers.routes.HomeController.show()
  private val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  def show(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user => handleRequest(isAgent)
  }

  def submit(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user => handleSubmitRequest(isAgent)
  }

  private def handleRequest(isAgent: Boolean)(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    val taxYear = TaxYear(2024)
    val form = ConfirmOptOutSingleTaxYearForm(taxYear)

    Future(Ok(view(
      taxYear = taxYear,
      form = form,
      submitAction = submitAction(isAgent),
      isAgent = isAgent,
      backUrl = backUrl)))
  }

  private def handleSubmitRequest(isAgent: Boolean)(implicit mtdItUser: MtdItUser[_]): Future[Result] = {

    val taxYear = TaxYear(2024)

    ConfirmOptOutSingleTaxYearForm(taxYear).bindFromRequest().fold(
      formWithError => {
        Future.successful(BadRequest(view(
          taxYear = taxYear,
          form = formWithError,
          submitAction = submitAction(isAgent),
          backUrl = backUrl,
          isAgent = isAgent
        )))
      },
      {
        case ConfirmOptOutSingleTaxYearForm(Some(true), _) => Future.successful(Redirect(nextPage))
        case ConfirmOptOutSingleTaxYearForm(Some(false), _) => Future.successful(Redirect(previousPage(isAgent)))
        case _ => Future.successful(errorHandler(isAgent).showInternalServerError())
      }
    )
  }

}
