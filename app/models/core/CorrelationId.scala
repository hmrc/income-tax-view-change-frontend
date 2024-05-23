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

package models.core

import models.core.CorrelationId.correlationId
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import java.util.UUID.randomUUID;

case class CorrelationId(id: UUID = randomUUID()) {
  def asHeader(): (String, String) = (correlationId, id.toString)
}

object CorrelationId {

  val correlationId = "Correlation-Id"
  def fromHeaderCarrier(hc: HeaderCarrier): Option[CorrelationId] = {
    hc.headers(Seq(correlationId))
      .headOption
      .map(header => CorrelationId(UUID.fromString(header._2)))
  }
}
