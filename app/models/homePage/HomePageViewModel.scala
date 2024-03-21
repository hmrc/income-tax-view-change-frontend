package models.homePage

import models.nextUpdates.NextUpdatesTileViewModel

import java.time.LocalDate

case class HomePageViewModel(
                            isAgent: Boolean,
                            utr: Option[String],
                            nextPaymentsTileViewModel: NextPaymentsTileViewModel,
                            returnsTileViewModel: ReturnsTileViewModel,
                            nextUpdatesTileViewModel: NextUpdatesTileViewModel,
                            paymentCreditAndRefundHistoryTileViewModel: PaymentCreditAndRefundHistoryTileViewModel,
                            yourBusinessesTileViewModel: YourBusinessesTileViewModel,
                            dunningLockExists: Boolean = false,
                            origin: Option[String] = None
                            )

case class NextPaymentsTileViewModel(nextPaymentDueDate: Option[LocalDate], overDuePaymentsCount: Option[Int])

case class ReturnsTileViewModel(currentTaxYear: Int, ITSASubmissionIntegrationEnabled: Boolean)

case class YourBusinessesTileViewModel(displayCeaseAnIncome: Boolean, incomeSourcesEnabled: Boolean,
                                       incomeSourcesNewJourneyEnabled: Boolean)