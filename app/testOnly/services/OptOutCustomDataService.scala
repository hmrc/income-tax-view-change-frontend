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

package testOnly.services

import config.FrontendAppConfig
import models.incomeSourceDetails.TaxYear
import services.DateServiceInterface
import testOnly.models.Nino
import testOnly.utils.OptOutCustomDataUploadHelper
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.Future

class OptOutCustomDataService @Inject()(implicit val appConfig: FrontendAppConfig,
                                        implicit val dateService: DateServiceInterface,
                                        implicit val dynamicStubService: DynamicStubService) extends OptOutCustomDataUploadHelper {

  def uploadCalculationListData(nino: Nino, taxYear: TaxYear, status: String)(implicit hc: HeaderCarrier)
  : Future[Unit] = {
    handleDefaultValues(status = status) {
      dynamicStubService.overwriteCalculationList(nino = nino, taxYearRange = taxYear.formatTaxYearRange, crystallisationStatus = status)
    }
  }

  def uploadITSAStatusData(nino: Nino, taxYear: TaxYear, status: String)(implicit hc: HeaderCarrier)
  : Future[Unit] = {
    handleDefaultValues(status = status) {
      dynamicStubService.overwriteItsaStatus(nino = nino, taxYearRange = taxYear.formatTaxYearRange, ITSAStatus = status)
    }
  }
}
