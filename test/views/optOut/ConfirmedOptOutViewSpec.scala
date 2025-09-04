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

package views.optOut

import config.FrontendAppConfig
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.Voluntary
import models.optout._
import org.jsoup.Jsoup
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import services.optout._
import testUtils.TestSupport
import views.html.optOut.ConfirmedOptOutView

class ConfirmedOptOutViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val confirmedOptOutView: ConfirmedOptOutView = app.injector.instanceOf[ConfirmedOptOutView]

  val taxYear: TaxYear = TaxYear.forYearEnd(2024)
  val optOutTaxYear: OptOutTaxYear = CurrentOptOutTaxYear(Voluntary, taxYear)
  val nextUpdatesUrl = controllers.routes.NextUpdatesController.show().url

  object Selectors {

    val greenPanel = "out-opt-complete-green-panel"

    val h2SubmitYourTaxReturn = "submit-your-tax-return-heading"
    val submitTaxReturnParagraph1 = "now-you-have-opted-out"
    val submitTaxReturnParagraph2 = "tax-year-reporting-quarterly"
    val selfAssessmentTaxReturnLink = "self-assessment-tax-return-link"
    val compatibleSoftwareLink = "compatible-software-link"

    val yourReportingObligationsHeading = "your-reporting-obligations-heading"

    val useMtdInFuture = "use-mtd-in-future"
    val requiredToUseMtdInset = "required-to-use-mtd-inset"
    val thisIsBecause = "this-could-be-because"

    val yourObligationsBullet: Int => String = (i: Int) => s"your-obligations-bullet-$i"
    val grossIncomeThresholdWarning = "gross-income-threshold-warning"
    val weWillLetYouKnow = "we-will-let-you-know"
    val youCanCheckThresholds = "you-can-check-thresholds"

    val revisedDeadlinesHeading = "revised-deadlines-heading"
    val revisedDeadlinesP1 = "revised-deadlines-p1"
    val yourReportingFrequencyBlock = "your-reporting-frequency-block"
    val yourReportingFrequencyLink = "your-reporting-frequency-link"

    val viewUpcomingUpdatesLink = "view-upcoming-updates-link"
    val reportingUpdateBlock = "reporting-updates"
  }

  "ConfirmedOptOutView" when {

    "Scenario1" when {

      val isAgent = false

      val viewModel = ConfirmedOptOutViewModel(optOutTaxYear = optOutTaxYear.taxYear, state = Some(OneYearOptOutFollowedByMandated))
      val pageDocument = Jsoup.parse(contentAsString(confirmedOptOutView(
        viewModel = viewModel,
        isAgent = isAgent,
        showReportingFrequencyContent = true,
        confirmedOptOutViewScenarios = Scenario1Content,
        selfAssessmentTaxReturnLink = mockAppConfig.selfAssessmentTaxReturnLink,
        compatibleSoftwareLink = mockAppConfig.compatibleSoftwareLink,
      )))

      "show the green panel" in {

        pageDocument.getElementById(Selectors.greenPanel).text() shouldBe "Opt out completed You no longer need to use Making Tax Digital for Income Tax"
      }

      "show the revised details section" in {

        pageDocument.getElementById(Selectors.revisedDeadlinesHeading).text() shouldBe "Your revised deadlines"
        pageDocument.getElementById(Selectors.revisedDeadlinesP1).text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025."
        pageDocument.getElementById(Selectors.viewUpcomingUpdatesLink).text() shouldBe "View your upcoming deadlines"
        pageDocument.getElementById(Selectors.yourReportingFrequencyBlock).text() shouldBe "You can decide at any time to sign back up to Making Tax Digital for Income Tax for all of your businesses on your reporting obligations page."
      }

      "show the submit your tax return section" in {

        pageDocument.getElementById(Selectors.h2SubmitYourTaxReturn).text() shouldBe "Submit your tax return"
        pageDocument.getElementById(Selectors.submitTaxReturnParagraph1).text() shouldBe "Now you have opted out, you will need to go back to the way you have previously filed your Self Assessment tax return (opens in new tab)."

        pageDocument.getElementById(Selectors.selfAssessmentTaxReturnLink).text() shouldBe "filed your Self Assessment tax return (opens in new tab)."
      }

      "show the 'Your reporting obligations' section" in {

        pageDocument.getElementById(Selectors.yourReportingObligationsHeading).text() shouldBe "Your reporting obligations in the future"
        pageDocument.getElementById(Selectors.useMtdInFuture).text() shouldBe "You could be required to use Making Tax Digital for Income Tax again in the future if:"
        pageDocument.getElementById(Selectors.yourObligationsBullet(1)).text() shouldBe "HMRC lowers the income threshold for it"
        pageDocument.getElementById(Selectors.yourObligationsBullet(2)).text() shouldBe "you report an increase in your qualifying income in a tax return"
        pageDocument.getElementById(Selectors.grossIncomeThresholdWarning).text() shouldBe "For example, if your income from self-employment or property, or both, exceeds the £50,000 threshold in the 2024 to 2025 tax year, you would have to use Making Tax Digital for Income Tax from 6 April 2026."
        pageDocument.getElementById(Selectors.weWillLetYouKnow).text() shouldBe "If this happens, we will write to you to let you know."
        pageDocument.getElementById(Selectors.youCanCheckThresholds).text() shouldBe "You can check the threshold for qualifying income in the criteria for people who will need to sign up for Making Tax Digital for Income Tax (opens in new tab)."
      }
    }

    "Scenario2" when {

      val isAgent = false

      val viewModel = ConfirmedOptOutViewModel(optOutTaxYear = optOutTaxYear.taxYear, state = Some(OneYearOptOutFollowedByMandated))

      val pageDocument = Jsoup.parse(contentAsString(confirmedOptOutView(
        viewModel = viewModel,
        isAgent = isAgent,
        showReportingFrequencyContent = true,
        confirmedOptOutViewScenarios = Scenario2Content,
        selfAssessmentTaxReturnLink = mockAppConfig.compatibleSoftwareLink,
        compatibleSoftwareLink = mockAppConfig.selfAssessmentTaxReturnLink
      )))

      "show the green panel" in {

        pageDocument.getElementById(Selectors.greenPanel).text() shouldBe "Opt out completed You do not need to use Making Tax Digital for Income Tax for the 2023 to 2024 tax year"
      }

      "show the revised details section" in {

        pageDocument.getElementById(Selectors.revisedDeadlinesHeading).text() shouldBe "Your revised deadlines"
        pageDocument.getElementById(Selectors.revisedDeadlinesP1).text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025."
        pageDocument.getElementById(Selectors.viewUpcomingUpdatesLink).text() shouldBe "View your upcoming deadlines"
        pageDocument.getElementById(Selectors.yourReportingFrequencyBlock).text() shouldBe "You can decide at any time to sign back up to Making Tax Digital for Income Tax for all of your businesses on your reporting obligations page."
        pageDocument.getElementById(Selectors.yourReportingFrequencyLink).attr("href") shouldBe controllers.routes.ReportingFrequencyPageController.show(false).url
      }

      "show the submit your tax return section" in {

        pageDocument.getElementById(Selectors.h2SubmitYourTaxReturn).text() shouldBe "Submit your tax return"
        pageDocument.getElementById(Selectors.submitTaxReturnParagraph1).text() shouldBe "Now you have opted out, you will need to go back to the way you have previously filed your Self Assessment tax return (opens in new tab)."
        pageDocument.getElementById(Selectors.submitTaxReturnParagraph2).text() shouldBe "For any tax year you are reporting quarterly, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."

        pageDocument.getElementById(Selectors.selfAssessmentTaxReturnLink).text() shouldBe "filed your Self Assessment tax return (opens in new tab)."
        pageDocument.getElementById(Selectors.compatibleSoftwareLink).text() shouldBe "software compatible with Making Tax Digital for Income Tax (opens in new tab)."

      }

      "show the 'Your reporting obligations' section" in {

        pageDocument.getElementById(Selectors.yourReportingObligationsHeading).text() shouldBe "Your reporting obligations from the next tax year onwards"
        pageDocument.getElementById(Selectors.requiredToUseMtdInset).text() shouldBe "From 6 April 2024, you will be required to use Making Tax Digital for Income tax."
        pageDocument.getElementById(Selectors.thisIsBecause).text() shouldBe "This could be because:"
        pageDocument.getElementById(Selectors.yourObligationsBullet(1)).text() shouldBe "HMRC lowers the income threshold for it"
        pageDocument.getElementById(Selectors.yourObligationsBullet(2)).text() shouldBe "you report an increase in your qualifying income in a tax return"
        pageDocument.getElementById(Selectors.youCanCheckThresholds).text() shouldBe "You can check the threshold for qualifying income in the criteria for people who will need to sign up for Making Tax Digital for Income Tax (opens in new tab)."
      }
    }

    "Scenario3" when {

      val isAgent = false

      val viewModel = ConfirmedOptOutViewModel(optOutTaxYear = optOutTaxYear.taxYear, state = Some(OneYearOptOutFollowedByMandated))
      val pageDocument = Jsoup.parse(contentAsString(confirmedOptOutView(
        viewModel = viewModel,
        isAgent = isAgent,
        showReportingFrequencyContent = true,
        confirmedOptOutViewScenarios = Scenario3Content,
        selfAssessmentTaxReturnLink = mockAppConfig.selfAssessmentTaxReturnLink,
        compatibleSoftwareLink = mockAppConfig.compatibleSoftwareLink,
      )))

      "show the green panel" in {

        pageDocument.getElementById(Selectors.greenPanel).text() shouldBe "Opt out completed From the 2023 to 2024 tax year onwards you do not need to use Making Tax Digital for Income Tax"
      }

      "show the revised details section" in {

        pageDocument.getElementById(Selectors.revisedDeadlinesHeading).text() shouldBe "Your revised deadlines"
        pageDocument.getElementById(Selectors.revisedDeadlinesP1).text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025."
        pageDocument.getElementById(Selectors.viewUpcomingUpdatesLink).text() shouldBe "View your upcoming deadlines"
        pageDocument.getElementById(Selectors.yourReportingFrequencyBlock).text() shouldBe "You can decide at any time to sign back up to Making Tax Digital for Income Tax for all of your businesses on your reporting obligations page."
      }

      "show the submit your tax return section" in {

        pageDocument.getElementById(Selectors.h2SubmitYourTaxReturn).text() shouldBe "Submit your tax return"
        pageDocument.getElementById(Selectors.submitTaxReturnParagraph1).text() shouldBe "Now you have opted out, you will need to go back to the way you have previously filed your Self Assessment tax return (opens in new tab)."

        pageDocument.getElementById(Selectors.selfAssessmentTaxReturnLink).text() shouldBe "filed your Self Assessment tax return (opens in new tab)."
        pageDocument.getElementById(Selectors.compatibleSoftwareLink).text() shouldBe "software compatible with Making Tax Digital for Income Tax (opens in new tab)."
      }

      "show the 'Your reporting obligations' section" in {

        pageDocument.getElementById(Selectors.yourReportingObligationsHeading).text() shouldBe "Your reporting obligations in the future"

        pageDocument.getElementById(Selectors.useMtdInFuture).text() shouldBe "You are opted out from next tax year onwards, but you could be required to use Making Tax Digital for Income Tax again in the future if:"
        pageDocument.getElementById(Selectors.yourObligationsBullet(1)).text() shouldBe "HMRC lowers the income threshold for it"
        pageDocument.getElementById(Selectors.yourObligationsBullet(2)).text() shouldBe "you report an increase in your qualifying income in a tax return"
        pageDocument.getElementById(Selectors.grossIncomeThresholdWarning).text() shouldBe "For example, if your total gross income from self-employment or property, or both, exceeds the £50,000 threshold in the 2024 to 2025 tax year, you would have to use Making Tax Digital for Income Tax from 6 April 2026."
        pageDocument.getElementById(Selectors.weWillLetYouKnow).text() shouldBe "If this happens, we will write to you to let you know."
        pageDocument.getElementById(Selectors.youCanCheckThresholds).text() shouldBe "You can check the threshold for qualifying income in the criteria for people who will need to sign up for Making Tax Digital for Income Tax (opens in new tab)."
      }
    }

    "Scenario4" when {

      val isAgent = false

      val viewModel = ConfirmedOptOutViewModel(optOutTaxYear = optOutTaxYear.taxYear, state = Some(OneYearOptOutFollowedByMandated))
      val pageDocument = Jsoup.parse(contentAsString(confirmedOptOutView(
        viewModel = viewModel,
        isAgent = isAgent,
        showReportingFrequencyContent = true,
        confirmedOptOutViewScenarios = Scenario4Content,
        selfAssessmentTaxReturnLink = mockAppConfig.selfAssessmentTaxReturnLink,
        compatibleSoftwareLink = mockAppConfig.compatibleSoftwareLink,
      )))

      "show the green panel" in {

        pageDocument.getElementById(Selectors.greenPanel).text() shouldBe "Opt out completed You no longer need to use Making Tax Digital for Income Tax"
      }

      "show the revised details section" in {

        pageDocument.getElementById(Selectors.revisedDeadlinesHeading).text() shouldBe "Your revised deadlines"
        pageDocument.getElementById(Selectors.revisedDeadlinesP1).text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025."
        pageDocument.getElementById(Selectors.viewUpcomingUpdatesLink).text() shouldBe "View your upcoming deadlines"
        pageDocument.getElementById(Selectors.yourReportingFrequencyBlock).text() shouldBe "You can decide at any time to sign back up to Making Tax Digital for Income Tax for all of your businesses on your reporting obligations page."
      }

      "show the submit your tax return section" in {

        pageDocument.getElementById(Selectors.h2SubmitYourTaxReturn).text() shouldBe "Submit your tax return"
        pageDocument.getElementById(Selectors.submitTaxReturnParagraph1).text() shouldBe "As you are opted out, you can find out here how to file your Self Assessment tax return (opens in new tab)."
        pageDocument.getElementById(Selectors.selfAssessmentTaxReturnLink).text() shouldBe "file your Self Assessment tax return (opens in new tab)."
      }

      "show the 'Your reporting obligations' section" in {

        pageDocument.getElementById(Selectors.yourReportingObligationsHeading).text() shouldBe "Your reporting obligations in the future"

        pageDocument.getElementById(Selectors.useMtdInFuture).text() shouldBe "You could be required to use Making Tax Digital for Income Tax again in the future if:"
        pageDocument.getElementById(Selectors.yourObligationsBullet(1)).text() shouldBe "HMRC lowers the income threshold for it"
        pageDocument.getElementById(Selectors.yourObligationsBullet(2)).text() shouldBe "you report an increase in your qualifying income in a tax return"
        pageDocument.getElementById(Selectors.grossIncomeThresholdWarning).text() shouldBe "For example, if your total gross income from self-employment or property, or both, exceeds the £50,000 threshold in the 2024 to 2025 tax year, you would have to use Making Tax Digital for Income Tax from 6 April 2026."
        pageDocument.getElementById(Selectors.weWillLetYouKnow).text() shouldBe "If this happens, we will write to you to let you know."
        pageDocument.getElementById(Selectors.youCanCheckThresholds).text() shouldBe "You can check the threshold for qualifying income in the criteria for people who will need to sign up for Making Tax Digital for Income Tax (opens in new tab)."
      }
    }


    "Scenario5" when {

      val isAgent = false

      val viewModel = ConfirmedOptOutViewModel(optOutTaxYear = optOutTaxYear.taxYear, state = Some(OneYearOptOutFollowedByMandated))
      val pageDocument = Jsoup.parse(contentAsString(confirmedOptOutView(
        viewModel = viewModel,
        isAgent = isAgent,
        showReportingFrequencyContent = true,
        confirmedOptOutViewScenarios = Scenario5Content,
        selfAssessmentTaxReturnLink = mockAppConfig.selfAssessmentTaxReturnLink,
        compatibleSoftwareLink = mockAppConfig.compatibleSoftwareLink,
      )))

      "show the green panel" in {

        pageDocument.getElementById(Selectors.greenPanel).text() shouldBe "Opt out completed You no longer need to use Making Tax Digital for Income Tax"
      }

      "show the revised details section" in {

        pageDocument.getElementById(Selectors.revisedDeadlinesHeading).text() shouldBe "Your revised deadlines"
        pageDocument.getElementById(Selectors.revisedDeadlinesP1).text() shouldBe "Your tax return for the 2023 to 2024 tax year is due by 31 January 2025."
        pageDocument.getElementById(Selectors.viewUpcomingUpdatesLink).text() shouldBe "View your upcoming deadlines"
        pageDocument.getElementById(Selectors.yourReportingFrequencyBlock).text() shouldBe "You can decide at any time to sign back up to Making Tax Digital for Income Tax for all of your businesses on your reporting obligations page."
      }

      "show the submit your tax return section" in {

        pageDocument.getElementById(Selectors.h2SubmitYourTaxReturn).text() shouldBe "Submit your tax return"
        pageDocument.getElementById(Selectors.submitTaxReturnParagraph1).text() shouldBe "Now you have opted out, you will need to go back to the way you have previously filed your Self Assessment tax return (opens in new tab)."
        pageDocument.getElementById(Selectors.selfAssessmentTaxReturnLink).text() shouldBe "filed your Self Assessment tax return (opens in new tab)."
      }

      "show the 'Your reporting obligations' section" in {

        pageDocument.getElementById(Selectors.yourReportingObligationsHeading).text() shouldBe "Your reporting obligations in the future"

        pageDocument.getElementById(Selectors.useMtdInFuture).text() shouldBe "You could be required to use Making Tax Digital for Income Tax again in the future if:"
        pageDocument.getElementById(Selectors.yourObligationsBullet(1)).text() shouldBe "HMRC lowers the income threshold for it"
        pageDocument.getElementById(Selectors.yourObligationsBullet(2)).text() shouldBe "you report an increase in your qualifying income in a tax return"
        pageDocument.getElementById(Selectors.grossIncomeThresholdWarning).text() shouldBe "For example, if your income from self-employment or property, or both, exceeds the £50,000 threshold in the 2024 to 2025 tax year, you would have to use Making Tax Digital for Income Tax from 6 April 2026."
        pageDocument.getElementById(Selectors.weWillLetYouKnow).text() shouldBe "If this happens, we will write to you to let you know."
        pageDocument.getElementById(Selectors.youCanCheckThresholds).text() shouldBe "You can check the threshold for qualifying income in the criteria for people who will need to sign up for Making Tax Digital for Income Tax (opens in new tab)."
      }
    }
  }
}
