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

package controllers

import auth.authV2.AuthActions
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import models.ReportingFrequencyViewModel
import models.admin.ReportingFrequencyPage
import models.optout.{OptOutMultiYearViewModel, OptOutOneYearViewModel}
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.optIn.OptInService
import services.optout.OptOutService
import services.DateServiceInterface
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import viewUtils.ReportingFrequencyViewUtils
import views.html.ReportingFrequencyView
import views.html.errorPages.templates.ErrorTemplate

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ReportingFrequencyPageController @Inject()(
                                                  optOutService: OptOutService,
                                                  optInService: OptInService,
                                                  val auth: AuthActions,
                                                  errorTemplate: ErrorTemplate,
                                                  reportingFrequencyViewUtils: ReportingFrequencyViewUtils,
                                                  view: ReportingFrequencyView
                                                )(
                                                  implicit val appConfig: FrontendAppConfig,
                                                  val dateService: DateServiceInterface,
                                                  mcc: MessagesControllerComponents,
                                                  val ec: ExecutionContext
                                                )

  extends FrontendController(mcc) with FeatureSwitching with I18nSupport {

  def show(isAgent: Boolean): Action[AnyContent] =
    auth.asIndividualOrAgent(isAgent).async { implicit user =>

      for {
        (optOutProposition, optOutJourneyType) <- optOutService.reportingFrequencyViewModels()
        optInTaxYears <- optInService.availableOptInTaxYear()

      } yield {
        if (isEnabled(ReportingFrequencyPage) && reportingFrequencyViewUtils.itsaStatusTable(optOutProposition).nonEmpty) {

          val optOutUrl: Option[String] = {
            optOutJourneyType.map {
              case _: OptOutOneYearViewModel =>
                controllers.optOut.routes.ConfirmOptOutController.show(user.isAgent()).url
              case _: OptOutMultiYearViewModel =>
                controllers.optOut.routes.OptOutChooseTaxYearController.show(user.isAgent()).url
            }
          }

          val isCeased = user.incomeSources.businesses.forall(_.isCeased)

          Ok(view(
            ReportingFrequencyViewModel(
              isAgent = user.isAgent(),
              optOutJourneyUrl = optOutUrl,
              optOutTaxYears = optOutProposition.availableTaxYearsForOptOut,
              optInTaxYears = optInTaxYears,
              itsaStatusTable = reportingFrequencyViewUtils.itsaStatusTable(optOutProposition),
              displayCeasedBusinessWarning = isCeased
            )
          ))
        } else {
          InternalServerError(
            errorTemplate(
              pageTitle = "standardError.heading",
              heading = "standardError.heading",
              message = "standardError.message",
              isAgent = user.isAgent()
            )
          )
        }
      }
    }
}