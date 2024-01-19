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

package audit.models

import audit.Utilities.userAuditDetails
import auth.MtdItUserWithNino
import implicits.ImplicitDateParser
import models.liabilitycalculation.{EndOfYearEstimate, IncomeSource}
import play.api.libs.json.{JsNumber, JsObject, JsValue, Json}
import utils.Utilities._


case class ForecastIncomeAuditModel(user: MtdItUserWithNino[_], endOfYearEstimate: EndOfYearEstimate)
  extends ExtendedAuditModel with ImplicitDateParser {

  override val transactionName: String = enums.TransactionName.ForecastIncome
  override val auditType: String = enums.AuditType.ForecastIncome

  private val totalEstimatedIncome: Option[Int] = endOfYearEstimate.totalEstimatedIncome
  private val incomeSource: Option[List[IncomeSource]] = endOfYearEstimate.incomeSource

  private var payFromIncomeType: List[JsObject] = List()

  private val incomeTypeValues = Map(
    "02" -> "profitFromUKLandandProperty",
    "03" -> "profitFromEEAholidayPropertyLettings",
    "04" -> "profitFromUKFurnishedHolidayLettings",
    "06" -> "foreignIncome",
    "07" -> "dividendsFromForeignCompanies",
    "08" -> "profitsFromTrustsAndEstates",
    "09" -> "interestFromBanks",
    "10" -> "dividendsFromUKCompanies",
    "11" -> "stateBenefitIncome",
    "12" -> "gainsOnLifeInsurance",
    "13" -> "shareSchemes",
    "14" -> "profitFromPartnership",
    "15" -> "profitFromForeignLand",
    "16" -> "foreignInterest",
    "17" -> "otherDividends",
    "18" -> "UKSecurities",
    "19" -> "otherIncome",
    "20" -> "foreignPension",
    "21" -> "nonPayeIncome",
    "22" -> "capitalGains",
    "98" -> "giftAidAndPayrollGiving"
  )

  incomeSource.map(incomeSources => {
    incomeSources.map(incomeSource => {
      val amount = incomeSource.taxableIncome
      incomeSource.incomeSourceType match {
        case "05" =>
          val incomeType = incomeSource.incomeSourceName.getOrElse("employment")
          payFromIncomeType = payFromIncomeType.appended(Json.obj("name" -> incomeType , "amount" -> amount))
        case _ =>
      }
    })
  })

  private def getProfitFromIncome: List[JsObject] = {
    incomeSource match {
      case Some(incomeSources) =>
        incomeSources.foldLeft( List(Json.obj()) ) { ( acc, current) =>
          current.incomeSourceType match {
            case "01"  =>
              val amount = current.taxableIncome
              val incomeType = current.incomeSourceName.getOrElse("self-employment")
              acc :+ Json.obj("name" -> incomeType , "amount" -> amount)
            case _ =>
              acc
          }
        }
      case None =>
        List.empty
    }
  }


  private def getAllOtherIncomeType: JsObject = {
    incomeSource match {
      case Some(incomeSources) =>
        incomeSources.foldLeft( Json.obj() ) { ( acc, current) =>
          current.incomeSourceType match {
            case "01" | "05" =>
              acc
            case _ =>
              val amount = current.taxableIncome
              acc + (incomeTypeValues.getOrElse(current.incomeSourceType , "") -> JsNumber(amount))
          }
        }
      case None => Json.obj()
    }
  }

  override val detail: JsValue = {
    userAuditDetails(user) ++
      Json.obj() ++
      ("totalForecastIncome", totalEstimatedIncome) ++
      Json.obj("profitFrom" -> getProfitFromIncome) ++
      Json.obj("payFrom" -> payFromIncomeType) ++
      getAllOtherIncomeType
  }

}
