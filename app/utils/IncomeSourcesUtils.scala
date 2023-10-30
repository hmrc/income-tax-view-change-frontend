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
}

object IncomeSourcesUtils {

  case class MissingKey(msg: String)

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
          .getOrElse(Left(new Error("Unable to construct UK property view model")))
      case (_, _) =>
        Left(new Error("Error occurred when retrieving start dat and accounting method from session storage"))
    }
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
