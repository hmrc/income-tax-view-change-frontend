package services.optout

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.{ITSAStatus, StatusDetail}
import models.optOut.YearStatusDetail
import services.optout.OptOutInTheDomain.{Crystallized, OptOutStatus}
import services.optout.OptOutRulesService.{toFinalized, toQuery, toSymbol}

object OptOutInTheDomain {

  val optOutService = new OptOutRulesService()

  sealed trait OptOutYearOption {
    val taxYear: TaxYear
  }
  case class OptOutPreviousYearOption(taxYear: TaxYear) extends OptOutYearOption
  case class OptOutCurrentYearOption(taxYear: TaxYear) extends OptOutYearOption
  case class OptOutNextYearOption(taxYear: TaxYear) extends OptOutYearOption

  case class Crystallized(value: Boolean, taxYear: TaxYear)
  case class OptOutStatus(crystallized: Crystallized,
                          previousYearItsaStatus: YearStatusDetail,
                          currentYearItsaStatus: YearStatusDetail,
                          nextYearItsaStatus: YearStatusDetail) {

    val optOutOptions: Array[OptOutYearOption] = {

      optOutService.findOptOutOptions(
        toQuery(toFinalized(crystallized.value),
          toSymbol(previousYearItsaStatus.statusDetail),
          toSymbol(previousYearItsaStatus.statusDetail),
          toSymbol(previousYearItsaStatus.statusDetail)
        )

      ).flatMap {
        case "PY" => List(OptOutPreviousYearOption(previousYearItsaStatus.taxYear))
        case "CY" => List(OptOutCurrentYearOption(currentYearItsaStatus.taxYear))
        case "NY" => List(OptOutNextYearOption(nextYearItsaStatus.taxYear))
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

  runFor(
    Crystallized(value = false, taxYear = TaxYear(2023)),
    YearStatusDetail(TaxYear(2023), StatusDetail("", ITSAStatus.Voluntary, "")),
    YearStatusDetail(TaxYear(2024), StatusDetail("", ITSAStatus.Voluntary, "")),
    YearStatusDetail(TaxYear(2025), StatusDetail("", ITSAStatus.Voluntary, "")),
  )

  runFor(
    Crystallized(value = true, taxYear = TaxYear(2023)),
    YearStatusDetail(TaxYear(2023), StatusDetail("", ITSAStatus.Voluntary, "")),
    YearStatusDetail(TaxYear(2024), StatusDetail("", ITSAStatus.Voluntary, "")),
    YearStatusDetail(TaxYear(2025), StatusDetail("", ITSAStatus.Mandated, "")),
  )

  runFor(
    Crystallized(value = true, taxYear = TaxYear(2023)),
    YearStatusDetail(TaxYear(2023), StatusDetail("", ITSAStatus.Mandated, "")),
    YearStatusDetail(TaxYear(2024), StatusDetail("", ITSAStatus.Mandated, "")),
    YearStatusDetail(TaxYear(2025), StatusDetail("", ITSAStatus.Mandated, "")),
  )

  def runFor(previousYearCrystalized: Crystallized,
             previousYearStatusDetail: YearStatusDetail,
             currentYearStatusDetail: YearStatusDetail,
             nextYearStatusDetail: YearStatusDetail
            ): Unit = {

    val optOutStatus = OptOutStatus(previousYearCrystalized, previousYearStatusDetail, currentYearStatusDetail, nextYearStatusDetail)

    println()
    println(optOutStatus.asText)
    println(s"User can opt-out: ${optOutStatus.canOptOut}")
    println("User can opt-out of the following years:")
    optOutStatus.optOutOptions.map(v => s"\t${v.taxYear.toString}").foreach(println)
    println(s"User can opt-out of year ${previousYearStatusDetail.taxYear}: ${optOutStatus.canOptOutOfYear(previousYearCrystalized.taxYear)}")
    println(s"User can opt-out of year ${TaxYear(2021)}: ${optOutStatus.canOptOutOfYear(TaxYear(2021))}")

  }

}
