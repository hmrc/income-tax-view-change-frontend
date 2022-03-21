/*
 * Copyright 2022 HM Revenue & Customs
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

import audit.AuditingService
import audit.models.TaxYearOverviewResponseAuditModel
import auth.MtdItUser
import config.featureswitch.{CodingOut, FeatureSwitching}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates._
import forms.utils.SessionKeys
import models.financialDetails.{DocumentDetailWithDueDate, FinancialDetailsErrorModel, FinancialDetailsModel}
import models.liabilitycalculation.viewmodels.TaxYearOverviewViewModel
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse, LiabilityCalculationResponseModel}
import models.nextUpdates.ObligationsModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{CalculationService, FinancialDetailsService, NextUpdatesService}
import views.html.TaxYearOverview

import java.net.URI
import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxYearOverviewController @Inject()(taxYearOverviewView: TaxYearOverview,
                                          authenticate: AuthenticationPredicate,
                                          calculationService: CalculationService,
                                          checkSessionTimeout: SessionTimeoutPredicate,
                                          financialDetailsService: FinancialDetailsService,
                                          itvcErrorHandler: ItvcErrorHandler,
                                          retrieveIncomeSourcesNoCache: IncomeSourceDetailsPredicateNoCache,
                                          retrieveNino: NinoPredicate,
                                          nextUpdatesService: NextUpdatesService,
                                          val retrieveBtaNavBar: BtaNavBarPredicate,
                                          val auditingService: AuditingService)
                                         (implicit val appConfig: FrontendAppConfig,
                                          mcc: MessagesControllerComponents,
                                          val executionContext: ExecutionContext)
  extends BaseController with FeatureSwitching with I18nSupport {

  val action: ActionBuilder[MtdItUser, AnyContent] = checkSessionTimeout andThen authenticate andThen
    retrieveNino andThen retrieveIncomeSourcesNoCache andThen retrieveBtaNavBar

  private def view(liabilityCalc: LiabilityCalculationResponseModel,
                   documentDetailsWithDueDates: List[DocumentDetailWithDueDate],
                   taxYear: Int,
                   obligations: ObligationsModel,
                   codingOutEnabled: Boolean,
                   backUrl: String
                  )(implicit mtdItUser: MtdItUser[_]): Result = {
    liabilityCalc match {
      case liabilityCalc: LiabilityCalculationResponse =>
        val taxYearOverviewViewModel: TaxYearOverviewViewModel = TaxYearOverviewViewModel(liabilityCalc)
        auditingService.extendedAudit(TaxYearOverviewResponseAuditModel(
          mtdItUser, documentDetailsWithDueDates, obligations, Some(taxYearOverviewViewModel)))

        Logger("application").info(
          s"[TaxYearOverviewController][view][$taxYear]] Rendered Tax year overview page with Calc data")

        Ok(taxYearOverviewView(
          taxYear = taxYear,
          overviewOpt = Some(taxYearOverviewViewModel),
          charges = documentDetailsWithDueDates,
          obligations = obligations,
          codingOutEnabled = codingOutEnabled,
          backUrl = backUrl
        ))
      case error: LiabilityCalculationError if error.status == NOT_FOUND =>
        auditingService.extendedAudit(TaxYearOverviewResponseAuditModel(
          mtdItUser, documentDetailsWithDueDates, obligations, None))

        Logger("application").info(
          s"[TaxYearOverviewController][view][$taxYear]] Rendered Tax year overview page with No Calc data")

        Ok(taxYearOverviewView(
          taxYear = taxYear,
          overviewOpt = None,
          charges = documentDetailsWithDueDates,
          obligations = obligations,
          codingOutEnabled = codingOutEnabled,
          backUrl = backUrl
        ))
      case _: LiabilityCalculationError =>
        Logger("application").error(
          s"[TaxYearOverviewController][view][$taxYear]] No new calc deductions data error found. Downstream error")
        itvcErrorHandler.showInternalServerError()
    }
  }

  private def withTaxYearFinancials(taxYear: Int)(f: List[DocumentDetailWithDueDate] => Future[Result])
                                   (implicit user: MtdItUser[AnyContent]): Future[Result] = {

    financialDetailsService.getFinancialDetails(taxYear, user.nino) flatMap {
      case financialDetails@FinancialDetailsModel(_, _, documentDetails, _) =>
        val docDetailsNoPayments = documentDetails.filter(_.paymentLot.isEmpty)
        val docDetailsCodingOut = docDetailsNoPayments.filter(_.isCodingOutDocumentDetail(isEnabled(CodingOut)))
        val documentDetailsWithDueDates: List[DocumentDetailWithDueDate] = {
          docDetailsNoPayments.filter(_.isNotCodingOutDocumentDetail).filter(_.originalAmountIsNotZero).map(
            documentDetail => DocumentDetailWithDueDate(documentDetail, financialDetails.findDueDateByDocumentDetails(documentDetail),
              dunningLock = financialDetails.dunningLockExists(documentDetail.transactionId)))
        }
        val documentDetailsWithDueDatesForLpi: List[DocumentDetailWithDueDate] = {
          docDetailsNoPayments.filter(_.isLatePaymentInterest).map(
            documentDetail => DocumentDetailWithDueDate(documentDetail, documentDetail.interestEndDate, isLatePaymentInterest = true,
              dunningLock = financialDetails.dunningLockExists(documentDetail.transactionId)))
        }
        val documentDetailsWithDueDatesCodingOutPaye: List[DocumentDetailWithDueDate] = {
          docDetailsCodingOut.filter(dd => dd.isPayeSelfAssessment && dd.originalAmountIsNotZero).map(
            documentDetail => DocumentDetailWithDueDate(documentDetail, financialDetails.getDueDateFor(documentDetail),
              dunningLock = financialDetails.dunningLockExists(documentDetail.transactionId)))
        }
        val documentDetailsWithDueDatesCodingOut: List[DocumentDetailWithDueDate] = {
          docDetailsCodingOut.filter(dd => !dd.isPayeSelfAssessment && dd.originalAmountIsNotZero).map(
            documentDetail => DocumentDetailWithDueDate(documentDetail, financialDetails.getDueDateFor(documentDetail),
              dunningLock = financialDetails.dunningLockExists(documentDetail.transactionId)))
        }
        f(documentDetailsWithDueDates ++ documentDetailsWithDueDatesForLpi ++ documentDetailsWithDueDatesCodingOutPaye ++ documentDetailsWithDueDatesCodingOut)
      case FinancialDetailsErrorModel(NOT_FOUND, _) => f(List.empty)
      case _ =>
        Logger("application").error(s"[TaxYearOverviewController][withTaxYearFinancials] - Could not retrieve financial details for year: $taxYear")
        Future.successful(itvcErrorHandler.showInternalServerError())
    }
  }

  private def withObligationsModel(taxYear: Int)(implicit user: MtdItUser[AnyContent]) = {
    nextUpdatesService.getNextUpdates(
      fromDate = LocalDate.of(taxYear - 1, 4, 6),
      toDate = LocalDate.of(taxYear, 4, 5)
    )
  }

  private def getBackURL(referer: Option[String]): String = {
    referer.map(URI.create(_).getPath.equals(taxYearsUrl)) match {
      case Some(true) => taxYearsUrl
      case Some(false) if referer.map(URI.create(_).getPath.equals(whatYouOweUrl)).get => whatYouOweUrl
      case _ => homeUrl
    }
  }

  private def showTaxYearOverview(taxYear: Int): Action[AnyContent] = action.async {
    implicit user =>
      withTaxYearFinancials(taxYear) { charges =>
        withObligationsModel(taxYear) flatMap {
          case obligationsModel: ObligationsModel =>
            val codingOutEnabled = isEnabled(CodingOut)
            calculationService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYear).map { liabilityCalcResponse =>
              view(liabilityCalcResponse, charges, taxYear, obligationsModel, codingOutEnabled,
                backUrl = getBackURL(user.headers.get(REFERER)))
                .addingToSession(SessionKeys.chargeSummaryBackPage -> "taxYearOverview")
            }
          case _ => Future.successful(itvcErrorHandler.showInternalServerError())
        }
      }
  }

  def renderTaxYearOverviewPage(taxYear: Int): Action[AnyContent] = {
    if (taxYear.toString.matches("[0-9]{4}")) {
      showTaxYearOverview(taxYear)
    } else {
      action.async { implicit request =>
        Future.successful(itvcErrorHandler.showInternalServerError())
      }
    }
  }

  lazy val taxYearsUrl: String = controllers.routes.TaxYearsController.viewTaxYears().url
  lazy val whatYouOweUrl: String = controllers.routes.WhatYouOweController.show().url


  lazy val homeUrl: String = controllers.routes.HomeController.home().url
}

