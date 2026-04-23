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

package obligations.repositories

import auth.MtdItUser
import cats.data.OptionT
import enums.JourneyType.{Opt, OptOutJourney, SignUpJourney}
import models.UIJourneySessionData
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import obligations.models.reportingObligations.signUp.SignUpSessionData
import obligations.repositories.OptOutContextData
import obligations.services.reportingObligations.optOut.OptOutProposition
import obligations.services.reportingObligations.optOut.OptOutProposition.createOptOutProposition
import play.api.Logger
import play.api.libs.json.{Json, OFormat}
import repositories.UIJourneySessionDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SigninSessionDataRepository @Inject()(val repository: UIJourneySessionDataRepository) {

  def saveIntent(intent: TaxYear)(implicit hc: HeaderCarrier,
                                  ec: ExecutionContext): Future[Boolean] = {
    OptionT(fetchExistingUIJourneySessionDataOrInit()).
      map(journeySd => journeySd.copy(signUpSessionData = journeySd.signUpSessionData.map(_.copy(selectedSignUpYear = Some(intent.toString))))).
      flatMap(journeySd => OptionT.liftF(repository.set(journeySd))).
      getOrElse(false)
  }
  
  def fetchSigninSessionData()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SignUpSessionData]] = {
    repository.get(hc.sessionId.get.value, Opt(SignUpJourney)).map(_.flatMap(_.signUpSessionData))
  }

  def fetchExistingUIJourneySessionDataOrInit(attempt: Int = 1)(implicit hc: HeaderCarrier,
                                                                ec: ExecutionContext): Future[Option[UIJourneySessionData]] = {
    repository.get(hc.sessionId.get.value, Opt(SignUpJourney)).flatMap {
      case Some(jsd) => Future.successful(Some(jsd))
      case None if attempt < 2 => setupSessionData().filter(b => b).flatMap(_ => fetchExistingUIJourneySessionDataOrInit(2))
      case _ => Future.successful(None)
    }
  }

  def setupSessionData()(implicit hc: HeaderCarrier): Future[Boolean] =
    repository.set(UIJourneySessionData(hc.sessionId.get.value, Opt(SignUpJourney).toString, signUpSessionData = Some(SignUpSessionData(None, None, Some(false)))))

  def setJourneyCompleteStatus(journeyComplete: Boolean)
                              (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    OptionT(fetchExistingUIJourneySessionDataOrInit())
      .map(journeySd => journeySd.copy(signUpSessionData = journeySd.signUpSessionData.map(_.copy(journeyIsComplete = Some(journeyComplete)))))
      .flatMap(journeySd => OptionT.liftF(repository.set(journeySd)))
      .getOrElse(false)
      .map {
        case false =>
          Logger("application").error(s"[OptInService][updateJourneyStatusInSessionData] Failed to set journeyIsComplete flag")
          false
        case x => x
      }
  }

  def setUpdatedSessionDataStatus(journeySd: UIJourneySessionData)
                              (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    repository.set(journeySd)
  }
}