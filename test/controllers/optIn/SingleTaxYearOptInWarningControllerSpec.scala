/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.optIn

import enums.{MTDIndividual, MTDUserRole}
import forms.optIn.SingleTaxYearOptInWarningForm
import mocks.auth.MockAuthActions
import mocks.services.MockOptInService
import models.admin.ReportingFrequencyPage
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api
import play.api.Application
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.optIn.OptInService
import testConstants.BaseTestConstants._
import testConstants.BusinessDetailsTestConstants.business1
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.singleBusinessIncome
import views.html.optIn.SingleTaxYearWarningView

import scala.concurrent.Future

class SingleTaxYearOptInWarningControllerSpec extends MockAuthActions with MockOptInService with MockitoSugar {

  def config: Map[String, Object] = Map(
    "feature-switches.read-from-mongo" -> "false"
  )

  override lazy val app: Application =
    applicationBuilderWithAuthBindings
      .overrides(
        api.
          inject.bind[OptInService].toInstance(mockOptInService)
      )
      .configure(config)
      .build()

  lazy val testController: SingleTaxYearOptInWarningController = app.injector.instanceOf[SingleTaxYearOptInWarningController]
  lazy val singleTaxYearWarningView: SingleTaxYearWarningView = app.injector.instanceOf[SingleTaxYearWarningView]

  val taxYear = TaxYear(2024, 2025)

  val form: Form[SingleTaxYearOptInWarningForm] = SingleTaxYearOptInWarningForm(taxYear)

