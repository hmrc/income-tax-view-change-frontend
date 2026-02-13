/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers

import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import models.admin.{ITSASubmissionIntegration, PostFinalisationAmendmentsR18}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.DateServiceInterface
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.TaxYearsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class TaxYearsController @Inject()(taxYearsView: TaxYearsView,
                                   val authActions: AuthActions,
                                   itvcErrorHandler: ItvcErrorHandler,
                                   agentItvcErrorHandler: AgentItvcErrorHandler)
                                  (implicit val appConfig: FrontendAppConfig,
                                   mcc: MessagesControllerComponents,
                                   val ec: ExecutionContext,
                                   val dateService: DateServiceInterface
                                  ) extends FrontendController(mcc)
  with I18nSupport with FeatureSwitching {


  def handleRequest(backUrl: String,
                    isAgent: Boolean,
                    origin: Option[String] = None)
                   (implicit user: MtdItUser[_]): Future[Result] = {
    Try {
      taxYearsView(
        taxYears = user.incomeSources.orderedTaxYearsByAccountingPeriods.reverse,
        backUrl,
        isAgent = isAgent,
        utr = user.saUtr,
        itsaSubmissionIntegrationEnabled = isEnabled(ITSASubmissionIntegration),
        isPostFinalisationAmendmentR18Enabled = isEnabled(PostFinalisationAmendmentsR18),
        earliestSubmissionTaxYear = user.incomeSources.earliestSubmissionTaxYear.getOrElse(2023),
        btaNavPartial = user.btaNavPartial,
        origin = origin
      )
    } match {
      case Success(taxView) => Future.successful(Ok(taxView))
      case Failure(ex) =>
        Logger("application").error(s"failed to render view file for taxYears due to ${ex.getMessage}")
        val errorHandler = if (isAgent) agentItvcErrorHandler else itvcErrorHandler
        Future.successful(errorHandler.showInternalServerError())
    }
  }

  def showTaxYears(origin: Option[String] = None): Action[AnyContent] =
    authActions.asMTDIndividual().async { implicit user =>
      handleRequest(
        backUrl = controllers.routes.HomeController.show(origin).url,
        isAgent = false,
        origin = origin
      )
    }

  def showAgentTaxYears: Action[AnyContent] =
    authActions.asMTDPrimaryAgent().async {
      implicit mtdItUser =>
        handleRequest(
          backUrl = controllers.routes.HomeController.showAgent().url,
          isAgent = true
        )
    }
}