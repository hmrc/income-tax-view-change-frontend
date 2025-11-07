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

package services.reporting_frequency

import audit.reporting_obligations._
import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import enums.AuditType.{IncomeSourceDetailsResponse => _}
import enums.{AuditType, TransactionName}
import models.admin.OptInOptOutContentUpdateR17
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Mandated, UnknownStatus, Voluntary}
import play.api.Logging
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}
import services.DateServiceInterface
import services.optIn.core.{CurrentOptInTaxYear, NextOptInTaxYear, OptInProposition}
import services.optout._
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
                                                  reportingFrequencyViewUtils: ReportingFrequencyViewUtils,
                                                )(implicit val appConfig: FrontendAppConfig, val dateService: DateServiceInterface) extends Logging with MtdConstants with FeatureSwitching {

  def buildOptOutCards(optOutProposition: OptOutProposition): Seq[ReportingObligationCard] =
    optOutProposition.availableOptOutYears match {
      case optOutTaxYears if optOutTaxYears.size == 1 =>
        optOutTaxYears.map(optOutTaxYear =>
          ReportingObligationCard(
            journeyType = OptOut,
            taxYear = optOutTaxYear.taxYear.startYear.toString,
            singleYearOrOnwards = SingleTaxYear
          )
        )
      case Seq(PreviousOptOutTaxYear(Voluntary, _, true), CurrentOptOutTaxYear(Voluntary, currentTaxYear), NextOptOutTaxYear(Voluntary, nextTaxYear, _)) =>
        Seq(
          ReportingObligationCard(
            journeyType = OptOut,
            taxYear = currentTaxYear.startYear.toString,
            singleYearOrOnwards = Onwards
          ),
          ReportingObligationCard(
            journeyType = OptOut,
            taxYear = nextTaxYear.startYear.toString,
            singleYearOrOnwards = SingleTaxYear
          ),
        )
      case Seq(PreviousOptOutTaxYear(Voluntary, prevTaxYear, false), CurrentOptOutTaxYear(Voluntary, currentTaxYear), NextOptOutTaxYear(Voluntary, nextTaxYear, _)) =>
        Seq(
          ReportingObligationCard(
            journeyType = OptOut,
            taxYear = prevTaxYear.startYear.toString,
            singleYearOrOnwards = Onwards
          ),
          ReportingObligationCard(
            journeyType = OptOut,
            taxYear = currentTaxYear.startYear.toString,
            singleYearOrOnwards = Onwards
          ),
          ReportingObligationCard(
            journeyType = OptOut,
            taxYear = nextTaxYear.startYear.toString,
            singleYearOrOnwards = SingleTaxYear
          ),
        )
      case Seq(PreviousOptOutTaxYear(Voluntary, prevTaxYear, false), NextOptOutTaxYear(Voluntary, nextTaxYear, _)) =>
        Seq(
          ReportingObligationCard(
            journeyType = OptOut,
            taxYear = prevTaxYear.startYear.toString,
            singleYearOrOnwards = Onwards
          ),
          ReportingObligationCard(
            journeyType = OptOut,
            taxYear = nextTaxYear.startYear.toString,
            singleYearOrOnwards = SingleTaxYear
          ),
        )
      case Seq(PreviousOptOutTaxYear(Voluntary, prevTaxYear, false), CurrentOptOutTaxYear(Voluntary, currentTaxYear)) =>
        Seq(
          ReportingObligationCard(
            journeyType = OptOut,
            taxYear = prevTaxYear.startYear.toString,
            singleYearOrOnwards = Onwards
          ),
          ReportingObligationCard(
            journeyType = OptOut,
            taxYear = currentTaxYear.startYear.toString,
            singleYearOrOnwards = SingleTaxYear
          ),
        )
      case Seq(CurrentOptOutTaxYear(Voluntary, currentTaxYear), NextOptOutTaxYear(Voluntary, nextTaxYear, _)) =>
        Seq(
          ReportingObligationCard(
            journeyType = OptOut,
            taxYear = currentTaxYear.startYear.toString,
            singleYearOrOnwards = Onwards
          ),
          ReportingObligationCard(
            journeyType = OptOut,
            taxYear = nextTaxYear.startYear.toString,
            singleYearOrOnwards = SingleTaxYear
          ),
        )
      case Seq(NextOptOutTaxYear(Voluntary, nextTaxYear, _)) =>
        Seq(
          ReportingObligationCard(
            journeyType = OptOut,
            taxYear = nextTaxYear.startYear.toString,
            singleYearOrOnwards = SingleTaxYear
          ),
        )
      case _ =>
        List.empty
    }

  def buildSignUpCards(optInProposition: OptInProposition): Seq[ReportingObligationCard] =
    optInProposition match {
      case OptInProposition(CurrentOptInTaxYear(Annual, currentTaxYear), NextOptInTaxYear(Annual, nextTaxYear, _)) =>
        Seq(
          ReportingObligationCard(
            journeyType = SignUp,
            taxYear = currentTaxYear.startYear.toString,
            singleYearOrOnwards = Onwards
          ),
          ReportingObligationCard(
            journeyType = SignUp,
            taxYear = nextTaxYear.startYear.toString,
            singleYearOrOnwards = SingleTaxYear
          )
        )
      case OptInProposition(CurrentOptInTaxYear(Annual, currentTaxYear), NextOptInTaxYear(_, _, _)) =>
        Seq(
          ReportingObligationCard(
            journeyType = SignUp,
            taxYear = currentTaxYear.startYear.toString,
            singleYearOrOnwards = SingleTaxYear
          )
        )
      case OptInProposition(CurrentOptInTaxYear(_, _), NextOptInTaxYear(Annual, nextTaxYear, _)) =>
        Seq(
          ReportingObligationCard(
            journeyType = SignUp,
            taxYear = nextTaxYear.startYear.toString,
            singleYearOrOnwards = SingleTaxYear
          )
        )
      case _ =>
        List.empty
    }

  def tableContentToItsaStatus(content: Option[String])(implicit messages: Messages, user: MtdItUser[_]): ITSAStatus = {
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
        yourStatus = tableContentToItsaStatus(yourStatus).toString
      )
    }.toList
  }


  def createAuditEvent(
                        optOutProposition: OptOutProposition,
                        optInProposition: OptInProposition,
                      )(implicit messages: Messages, mtdItUser: MtdItUser[_]): ReportingObligationsAuditModel = {

    val links: List[String] =
      (buildOptOutCards(optOutProposition) ++ buildSignUpCards(optInProposition)).map(_.auditModelToString()).toList

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
                      optInProposition: OptInProposition
                    )(implicit headerCarrier: HeaderCarrier,
                      messages: Messages,
                      ec: ExecutionContext,
                      mtdItUser: MtdItUser[_]
                    ): Future[AuditResult] = {

    val auditEventModel = createAuditEvent(optOutProposition, optInProposition)

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