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
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, Cease, JourneyType, Manage}
import models.core.{IncomeSourceId, IncomeSourceIdHash}
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.Logger
import enums.JourneyType.{Add, Cease, JourneyType, Manage}
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
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

  private lazy val isAgent: MtdItUser[_] => Boolean = (user: MtdItUser[_]) => user.userType match {
    case Some(Agent) =>
      true
    case _ =>
      false
  }

  private lazy val redirectUrl: JourneyType => MtdItUser[_] => Future[Result] = (journeyType: JourneyType) => user => {
    (journeyType.operation, isAgent(user)) match {
      case (Add, true) =>
        Future.successful {
          Redirect(controllers.incomeSources.add.routes.YouCannotGoBackErrorController.showAgent(journeyType.businessType))
        }
      case (Add, false) =>
        Future.successful {
          Redirect(controllers.incomeSources.add.routes.YouCannotGoBackErrorController.show(journeyType.businessType))
        }
      case (Manage, _) =>
        Future.successful {
          Redirect(controllers.incomeSources.manage.routes.CannotGoBackErrorController.show(isAgent(user), journeyType.businessType))
        }
      case (Cease, true) =>
        Future.successful {
          Redirect(controllers.incomeSources.cease.routes.IncomeSourceCeasedBackErrorController.showAgent(journeyType.businessType))
        }
      case (Cease, false) =>
        Future.successful {
          Redirect(controllers.incomeSources.cease.routes.IncomeSourceCeasedBackErrorController.show(journeyType.businessType))
        }
    }
  }

  /** *****************************TODO: remove in MISUV-6724 or MISUV-6734********************************************* */
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
    sessionService.getMongoKeyTyped[Boolean](AddIncomeSourceData.journeyIsCompleteField, journeyType).flatMap {
      case Right(Some(true)) => Future(true)
      case _ => Future(false)
    }
  }

  /** ***************************************************************************************************************** */

  def withSessionData(journeyType: JourneyType)(codeBlock: UIJourneySessionData => Future[Result])
                     (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    withIncomeSourcesFS {
      sessionService.getMongo(journeyType.toString).flatMap {
        case Right(Some(data: UIJourneySessionData)) if isJourneyComplete(data, journeyType) =>
          redirectUrl(journeyType)(user)
        case Right(Some(data: UIJourneySessionData)) =>
          codeBlock(data)
        case Right(None) =>
          sessionService.createSession(journeyType.toString).flatMap { _ =>
            val data = UIJourneySessionData(hc.sessionId.get.value, journeyType.toString)
            codeBlock(data)
          }
        case Left(ex) =>
          val agentPrefix = if (isAgent(user)) "[Agent]" else ""
          Logger("application").error(s"$agentPrefix" +
            s"[JourneyChecker][withSessionData]: Unable to retrieve Mongo data for journey type ${journeyType.toString}", ex)
          Future.failed(ex)
      }
    }
  }

  private def isJourneyComplete(data: UIJourneySessionData, journeyType: JourneyType): Boolean = {
    journeyType.operation match {
      case Add =>
        data.addIncomeSourceData.flatMap(_.journeyIsComplete).getOrElse(false)
      case Manage =>
        data.manageIncomeSourceData.flatMap(_.journeyIsComplete).getOrElse(false)
      case Cease =>
        data.ceaseIncomeSourceData.flatMap(_.journeyIsComplete).getOrElse(false)
      case _ => false
    }
  }
}
