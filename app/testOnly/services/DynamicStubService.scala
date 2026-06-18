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

package testOnly.services

import common.config.FrontendAppConfig
import common.config.featureswitch.FeatureSwitching
import common.connectors.ITSAStatusConnector
import common.models.incomeSourceDetails.TaxYear
import common.models.itsaStatus.ITSAStatusResponseModel
import play.api.Logger
import testOnly.connectors.DynamicStubConnector
import testOnly.models.customUsers.{CalculationTypeUser, IncomeSourcesUser, LatentBusinessUser}
import testOnly.models.{DataModel, Nino}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DynamicStubService @Inject()(itsaStatusConnector: ITSAStatusConnector,
                                   dynamicStubConnector: DynamicStubConnector,
                                   implicit val appConfig: FrontendAppConfig) extends FeatureSwitching {

  def overwriteCalculationList(nino: Nino, taxYearRange: String, crystallisationStatus: String)
                              (implicit headerCarrier: HeaderCarrier): Future[Unit] = {
    Logger("application").debug("" +
      s"Overwriting calculation list (1896) data via the dynamic stub with nino / taxYearRange: ${nino.value} - $taxYearRange")
    dynamicStubConnector.overwriteCalculationList(nino, taxYearRange, crystallisationStatus)
  }

  def getITSAStatusDetail(taxYear: TaxYear, nino: String, futureYears: Boolean = false)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[ITSAStatusResponseModel]] = {

    itsaStatusConnector.getITSAStatusDetail(
      nino = nino,
      taxYear = taxYear.formatAsShortYearRange,
      futureYears = futureYears,
      history = false
    ) map {
      case Right(itsaStatus: List[ITSAStatusResponseModel]) if itsaStatus.nonEmpty =>
        Logger("application").info(s"Success! >! ITSA Status Response Model: $itsaStatus !<")
        itsaStatus
      case Left(error) =>
        Logger("application").error(s"$error")
        throw new Exception("Failed to retrieve ITSAStatus")
      case _ =>
        Logger("application").error(s"Unexpected error. List of ITSAStatusResponseModels was empty!")
        throw new Exception("Unexpected error. List of ITSAStatusResponseModels was empty!")
    }
  }

  def overwriteItsaStatus(nino: Nino, taxYearRange: String, ITSAStatus: String)
                         (implicit headerCarrier: HeaderCarrier): Future[Unit] = {
    Logger("application").debug("" +
      s"Overwriting ITSA Status (1878) data via the dynamic stub with nino / taxYearRange: ${nino.value} - $taxYearRange")
    dynamicStubConnector.overwriteItsaStatus(nino, taxYearRange, ITSAStatus)
  }

  def overwriteBusinessData(mtdid: String, incomeSourcesUser: IncomeSourcesUser)(implicit headerCarrier: HeaderCarrier): Future[Unit] = {
    Logger("application").debug("Overwriting business data (5266) via the dynamic stub")
    
    dynamicStubConnector.overwriteBusinessData(mtdid, incomeSourcesUser)
  }
  
  def overwriteLatentBusinessData(mtdid: String, latentBusinessUser: LatentBusinessUser)(implicit headerCarrier: HeaderCarrier): Future[Unit] = {
    Logger("application").debug("Overwriting latent business data (5266) via the dynamic stub")
    
    dynamicStubConnector.overwriteLatentBusinessData(mtdid, latentBusinessUser)
  }

  def overwriteObligationsData(nino: String)(implicit headerCarrier: HeaderCarrier): Future[Unit] = {
    Logger("application").debug("Overwriting obligations data (1330) via the dynamic stub")
    dynamicStubConnector.overwriteObligationsData(nino)
  }
  
  def overwriteCalculationTypeData(nino: String, calculationTypeUser: CalculationTypeUser)(implicit headerCarrier: HeaderCarrier): Future[Unit] =  {
    Logger("application").debug("Overwriting calculation type data (2083) via the dynamic stub")
    dynamicStubConnector.overwriteCalculationTypeData(nino, calculationTypeUser)
  }

  def overwriteEstimatedRepaymentDate()(implicit headerCarrier: HeaderCarrier): Future[Unit] = {
    Logger("application").debug("Overwriting estimatedRepaymentDate the via dynamic stub")
    dynamicStubConnector.overwriteEstimatedRepaymentDate()
  }

  def addData(dataModel: DataModel)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    dynamicStubConnector.addData(dataModel).map { response =>
      Logger("application").info(s"${response.status} " + response.body)
      response
    }
  }

  def overwriteEffectiveDateOfPaymentUrl()(implicit headerCarrier: HeaderCarrier): Future[Unit] = {
    Logger("application").debug("Overwriting effectiveDateOfPayment the via dynamic stub")
    dynamicStubConnector.overwriteEffectiveDateOfPayment()
  }
}
