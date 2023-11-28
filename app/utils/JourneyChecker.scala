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
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait JourneyChecker extends IncomeSourcesUtils {
  self =>
  implicit val ec: ExecutionContext

  val sessionService: SessionService
  def withIncomeSourcesFSWithSessionCheck(journeyType: JourneyType)(codeBlock: => Future[Result])(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    withIncomeSourcesFS {
      journeyChecker(journeyType).flatMap {
        case true => user.userType match {
          case Some(Agent) => Future.successful(Redirect(controllers.incomeSources.add.routes.YouCannotGoBackErrorController.showAgent(journeyType.businessType)))
          case _ => Future.successful(Redirect(controllers.incomeSources.add.routes.YouCannotGoBackErrorController.show(journeyType.businessType)))
        }
        case false => codeBlock
      }
    }
  }

  private def journeyChecker(journeyType: JourneyType)(implicit hc: HeaderCarrier): Future[Boolean] = {
    sessionService.getMongoKeyTyped[Boolean](AddIncomeSourceData.hasBeenAddedField, journeyType).flatMap {
      case Right(Some(true)) => Future(true)
      case _ => Future(false)
    }
  }

}
