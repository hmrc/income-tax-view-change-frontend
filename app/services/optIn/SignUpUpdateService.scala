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

package services.optIn

import audit.AuditingService
import audit.models.OptInAuditModel
import auth.MtdItUser
import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponse, ITSAStatusUpdateResponseFailure}
import enums.JourneyType.{Opt, OptInJourney}
import models.UIJourneySessionData
import models.incomeSourceDetails.TaxYear
import models.optin.{OptInContextData, OptInSessionData}
import repositories.UIJourneySessionDataRepository
import services.optIn.core.OptInProposition
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SignUpUpdateService @Inject()(
                                     auditingService: AuditingService,
                                     itsaStatusUpdateConnector: ITSAStatusUpdateConnector,
                                     optInService: OptInService,
                                     uiJourneySessionDataRepository: UIJourneySessionDataRepository
                                   ) {

  def getOptInSessionData()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[OptInSessionData]] = {
    for {
      uiSessionData <- uiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptInJourney))
      optInSessionData: Option[OptInSessionData] = uiSessionData.flatMap(_.optInSessionData)
    } yield {
      optInSessionData
    }
  }

  def triggerOptInRequest()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[ITSAStatusUpdateResponse] = {
    for {
      optInSessionData: Option[OptInSessionData] <- getOptInSessionData()
      selectedOptInYear: Option[TaxYear] = optInSessionData.flatMap(_.selectedOptInYear).flatMap(TaxYear.`fromStringYYYY-YYYY`)
      optInContextData: Option[OptInContextData] = optInSessionData.flatMap(_.optInContextData)
      optInProposition: OptInProposition <- optInService.fetchOptInProposition()
      updateResponse <- selectedOptInYear match {
        case Some(taxYear) =>
          itsaStatusUpdateConnector.optIn(taxYear = taxYear, user.nino)
        case None =>
          Future(ITSAStatusUpdateResponseFailure.defaultFailure())
      }
      sendOptInAuditEvent <-
        selectedOptInYear match {
          case Some(taxYear) =>
            val optInAuditModel = OptInAuditModel(optInProposition, taxYear, updateResponse)
            auditingService.extendedAudit(optInAuditModel)
          case _ =>
            Future(()) // we don't send an audit if user has not selected a tax year
        }
    } yield {
      updateResponse
    }
  }


}