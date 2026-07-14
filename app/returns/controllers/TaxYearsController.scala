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

package returns.controllers

import common.auth.{AuthActions, MtdItUser}
import common.config.FrontendAppConfig
import common.config.featureswitch.FeatureSwitching
import common.models.admin.{ITSASubmissionIntegration, MortgageEvidence, PostFinalisationAmendmentsR18}
import common.models.incomeSourceDetails.TaxYear
import common.services.{DateServiceInterface, YearOfMigrationService}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import returns.views.html.TaxYearsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxYearsController @Inject()(taxYearsView: TaxYearsView,
                                   val authActions: AuthActions,
                                   yearOfMigrationService: YearOfMigrationService
                                  )
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

    yearOfMigrationService.orderedTaxYearsByYearOfMigration(user.nino).flatMap {
      case taxYearList @ orderedTaxYearsByYearOfMigration if orderedTaxYearsByYearOfMigration.nonEmpty =>
        Logger("application").debug(s"[TaxYearsController][handleRequest] taxYears = ${taxYearList.reverse}")
        Future(Ok(taxYearsView(
          taxYears = taxYearList.reverse,
          backUrl = backUrl,
          isAgent = isAgent,
          utr = user.saUtr,
          itsaSubmissionIntegrationEnabled = isEnabled(ITSASubmissionIntegration),
          isPostFinalisationAmendmentR18Enabled = isEnabled(PostFinalisationAmendmentsR18),
          isMortgageEvidenceEnabled = isEnabled(MortgageEvidence),
          earliestSubmissionTaxYear = user.incomeSources.earliestSubmissionTaxYear.getOrElse(2023),
          serviceNavigationPartial = user.serviceNavigationPartial,
          origin = origin
        )))
      case _ =>
        Logger("application").error(s"[TaxYearsController][handleRequest] failed to render taxYearsView for taxYears due to no orderedTaxYearsByYearOfMigration returned")
        Future(BadRequest(taxYearsView(
          taxYears = List(),
          backUrl = backUrl,
          isAgent = isAgent,
          utr = user.saUtr,
          itsaSubmissionIntegrationEnabled = isEnabled(ITSASubmissionIntegration),
          isPostFinalisationAmendmentR18Enabled = isEnabled(PostFinalisationAmendmentsR18),
          isMortgageEvidenceEnabled = isEnabled(MortgageEvidence),
          earliestSubmissionTaxYear = user.incomeSources.earliestSubmissionTaxYear.getOrElse(2023),
          serviceNavigationPartial = user.serviceNavigationPartial,
          origin = origin,
          errorTaxYear = Some(TaxYear.getCYPlusOneTaxYear),
        )))
    }
  }

  def showTaxYears(origin: Option[String] = None): Action[AnyContent] =
    authActions.asMTDIndividual().async { implicit user =>
      handleRequest(
        backUrl = appConfig.individualHomeUrlWithOrigin(origin),
        isAgent = false,
        origin = origin
      )
    }

  def showAgentTaxYears: Action[AnyContent] =
    authActions.asMTDPrimaryAgent().async {
      implicit mtdItUser =>
        handleRequest(
          backUrl = appConfig.agentHomeUrl,
          isAgent = true
        )
    }
}