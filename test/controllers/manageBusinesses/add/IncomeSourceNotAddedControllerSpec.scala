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

package controllers.manageBusinesses.add

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import org.jsoup.Jsoup
import org.mockito.Mockito.mock
import play.api
import play.api.Application
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.CreateBusinessDetailsService
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future

class IncomeSourceNotAddedControllerSpec extends MockAuthActions {

  lazy val mockBusinessDetailsService: CreateBusinessDetailsService = mock(classOf[CreateBusinessDetailsService])

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[CreateBusinessDetailsService].toInstance(mockBusinessDetailsService)
    ).build()

  lazy val testController = app.injector.instanceOf[IncomeSourceNotAddedController]

  val title: String = messages("incomeSources.add.error.standardError")
  val titleAgent: String = s"${messages("htmlTitle.agent", messages("incomeSources.add.error.standardError"))}"
  val titleIndividual: String = s"${messages("htmlTitle", title)}"


  lazy val errorUrlSE: String = controllers.manageBusinesses.add.routes.IncomeSourceNotAddedController.show(incomeSourceType = SelfEmployment).url
  lazy val agentErrorUrlSE: String = controllers.manageBusinesses.add.routes.IncomeSourceNotAddedController.showAgent(incomeSourceType = SelfEmployment).url
  lazy val errorUrlUK: String = controllers.manageBusinesses.add.routes.IncomeSourceNotAddedController.show(incomeSourceType = UkProperty).url
  lazy val agentErrorUrlUK: String = controllers.manageBusinesses.add.routes.IncomeSourceNotAddedController.showAgent(incomeSourceType = UkProperty).url
  lazy val errorUrlFP: String = controllers.manageBusinesses.add.routes.IncomeSourceNotAddedController.show(incomeSourceType = ForeignProperty).url
  lazy val agentErrorUrlFP: String = controllers.manageBusinesses.add.routes.IncomeSourceNotAddedController.showAgent(incomeSourceType = ForeignProperty).url

  val incomeSourceTypes: List[IncomeSourceType] = List(SelfEmployment, UkProperty, ForeignProperty)

  def mockIncomeSource(incomeSourceType: IncomeSourceType) = {
    incomeSourceType match {
      case SelfEmployment => mockBusinessIncomeSource()
      case UkProperty => mockUKPropertyIncomeSource()
      case ForeignProperty => mockForeignPropertyIncomeSource()
    }
  }

  def paragraphText(incomeSourceType: IncomeSourceType) = {
    incomeSourceType match {
      case SelfEmployment => messages("incomeSources.add.error.incomeSourceNotSaved.p1", "sole trader")
      case UkProperty => messages("incomeSources.add.error.incomeSourceNotSaved.p1", "UK property")
      case ForeignProperty => messages("incomeSources.add.error.incomeSourceNotSaved.p1", "foreign property")
    }
  }

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    incomeSourceTypes.foreach { incomeSourceType =>
      s"show${if (mtdRole != MTDIndividual) "Agent"}($incomeSourceType)" when {
        val action = if (mtdRole == MTDIndividual) testController.show(incomeSourceType) else testController.showAgent(incomeSourceType)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        s"the user is authenticated as a $mtdRole" should {
          "return 200 and render Income Source Not Added Error Page" when {
            "user is trying to add SE business" in {
              setupMockSuccess(mtdRole)
              mockIncomeSource(incomeSourceType)

              val result: Future[Result] = action(fakeRequest)

              val document = Jsoup.parse(contentAsString(result))

              status(result) shouldBe OK
              document.title shouldBe {if (isAgent) titleAgent else titleIndividual}
              document.getElementById("paragraph-1").text() shouldBe paragraphText(incomeSourceType)
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}