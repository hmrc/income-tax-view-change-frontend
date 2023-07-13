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

package utils

import auth.MtdItUser
import forms.utils.SessionKeys._
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckUKPropertyViewModel}

import java.time.LocalDate

case class MissingKey(msg: String)

object IncomeSourcesUtils {

  def getBusinessDetailsFromSession(implicit user: MtdItUser[_]): Either[Throwable, CheckBusinessDetailsViewModel] = {

    val errors: Seq[String] = Seq(
      user.session.data.get(businessName).orElse(Option(MissingKey("MissingKey: addBusinessName"))),
      user.session.data.get(businessStartDate).orElse(Option(MissingKey("MissingKey: addBusinessStartDate"))),
      user.session.data.get(businessTrade).orElse(Option(MissingKey("MissingKey: addBusinessTrade"))),
      user.session.data.get(addBusinessAddressLine1).orElse(Option(MissingKey("MissingKey: addBusinessAddressLine1"))),
      user.session.data.get(addBusinessPostalCode).orElse(Option(MissingKey("MissingKey: addBusinessPostalCode")))
    ).collect {
      case Some(MissingKey(msg)) => MissingKey(msg)
    }.map(e => e.msg)


    val result: Option[CheckBusinessDetailsViewModel] = for {
      businessName <- user.session.data.get(businessName)
      businessStartDate <- user.session.data.get(businessStartDate).map(LocalDate.parse)
      businessTrade <- user.session.data.get(businessTrade)
      businessAddressLine1 <- user.session.data.get(addBusinessAddressLine1)
      businessAccountingMethod <- user.session.data.get(addBusinessAccountingMethod)
      accountingPeriodEndDate <- user.session.data.get(addBusinessAccountingPeriodEndDate).map(LocalDate.parse)
    } yield {

      CheckBusinessDetailsViewModel(
        businessName = Some(businessName),
        businessStartDate = Some(businessStartDate),
        accountingPeriodEndDate = accountingPeriodEndDate,
        businessTrade = businessTrade,
        businessAddressLine1 = businessAddressLine1,
        businessAddressLine2 = user.session.data.get(addBusinessAddressLine2),
        businessAddressLine3 = user.session.data.get(addBusinessAddressLine3),
        businessAddressLine4 = user.session.data.get(addBusinessAddressLine4),
        businessPostalCode = user.session.data.get(addBusinessPostalCode),
        businessCountryCode = user.session.data.get(addBusinessCountryCode),
        businessAccountingMethod = user.session.data.get(addBusinessAccountingMethod),
        cashOrAccrualsFlag = businessAccountingMethod)
    }

    result match {
      case Some(checkBusinessDetailsViewModel) =>
        Right(checkBusinessDetailsViewModel)
      case None =>
        Left(new IllegalArgumentException(s"Missing required session data: ${errors.mkString(" ")}"))
    }
  }

  def getUKPropertyDetailsFromSession(implicit user: MtdItUser[_]): Either[Throwable, CheckUKPropertyViewModel] = {
    val errors: Seq[String] = Seq(
      user.session.data.get(addUkPropertyStartDate).orElse(Option(MissingKey(s"MissingKey: $addUkPropertyStartDate"))),
      user.session.data.get(businessTrade).orElse(Option(MissingKey(s"MissingKey: $businessTrade"))),
    ).collect {
      case Some(MissingKey(msg)) => MissingKey(msg)
    }.map(e => e.msg)

    val result: Option[CheckUKPropertyViewModel] = for {
      tradingStartDate <- user.session.data.get(addUkPropertyStartDate)
      cashOrAccrualsFlag <- user.session.data.get(addUkPropertyAccountingMethod)
    } yield {
      CheckUKPropertyViewModel(
        tradingStartDate = LocalDate.parse(tradingStartDate),
        cashOrAccrualsFlag = cashOrAccrualsFlag
      )
    }

    result match {
      case Some(propertyDetails) =>
        Right(propertyDetails)
      case None =>
        Left(new IllegalArgumentException(s"Missing required session data: ${errors.mkString(" ")}"))
    }
  }

  def removeIncomeSourceDetailsFromSession(implicit user: MtdItUser[_]): Unit = {
    val sessionKeysToRemove = Seq(
      "addUkPropertyStartDate",
      "addBusinessName",
      "addBusinessTrade",
      "addBusinessAccountingMethod",
      "addBusinessStartDate",
      "addBusinessAccountingPeriodStartDate",
      "addBusinessAccountingPeriodEndDate",
      "addBusinessStartDate",
      "addBusinessAddressLine1",
      "addBusinessAddressLine2",
      "addBusinessAddressLine3",
      "addBusinessAddressLine4",
      "addBusinessPostalCode",
      "addBusinessCountryCode",
      "ceaseForeignPropertyDeclare",
      "ceaseForeignPropertyEndDate",
      "ceaseUKPropertyDeclare",
      "ceaseUKPropertyEndDate"
    ) //TODO: check this is all the keys

    user.session -- sessionKeysToRemove
  }
}
