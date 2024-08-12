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

package services.optin

import cats.data.OptionT
import connectors.optout.ITSAStatusUpdateConnector
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import repositories.UIJourneySessionDataRepository
import services.{CalculationListService, DateServiceInterface, ITSAStatusService, NextUpdatesService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.OptInJourney

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OptInService @Inject()(itsaStatusUpdateConnector: ITSAStatusUpdateConnector,
                             itsaStatusService: ITSAStatusService,
                             calculationListService: CalculationListService,
                             nextUpdatesService: NextUpdatesService,
                             dateService: DateServiceInterface,
                             repository: UIJourneySessionDataRepository) {

  def saveIntent(intent: TaxYear)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    OptionT(repository.get(hc.sessionId.get.value, OptInJourney.Name)).
      map(journeySd => journeySd.copy(optInSessionData = journeySd.optInSessionData.map(_.copy(selectedOptInYear = Some(intent.toString))))).
      flatMap(journeySd => OptionT.liftF(repository.set(journeySd))).
      getOrElse(true)//todo this should default to false, set to true until journey setup is done correctly
  }

  def availableOptInTaxYear() = List(TaxYear.forYearEnd(2023), TaxYear.forYearEnd(2024)) //todo TBD

}