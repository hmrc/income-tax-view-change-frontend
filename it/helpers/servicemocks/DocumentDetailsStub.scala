
package helpers.servicemocks

import testConstants.BaseIntegrationTestConstants.taxYear
import models.financialDetails.{DocumentDetail, DocumentDetailWithDueDate}

import java.time.LocalDate

object DocumentDetailsStub {
  def docDetail(documentDescription: String): DocumentDetail = DocumentDetail(
    taxYear = taxYear,
    transactionId = "1040000124",
    documentDescription = Some(documentDescription),
    documentText = Some("documentText"),
    originalAmount = Some(10.34),
    outstandingAmount = Some(1.2),
    documentDate = LocalDate.of(2018, 3, 29)
  )

  def docDateDetail(dueDate: String, chargeType: String): DocumentDetailWithDueDate = DocumentDetailWithDueDate(
    documentDetail = docDetail(chargeType),
    dueDate = Some(LocalDate.parse(dueDate))
  )


  def docDetailWithInterest(documentDescription: String): DocumentDetail = DocumentDetail(
    taxYear = taxYear,
    transactionId = "1040000124",
    documentDescription = Some(documentDescription),
    documentText = Some("documentText"),
    originalAmount = Some(123.45),
    outstandingAmount = Some(1.2),
    documentDate = LocalDate.of(2018, 3, 29),
    latePaymentInterestAmount = Some(54.32),
    interestOutstandingAmount = Some(42.50),
    interestFromDate = Some(LocalDate.of(2018, 4, 14)),
    interestEndDate = Some(LocalDate.of(2019, 1, 1))
  )

  def docDateDetailWithInterest(dueDate: String, chargeType: String): DocumentDetailWithDueDate = DocumentDetailWithDueDate(
    documentDetail = docDetailWithInterest(chargeType),
    dueDate = Some(LocalDate.parse(dueDate))
  )
}
