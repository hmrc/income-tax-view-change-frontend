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
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Mandated, Voluntary}
import models.optout.{OptOutMultiYearViewModel, OptOutOneYearViewModel}
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.DateServiceInterface
import services.optIn.OptInService
import services.optout.{OptOutProposition, OptOutService, PreviousOptOutTaxYear}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.ReportingFrequencyView
import views.html.errorPages.templates.ErrorTemplate

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportingFrequencyPageController @Inject()(
                                                  optOutService: OptOutService,
                                                  optInService: OptInService,
                                                  val auth: AuthActions,
                                                  errorTemplate: ErrorTemplate,
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
        if (isEnabled(ReportingFrequencyPage) && itsaStatusTable(optOutProposition).nonEmpty) {

          val optOutUrl: Option[String] = {
            optOutJourneyType.map {
              case _: OptOutOneYearViewModel =>
                controllers.optOut.routes.ConfirmOptOutController.show(user.isAgent()).url
              case _: OptOutMultiYearViewModel =>
                controllers.optOut.routes.OptOutChooseTaxYearController.show(user.isAgent()).url
            }
          }

          Ok(view(
            ReportingFrequencyViewModel(
              isAgent = user.isAgent(),
              optOutJourneyUrl = optOutUrl,
              optOutTaxYears = optOutProposition.availableTaxYearsForOptOut,
              optInTaxYears = optInTaxYears,
              itsaStatusTable = itsaStatusTable(optOutProposition)
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

  private def itsaStatusString(ITSAStatus: ITSAStatus): Option[String] = {
    ITSAStatus match {
      case Mandated => Some("Quarterly (mandatory)")
      case Voluntary => Some("Quarterly")
      case Annual => Some("Annual")
      case _ => None
    }
  }

  private def itsaStatusTable(optOutProposition: OptOutProposition): Seq[(String, Option[String])] = {
    if(optOutProposition.previousTaxYear.crystallised) {
      Seq(
        (optOutProposition.currentTaxYear.taxYear.taxYearWithToString, itsaStatusString(optOutProposition.currentTaxYear.status)),
        (optOutProposition.nextTaxYear.taxYear.taxYearWithToString, itsaStatusString(optOutProposition.nextTaxYear.status))
      ).filter(_._2.nonEmpty)
    } else {
      Seq(
        (optOutProposition.previousTaxYear.taxYear.taxYearWithToString, itsaStatusString(optOutProposition.previousTaxYear.status)),
        (optOutProposition.currentTaxYear.taxYear.taxYearWithToString, itsaStatusString(optOutProposition.currentTaxYear.status)),
        (optOutProposition.nextTaxYear.taxYear.taxYearWithToString, itsaStatusString(optOutProposition.nextTaxYear.status))
      ).filter(_._2.nonEmpty)
    }
  }
}