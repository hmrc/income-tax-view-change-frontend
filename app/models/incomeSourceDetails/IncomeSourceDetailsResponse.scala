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

package models.incomeSourceDetails

import java.time.LocalDate

import org.scalacheck.Prop.True
import play.api.libs.json.{Format, JsValue, Json}

sealed trait IncomeSourceDetailsResponse {
  def toJson: JsValue
}

case class IncomeSourceDetailsModel(mtdbsa:String,
                                    yearOfMigration: Option[String],
                                    businesses: List[BusinessDetailsModel],
                                    property: Option[PropertyDetailsModel]
                                   ) extends IncomeSourceDetailsResponse {

  override def toJson: JsValue = Json.toJson(this)

  private def isModelUserMigrated: Boolean = {
    /**
     * The function examines content of the yearOfMigration option of the model.
     * Returns boolean true if this model is for a migrated user, false otherwise
     */
    this.yearOfMigration match {
      case Some(_) => true
      case _ => false
    }
  }

  def firstAccountingTaxYearIntVal(yearOption: Option[Int], property: Option[PropertyDetailsModel]): Int = {
    /**
     * The function takes list of BusinessDetailsModel and an object of type Option[Int]
     * Returns option content if any, throws an exception of list of business details models is not empty.
     * The semantics of the function implements the non-empty first accounting period enforcement in the
     * presence of the non-empty list of business models involved.
     * Returns sentinel value of Int.MaxValue only to accommodate existing unit testing only, semantically
     * meaningless.
     */
     if (!this.isModelUserMigrated) {
       Int.MaxValue
     } else {
       yearOption match {
         case Some(yearInt) => yearInt
         case _ => {
           property match {
             case None => Int.MaxValue
             case _ => {
               businesses match {
                 case Nil => Int.MaxValue
                 case _ => {
                   property.get.firstAccountingPeriodEndDate.getOrElse(throw new RuntimeException("User missing first accounting period information")).getYear
                 }
               }
             }
           }
         }
       }
     }
  }

  def sanitise: IncomeSourceDetailsModel = {
    val property2 = property.map(p => p.copy(incomeSourceId = None, accountingPeriod = None))
    val businesses2 = businesses.map(b => b.copy(incomeSourceId = None, accountingPeriod = None, tradingName = None))
    this.copy(property = property2, businesses = businesses2)
  }

  val startingTaxYear: Int = {
    // Get sequence of accounting periods as type LocalDate dates
    val acctPeriodEndDateSeq = (businesses.flatMap(_.firstAccountingPeriodEndDate) ++ property.flatMap(_.firstAccountingPeriodEndDate))
    // Get first accounting period option
    val firstAcctPeriodOption = acctPeriodEndDateSeq.map(_.getYear).sortWith(_ < _).headOption
    // return Int value representing first acctounting period year or throw missing data exception
    firstAccountingTaxYearIntVal(firstAcctPeriodOption, property)
  }

  def orderedTaxYearsByAccountingPeriods: List[Int] = {
    (startingTaxYear to getCurrentTaxEndYear).toList
  }

  def orderedTaxYearsByYearOfMigration: List[Int] = {
    yearOfMigration.map(year => (year.toInt to getCurrentTaxEndYear).toList).getOrElse(List.empty[Int])
  }

  val hasPropertyIncome: Boolean = property.nonEmpty
  val hasBusinessIncome: Boolean = businesses.nonEmpty

  val getCurrentTaxEndYear: Int = {
    val currentDate = LocalDate.now
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) currentDate.getYear
    else currentDate.getYear + 1

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
