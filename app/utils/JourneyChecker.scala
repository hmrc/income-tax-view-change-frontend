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
import enums.IncomeSourceJourney.IncomeSourceType
import enums.JourneyType.{Add, JourneyType}
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.SessionService
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait JourneyChecker extends IncomeSourcesUtils {
  self =>
  implicit val ec: ExecutionContext

  val sessionService: SessionService

  def withCustomSession(journeyType: JourneyType)(codeBlock: Option[UIJourneySessionData] => Future[Result])
                       (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    withIncomeSourcesFS {
      sessionService.getMongo(journeyType.toString).flatMap {
        case Right(Some(sessionData)) =>
          (customSessionStarted(sessionData), user.userType) match {
            case (true, Some(Agent)) =>
              Future.successful(Redirect(controllers.incomeSources.add.routes.YouCannotGoBackErrorController.showAgent(journeyType.businessType)))
            case (true, Some(Individual)) =>
              Future.successful(Redirect(controllers.incomeSources.add.routes.YouCannotGoBackErrorController.show(journeyType.businessType)))
            case (_, _) => codeBlock(Some(sessionData))
          }
        case _ =>
          codeBlock(None)
      }
    }
  }

  private def customSessionStarted(sessionData: UIJourneySessionData): Boolean = {
    sessionData.addIncomeSourceData match {
      case Some(value) if value.hasBeenAdded.contains(true) => true
      case _ => false
    }
  }

  // TODO: extend this method to support: Add / Manage and Ceased journey
  def startCustomSession(incomeSourceType: IncomeSourceType)
                        (implicit hc: HeaderCarrier): Future[Boolean] = {
    sessionService.getMongo(JourneyType(Add, incomeSourceType).toString).flatMap {
      case Right(Some(sessionData)) =>
        val oldAddIncomeSourceSessionData = sessionData.addIncomeSourceData.getOrElse(AddIncomeSourceData())
        val updatedAddIncomeSourceSessionData = oldAddIncomeSourceSessionData.copy(hasBeenAdded = Some(true))
        val uiJourneySessionData: UIJourneySessionData = sessionData.copy(addIncomeSourceData = Some(updatedAddIncomeSourceSessionData))

        sessionService.setMongoData(uiJourneySessionData)

      case _ =>
        Future.failed(new Exception(s"failed to retrieve session data"))
    }
  }

}
