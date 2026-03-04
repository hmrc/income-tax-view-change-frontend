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

package mocks.services

import models.incomeSourceDetails.TaxYear
import models.reportingObligations.signUp.SignUpTaxYearQuestionViewModel
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import services.reportingObligations.signUp.SignUpService
import services.reportingObligations.signUp.core.SignUpProposition
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockSignUpService extends UnitSpec with BeforeAndAfterEach {

  lazy val mockSignUpService: SignUpService = mock(classOf[SignUpService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSignUpService)
  }

  def mockSaveIntent(intent: TaxYear, isSuccessful: Boolean = true): Unit = {
    when(mockSignUpService.saveIntent(ArgumentMatchers.eq(intent))(any(), any(), any()))
      .thenReturn(Future.successful(isSuccessful))
  }

  def mockAvailableOptInTaxYear(choices: List[TaxYear]): Unit = {
    when(mockSignUpService.availableSignUpTaxYear()(any(), any(), any()))
      .thenReturn(Future.successful(choices))
  }

  def mockFetchSavedChosenTaxYear(intentOpl: Option[TaxYear]): Unit = {
    when(mockSignUpService.fetchSavedChosenTaxYear()(any(), any(), any()))
      .thenReturn(Future.successful(intentOpl))
  }

  def mockFetchSignUpProposition(propositionOpl: Option[SignUpProposition]): Unit = {
    propositionOpl.map { proposition =>
      when(mockSignUpService.fetchSignUpProposition()(any(), any(), any()))
        .thenReturn(Future.successful(proposition))
    } getOrElse {
      when(mockSignUpService.fetchSignUpProposition()(any(), any(), any()))
        .thenReturn(Future.failed(new RuntimeException("Some error")))
    }
  }

  def mockIsSignUpTaxYearValid(out: Future[Option[SignUpTaxYearQuestionViewModel]]): Unit = {
    when(mockSignUpService.isSignUpTaxYearValid(any())(any(), any(), any()))
      .thenReturn(out)
  }

  def mockFetchSavedSignUpSessionData(): Unit = {
    when(mockSignUpService.fetchSavedSignUpSessionData()(any(), any(), any()))
      .thenReturn(Future.successful(None))
  }

  def mockUpdateOptInJourneyStatusInSessionData(journeyComplete: Boolean = false): Unit = {
    when(mockSignUpService.updateJourneyStatusInSessionData(any())(any(), any(), any()))
      .thenReturn(Future.successful(journeyComplete))
  }
}