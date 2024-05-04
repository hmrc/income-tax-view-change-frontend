package services.optout

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.{ITSAStatus, StatusDetail}
import models.optOut.YearStatusDetail
import services.optout.OptOutInTheDomain.{Crystallized, OptOutStatus}
import services.optout.OptOutRulesService.{toFinalized, toQuery, toSymbol}

object OptOutInTheDomain {

  val optOutService = new OptOutRulesService()
  case class OptOutOption(taxYear: TaxYear)
  case class Crystallized(value: Boolean, taxYear: TaxYear)
  case class OptOutStatus(crystallized: Crystallized,
                          previousYearItsaStatus: YearStatusDetail,
                          currentYearItsaStatus: YearStatusDetail,
                          nextYearItsaStatus: YearStatusDetail) {

    val optOutOptions: Array[OptOutOption] = {

      optOutService.findOptOutOptions(
        toQuery(toFinalized(crystallized.value),
          toSymbol(previousYearItsaStatus.statusDetail),
          toSymbol(previousYearItsaStatus.statusDetail),
          toSymbol(previousYearItsaStatus.statusDetail)
        )

      ).flatMap {
        case "PY" => List(OptOutOption(previousYearItsaStatus.taxYear))
        case "CY" => List(OptOutOption(currentYearItsaStatus.taxYear))
        case "NY" => List(OptOutOption(nextYearItsaStatus.taxYear))
        case _ => List()
      }
    }.toList.sortBy(_.taxYear.toString).toArray

    val canOptOut: Boolean = optOutOptions.nonEmpty
    def canOptOutOfYear(taxYear: TaxYear): Boolean = optOutOptions.exists(_.taxYear == taxYear)
    def asText: String =
      s"PreviousYear Crystallized:${crystallized.value}, " +
      s"PreviousYear: (${previousYearItsaStatus.taxYear} - ${previousYearItsaStatus.statusDetail.status.toString}) " +
      s"CurrentYear: (${currentYearItsaStatus.taxYear} - ${currentYearItsaStatus.statusDetail.status.toString}) " +
      s"NextYear: (${nextYearItsaStatus.taxYear} - ${nextYearItsaStatus.statusDetail.status.toString})"
  }



}

object SomeMain extends App {

  val previousYearCrystalized = Crystallized(value = false, taxYear = TaxYear(2023))
  val previousYearStatusDetail= YearStatusDetail(TaxYear(2023), StatusDetail("", ITSAStatus.Voluntary, ""))
  val currentYearStatusDetail = YearStatusDetail(TaxYear(2024), StatusDetail("", ITSAStatus.Voluntary, ""))
  val nextYearStatusDetail = YearStatusDetail(TaxYear(2025), StatusDetail("", ITSAStatus.Voluntary, ""))

  val optOutStatus = OptOutStatus(previousYearCrystalized, previousYearStatusDetail, currentYearStatusDetail, nextYearStatusDetail)

  println()
  println(optOutStatus.asText)
  println(s"User can opt-out: ${optOutStatus.canOptOut}")
  println("User can opt-out of the following years:")
  optOutStatus.optOutOptions.map(v => s"\t${v.taxYear.toString}").foreach(println)
  println(s"User can opt-out of year ${previousYearStatusDetail.taxYear}: ${optOutStatus.canOptOutOfYear(previousYearCrystalized.taxYear)}")
  println(s"User can opt-out of year ${TaxYear(2021)}: ${optOutStatus.canOptOutOfYear(TaxYear(2021))}")

  val previousYearCrystalized2 = Crystallized(value = true, taxYear = TaxYear(2023))
  val previousYearStatusDetail2= YearStatusDetail(TaxYear(2023), StatusDetail("", ITSAStatus.Mandated, ""))
  val currentYearStatusDetail2 = YearStatusDetail(TaxYear(2024), StatusDetail("", ITSAStatus.Mandated, ""))
  val nextYearStatusDetail2 = YearStatusDetail(TaxYear(2025), StatusDetail("", ITSAStatus.Mandated, ""))

  val optOutStatus2 = OptOutStatus(previousYearCrystalized2, previousYearStatusDetail2, currentYearStatusDetail2, nextYearStatusDetail2)

  println()
  println(optOutStatus2.asText)
  println(s"User can opt-out: ${optOutStatus2.canOptOut}")
  println("User can opt-out of the following years:")
  optOutStatus2.optOutOptions.map(v => s"\t$v")
  println(s"User can opt-out of year ${previousYearStatusDetail2.taxYear}: ${optOutStatus2.canOptOutOfYear(previousYearCrystalized2.taxYear)}")
  println(s"User can opt-out of year ${TaxYear(2021)}: ${optOutStatus2.canOptOutOfYear(TaxYear(2021))}")

}
