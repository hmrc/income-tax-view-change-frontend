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
import auth.MtdItUser
import implicits.ImplicitDateParser
import models.liabilitycalculation.{EndOfYearEstimate, IncomeSource}
import play.api.libs.json.{JsNumber, JsObject, JsValue, Json}
import utils.Utilities.*

import scala.language.implicitConversions


case class ForecastIncomeAuditModel(user: MtdItUser[_], endOfYearEstimate: EndOfYearEstimate)
  extends ExtendedAuditModel with ImplicitDateParser {

  override val transactionName: String = enums.TransactionName.ForecastIncome
  override val auditType: String = enums.AuditType.AuditType.ForecastIncome

  private val totalEstimatedIncome: Option[Int] = endOfYearEstimate.totalEstimatedIncome
  private val incomeSource: Option[List[IncomeSource]] = endOfYearEstimate.incomeSource

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

  private def getPayFromIncomeType: List[JsObject] = {
    incomeSource match {
      case Some(incomeSources) =>
        incomeSources.foldLeft[List[JsObject]]( List.empty ) { ( acc, current) =>
          current.incomeSourceType match {
            case "05"  =>
              val incomeType : String = current.incomeSourceName.getOrElse("employment")
              acc :+ Json.obj("name" -> incomeType , "amount" -> current.taxableIncome)
            case _ =>
              acc
          }
        }
      case None =>
        List.empty
    }
  }

  private def getProfitFromIncome: List[JsObject] = {
    incomeSource match {
      case Some(incomeSources) =>
        incomeSources.foldLeft[List[JsObject]]( List.empty ) { ( acc, current) =>
          current.incomeSourceType match {
            case "01"  =>
              val incomeType: String = current.incomeSourceName.getOrElse("self-employment")
              acc :+ Json.obj("name" -> incomeType , "amount" -> current.taxableIncome)
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
      Json.obj("totalForecastIncome"-> totalEstimatedIncome) ++
      Json.obj("profitFrom" -> getProfitFromIncome) ++
      Json.obj("payFrom" -> getPayFromIncomeType) ++
      getAllOtherIncomeType
  }

}
