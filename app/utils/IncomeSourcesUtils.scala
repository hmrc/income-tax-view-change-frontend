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
import config.featureswitch.{FeatureSwitching, IncomeSources}
import forms.utils.SessionKeys
import forms.utils.SessionKeys._
import models.incomeSourceDetails.viewmodels.CheckUKPropertyViewModel
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.SessionService
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

trait IncomeSourcesUtils extends FeatureSwitching {
  def withIncomeSourcesFS(codeBlock: => Future[Result])(implicit user: MtdItUser[_]): Future[Result] = {
    if (isDisabled(IncomeSources)) {
      user.userType match {
        case Some(Agent) => Future.successful(Redirect(controllers.routes.HomeController.showAgent))
        case _ => Future.successful(Redirect(controllers.routes.HomeController.show()))
      }
    } else {
      codeBlock
    }
  }

  def newWithIncomeSourcesRemovedFromSession(redirect: Result, sessionService: SessionService, errorRedirect: Result)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    val incomeSourcesSessionKeys = Seq(
      SessionKeys.addUkPropertyStartDate,
      SessionKeys.foreignPropertyStartDate,
      SessionKeys.businessName,
      SessionKeys.businessTrade,
      SessionKeys.addIncomeSourcesAccountingMethod,
      SessionKeys.addBusinessStartDate,
      SessionKeys.addBusinessAccountingPeriodStartDate,
      SessionKeys.addBusinessAccountingPeriodEndDate,
      SessionKeys.addBusinessStartDate,
      SessionKeys.addBusinessAddressLine1,
      SessionKeys.addBusinessAddressLine2,
      SessionKeys.addBusinessAddressLine3,
      SessionKeys.addBusinessAddressLine4,
      SessionKeys.addBusinessPostalCode,
      SessionKeys.addBusinessCountryCode,
      SessionKeys.ceaseForeignPropertyDeclare,
      SessionKeys.ceaseForeignPropertyEndDate,
      SessionKeys.ceaseUKPropertyDeclare,
      SessionKeys.ceaseUKPropertyEndDate
    ) //TODO: check this is all the keys

    sessionService.remove(incomeSourcesSessionKeys, redirect).map {
      case Left(_) => errorRedirect
      case Right(result) => result
    }
  }

  def withIncomeSourcesRemovedFromSession(redirect: Result)(implicit user: MtdItUser[_]): Result = {
    val incomeSourcesSessionKeys = Seq(
      "addUkPropertyStartDate",
      "addBusinessName",
      "addBusinessTrade",
      "addIncomeSourcesAccountingMethod",
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
    val newSession = user.session -- incomeSourcesSessionKeys

    redirect.withSession(newSession)
  }
}

object IncomeSourcesUtils {

  case class MissingKey(msg: String)

  def getUKPropertyDetailsFromSession(implicit user: MtdItUser[_]): Either[Throwable, CheckUKPropertyViewModel] = {
    val result: Option[Either[Throwable, CheckUKPropertyViewModel]] = for {
      tradingStartDate <- user.session.data.get(addUkPropertyStartDate)
      cashOrAccrualsFlag <- user.session.data.get(addIncomeSourcesAccountingMethod)
    } yield {
      Right(CheckUKPropertyViewModel(
        tradingStartDate = LocalDate.parse(tradingStartDate),
        cashOrAccrualsFlag = cashOrAccrualsFlag
      ))
    }

    result match {
      case Some(propertyDetails) =>
        propertyDetails
      case None =>
        val errors: Seq[String] = Seq(
          user.session.data.get(addUkPropertyStartDate).orElse(Some(MissingKey(s"MissingKey: $addUkPropertyStartDate"))),
          user.session.data.get(businessTrade).orElse(Some(MissingKey(s"MissingKey: $businessTrade")))
        ).collect {
          case Some(MissingKey(msg)) => msg
        }
        Left(new IllegalArgumentException(s"Missing required session data: ${errors.mkString(" ")}"))
    }
  }
}
