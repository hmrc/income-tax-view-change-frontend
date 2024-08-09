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
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.optout.OptOutSessionData
import play.api.libs.json.{Json, OFormat}
import repositories.OptOutContextData.{statusToString, stringToStatus}
import services.optout.OptOutProposition
import services.optout.OptOutService.OptOutInitialState
import uk.gov.hmrc.http.HeaderCarrier
import utils.OptOutJourney

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OptOutSessionDataRepository @Inject()(val repository: UIJourneySessionDataRepository) {

  def saveIntent(intent: TaxYear)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    OptionT(repository.get(hc.sessionId.get.value, OptOutJourney.Name)).
      map(journeySd => journeySd.copy(optOutSessionData = journeySd.optOutSessionData.map(_.copy(selectedOptOutYear = Some(intent.toString))))).
      flatMap(journeySd => OptionT.liftF(repository.set(journeySd))).
      getOrElse(false)
  }

  def recallOptOutInitialState()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[OptOutInitialState]] = {
    repository.get(hc.sessionId.get.value, OptOutJourney.Name) map { sessionData =>
      for {
        data <- sessionData
        optOutData <- data.optOutSessionData
        contextData <- optOutData.optOutContextData
      }
      yield OptOutInitialState(
        contextData.crystallisationStatus,
        stringToStatus(contextData.previousYearITSAStatus),
        stringToStatus(contextData.currentYearITSAStatus),
        stringToStatus(contextData.nextYearITSAStatus))
    }
  }

  def fetchSavedIntent()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[TaxYear]] = {
    repository.get(hc.sessionId.get.value, OptOutJourney.Name) map { sessionData =>
      for {
        data <- sessionData
        optOutData <- data.optOutSessionData
        selected <- optOutData.selectedOptOutYear
        parsed <- TaxYear.getTaxYearModel(selected)
      } yield parsed
    }
  }

  def initialiseOptOutJourney(oop :OptOutProposition)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val data = UIJourneySessionData(
      sessionId = hc.sessionId.get.value,
      journeyType = OptOutJourney.Name,
      optOutSessionData = Some(OptOutSessionData(Some(buildOptOutContextData(oop)), selectedOptOutYear = None))
    )
    repository.set(data)
  }

  private def buildOptOutContextData(oop: OptOutProposition): OptOutContextData = {
    OptOutContextData(oop.previousTaxYear.crystallised,
      statusToString(oop.previousTaxYear.status),
      statusToString(oop.currentTaxYear.status),
      statusToString(oop.nextTaxYear.status))
  }

}

case class OptOutContextData(crystallisationStatus: Boolean,
                             previousYearITSAStatus: String,
                             currentYearITSAStatus: String,
                             nextYearITSAStatus: String)

object OptOutContextData {
  implicit val format: OFormat[OptOutContextData] = Json.format[OptOutContextData]

  def statusToString(status: ITSAStatus): String =
    status match {
      case ITSAStatus.NoStatus  => "U"
      case ITSAStatus.Voluntary => "V"
      case ITSAStatus.Annual    => "A"
      case ITSAStatus.Mandated  => "M"
      // This will be validated earlier on in a future ticket
      case _ => throw new RuntimeException("Unexpected status")
    }

  def stringToStatus(status: String): ITSAStatus.Value =
    status match {
      case "U" => ITSAStatus.NoStatus
      case "V" => ITSAStatus.Voluntary
      case "A" => ITSAStatus.Annual
      case "M" => ITSAStatus.Mandated
      case _ => throw new RuntimeException("Unexpected status")
    }
}
