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

package services

import connectors.BusinessDetailsConnector
import models.core.NinoResponse
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class NinoLookupService @Inject() (val businessDetailsConnector: BusinessDetailsConnector) {

  def getNino(mtdRef: String)(implicit hc: HeaderCarrier): Future[NinoResponse] = {
    Logger("application").debug(s"Requesting NINO from connector for user with MtdRef: $mtdRef")
    businessDetailsConnector.getNino(mtdRef)
  }
}
