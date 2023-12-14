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
import enums.JourneyType._
import models.core.{IncomeSourceId, IncomeSourceIdHash}
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.Logger
import play.api.mvc.Result
import play.api.mvc.Results.{Redirect, contentDispositionHeader}
import services.SessionService
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, jsonFormat}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait JourneyChecker extends IncomeSourcesUtils {
  self =>
  implicit val ec: ExecutionContext

  val sessionService: SessionService

  private lazy val isAgent: MtdItUser[_] => Boolean = (user: MtdItUser[_]) => user.userType.contains(Agent)

  private lazy val redirectUrl: (JourneyType, Boolean) => MtdItUser[_] => Future[Result] =
    (journeyType: JourneyType, useDefault: Boolean) => user => {
      (journeyType.operation, isAgent(user), useDefault) match {
        case (Add, true, true) =>
          Future.successful {
            Redirect(controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.showAgent(journeyType.businessType))
          }
        case (Add, true, false) =>
          Future.successful {
            Redirect(controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.showAgent(journeyType.businessType))
          }
        case (Add, false, true) =>
          Future.successful {
            Redirect(controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.show(journeyType.businessType))
          }
        case (Add, false, false) =>
          Future.successful {
             Redirect(controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.show(journeyType.businessType))
          }
        case (Manage, _, _) =>
          Future.successful {
            Redirect(controllers.incomeSources.manage.routes.CannotGoBackErrorController.show(isAgent(user), journeyType.businessType))
          }
        case (Cease, _, _) =>
          Future.successful {
            Redirect(controllers.routes.HomeController.show()) //TODO: fix
          }
      }
    }

  private def useDefaultRedirect(data: UIJourneySessionData, journeyType: JourneyType, midwayFlag: Boolean): Boolean = {
    journeyType.operation match {
      case Add => !(midwayFlag && data.addIncomeSourceData.flatMap(_.incomeSourceAdded).getOrElse(false))
      case _ => true
    }
  }

  def withSessionData(journeyType: JourneyType, midwayFlag: Boolean = true)(codeBlock: UIJourneySessionData => Future[Result])
                     (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    withIncomeSourcesFS {
      sessionService.getMongo(journeyType.toString).flatMap {
        case Right(Some(data: UIJourneySessionData)) if isJourneyComplete(data, journeyType, midwayFlag) =>
          redirectUrl(journeyType, useDefaultRedirect(data, journeyType, midwayFlag))(user)
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

  private def isJourneyComplete(data: UIJourneySessionData, journeyType: JourneyType, midwayFlag: Boolean): Boolean = {
    (journeyType.operation, midwayFlag) match {
      case (Add, true) =>
        data.addIncomeSourceData.flatMap(_.journeyIsComplete).getOrElse(false) ||
        data.addIncomeSourceData.flatMap(_.incomeSourceAdded).getOrElse(false)
      case (Add, false) =>
        data.addIncomeSourceData.flatMap(_.journeyIsComplete).getOrElse(false)
      case (Manage,_ ) =>
        data.manageIncomeSourceData.flatMap(_.journeyIsComplete).getOrElse(false)
      case (Cease,_) =>
        data.manageIncomeSourceData.flatMap(_.journeyIsComplete).getOrElse(false)
      case _ => false
    }
  }
}
