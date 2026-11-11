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

package v1.controllers

import auth.{AuthActions, MtdItUser}
import config.FrontendAppConfig
import services.{AuditingService, YearOfMigrationService}
import audit.models.ChooseTaxYearsRangeSubmittedAuditModel
import forms.{ChooseTaxYearsRangeForm, ChooseTaxYearsRangeOption}
import models.TaxYearRangeLabels
import views.html.ChooseTaxYearsRangeView
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Request}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ChooseTaxYearsRangeController @Inject()(
                                              authActions: AuthActions,
                                              view: ChooseTaxYearsRangeView,
                                              auditingService: AuditingService,
                                              yearOfMigrationService: YearOfMigrationService
                                            )(
                                              implicit val appConfig: FrontendAppConfig,
                                              mcc: MessagesControllerComponents,
                                              ec: ExecutionContext
                                            ) extends FrontendController(mcc) with I18nSupport {
  private val submitCall = {
    val submitRoute = routes.ChooseTaxYearsRangeController.submit()
    Call(submitRoute.method, appConfig.appUrl(submitRoute.url))
  }
  private val submitPtaMultipleEnrolmentCall = {
    val submitRoute = routes.ChooseTaxYearsRangeController.submitPtaMultipleEnrolment()
    Call(submitRoute.method, appConfig.appUrl(submitRoute.url))
  }

  private def migrationYear(user: MtdItUser[_])(implicit hc: HeaderCarrier): Future[Int] =
    yearOfMigrationService.getYearOfMigration(user.nino)
      .map{
        _.yearOfMigrationEndYear.map(_.toInt)
        .getOrElse(throw new IllegalStateException("Year of migration is missing for ChooseTaxYearsRange journey"))
      }
    
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
                        )(implicit request: Request[_], messages: Messages) = view(
                          form,
                          submitCall,
                          labels
                        )

  private def auditSubmission(labels: TaxYearRangeLabels, taxYearRangeSelected: String, taxYearRadioSelected: String)(implicit user: MtdItUser[_]): Unit =
    auditingService.extendedAudit(
      ChooseTaxYearsRangeSubmittedAuditModel(
        taxYearsPresented = labels.presentedTaxYears,
        taxYearRangeSelected = taxYearRangeSelected,
        taxYearRadioSelected = taxYearRadioSelected
      )
    )

  def show(): Action[AnyContent] = authActions.asMTDIndividual().async { implicit user =>
    migrationYear(user).map {
      year => Ok(renderView(ChooseTaxYearsRangeForm(), taxYearRangeLabels(year)))
    }
  }

  def submit(): Action[AnyContent] = authActions.asMTDIndividual().async { implicit user =>
    migrationYear(user).map { year =>
      ChooseTaxYearsRangeForm().bindFromRequest().fold(
        formWithErrors => BadRequest(renderView(formWithErrors, taxYearRangeLabels(year))),
        form => form.selection match {
          case Some(ChooseTaxYearsRangeForm.mtdOption) =>
            auditSubmission(taxYearRangeLabels(year), ChooseTaxYearsRangeForm.mtdOption, taxYearRangeLabels(year).mtdTaxYearRange)
            Redirect(newHomePage.routes.HandleYourTasksController.show())

          case Some(ChooseTaxYearsRangeForm.legacyOption) =>
            auditSubmission(taxYearRangeLabels(year), ChooseTaxYearsRangeForm.legacyOption, taxYearRangeLabels(year).saTaxYearRange)
            val utr = user.saUtr.getOrElse(
              throw new IllegalStateException("SA UTR is missing for legacy SA redirect")
            )
            Redirect(appConfig.saViewLandPService(utr))

          case Some(other) => throw new IllegalStateException(s"Unexpected tax years range selection: $other")
          case None => throw new IllegalStateException("Missing tax years range selection after successful form bind")
        }
      )
    }
  }

  def showPtaMultipleEnrolment(): Action[AnyContent] = authActions.asMTDIndividual().async { implicit user =>
    migrationYear(user).map {
      year => Ok(view(ChooseTaxYearsRangeForm(), submitPtaMultipleEnrolmentCall, taxYearRangeLabels(year)))
    }
  }

  def submitPtaMultipleEnrolment(): Action[AnyContent] = authActions.asMTDIndividual().async { implicit user =>
      migrationYear(user).map {
        year =>
          ChooseTaxYearsRangeForm().bindFromRequest().fold(
            formWithErrors => BadRequest(view(formWithErrors, submitPtaMultipleEnrolmentCall, taxYearRangeLabels(year))),
            form => form.selection match
              case Some(ChooseTaxYearsRangeForm.mtdOption) =>
                auditSubmission(taxYearRangeLabels(year), ChooseTaxYearsRangeForm.mtdOption, taxYearRangeLabels(year).mtdTaxYearRange)
                Redirect(newHomePage.routes.HandleYourTasksController.show())

              case Some(ChooseTaxYearsRangeForm.legacyOption) =>
                auditSubmission(taxYearRangeLabels(year), ChooseTaxYearsRangeForm.legacyOption, taxYearRangeLabels(year).saTaxYearRange)
                Redirect(appConfig.personalTaxAccountWrongAccountUrl)

              case Some(other) => throw new IllegalStateException(s"Unexpected tax years range selection: $other")
              case None => throw new IllegalStateException("Missing tax years range selection after successful form bind")

          )
      }
    }
}
                                          
