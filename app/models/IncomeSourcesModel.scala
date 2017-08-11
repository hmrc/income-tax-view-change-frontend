/*
 * Copyright 2017 HM Revenue & Customs
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

package models

import play.api.libs.json.{Json, OFormat}
import java.time.LocalDate
import utils.ImplicitDateFormatter._
import play.api.Logger

case class IncomeSourcesModel(
                              businessDetails: Option[BusinessIncomeModel],
                              propertyDetails: Option[PropertyIncomeModel]
                            ) {

  val hasPropertyIncome: Boolean = propertyDetails.nonEmpty
  val hasBusinessIncome: Boolean = businessDetails.nonEmpty
  val hasBothIncomeSources: Boolean = hasPropertyIncome && hasBusinessIncome

  val orderedTaxYears: List[Int] =
    List(
      propertyDetails.map(_.accountingPeriod.determineTaxYear),
      businessDetails.map(_.accountingPeriod.determineTaxYear)
    )
      .flatten
      .sortWith(_ < _)
      .distinct

  val earliestTaxYear: Option[Int] = orderedTaxYears.headOption
  val lastTaxYear: Option[Int] = orderedTaxYears.lastOption

  val earliestAccountingPeriodStart: Int => Option[LocalDate] = taxYear => (hasBusinessIncome, hasPropertyIncome) match {
    case (b,p) if b && p =>
      if(propertyDetails.get.accountingPeriod.determineTaxYear == taxYear) Some(s"${taxYear-1}-4-6".toLocalDate)
      else if(businessDetails.get.accountingPeriod.determineTaxYear == taxYear) Some(businessDetails.get.accountingPeriod.start)
      else {
        Logger.error(s"[IncomeSourcesModel][earliestAccountingPeriodStart] - Neither income source matched taxYear: $taxYear")
        None}
    case (_,p) if p =>
      if(propertyDetails.get.accountingPeriod.determineTaxYear == taxYear) Some(s"${taxYear-1}-4-6".toLocalDate)
      else {
        Logger.warn(s"[IncomeSourcesModel][earliestAccountingPeriodStart] - Property income source did not match taxYear: $taxYear")
        None}
    case (b,_) if b =>
      if(businessDetails.get.accountingPeriod.determineTaxYear == taxYear) Some(businessDetails.get.accountingPeriod.start)
      else {
        Logger.warn(s"[IncomeSourcesModel][earliestAccountingPeriodStart] - Business income source did not match taxYear: $taxYear")
        None}
    case _ =>
      Logger.warn(s"[IncomeSourcesModel][earliestAccountingPeriodStart] - No income sources matched taxYear: $taxYear")
      None
  }
}

object IncomeSourcesModel {
  val format: OFormat[IncomeSourcesModel] = Json.format[IncomeSourcesModel]

}


case class BusinessIncomeModel(selfEmploymentId: String, accountingPeriod: AccountingPeriodModel, tradingName: String)

object BusinessIncomeModel {
  implicit val format: OFormat[BusinessIncomeModel] = Json.format[BusinessIncomeModel]
}


case class PropertyIncomeModel(accountingPeriod: AccountingPeriodModel)

object PropertyIncomeModel {
  implicit val format: OFormat[PropertyIncomeModel] = Json.format[PropertyIncomeModel]
}


