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
import enums.JourneyType.JourneyType
import models.incomeSourceDetails.AddIncomeSourceData
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.SessionService
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, jsonFormat}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait JourneyChecker extends IncomeSourcesUtils {
  self =>
  implicit val ec: ExecutionContext

  val sessionService: SessionService

  def withIncomeSourcesFSWithSessionCheck(journeyType: JourneyType, checkAdded: Boolean = true)(codeBlock: => Future[Result])(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    withIncomeSourcesFS {
      journeyChecker(journeyType, checkAdded, codeBlock)
    }
  }

  private def journeyChecker(journeyType: JourneyType, checkAdded: Boolean, codeBlock: => Future[Result])(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    sessionService.getMongoKeyTyped[Boolean](AddIncomeSourceData.incomeSourceAddedField, journeyType).flatMap {
      added: Either[Throwable, Option[Boolean]] =>
        sessionService.getMongoKeyTyped[Boolean](AddIncomeSourceData.reportingMethodSetField, journeyType).flatMap {
          reportingSet: Either[Throwable, Option[Boolean]] =>
            val addedValue = if (checkAdded) added else Right(Some(false))
            (addedValue, reportingSet) match {
              case (_, Right(Some(true))) => reportingMethodSetBack(journeyType)
              case (Right(Some(true)), _) => incomeSourceAddedSetBack(journeyType)
              case _ => codeBlock
            }
        }
    }
  }

  private def reportingMethodSetBack(journeyType: JourneyType)(implicit user: MtdItUser[_]) = {
    user.userType match {
      case Some(Agent) => Future.successful(Redirect(controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.showAgent(journeyType.businessType)))
      case _ => Future.successful(Redirect(controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.show(journeyType.businessType)))
    }
  }

  private def incomeSourceAddedSetBack(journeyType: JourneyType)(implicit user: MtdItUser[_]) = {
    user.userType match {
      case Some(Agent) => Future.successful(Redirect(controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.showAgent(journeyType.businessType)))
      case _ => Future.successful(Redirect(controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.show(journeyType.businessType)))
    }
  }

}
