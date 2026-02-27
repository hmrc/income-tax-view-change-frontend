/*
 * Copyright 2025 HM Revenue & Customs
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

package services.reportingObligations

import audit.reporting_obligations.*
import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import enums.AuditType.IncomeSourceDetailsResponse as _
import enums.{AuditType, TransactionName}
import models.admin.OptInOptOutContentUpdateR17
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Mandated, UnknownStatus, Voluntary}
import play.api.Logging
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}
import services.DateServiceInterface
import services.reportingObligations.optOut.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import utils.MtdConstants
import viewUtils.ReportingFrequencyViewUtils

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportingObligationsAuditService @Inject()(
                                                  auditConnector: AuditConnector,
                                                  reportingFrequencyViewUtils: ReportingFrequencyViewUtils
                                                )(implicit val appConfig: FrontendAppConfig, val dateService: DateServiceInterface) extends Logging with MtdConstants with FeatureSwitching {

  def buildCards(summaryCardSuffixes: List[Option[String]]): List[ReportingObligationCard] = {
    summaryCardSuffixes.flatMap { suffix =>
      suffix.map {
        case "optOut.previousYear.onwards" =>
          ReportingObligationCard(
            journeyType = OptOut,
            taxYear = dateService.getCurrentTaxYear.previousYear.startYear.toString,
            singleYearOrOnwards = Onwards
          )
        case "optOut.previousYear.single" =>
          ReportingObligationCard(
            journeyType = OptOut,
            taxYear = dateService.getCurrentTaxYear.previousYear.startYear.toString,
            singleYearOrOnwards = SingleTaxYear
          )
        case "optOut.currentYear.onwards" =>
          ReportingObligationCard(
            journeyType = OptOut,
            taxYear = dateService.getCurrentTaxYear.startYear.toString,
            singleYearOrOnwards = Onwards
          )
        case "optOut.currentYear.single" =>
          ReportingObligationCard(
            journeyType = OptOut,
            taxYear = dateService.getCurrentTaxYear.startYear.toString,
            singleYearOrOnwards = SingleTaxYear
          )
        case "signUp.currentYear.onwards" =>
          ReportingObligationCard(
            journeyType = SignUp,
            taxYear = dateService.getCurrentTaxYear.startYear.toString,
            singleYearOrOnwards = Onwards
          )
        case "signUp.currentYear.single" =>
          ReportingObligationCard(
            journeyType = SignUp,
            taxYear = dateService.getCurrentTaxYear.startYear.toString,
            singleYearOrOnwards = SingleTaxYear
          )
        case "optOut.nextYear" =>
          ReportingObligationCard(
            journeyType = OptOut,
            taxYear = dateService.getCurrentTaxYear.nextYear.startYear.toString,
            singleYearOrOnwards = SingleTaxYear
          )
        case "signUp.nextYear" =>
          ReportingObligationCard(
            journeyType = SignUp,
            taxYear = dateService.getCurrentTaxYear.nextYear.startYear.toString,
            singleYearOrOnwards = SingleTaxYear
          )
      }
    }
  }

  private def tableContentToItsaStatus(content: Option[String])(implicit messages: Messages, user: MtdItUser[_]): ITSAStatus = {
    if (isEnabled(OptInOptOutContentUpdateR17)) {
      content match {
        case Some(tableContent) if tableContent == messages("reporting.frequency.table.mandated.r17") => Mandated
        case Some(tableContent) if tableContent == messages("reporting.frequency.table.voluntary.r17") => Voluntary
        case Some(tableContent) if tableContent == messages("reporting.frequency.table.annual.r17") => Annual
        case _ => UnknownStatus
      }
    } else {
      content match {
        case Some(tableContent) if tableContent == messages("reporting.frequency.table.mandated") => Mandated
        case Some(tableContent) if tableContent == messages("reporting.frequency.table.voluntary") => Voluntary
        case Some(tableContent) if tableContent == messages("reporting.frequency.table.annual") => Annual
        case _ => UnknownStatus
      }
    }
  }

  def buildItsaTableContentToCapture(optOutProposition: OptOutProposition)(
    implicit messages: Messages,
    mtdItUser: MtdItUser[_]
  ): List[ItsaStatusTableDetails] = {

    reportingFrequencyViewUtils.itsaStatusTable(optOutProposition).map { case (taxYearContent, isUsingMTD, yourStatus) =>

      val formatTaxYearMessage: String = taxYearContent.replace(" to ", "-").replace(" i ", "-")
      val maybeTaxYear: Option[TaxYear] = TaxYear.`fromStringYYYY-YYYY`(formatTaxYearMessage)

      val determineTaxYear: String =
        maybeTaxYear match {
          case Some(taxYear) if taxYear == dateService.getCurrentTaxYear.previousYear => "PreviousTaxYear"
          case Some(taxYear) if taxYear == dateService.getCurrentTaxYear => "CurrentTaxYear"
          case Some(taxYear) if taxYear == dateService.getCurrentTaxYear.nextYear => "NextTaxYear"
          case _ => "Unknown tax year"
        }

      ItsaStatusTableDetails(
        taxYearPeriod = determineTaxYear,
        taxYear = taxYearContent,
        usingMakingTaxDigitalForIncomeTax = isUsingMTD,
        userCurrentItsaStatus = tableContentToItsaStatus(yourStatus).toString
      )
    }.toList
  }


  def createAuditEvent(
                        optOutProposition: OptOutProposition,
                        summaryCardSuffixes: List[Option[String]]
                      )(implicit messages: Messages, mtdItUser: MtdItUser[_]): ReportingObligationsAuditModel = {

    val links: List[String] =
      buildCards(summaryCardSuffixes).map(_.auditModelToString())

    ReportingObligationsAuditModel(
      agentReferenceNumber = mtdItUser.arn,
      auditType = enums.AuditType.ReportingObligationsPage.name,
      credId = mtdItUser.credId,
      mtditid = mtdItUser.mtditid,
      nino = mtdItUser.nino,
      saUtr = mtdItUser.saUtr,
      userType = mtdItUser.usersRole,
      grossIncomeThreshold = getMtdThreshold(),
      crystallisationStatusForPreviousTaxYear = optOutProposition.previousTaxYear.crystallised,
      itsaStatusTable = buildItsaTableContentToCapture(optOutProposition),
      links = links
    )
  }

  private[services] def createJsonAuditBody(auditModel: ReportingObligationsAuditModel)(implicit headerCarrier: HeaderCarrier): JsObject =
    Json.toJson(AuditExtensions.auditHeaderCarrier(headerCarrier).toAuditDetails()).as[JsObject] ++ auditModel.detail


  def sendAuditEvent(
                      optOutProposition: OptOutProposition,
                      summaryCardSuffixes: List[Option[String]]
                    )(implicit headerCarrier: HeaderCarrier,
                      messages: Messages,
                      ec: ExecutionContext,
                      mtdItUser: MtdItUser[_]
                    ): Future[AuditResult] = {

    val auditEventModel = createAuditEvent(optOutProposition, summaryCardSuffixes)

    val transactionName: String =
      TransactionName.ReportingObligationsPage.name

    val extendedDataEvent =
      ExtendedDataEvent(
        auditSource = appConfig.appName,
        auditType = AuditType.ReportingObligationsPage,
        tags = AuditExtensions.auditHeaderCarrier(headerCarrier).toAuditTags(transactionName, mtdItUser.path),
        detail = createJsonAuditBody(auditEventModel)
      )

    auditConnector.sendExtendedEvent(extendedDataEvent)
  }
}