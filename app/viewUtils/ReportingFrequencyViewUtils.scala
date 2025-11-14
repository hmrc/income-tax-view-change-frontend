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

package viewUtils

import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import models.admin.OptInOptOutContentUpdateR17
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.{Annual, DigitallyExempt, Exempt, ITSAStatus, Mandated, Voluntary}
import play.api.i18n.Messages
import services.DateServiceInterface
import services.optout.OptOutProposition

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ReportingFrequencyViewUtils @Inject()()(
  implicit val appConfig: FrontendAppConfig,
  val dateService: DateServiceInterface,
  val ec: ExecutionContext
) extends FeatureSwitching {


  def itsaStatusString(itsaStatus: ITSAStatus)(implicit messages: Messages, user: MtdItUser[_]): Option[String] = {
    if(isEnabled(OptInOptOutContentUpdateR17)){
      itsaStatus match {
        case Mandated => Some(messages("reporting.frequency.table.mandated.r17"))
        case Voluntary => Some(messages("reporting.frequency.table.voluntary.r17"))
        case Annual => Some(messages("reporting.frequency.table.annual.r17"))
        case Exempt => Some(messages("reporting.frequency.table.exempt.r17"))
        case DigitallyExempt => Some(messages("reporting.frequency.table.exempt.r17"))
        case _ => None
      }
    }else{
      itsaStatus match {
        case Mandated => Some(messages("reporting.frequency.table.mandated"))
        case Voluntary => Some(messages("reporting.frequency.table.voluntary"))
        case Annual => Some(messages("reporting.frequency.table.annual"))
        case Exempt => Some(messages("reporting.frequency.table.exempt"))
        case DigitallyExempt => Some(messages("reporting.frequency.table.exempt"))
        case _ => None
      }
    }
  }

  private def isUsingMTD(itsaStatus: ITSAStatus)(implicit messages: Messages): Option[String] = {
    itsaStatus match {
      case Mandated => Some(messages("reporting.frequency.table.MTD.isUsingMTD"))
      case Voluntary => Some(messages("reporting.frequency.table.MTD.isUsingMTD"))
      case Annual => Some(messages("reporting.frequency.table.MTD.isNotUsingMTD"))
      case Exempt => Some(messages("reporting.frequency.table.MTD.isNotUsingMTD"))
      case DigitallyExempt => Some(messages("reporting.frequency.table.MTD.isNotUsingMTD"))
      case _ => None
    }
  }

  def itsaStatusTable(optOutProposition: OptOutProposition)(implicit messages: Messages, user: MtdItUser[_]): Seq[(String, Option[String], Option[String])] = {
    if (optOutProposition.previousTaxYear.crystallised) {
      if(isEnabled(OptInOptOutContentUpdateR17)) {
        Seq(
          (messages("reporting.frequency.table.taxYear", optOutProposition.currentTaxYear.taxYear.startYear.toString, optOutProposition.currentTaxYear.taxYear.endYear.toString), isUsingMTD(optOutProposition.currentTaxYear.status), itsaStatusString(optOutProposition.currentTaxYear.status)),
          (messages("reporting.frequency.table.taxYear", optOutProposition.nextTaxYear.taxYear.startYear.toString, optOutProposition.nextTaxYear.taxYear.endYear.toString), isUsingMTD(optOutProposition.nextTaxYear.status), itsaStatusString(optOutProposition.nextTaxYear.status))
        ).filter(_._3.nonEmpty)
      }else{
        Seq(
          (messages("reporting.frequency.table.taxYear", optOutProposition.currentTaxYear.taxYear.startYear.toString, optOutProposition.currentTaxYear.taxYear.endYear.toString), None, itsaStatusString(optOutProposition.currentTaxYear.status)),
          (messages("reporting.frequency.table.taxYear", optOutProposition.nextTaxYear.taxYear.startYear.toString, optOutProposition.nextTaxYear.taxYear.endYear.toString), None, itsaStatusString(optOutProposition.nextTaxYear.status))
        ).filter(_._3.nonEmpty)
      }
    } else {
      if(isEnabled(OptInOptOutContentUpdateR17)) {
        Seq(
          (messages("reporting.frequency.table.taxYear", optOutProposition.previousTaxYear.taxYear.startYear.toString, optOutProposition.previousTaxYear.taxYear.endYear.toString), isUsingMTD(optOutProposition.previousTaxYear.status), itsaStatusString(optOutProposition.previousTaxYear.status)),
          (messages("reporting.frequency.table.taxYear", optOutProposition.currentTaxYear.taxYear.startYear.toString, optOutProposition.currentTaxYear.taxYear.endYear.toString), isUsingMTD(optOutProposition.currentTaxYear.status), itsaStatusString(optOutProposition.currentTaxYear.status)),
          (messages("reporting.frequency.table.taxYear", optOutProposition.nextTaxYear.taxYear.startYear.toString, optOutProposition.nextTaxYear.taxYear.endYear.toString), isUsingMTD(optOutProposition.nextTaxYear.status), itsaStatusString(optOutProposition.nextTaxYear.status))
        ).filter(_._3.nonEmpty)
      }else{
        Seq(
          (messages("reporting.frequency.table.taxYear", optOutProposition.previousTaxYear.taxYear.startYear.toString, optOutProposition.previousTaxYear.taxYear.endYear.toString), None, itsaStatusString(optOutProposition.previousTaxYear.status)),
          (messages("reporting.frequency.table.taxYear", optOutProposition.currentTaxYear.taxYear.startYear.toString, optOutProposition.currentTaxYear.taxYear.endYear.toString), None, itsaStatusString(optOutProposition.currentTaxYear.status)),
          (messages("reporting.frequency.table.taxYear", optOutProposition.nextTaxYear.taxYear.startYear.toString, optOutProposition.nextTaxYear.taxYear.endYear.toString), None, itsaStatusString(optOutProposition.nextTaxYear.status))
        ).filter(_._3.nonEmpty)
      }
    }
  }
}