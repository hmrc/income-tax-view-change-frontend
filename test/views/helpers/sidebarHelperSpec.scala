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

package views.helpers

import assets.Messages.{Sidebar => messages}
import assets.TestConstants._
import auth.MtdItUser
import config.FrontendAppConfig
import models.IncomeSourcesModel
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.twirl.api.Html
import utils.TestSupport
import views.html.helpers.sidebarHelper

class sidebarHelperSpec extends TestSupport {

  lazy val appConfig = app.injector.instanceOf[FrontendAppConfig]

  def html(page: Page, user: MtdItUser, taxYear: Option[Int], incomeSourcesModel: IncomeSourcesModel): Html =
    sidebarHelper(page, incomeSourcesModel, taxYear)(
      applicationMessages,
      appConfig,
      user
    )

  "The sidebarHelper" when {

    "being rendered for the Estimates page" when {

      "the user has estimates for multiple tax years" when {

        "on the earlier Tax Year" should {

          "render a Future Tax Years section" which {

            lazy val page = html(EstimatesPage, testMtdItUser, Some(2018), IncomeSourceDetails.bothIncomeSourceSuccessMisalignedTaxYear)
            lazy val document = Jsoup.parse(page.body)

            s"has a h3 heading '${messages.estimatesHeading}'" in {
              document.getElementById("estimates-heading").text() shouldBe messages.estimatesHeading
            }

            "has an Estimates link to 2019 that" should {

              s"have the text '${messages.estimatesLinkYear(2019)}'" in {
                document.getElementById("estimate-link-2019").text() shouldBe messages.estimatesLinkYear(2019)
              }

              s"have a link to '${controllers.routes.FinancialDataController.getFinancialData(2019)}'" in {
                document.getElementById("estimate-link-2019").attr("href") shouldBe controllers.routes.FinancialDataController.getFinancialData(2019).url
              }
            }
          }

          "render a Previous Tax Years section" which {

            lazy val page = html(EstimatesPage, testMtdItUser, Some(2019), IncomeSourceDetails.bothIncomeSourceSuccessMisalignedTaxYear)
            lazy val document = Jsoup.parse(page.body)

            s"has a h3 heading '${messages.previousTaxYearsHeading}'" in {
              document.getElementById("previous-years-heading").text() shouldBe messages.previousTaxYearsHeading
            }

            "has a View annual returns link" should {

              s"have the text '${messages.selfAssessmentLink}" in {
                document.getElementById("sa-link").text() shouldBe messages.selfAssessmentLink
              }

              s"have a link to '${appConfig.businessTaxAccount}/self-assessment'" in {
                document.getElementById("sa-link").attr("href") shouldBe s"${appConfig.businessTaxAccount}/self-assessment"
              }

            }
          }
        }

        "on the later Tax Year should" should {

          "render a Previous Tax Years section" which {

            lazy val page = html(EstimatesPage, testMtdItUser, Some(2019), IncomeSourceDetails.bothIncomeSourceSuccessMisalignedTaxYear)
            lazy val document = Jsoup.parse(page.body)

            s"has a h3 heading '${messages.previousTaxYearsHeading}'" in {
              document.getElementById("previous-years-heading").text() shouldBe messages.previousTaxYearsHeading
            }

            "has an Estimates link to 2018 that" should {

              s"have the text '${messages.estimatesLinkYear(2018)}'" in {
                document.getElementById("estimate-link-2018").text() shouldBe messages.estimatesLinkYear(2018)
              }

              s"have a link to '${controllers.routes.FinancialDataController.getFinancialData(2019)}'" in {
                document.getElementById("estimate-link-2018").attr("href") shouldBe controllers.routes.FinancialDataController.getFinancialData(2018).url
              }
            }

            "has a View annual returns link" should {

              s"have the text '${messages.selfAssessmentLink}" in {
                document.getElementById("sa-link").text() shouldBe messages.selfAssessmentLink
              }

              s"have a link to '${appConfig.businessTaxAccount}/self-assessment'" in {
                document.getElementById("sa-link").attr("href") shouldBe s"${appConfig.businessTaxAccount}/self-assessment"
              }

            }
          }
        }
      }

      "the user has estimates for a single tax year" when {

        "render a Previous Tax Years section" which {

          lazy val page = html(EstimatesPage, testMtdItUser, Some(2019), IncomeSourceDetails.bothIncomeSourceSuccessMisalignedTaxYear)
          lazy val document = Jsoup.parse(page.body)

          s"has a h3 heading '${messages.previousTaxYearsHeading}'" in {
            document.getElementById("previous-years-heading").text() shouldBe messages.previousTaxYearsHeading
          }

          "has a View annual returns link" should {

            s"have the text '${messages.selfAssessmentLink}" in {
              document.getElementById("sa-link").text() shouldBe messages.selfAssessmentLink
            }

            s"have a link to '${appConfig.businessTaxAccount}/self-assessment'" in {
              document.getElementById("sa-link").attr("href") shouldBe s"${appConfig.businessTaxAccount}/self-assessment"
            }

          }
        }
      }

      "being viewed for any tax year has a link to view Obligations" should {

        lazy val page = html(EstimatesPage, testMtdItUser, Some(2019), IncomeSourceDetails.bothIncomeSourceSuccessMisalignedTaxYear)
        lazy val document = Jsoup.parse(page.body)

        s"has a h3 heading '${messages.reportsHeading}'" in {
          document.getElementById("obligations-heading").text() shouldBe messages.reportsHeading
        }

        "has a View Obligations link that" should {

          s"have the text '${messages.reportsLink}" in {
            document.getElementById("obligations-link").text() shouldBe messages.reportsLink
          }

          s"have a link to '${controllers.routes.ObligationsController.getObligations().url}/self-assessment'" in {
            document.getElementById("obligations-link").attr("href") shouldBe controllers.routes.ObligationsController.getObligations().url
          }

        }
      }
    }

    "being rendered for the Obligations page" when {

      "the user has estimates for multiple tax years" should {

          "render an Estimates section" which {

            lazy val page = html(ObligationsPage, testMtdItUser, None, IncomeSourceDetails.bothIncomeSourceSuccessMisalignedTaxYear)
            lazy val document = Jsoup.parse(page.body)

            s"has a h3 heading '${messages.estimatesHeading}'" in {
              document.getElementById("estimates-heading").text() shouldBe messages.estimatesHeading
            }

            "has an Estimates link to 2018 that" should {

              s"have the text '${messages.estimatesLinkYear(2018)}'" in {
                document.getElementById("estimate-link-2018").text() shouldBe messages.estimatesLinkYear(2018)
              }

              s"have a link to '${controllers.routes.FinancialDataController.getFinancialData(2018)}'" in {
                document.getElementById("estimate-link-2018").attr("href") shouldBe controllers.routes.FinancialDataController.getFinancialData(2018).url
              }
            }

            "has an Estimates link to 2019 that" should {

              s"have the text '${messages.estimatesLinkYear(2019)}'" in {
                document.getElementById("estimate-link-2019").text() shouldBe messages.estimatesLinkYear(2019)
              }

              s"have a link to '${controllers.routes.FinancialDataController.getFinancialData(2019)}'" in {
                document.getElementById("estimate-link-2019").attr("href") shouldBe controllers.routes.FinancialDataController.getFinancialData(2019).url
              }
            }
          }

          "render a Previous Tax Years section" which {

            lazy val page = html(EstimatesPage, testMtdItUser, Some(2019), IncomeSourceDetails.bothIncomeSourceSuccessMisalignedTaxYear)
            lazy val document = Jsoup.parse(page.body)

            s"has a h3 heading '${messages.previousTaxYearsHeading}'" in {
              document.getElementById("previous-years-heading").text() shouldBe messages.previousTaxYearsHeading
            }

            "has a View annual returns link" should {

              s"have the text '${messages.selfAssessmentLink}" in {
                document.getElementById("sa-link").text() shouldBe messages.selfAssessmentLink
              }

              s"have a link to '${appConfig.businessTaxAccount}/self-assessment'" in {
                document.getElementById("sa-link").attr("href") shouldBe s"${appConfig.businessTaxAccount}/self-assessment"
              }

            }
          }
        }

      "the user has estimates for a single tax year" should  {

        "render an Estimates section" which {

          lazy val page = html(ObligationsPage, testMtdItUser, None, IncomeSourceDetails.propertyIncomeSourceSuccess)
          lazy val document = Jsoup.parse(page.body)

          s"has a h3 heading '${messages.estimatesHeading}'" in {
            document.getElementById("estimates-heading").text() shouldBe messages.estimatesHeading
          }

          "has an Estimates link to 2018 that" should {

            s"have the text '${messages.estimatesLinkYear(2018)}'" in {
              document.getElementById("estimate-link-2018").text() shouldBe messages.estimatesLinkYear(2018)
            }

            s"have a link to '${controllers.routes.FinancialDataController.getFinancialData(2019)}'" in {
              document.getElementById("estimate-link-2018").attr("href") shouldBe controllers.routes.FinancialDataController.getFinancialData(2018).url
            }
          }
        }

        "render a Previous Tax Years section" which {

          lazy val page = html(EstimatesPage, testMtdItUser, Some(2019), IncomeSourceDetails.bothIncomeSourceSuccessMisalignedTaxYear)
          lazy val document = Jsoup.parse(page.body)

          s"has a h3 heading '${messages.previousTaxYearsHeading}'" in {
            document.getElementById("previous-years-heading").text() shouldBe messages.previousTaxYearsHeading
          }

          "has a View annual returns link" should {

            s"have the text '${messages.selfAssessmentLink}" in {
              document.getElementById("sa-link").text() shouldBe messages.selfAssessmentLink
            }

            s"have a link to '${appConfig.businessTaxAccount}/self-assessment'" in {
              document.getElementById("sa-link").attr("href") shouldBe s"${appConfig.businessTaxAccount}/self-assessment"
            }
          }
        }
      }
    }

    "being viewed for either obligations or estimates regardless of Tax Year" should {

      "have a Income Tax Reference section on obligations page" which {

        lazy val page = html(ObligationsPage, testMtdItUser, None, IncomeSourceDetails.propertyIncomeSourceSuccess)
        lazy val document = Jsoup.parse(page.body)

        s"has the h3 heading '${messages.mtditidHeading}'" in {
          document.getElementById("it-reference-heading").text() shouldBe messages.mtditidHeading
        }

        s"has the users Income Tax Reference '${testMtdItUser.mtditid}'" in {
          document.getElementById("it-reference").text() shouldBe testMtdItUser.mtditid
        }

      }

      "have a Income Tax Reference section on Estimates page" which {

        lazy val page = html(EstimatesPage, testMtdItUser, None, IncomeSourceDetails.propertyIncomeSourceSuccess)
        lazy val document = Jsoup.parse(page.body)

        s"has the h3 heading '${messages.mtditidHeading}'" in {
          document.getElementById("it-reference-heading").text() shouldBe messages.mtditidHeading
        }

        s"has the users Income Tax Reference '${testMtdItUser.mtditid}'" in {
          document.getElementById("it-reference").text() shouldBe testMtdItUser.mtditid
        }
      }
    }
  }
}

