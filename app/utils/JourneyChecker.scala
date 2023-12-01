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
import enums.JourneyType.{Add, Cease, JourneyType, Manage}
import models.incomeSourceDetails.UIJourneySessionData
import play.api.Logger
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.SessionService
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait JourneyChecker extends IncomeSourcesUtils {
  self =>
  val sessionService: SessionService

  implicit val ec: ExecutionContext

  def withSessionData(journeyType: JourneyType)(codeBlock: UIJourneySessionData => Future[Result])
                     (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    withIncomeSourcesFS {
      sessionService.getMongo(journeyType.toString).flatMap {
        case Right(Some(data: UIJourneySessionData)) =>
          if (isJourneyComplete(data, journeyType)) {
            redirectUrl(journeyType)
          } else {
            codeBlock(data)
          }
        case Right(None) =>
          Logger("application").warn(s"No data found for journey type ${journeyType.toString}")
          redirectUrl(journeyType)
        case Left(ex) =>
          Logger("application").error(s"Error accessing Mongo for journey type ${journeyType.toString}. Exception: ${ex.getMessage}", ex)
          Future.failed(ex)
      }
    }
  }

  private def isJourneyComplete(data: UIJourneySessionData, journeyType: JourneyType): Boolean = {
    journeyType.operation match {
      case Add => data.addIncomeSourceData.flatMap(_.journeyIsComplete).getOrElse(false)
      case Manage => data.manageIncomeSourceData.flatMap(_.journeyIsComplete).getOrElse(false)
      case Cease => data.manageIncomeSourceData.flatMap(_.journeyIsComplete).getOrElse(false)
      case _ => false
    }
  }

  private def redirectUrl(journeyType: JourneyType)(implicit user: MtdItUser[_]): Future[Result] =
    user.userType match {
      case Some(Agent) =>
        Future.successful(Redirect(controllers.incomeSources.add.routes.YouCannotGoBackErrorController.showAgent(journeyType.businessType)))
      case _ =>
        Future.successful(Redirect(controllers.incomeSources.add.routes.YouCannotGoBackErrorController.show(journeyType.businessType)))
    }
}
