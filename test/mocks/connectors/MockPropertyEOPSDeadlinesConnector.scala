/*
 * Copyright 2018 HM Revenue & Customs
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

package mocks.connectors

import connectors.PropertyEOPSDeadlinesConnector
import models.ReportDeadlinesResponseModel
import org.mockito.ArgumentMatchers
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Mockito._

import scala.concurrent.Future

trait MockPropertyEOPSDeadlinesConnector extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val mockPropertyEOPSDeadlinesConnector: PropertyEOPSDeadlinesConnector = mock[PropertyEOPSDeadlinesConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockPropertyEOPSDeadlinesConnector)
  }

  def setupMockPropertyEOPSDeadline(nino: String)(response: ReportDeadlinesResponseModel): Unit = {
    when(mockPropertyEOPSDeadlinesConnector.getPropertyEOPSDeadlines(
      ArgumentMatchers.eq(nino))(ArgumentMatchers.any())).thenReturn(Future.successful(response))
  }

}
