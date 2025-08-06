/*
 * Copyright 2024 HM Revenue & Customs
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

package repositories

import cats.data.OptionT
import enums.JourneyType.{Opt, OptOutJourney}
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.optout.OptOutSessionData
import play.api.libs.json.{Json, OFormat}
import repositories.ITSAStatusRepositorySupport.{statusToString, stringToStatus}
import services.optout.OptOutProposition
import services.optout.OptOutProposition.createOptOutProposition
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OptOutSessionDataRepository @Inject()(val repository: UIJourneySessionDataRepository) {

  def saveIntent(intent: TaxYear)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    OptionT(repository.get(hc.sessionId.get.value, Opt(OptOutJourney)))
      .map(journeySd => journeySd.copy(optOutSessionData = journeySd.optOutSessionData.map(_.copy(selectedOptOutYear = Some(intent.toString)))))
      .flatMap(journeySd => OptionT.liftF(repository.set(journeySd)))
      .getOrElse(false)
  }

  private def buildResult(
                           contextData: OptOutContextData,
                           selectedOptOutYear: Option[String]
                         ): Option[(OptOutProposition, Option[TaxYear])] = {
    val currentYearOpt = TaxYear.getTaxYearModel(contextData.currentYear)
    currentYearOpt.map { currentYear =>
      val proposition = createOptOutProposition(
        currentYear = currentYear,
        previousYearCrystallised = contextData.crystallisationStatus,
        previousYearItsaStatus = stringToStatus(contextData.previousYearITSAStatus),
        currentYearItsaStatus = stringToStatus(contextData.currentYearITSAStatus),
        nextYearItsaStatus = stringToStatus(contextData.nextYearITSAStatus)
      )
      val selectedTaxYear = selectedOptOutYear.flatMap(year => TaxYear.getTaxYearModel(year))
      (proposition, selectedTaxYear)
    }
  }

  def recallOptOutPropositionWithIntent()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[(OptOutProposition, Option[TaxYear])]] = {

    repository
      .get(hc.sessionId.get.value, Opt(OptOutJourney))
      .map { sessionData: Option[UIJourneySessionData] =>
        for {
          data <- sessionData
          optOutData <- data.optOutSessionData
          contextData <- optOutData.optOutContextData
          result <- buildResult(contextData, optOutData.selectedOptOutYear)
        } yield result
      }
  }

  def fetchSavedIntent()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[TaxYear]] = {
    repository.get(hc.sessionId.get.value, Opt(OptOutJourney)) map { sessionData =>
      for {
        data <- sessionData
        optOutData <- data.optOutSessionData
        selected <- optOutData.selectedOptOutYear
        parsed <- TaxYear.getTaxYearModel(selected)
      } yield parsed
    }
  }

  def initialiseOptOutJourney(oop: OptOutProposition)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val data = UIJourneySessionData(
      sessionId = hc.sessionId.get.value,
      journeyType = OptOutJourney.toString,
      optOutSessionData = Some(OptOutSessionData(Some(buildOptOutContextData(oop)), selectedOptOutYear = None))
    )
    repository.set(data)
  }

  private def buildOptOutContextData(oop: OptOutProposition): OptOutContextData = {
    OptOutContextData(
      oop.currentTaxYear.taxYear.toString,
      oop.previousTaxYear.crystallised,
      statusToString(oop.previousTaxYear.status),
      statusToString(oop.currentTaxYear.status),
      statusToString(oop.nextTaxYear.status))
  }

}

case class OptOutContextData(currentYear: String,
                             crystallisationStatus: Boolean,
                             previousYearITSAStatus: String,
                             currentYearITSAStatus: String,
                             nextYearITSAStatus: String)

object OptOutContextData {
  implicit val format: OFormat[OptOutContextData] = Json.format[OptOutContextData]
}
