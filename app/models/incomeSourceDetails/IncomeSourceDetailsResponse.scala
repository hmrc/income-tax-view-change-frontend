/*
 * Copyright 2023 HM Revenue & Customs
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

package models.incomeSourceDetails

import play.api.Logging
import play.api.libs.json.{Format, JsValue, Json}
import services.DateServiceInterface
import uk.gov.hmrc.http.InternalServerException

sealed trait IncomeSourceDetailsResponse {
  def toJson: JsValue
}

case class IncomeSourceDetailsModel(mtdbsa: String,
                                    yearOfMigration: Option[String],
                                    businesses: List[BusinessDetailsModel],
                                    properties: List[PropertyDetailsModel]
                                   ) extends IncomeSourceDetailsResponse with Logging {

  val hasPropertyIncome: Boolean = properties.nonEmpty
  val hasBusinessIncome: Boolean = businesses.nonEmpty
  val hasOngoingBusinessOrPropertyIncome: Boolean = businesses.exists(b => b.cessation.forall(_.date.isEmpty)) ||
    properties.exists(p => p.cessation.forall(_.date.isEmpty))
  def isOngoingBusinessOrPropertyIncome(id: String): Boolean = businesses.exists(b => b.incomeSourceId.contains(id) && b.cessation.forall(_.date.isEmpty)) ||
    properties.exists(p => p.incomeSourceId.contains(id) && p.cessation.forall(_.date.isEmpty))

  override def toJson: JsValue = Json.toJson(this)

  def sanitise: IncomeSourceDetailsModel = {
    val property2 = properties.map(p => p.copy(incomeSourceId = None, accountingPeriod = None))
    val businesses2 = businesses.map(b => b.copy(incomeSourceId = None, accountingPeriod = None))
    this.copy(properties = property2, businesses = businesses2)
  }

  def orderedTaxYearsByAccountingPeriods(implicit dateService: DateServiceInterface): List[Int] = {
    (startingTaxYear to dateService.getCurrentTaxYearEnd()).toList
  }

  def startingTaxYear: Int = (businesses.flatMap(_.firstAccountingPeriodEndDate) ++ properties.flatMap(_.firstAccountingPeriodEndDate))
    .map(_.getYear).sortWith(_ < _).headOption.getOrElse(throw new RuntimeException("User missing first accounting period information"))

  def orderedTaxYearsByYearOfMigration(implicit dateService: DateServiceInterface): List[Int] = {
    yearOfMigration.map(year => (year.toInt to dateService.getCurrentTaxYearEnd()).toList).getOrElse(List.empty[Int])
  }
}

case class IncomeSourceDetailsError(status: Int, reason: String) extends IncomeSourceDetailsResponse {
  override def toJson: JsValue = Json.toJson(this)
}

object IncomeSourceDetailsModel {
  implicit val format: Format[IncomeSourceDetailsModel] = Json.format[IncomeSourceDetailsModel]
}

object IncomeSourceDetailsError {
  implicit val format: Format[IncomeSourceDetailsError] = Json.format[IncomeSourceDetailsError]
}
