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
import enums.JourneyType.{Add, Cease, IncomeSourceJourneyType, Manage}
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

  private lazy val isAgent: MtdItUser[_] => Boolean = (user: MtdItUser[_]) => user.userType.contains(Agent)

  private lazy val redirectUrl: (IncomeSourceJourneyType, Boolean) => MtdItUser[_] => Future[Result] =
    (incomeSources: IncomeSourceJourneyType, useDefault: Boolean) => user => {
      (incomeSources.operation, isAgent(user), useDefault) match {
        case (Add, true, true) =>
          Future.successful {
            Redirect(controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.showAgent(incomeSources.businessType))
          }
        case (Add, true, false) =>
          Future.successful {
            Redirect(controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.showAgent(incomeSources.businessType))
          }
        case (Add, false, true) =>
          Future.successful {
            Redirect(controllers.incomeSources.add.routes.ReportingMethodSetBackErrorController.show(incomeSources.businessType))
          }
        case (Add, false, false) =>
          Future.successful {
            Redirect(controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController.show(incomeSources.businessType))
          }
        case (Manage, _, _) =>
          Future.successful {
            Redirect(controllers.incomeSources.manage.routes.CannotGoBackErrorController.show(isAgent(user), incomeSources.businessType))
          }
        case (Cease, true, _) =>
          Future.successful {
            Redirect(controllers.incomeSources.cease.routes.IncomeSourceCeasedBackErrorController.showAgent(incomeSources.businessType))
          }
        case (Cease, false, _) =>
          Future.successful {
            Redirect(controllers.incomeSources.cease.routes.IncomeSourceCeasedBackErrorController.show(incomeSources.businessType))
          }
        case (e, _, _) =>
          throw new Exception(s"Invalid operation found: ${e.operationType}")
      }
    }

  private lazy val journeyRestartUrl: (IncomeSourceJourneyType) => MtdItUser[_] => Future[Result] =
    (incomeSources: IncomeSourceJourneyType) => user => {
      Future.successful {
        (incomeSources.operation, isAgent(user)) match {
          case (Add, false) => Redirect(controllers.incomeSources.add.routes.AddIncomeSourceController.show())
          case (Add, true) => Redirect(controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent())
          case (Manage, _) => Redirect(controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(isAgent(user)))
          case (Cease, false) => Redirect(controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show())
          case (Cease, true) => Redirect(controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent())
          case (e, _) => throw new Exception(s"Invalid operation found: ${e.operationType}")
        }
      }
    }

  private def useDefaultRedirect(data: UIJourneySessionData, incomeSources: IncomeSourceJourneyType, journeyState: JourneyState): Boolean = {
    incomeSources.operation match {
      case Add => !((journeyState == BeforeSubmissionPage || journeyState == InitialPage) && data.addIncomeSourceData.flatMap(_.incomeSourceAdded).getOrElse(false))
      case _ => true
    }
  }

  def withSessionDataAndOldIncomeSourceFS(incomeSources: IncomeSourceJourneyType, journeyState: JourneyState)(codeBlock: UIJourneySessionData => Future[Result])
                                         (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    withIncomeSourcesFS {
      sessionService.getMongo(incomeSources).flatMap {
        case Right(Some(data: UIJourneySessionData)) if showCannotGoBackErrorPage(data, incomeSources, journeyState) =>
          redirectUrl(incomeSources, useDefaultRedirect(data, incomeSources, journeyState))(user)
        case Right(Some(data: UIJourneySessionData)) =>
          codeBlock(data)
        case Right(None) =>
          if (journeyState == InitialPage) {
            sessionService.createSession(incomeSources).flatMap { _ =>
              val data = UIJourneySessionData(hc.sessionId.get.value, incomeSources.toString)
              codeBlock(data)
            }
          }
          else journeyRestartUrl(incomeSources)(user)
        case Left(ex) =>
          val agentPrefix = if (isAgent(user)) "[Agent]" else ""
          Logger("application").error(s"$agentPrefix" +
            s"Unable to retrieve Mongo data for journey type ${incomeSources}", ex)
          journeyRestartUrl(incomeSources)(user)
      }
    }
  }

  private def showCannotGoBackErrorPage(data: UIJourneySessionData, incomeSources: IncomeSourceJourneyType, journeyState: JourneyState): Boolean = {
    (incomeSources.operation, journeyState) match {
      case (_, CannotGoBackPage) => false
      case (Add, BeforeSubmissionPage) | (Add, InitialPage) =>
        data.addIncomeSourceData.flatMap(_.incomeSourceCreatedJourneyComplete).getOrElse(false) ||
          data.addIncomeSourceData.flatMap(_.incomeSourceAdded).getOrElse(false)
      case (Add, _) =>
        data.addIncomeSourceData.flatMap(_.incomeSourceCreatedJourneyComplete).getOrElse(false)
      case (Manage, _) =>
        data.manageIncomeSourceData.flatMap(_.journeyIsComplete).getOrElse(false)
      case (Cease, _) =>
        data.ceaseIncomeSourceData.flatMap(_.journeyIsComplete).getOrElse(false)
      case _ => false
    }
  }
}
