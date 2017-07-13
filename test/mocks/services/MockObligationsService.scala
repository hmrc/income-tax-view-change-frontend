/*
 * Copyright 2017 HM Revenue & Customs
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

package mocks.services

import assets.TestConstants.BusinessDetails.businessIncomeModel
import assets.TestConstants.testNino
import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import services.ObligationsService
import uk.gov.hmrc.play.test.UnitSpec
import utils.ImplicitDateFormatter

import scala.concurrent.Future


trait MockObligationsService extends UnitSpec with MockitoSugar with BeforeAndAfterEach with ImplicitDateFormatter {

  val mockObligationsService: ObligationsService = mock[ObligationsService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockObligationsService)
  }

  def setupMockBusinessObligationsResult(nino: String, businessIncome: Option[BusinessIncomeModel])(response: ObligationsResponseModel): Unit = {
    when(mockObligationsService.getBusinessObligations(ArgumentMatchers.eq(nino), ArgumentMatchers.eq(businessIncome))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def setupMockPropertyObligationsResult(nino: String)(response: ObligationsResponseModel): Unit = {
    when(mockObligationsService.getPropertyObligations(ArgumentMatchers.eq(nino))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def mockBusinessSuccess(): Unit = setupMockBusinessObligationsResult(testNino, Some(businessIncomeModel))(
    ObligationsModel(
      List(
        ObligationModel(
          start = "2017-04-06",
          end = "2017-07-05",
          due = "2017-08-05",
          met = true
        ),
        ObligationModel(
          start = "2017-07-06",
          end = "2017-10-05",
          due = "2017-11-05",
          met = true
        ),
        ObligationModel(
          start = "2017-10-06",
          end = "2018-01-05",
          due = "2018-02-05",
          met = false
        ),
        ObligationModel(
          start = "2018-01-06",
          end = "2018-04-05",
          due = "2018-05-06",
          met = false
        )
      )
    )
  )
  def mockBusinessError(): Unit = setupMockBusinessObligationsResult(testNino, Some(businessIncomeModel))(
    ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, "Test")
  )

  def mockPropertySuccess(): Unit = setupMockPropertyObligationsResult(testNino)(
    ObligationsModel(
      List(
        ObligationModel(
          start = "2017-04-06",
          end = "2017-07-05",
          due = "2017-08-05",
          met = true
        ),
        ObligationModel(
          start = "2017-07-06",
          end = "2017-10-05",
          due = "2017-11-05",
          met = true
        ),
        ObligationModel(
          start = "2017-10-06",
          end = "2018-01-05",
          due = "2018-02-05",
          met = false
        ),
        ObligationModel(
          start = "2018-01-06",
          end = "2018-04-05",
          due = "2018-05-06",
          met = false
        )
      )
    )
  )
  def mockPropertyError(): Unit = setupMockPropertyObligationsResult(testNino)(
    ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, "Test")
  )
}
