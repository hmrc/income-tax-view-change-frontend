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

package controllers.reportingObligations

import auth.authV2.AuthActions
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import models.admin.{OptInOptOutContentUpdateR17, OptOutFs, ReportingFrequencyPage, SignUpFs}
import models.reportingObligations.ReportingFrequencyViewModel
import models.reportingObligations.optOut.{OptOutMultiYearViewModel, OptOutOneYearViewModel}
import play.api.i18n.I18nSupport
import play.api.mvc.*
import services.DateServiceInterface
import services.reportingObligations.ReportingObligationsAuditService
import services.reportingObligations.signUp.OptInService
import services.reportingObligations.optOut.OptOutService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.MtdConstants
import viewUtils.ReportingFrequencyViewUtils
import views.html.errorPages.templates.ErrorTemplate
import views.html.reportingObligations.ReportingFrequencyView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportingFrequencyPageController @Inject()(
                                                  optOutService: OptOutService,
                                                  optInService: OptInService,
                                                  val auth: AuthActions,
                                                  errorTemplate: ErrorTemplate,
                                                  reportingObligationsAuditService: ReportingObligationsAuditService,
                                                  reportingFrequencyViewUtils: ReportingFrequencyViewUtils,
                                                  view: ReportingFrequencyView
                                                )(
                                                  implicit val appConfig: FrontendAppConfig,
                                                  val dateService: DateServiceInterface,
                                                  mcc: MessagesControllerComponents,
                                                  val ec: ExecutionContext
                                                )
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport with MtdConstants {

  def show(isAgent: Boolean): Action[AnyContent] =
    auth.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
      for {
        (optOutProposition, optOutJourneyType) <- optOutService.reportingFrequencyViewModels()
        optInProposition <- optInService.fetchOptInProposition()
        optInTaxYears <- optInService.availableOptInTaxYear()
        _ <- optOutService.updateJourneyStatusInSessionData(journeyComplete = false)
        _ <- optInService.updateJourneyStatusInSessionData(journeyComplete = false)
        viewModel =
          ReportingFrequencyViewModel(
            isAgent = user.isAgent(),
            optInTaxYears = optInTaxYears,
            itsaStatusTable = reportingFrequencyViewUtils.itsaStatusTable(optOutProposition),
            displayCeasedBusinessWarning = user.incomeSources.areAllBusinessesCeased,
            isAnyOfBusinessLatent = user.incomeSources.isAnyOfActiveBusinessesLatent,
            displayManageYourReportingFrequencySection =
              !(optOutProposition.areAllTaxYearsMandated || user.incomeSources.areAllBusinessesCeased),
            mtdThreshold = getMtdThreshold(),
            proposition = optOutProposition,
            isSignUpEnabled = isEnabled(SignUpFs),
            isOptOutEnabled = isEnabled(OptOutFs)
          )
        nextUpdatesLink =
          if (isAgent) controllers.routes.NextUpdatesController.showAgent().url
          else controllers.routes.NextUpdatesController.show().url
        result <-
          if (isEnabled(ReportingFrequencyPage) && reportingFrequencyViewUtils.itsaStatusTable(optOutProposition).nonEmpty) {
            reportingObligationsAuditService
              .sendAuditEvent(optOutProposition, viewModel.getSummaryCardSuffixes)
              .map { _ =>
                Ok(
                  view(
                    viewModel,
                    isEnabled(OptInOptOutContentUpdateR17),
                    nextUpdatesLink
                  )
                )
              }
          } else {
            Future.successful(
              InternalServerError(
                errorTemplate(
                  pageTitle = "standardError.heading",
                  heading = "standardError.heading",
                  message = "standardError.message",
                  isAgent = user.isAgent()
                )
              )
            )
          }
      } yield result
    }

}