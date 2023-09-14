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
import config.ShowInternalServerError
import config.featureswitch.{FeatureSwitching, IncomeSources}
import forms.utils.SessionKeys._
import models.incomeSourceDetails.BusinessDetailsModel
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckUKPropertyViewModel}
import services.SessionService
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
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
      "addUkPropertyStartDate",
      "addForeignPropertyStartDate",
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

    sessionService.remove(incomeSourcesSessionKeys, redirect).map {
      case Left(_) => errorRedirect
      case Right(result) => result
    }
  }

  def withIncomeSourcesRemovedFromSession(redirect: Result)(implicit user: MtdItUser[_]): Result = {
    val incomeSourcesSessionKeys = Seq(
      "addUkPropertyStartDate",
      "addForeignPropertyStartDate",
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

  def getBusinessDetailsFromSession(implicit user: MtdItUser[_]): Either[Throwable, CheckBusinessDetailsViewModel] = {
    val userActiveBusinesses: List[BusinessDetailsModel] = user.incomeSources.businesses.filterNot(_.isCeased)
    val skipAccountingMethod: Boolean = userActiveBusinesses.isEmpty

    val result: Option[Either[Throwable, CheckBusinessDetailsViewModel]] = for {
      businessName <- user.session.data.get(businessName) //sessionService.get(businessName)
      businessStartDate <- user.session.data.get(businessStartDate).map(LocalDate.parse)
      businessTrade <- user.session.data.get(businessTrade)
      businessAddressLine1 <- user.session.data.get(addBusinessAddressLine1)
      businessAccountingMethod <- user.session.data.get(addIncomeSourcesAccountingMethod)
      accountingPeriodEndDate <- user.session.data.get(addBusinessAccountingPeriodEndDate).map(LocalDate.parse)
    } yield {
      Right(CheckBusinessDetailsViewModel(
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
        incomeSourcesAccountingMethod = user.session.data.get(addIncomeSourcesAccountingMethod),
        cashOrAccrualsFlag = businessAccountingMethod,
        skippedAccountingMethod = skipAccountingMethod
      ))
    }

    result match {
      case Some(checkBusinessDetailsViewModel) =>
        checkBusinessDetailsViewModel
      case None =>
        val errors: Seq[String] = Seq(
          user.session.data.get(businessName).orElse(Some(MissingKey("MissingKey: addBusinessName"))),
          user.session.data.get(businessStartDate).orElse(Some(MissingKey("MissingKey: addBusinessStartDate"))),
          user.session.data.get(businessTrade).orElse(Some(MissingKey("MissingKey: addBusinessTrade"))),
          user.session.data.get(addBusinessAddressLine1).orElse(Some(MissingKey("MissingKey: addBusinessAddressLine1"))),
          user.session.data.get(addBusinessPostalCode).orElse(Some(MissingKey("MissingKey: addBusinessPostalCode")))
        ).collect {
          case Some(MissingKey(msg)) => msg
        }
        Left(new IllegalArgumentException(s"Missing required session data: ${errors.mkString(" ")}"))
    }
  }

  def getUKPropertyDetailsFromSession(sessionService: SessionService)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Either[Throwable, CheckUKPropertyViewModel]] = {
    for {
      startDate <- sessionService.get(addUkPropertyStartDate)
      accMethod <- sessionService.get(addIncomeSourcesAccountingMethod)
    } yield (startDate, accMethod) match {
      case (Right(dateMaybe), Right(methodMaybe)) =>
        val maybeModel = for {
          foreignPropertyStartDate <- dateMaybe.map(LocalDate.parse)
          cashOrAccrualsFlag <- methodMaybe
        } yield {
          CheckUKPropertyViewModel(
            tradingStartDate = foreignPropertyStartDate,
            cashOrAccrualsFlag = cashOrAccrualsFlag)
        }
        maybeModel.map(Right(_))
          .getOrElse(Left(new Error("Unable to construct model")))
      case (_, _) =>
        Left(new Error("Some error"))
    }

    /*sessionService.get(addUkPropertyStartDate).flatMap { startDate: Either[Throwable, Option[String]] =>
      sessionService.get(addIncomeSourcesAccountingMethod).map { accMethod: Either[Throwable, Option[String]] =>
        val result = startDate match {
          case Left(_) => None
          case Right(dateMaybe) =>
            accMethod match {
              case Left(_) => None
              case Right(methodMaybe) =>
                for {
                  foreignPropertyStartDate <- dateMaybe.map(LocalDate.parse)
                  cashOrAccrualsFlag <- methodMaybe
                } yield {
                  CheckUKPropertyViewModel(
                    tradingStartDate = foreignPropertyStartDate,
                    cashOrAccrualsFlag = cashOrAccrualsFlag)
                }
            }
        }
        result match {
          case Some(propertyDetails) =>
            Right(propertyDetails)
          case None =>
            val errors: Seq[String] = getErrors(startDate, accMethod)
            Left(new IllegalArgumentException(s"Missing required session data: ${errors.mkString(" ")}"))
        }
      }
    }*/
  }

  def getErrors(startDate: Either[Throwable, Option[String]], accMethod: Either[Throwable, Option[String]]): Seq[String] = {

    def checkError(field: Either[Throwable, Option[String]]): String = {
      field match {
        case Right(nameOpt) => nameOpt match {
          case Some(name) => name
          case None => "MissingKey: addUKPropertyStartDate"
        }
        case Left(_) => "MissingKey: addUKPropertyStartDate"
      }
    }

    Seq(checkError(startDate), checkError(accMethod))
  }
}
