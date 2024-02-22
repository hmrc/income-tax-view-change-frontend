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

package testOnly.models

import config.FrontendAppConfig
import config.featureswitch.{FeatureSwitching, TimeMachineAddYear}
import services.{CalculationListService, DateServiceInterface}
import testOnly.services.DynamicStubService
import uk.gov.hmrc.http.HeaderCarrier
import utils.OptOutCustomDataUploadHelper

import javax.inject.Inject
import scala.concurrent.Future

case class CrystallisationStatus @Inject()(appConfig: FrontendAppConfig)(status: String) extends OptOutCustomDataUploadHelper with FeatureSwitching {

  private def currentTaxYearMinusOneRange(implicit dateService: DateServiceInterface): String = dateService.getCurrentTaxYearMinusOneRange(isEnabled(TimeMachineAddYear))

  def uploadData(nino: Nino)(implicit dynamicStubService: DynamicStubService, hc: HeaderCarrier, dateService: DateServiceInterface)
  : Future[Unit] = {
    handleDefaultValues(status = status) {
      dynamicStubService.overwriteCalculationList(nino = nino, taxYearRange = currentTaxYearMinusOneRange, crystallisationStatus = status)
    }
  }
}
