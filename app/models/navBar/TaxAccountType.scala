
package models.navBar

sealed trait TaxAccountType

case object BusinessTaxAccount extends TaxAccountType
case object PersonalTaxAccount extends TaxAccountType