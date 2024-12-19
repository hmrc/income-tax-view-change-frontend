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

import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Mandated, Voluntary}
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


  def itsaStatusString(itsaStatus: ITSAStatus)(implicit messages: Messages): Option[String] = {
    itsaStatus match {
      case Mandated => Some(messages("reporting.frequency.table.mandated"))
      case Voluntary => Some(messages("reporting.frequency.table.voluntary"))
      case Annual => Some(messages("reporting.frequency.table.annual"))
      case _ => None
    }
  }

  def itsaStatusTable(optOutProposition: OptOutProposition)(implicit messages: Messages): Seq[(String, Option[String])] = {
    if (optOutProposition.previousTaxYear.crystallised) {
      Seq(
        messages("reporting.frequency.table.taxYear", optOutProposition.currentTaxYear.taxYear.startYear.toString, optOutProposition.currentTaxYear.taxYear.endYear.toString) -> itsaStatusString(optOutProposition.currentTaxYear.status),
        messages("reporting.frequency.table.taxYear", optOutProposition.nextTaxYear.taxYear.startYear.toString, optOutProposition.nextTaxYear.taxYear.endYear.toString) -> itsaStatusString(optOutProposition.nextTaxYear.status)
      ).filter(_._2.nonEmpty)
    } else {
      Seq(
        messages("reporting.frequency.table.taxYear", optOutProposition.previousTaxYear.taxYear.startYear.toString, optOutProposition.previousTaxYear.taxYear.endYear.toString) -> itsaStatusString(optOutProposition.previousTaxYear.status),
        messages("reporting.frequency.table.taxYear", optOutProposition.currentTaxYear.taxYear.startYear.toString, optOutProposition.currentTaxYear.taxYear.endYear.toString) -> itsaStatusString(optOutProposition.currentTaxYear.status),
        messages("reporting.frequency.table.taxYear", optOutProposition.nextTaxYear.taxYear.startYear.toString, optOutProposition.nextTaxYear.taxYear.endYear.toString) -> itsaStatusString(optOutProposition.nextTaxYear.status)
      ).filter(_._2.nonEmpty)
    }
  }
}