  mtdAllRoles.foreach { mtdRole: MTDUserRole =>
    val isAgent = mtdRole != MTDIndividual

    s".show(isAgent = $isAgent)" when {

      s"the user is authenticated as a $mtdRole" when {

        "a there is a single available tax year" should {

          "render the SingleTaxYearWarningView" in {
            enable(ReportingFrequencyPage)

            val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)
            setupMockSuccess(mtdRole)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
            ).thenReturn(Future(singleBusinessIncome))


            setupMockSuccess(mtdRole)

            when(mockOptInService.availableOptInTaxYear()(any(), any(), any()))
              .thenReturn(
                Future(List(TaxYear(2025, 2026)))
              )

            val action = testController.show(isAgent)
            val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
            val result = action(fakeRequest)

            status(result) shouldBe OK
            contentAsString(result) shouldBe
              singleTaxYearWarningView(
                form = form,
                submitAction = controllers.optIn.routes.SingleTaxYearOptInWarningController.submit(isAgent),
                isAgent = isAgent,
                taxYear = TaxYear(2025, 2026)
              ).toString
          }
        }

        "no tax years are returned" should {

          "redirect to ChooseYear page - SEE_OTHER" in {
            enable(ReportingFrequencyPage)

            val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)
            setupMockSuccess(mtdRole)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
            ).thenReturn(Future(singleBusinessIncome))


            setupMockSuccess(mtdRole)

            when(mockOptInService.availableOptInTaxYear()(any(), any(), any()))
              .thenReturn(
                Future(List())
              )

            val action = testController.show(isAgent)
            val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
            val result = action(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.optIn.routes.ChooseYearController.show(isAgent).url)
          }
        }

        "show the Error Template view" when {

          "there is some unexpected error" should {

            "recover and return error template Internal Server Error - 500" in {
              enable(ReportingFrequencyPage)

              val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)
              setupMockSuccess(mtdRole)

              when(
                mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
              ).thenReturn(Future(singleBusinessIncome))


              setupMockSuccess(mtdRole)

              when(mockOptInService.availableOptInTaxYear()(any(), any(), any()))
                .thenReturn(
                  Future(throw new Exception("some fake error"))
                )

              val action = testController.show(isAgent)
              val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
              val result = action(fakeRequest)

              status(result) shouldBe INTERNAL_SERVER_ERROR
              contentAsString(result).contains("Sorry, there is a problem with the service") shouldBe true
            }
          }
        }

        "render the home page" when {

          "the ReportingFrequencyPage feature switch is disabled" in {
            disable(ReportingFrequencyPage)
            setupMockSuccess(mtdRole)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
            ).thenReturn(Future(singleBusinessIncome))

            val action = testController.show(isAgent)
            val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)

            val result = action(fakeRequest)

            val redirectUrl = if (isAgent) {
              "/report-quarterly/income-and-expenses/view/agents/client-income-tax"
            } else {
              "/report-quarterly/income-and-expenses/view"
            }

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
      }

      val action = testController.show(isAgent)
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
      testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
    }

    s".submit(isAgent = $isAgent)" when {

      s"the user is authenticated as a $mtdRole" when {

        "a there is a single available tax year" when {

          "user answers Yes - true" should {

            "handle the submit request and redirect to the ConfirmTaxYear page" in {
              enable(ReportingFrequencyPage)

              val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)

              setupMockSuccess(mtdRole)

              when(
                mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
              ).thenReturn(Future(singleBusinessIncome))

              when(mockOptInService.availableOptInTaxYear()(any(), any(), any()))
                .thenReturn(
                  Future(Seq(TaxYear(2025, 2026)))
                )

              when(mockOptInService.saveIntent(any())(any(), any(), any()))
                .thenReturn(Future(true))

              val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)

              val request: FakeRequest[AnyContentAsFormUrlEncoded] =
                fakeRequest
                  .withFormUrlEncodedBody(
                    SingleTaxYearOptInWarningForm.choiceField -> "true"
                  )

              val result = testController.submit(isAgent)(request)

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(controllers.optIn.routes.ConfirmTaxYearController.show(isAgent).url)
            }
          }

          "user answers No - false" should {

            "handle the submit request and redirect to the Opt In Cancelled page"  in {
              enable(ReportingFrequencyPage)

              val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)

              setupMockSuccess(mtdRole)

              when(
                mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
              ).thenReturn(Future(singleBusinessIncome))

              when(mockOptInService.availableOptInTaxYear()(any(), any(), any()))
                .thenReturn(
                  Future(Seq(TaxYear(2025, 2026)))
                )

              when(mockOptInService.saveIntent(any())(any(), any(), any()))
                .thenReturn(Future(true))

              val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)

              val request: FakeRequest[AnyContentAsFormUrlEncoded] =
                fakeRequest
                  .withFormUrlEncodedBody(
                    SingleTaxYearOptInWarningForm.choiceField -> "false"
                  )

              val result = testController.submit(isAgent)(request)

              val expectedUrl =
                if (isAgent) controllers.optIn.routes.OptInCancelledController.showAgent().url
                else controllers.optIn.routes.OptInCancelledController.show().url

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedUrl)
            }
          }

          "does not select an answer" should {

            "handle the submit request and return the page with an error summary" in {
              enable(ReportingFrequencyPage)

              val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)

              setupMockSuccess(mtdRole)

              when(
                mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
              ).thenReturn(Future(singleBusinessIncome))

              when(mockOptInService.availableOptInTaxYear()(any(), any(), any()))
                .thenReturn(
                  Future(Seq(TaxYear(2025, 2026)))
                )

              when(mockOptInService.saveIntent(any())(any(), any(), any()))
                .thenReturn(Future(true))

              val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)

              val request: FakeRequest[AnyContentAsFormUrlEncoded] =
                fakeRequest
                  .withFormUrlEncodedBody("" -> "")

              val result = testController.submit(isAgent)(request)

              status(result) shouldBe BAD_REQUEST
              contentAsString(result).contains("Select yes to opt in for the 2025 to 2026 tax year") shouldBe true
            }
          }
        }

        "no tax years are returned" should {

          "redirect to ChooseYear page - OK SEE_OTHER" in {
            enable(ReportingFrequencyPage)

            val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)
            setupMockSuccess(mtdRole)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
            ).thenReturn(Future(singleBusinessIncome))


            setupMockSuccess(mtdRole)

            when(mockOptInService.availableOptInTaxYear()(any(), any(), any()))
              .thenReturn(
                Future(List())
              )

            val action = testController.submit(isAgent)
            val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)

            val result = action(fakeRequest)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.optIn.routes.ChooseYearController.show(isAgent).url)
          }
        }

        "show the Error Template view" when {

          "there is some unexpected error" should {

            "recover and return error template Internal Server Error - 500" in {
              enable(ReportingFrequencyPage)

              val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)
              setupMockSuccess(mtdRole)

              when(
                mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
              ).thenReturn(Future(singleBusinessIncome))


              setupMockSuccess(mtdRole)

              when(mockOptInService.availableOptInTaxYear()(any(), any(), any()))
                .thenReturn(
                  Future(throw new Exception("some fake error"))
                )

              val action = testController.submit(isAgent)
              val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)

              val result = action(fakeRequest)

              status(result) shouldBe INTERNAL_SERVER_ERROR
              contentAsString(result).contains("Sorry, there is a problem with the service") shouldBe true
            }
          }
        }

        "render the home page" when {

          "the ReportingFrequencyPage feature switch is disabled" in {
            disable(ReportingFrequencyPage)
            setupMockSuccess(mtdRole)
            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
            ).thenReturn(Future(singleBusinessIncome))

            val action = testController.submit(isAgent)
            val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)

            val result = action(fakeRequest)

            val redirectUrl = if (isAgent) {
              "/report-quarterly/income-and-expenses/view/agents/client-income-tax"
            } else {
              "/report-quarterly/income-and-expenses/view"
            }

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
      }

      val action = testController.submit(isAgent)
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
      testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
    }
  }
}
