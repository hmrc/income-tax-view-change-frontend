package services

import auth.MtdItUser
import authV2.AuthActionsTestData.defaultMTDITUser
import models.creditsandrefunds.CreditsModel
import models.financialDetails.ChargeItem
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.newHomePage.SubmissionDeadlinesViewModel
import models.obligations.{SingleObligationModel, StatusOpen}
import testConstants.BaseTestConstants.testNino
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

import java.time.LocalDate

class HandleYourTasksServiceSpec extends TestSupport {

  val service = new HandleYourTasksService(appConfig)

  def singleObligation(currentDate: LocalDate, isQuarterly: Boolean) = {
    val obligationType = if(isQuarterly) "Quarterly" else "Crystallisation"
    SingleObligationModel(
      currentDate,
      currentDate.plusYears(1),
      currentDate.plusYears(2),
      obligationType,
      None,
      "periodKey",
      StatusOpen
    )
  }

  def creditsModel(moneyInAccount: BigDecimal): CreditsModel = CreditsModel(0, 0, 0, moneyInAccount, None, None, List.empty)

  class TestSetup(isAgent: Boolean,
                  supportingAgent: Boolean,
                  hasUpcomingQuarterlyDate: Boolean,
                  hasUpcomingAnnualDate: Boolean,
                  creditsRefundsRepayEnabled: Boolean,
                  credits: CreditsModel = creditsModel(0),
                  currentItsaStatus: ITSAStatus = ITSAStatus.Annual,
                  obligations: Seq[SingleObligationModel] = Seq.empty,
                  chargeItemList: List[ChargeItem] = List.empty) {

    val nextQuarterlyDueDate = if(hasUpcomingQuarterlyDate) Some(fixedDate.plusYears(3)) else None
    val nextTaxReturnDueDate = if (hasUpcomingAnnualDate) Some(fixedDate.plusYears(3)) else None

    val submissionsViewModel = SubmissionDeadlinesViewModel(obligations, fixedDate, nextQuarterlyDueDate, nextTaxReturnDueDate)

    val affinityGroup = if(isAgent) Agent else Individual
    val testUser: MtdItUser[_] = defaultMTDITUser(Some(affinityGroup), IncomeSourceDetailsModel(testNino, "123", Some("2023"), List.empty, List.empty), isSupportingAgent = supportingAgent)

    val resultViewModel = service.getYourTasksCards(submissionsViewModel, isAgent, chargeItemList, credits, creditsRefundsRepayEnabled, currentItsaStatus)(testUser)
  }

  "getYourTasksCards" should {
    "return overdue task cards" when {
      "there's an overdue charge" in {

      }
      "there's a late payment penalty" in {

      }
      "there's a late submission penalty" in {

      }
      "there's an overdue annual submission" in {

      }
      "there's an overdue quarterly submission" in {

      }
      "there's multiple overdue annual submissions" in {

      }
      "there's multiple overdue quarterly submissions" in {

      }
    }
    "return a dateless task card" when {
      "there is money in a user's account" in {

      }
    }

    "return the correct upcoming task cards" when {
      "there is a future annual submission" in {

      }

      "there is a future quarterly submission and the user's ITSA status is mandated" in {

      }
      "there is a future quarterly submission and the user's ITSA status is voluntary" in {

      }

    }
    "return the no task card" when {
      "there are no overdue, dateless or upcoming tasks" in {

      }
    }

    "not show specific task cards" when {
      "the user's ITSA status is annual and they have overdue quarterly obligations marked" in {

      }
      "the user's ITSA status is exempt and they have overdue quarterly obligations marked" in {

      }
      "the user's ITSA status is digitally exempt and they have overdue quarterly obligations marked" in {

      }
      "the user's ITSA status is annual and they have upcoming quarterly obligations marked" in {

      }
      "the user's ITSA status is exempt and they have upcoming quarterly obligations marked" in {

      }
      "the user's ITSA status is digitally exempt and they have upcoming quarterly obligations marked" in {

      }
      "the user is a supporting agent there is money in a user's account" in {

      }
      "the user is a supporting agent there's an overdue charge" in {

      }
      "the user is a supporting agent there's a late payment penalty" in {

      }
      "the user is a supporting agent there's a late submission penalty" in {

      }
      "the user has money in their account but creditsRefundsRepayEnabled is false" in {
        
      }
    }
  }
}
