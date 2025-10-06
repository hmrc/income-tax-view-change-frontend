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
import enums.IncomeSourceJourney._
import enums.{BeforeSubmissionPage, CannotGoBackPage, InitialPage, JourneyState, ReportingFrequencyPages}
import enums.JourneyType.{Add, Cease, IncomeSourceJourneyType, Manage}
import models.incomeSourceDetails.UIJourneySessionData
import play.api.Logger
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.SessionService
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait JourneyCheckerManageBusinesses extends IncomeSourcesUtils {
  self =>

  val sessionService: SessionService

  implicit val ec: ExecutionContext

  private lazy val isAgent: MtdItUser[_] => Boolean = (user: MtdItUser[_]) => user.userType.contains(Agent)

  private lazy val redirectUrl: (IncomeSourceJourneyType, Boolean) => MtdItUser[_] => Future[Result] =
    (incomeSources: IncomeSourceJourneyType, useDefault: Boolean) => user => {
      (incomeSources.operation, isAgent(user), useDefault) match {
        case (Add, true, true) =>
          Future.successful {
            Redirect(controllers.manageBusinesses.add.routes.ReportingMethodSetBackErrorController.showAgent(incomeSources.businessType))
          }
        case (Add, true, false) =>
          Future.successful {
            Redirect(controllers.manageBusinesses.add.routes.IncomeSourceAddedBackErrorController.showAgent(incomeSources.businessType))
          }
        case (Add, false, true) =>
          Future.successful {
            Redirect(controllers.manageBusinesses.add.routes.ReportingMethodSetBackErrorController.show(incomeSources.businessType))
          }
        case (Add, false, false) =>
          Future.successful {
            Redirect(controllers.manageBusinesses.add.routes.IncomeSourceAddedBackErrorController.show(incomeSources.businessType))
          }
        case (Manage, _, _) =>
          Future.successful {
            Redirect(controllers.manageBusinesses.manage.routes.CannotGoBackErrorController.show(isAgent(user), incomeSources.businessType))
          }
        case (Cease, true, _) =>
          Future.successful {
            Redirect(controllers.manageBusinesses.cease.routes.IncomeSourceCeasedBackErrorController.showAgent(incomeSources.businessType))
          }
        case (Cease, false, _) =>
          Future.successful {
            Redirect(controllers.manageBusinesses.cease.routes.IncomeSourceCeasedBackErrorController.show(incomeSources.businessType))
          }
        case (e, _, _) =>
          throw new Exception(s"Invalid operation found: ${e.operationType}")
      }
    }

  private lazy val journeyRestartUrl: MtdItUser[_] => Future[Result] =
    user => {
      Future.successful {
        val manageBusinessesCall = if(isAgent(user)) {
          controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent()
        } else {
          controllers.manageBusinesses.routes.ManageYourBusinessesController.show()
        }
        Redirect(manageBusinessesCall)
      }
    }

  private def useDefaultRedirect(data: UIJourneySessionData, incomeSources: IncomeSourceJourneyType, journeyState: JourneyState): Boolean = {
    incomeSources.operation match {
      case Add => !((journeyState == BeforeSubmissionPage || journeyState == InitialPage) &&
        data.addIncomeSourceData.flatMap(_.incomeSourceAdded).getOrElse(false))
      case _ => true
    }
  }

  def withSessionData(incomeSources: IncomeSourceJourneyType, journeyState: JourneyState)
                                          (codeBlock: UIJourneySessionData => Future[Result])
                                          (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    sessionService.getMongo(incomeSources).flatMap {
      case Right(Some(data: UIJourneySessionData)) if showCannotGoBackErrorPage(data, incomeSources, journeyState) =>
        redirectUrl(incomeSources, useDefaultRedirect(data, incomeSources, journeyState))(user)
      case Right(Some(data: UIJourneySessionData)) => codeBlock(data)
      case Right(None) =>
        if (journeyState == InitialPage) {
          sessionService.createSession(incomeSources).flatMap { _ =>
            val data = UIJourneySessionData(hc.sessionId.get.value, incomeSources.toString)
            codeBlock(data)
          }
        }
        else journeyRestartUrl(user)
      case Left(ex) =>
        val agentPrefix = if (isAgent(user)) "[Agent]" else ""
        Logger("application").error(s"$agentPrefix" +
          s"Unable to retrieve Mongo data for journey type ${incomeSources.toString}", ex)
        journeyRestartUrl(user)
    }
  }

  private def showCannotGoBackErrorPage(data: UIJourneySessionData, incomeSources: IncomeSourceJourneyType, journeyState: JourneyState): Boolean = {
    val incomeSourceCreatedJourneyComplete = data.addIncomeSourceData.flatMap(_.incomeSourceCreatedJourneyComplete).getOrElse(false)
    val incomeSourceAdded = data.addIncomeSourceData.flatMap(_.incomeSourceAdded).getOrElse(false)
    (incomeSources.operation, journeyState) match {
      case (_, CannotGoBackPage) => false
      case (Add, BeforeSubmissionPage) | (Add, InitialPage) => incomeSourceCreatedJourneyComplete || incomeSourceAdded
      case (Add, ReportingFrequencyPages) => (incomeSourceCreatedJourneyComplete || incomeSourceAdded) && data.addIncomeSourceData.flatMap(_.incomeSourceRFJourneyComplete).getOrElse(false)
      case (Add, _) => incomeSourceCreatedJourneyComplete
      case (Manage, _) => data.manageIncomeSourceData.flatMap(_.journeyIsComplete).getOrElse(false)
      case (Cease, _) => data.ceaseIncomeSourceData.flatMap(_.journeyIsComplete).getOrElse(false)
      case _ => false
    }
  }
}
