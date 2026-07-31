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

package hub.controllers

import common.auth.MtdItUser
import common.auth.AuthActions
import common.config.FrontendAppConfig
import common.services.AuditingService
import hub.audit.models.ChooseTaxYearsRangeSubmittedAuditModel
import hub.forms.{ChooseTaxYearsRangeForm, ChooseTaxYearsRangeOption}
import hub.models.TaxYearRangeLabels
import play.api.data.Form
import play.api.i18n.Messages
import hub.views.html.ChooseTaxYearsRangeView
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ChooseTaxYearsRangeController @Inject()(
                                              authActions: AuthActions,
                                              view: ChooseTaxYearsRangeView,
                                              auditingService: AuditingService
                                            )(
                                              implicit val appConfig: FrontendAppConfig,
                                              mcc: MessagesControllerComponents,
                                              ec: ExecutionContext
                                            ) extends FrontendController(mcc) with I18nSupport {
  private val submitCall = routes.ChooseTaxYearsRangeController.submit()

  private def migrationYear(user: MtdItUser[_]): Int =
    user.incomeSources.yearOfMigration
      .map(_.toInt)
      .getOrElse(throw new IllegalStateException("Year of migration is missing for ChooseTaxYearsRange journey"))

  private def taxYearRangeLabels(yearOfMigration: Int): TaxYearRangeLabels =
    TaxYearRangeLabels(
      mtdFromYear = yearOfMigration.toString,
      mtdToYear = (yearOfMigration + 1).toString,
      saFromYear = (yearOfMigration - 1).toString,
      saToYear = yearOfMigration.toString
    )

  private def renderView(
                          form: Form[ChooseTaxYearsRangeOption],
                          labels: TaxYearRangeLabels
                        )(implicit request: Request[_], messages: Messages) =
    view(
      form,
      submitCall,
      labels
    )

  private def auditSubmission(labels: TaxYearRangeLabels, taxYearRangeSelected: String)(implicit user: MtdItUser[_]): Unit =
    auditingService.extendedAudit(
      ChooseTaxYearsRangeSubmittedAuditModel(
        taxYearsPresented = labels.presentedTaxYears,
        taxYearRangeSelected = taxYearRangeSelected
      )
    )

  def show(): Action[AnyContent] = authActions.asMTDIndividualWithIncomeSources().async { implicit user =>
    if (user.saUtr.isDefined) {
      val labels = taxYearRangeLabels(migrationYear(user))
      Future.successful(Ok(renderView(ChooseTaxYearsRangeForm(), labels)))
    } else {
      Future.successful(Redirect(appConfig.homePageUrl(isAgent = false)))
    }
  }

  def submit(): Action[AnyContent] = authActions.asMTDIndividualWithIncomeSources().async { implicit user =>
    if (user.saUtr.isEmpty) {
      Future.successful(Redirect(appConfig.homePageUrl(isAgent = false)))
    } else {
      val labels = taxYearRangeLabels(migrationYear(user))

      ChooseTaxYearsRangeForm().bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(renderView(formWithErrors, labels))),
        form => form.selection match {
          case Some(ChooseTaxYearsRangeForm.mtdOption) =>
            auditSubmission(labels, ChooseTaxYearsRangeForm.mtdOption)
            Future.successful(Redirect(appConfig.homePageUrl(isAgent = false)))

          case Some(ChooseTaxYearsRangeForm.legacyOption) =>
            auditSubmission(labels, ChooseTaxYearsRangeForm.legacyOption)
            // TODO: replace with the confirmed legacy SA destination once provided.
            Future.successful(NotImplemented("TODO: legacy Self Assessment destination route still to be confirmed"))

          case Some(other) => throw new IllegalStateException(s"Unexpected tax years range selection: $other")
          case None => throw new IllegalStateException("Missing tax years range selection after successful form bind")
        }
      )
    }
  }
}
