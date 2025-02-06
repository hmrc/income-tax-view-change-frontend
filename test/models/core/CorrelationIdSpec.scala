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

import testUtils.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID.randomUUID

class CorrelationIdSpec extends UnitSpec {

  val id            = randomUUID()
  val correlationId = CorrelationId(id)

  ".asHeader" should {
    "return tuple with CorrelationId header" in {
      correlationId.asHeader() shouldBe ("Correlation-Id", id.toString)
    }
  }

  ".fromHeaderCarrier" should {

    "return Option of correlationId from HeaderCarrier, if one exists" in {
      val hc = HeaderCarrier(otherHeaders = Seq((CorrelationId.correlationId, id.toString)))
      CorrelationId.fromHeaderCarrier(hc) shouldBe Some(CorrelationId(id))
    }

    "return None from HeaderCarrier, if none exists" in {
      val hc = HeaderCarrier()
      CorrelationId.fromHeaderCarrier(hc) shouldBe None
    }
  }
}
