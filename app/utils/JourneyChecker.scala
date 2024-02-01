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
import enums.IncomeSourceJourney.{BeforeSubmissionPage, CannotGoBackPage, InitialPage, JourneyState}
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

  private lazy val redirectUrl: (JourneyType, Boolean) => MtdItUser[_] => Future[Result] =
    (journeyType: JourneyType, useDefault: Boolean) => user => {
      Future.successful {
        Redirect(
          (journeyType.operation, useDefault) match {
            case (Add, true) =>
              controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.show(user.isAgent, journeyType.businessType)
            case (Add, false) =>
              controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.show(user.isAgent, journeyType.businessType)
            case (Manage, _) =>
              controllers.incomeSources.manage.routes.CannotGoBackErrorController.show(user.isAgent, journeyType.businessType)
            case (Cease, _) if user.isAgent =>
              controllers.incomeSources.cease.routes.IncomeSourceCeasedBackErrorController.showAgent(journeyType.businessType)
            case (Cease, _) =>
              controllers.incomeSources.cease.routes.IncomeSourceCeasedBackErrorController.show(journeyType.businessType)
          }
        )
      }
    }

  private lazy val journeyRestartUrl: (JourneyType) => MtdItUser[_] => Future[Result] =
    (journeyType: JourneyType) => user => {
      Future.successful {
        journeyType.operation match {
          case Add                    => Redirect(controllers.incomeSources.add.routes.AddIncomeSourceController.show(user.isAgent))
          case Manage                 => Redirect(controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(user.isAgent))
          case Cease if user.isAgent  => Redirect(controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent())
          case Cease                  => Redirect(controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show())
        }
      }
    }

  private def useDefaultRedirect(data: UIJourneySessionData, journeyType: JourneyType, journeyState: JourneyState): Boolean = {
    journeyType.operation match {
      case Add => !((journeyState == BeforeSubmissionPage || journeyState == InitialPage) && data.addIncomeSourceData.flatMap(_.incomeSourceAdded).getOrElse(false))
      case _ => true
    }
  }

  def withSessionData(journeyType: JourneyType, journeyState: JourneyState)(codeBlock: UIJourneySessionData => Future[Result])
                     (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    withIncomeSourcesFS {
      sessionService.getMongo(journeyType).flatMap {
        case Right(Some(data: UIJourneySessionData)) if showCannotGoBackErrorPage(data, journeyType, journeyState) =>
          redirectUrl(journeyType, useDefaultRedirect(data, journeyType, journeyState))(user)
        case Right(Some(data: UIJourneySessionData)) =>
          codeBlock(data)
        case Right(None) =>
          if (journeyState == InitialPage) {
            sessionService.createSession(journeyType.toString).flatMap { _ =>
              val data = UIJourneySessionData(hc.sessionId.get.value, journeyType.toString)
              codeBlock(data)
            }
          }
          else journeyRestartUrl(journeyType)(user)
        case Left(ex) =>
          val agentPrefix = if (user.isAgent) "[Agent]" else ""
          Logger("application").error(s"$agentPrefix" +
            s"[JourneyChecker][withSessionData]: Unable to retrieve Mongo data for journey type ${journeyType.toString}", ex)
          journeyRestartUrl(journeyType)(user)
      }
    }
  }

  private def showCannotGoBackErrorPage(data: UIJourneySessionData, journeyType: JourneyType, journeyState: JourneyState): Boolean = {
    (journeyType.operation, journeyState) match {
      case (_, CannotGoBackPage) => false
      case (Add, BeforeSubmissionPage) | (Add, InitialPage) =>
        data.addIncomeSourceData.flatMap(_.journeyIsComplete).getOrElse(false) ||
          data.addIncomeSourceData.flatMap(_.incomeSourceAdded).getOrElse(false)
      case (Add, _) =>
        data.addIncomeSourceData.flatMap(_.journeyIsComplete).getOrElse(false)
      case (Manage, _) =>
        data.manageIncomeSourceData.flatMap(_.journeyIsComplete).getOrElse(false)
      case (Cease, _) =>
        data.ceaseIncomeSourceData.flatMap(_.journeyIsComplete).getOrElse(false)
      case _ => false
    }
  }
}
