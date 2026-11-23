package com.mehmtcan.brainscroll.ui.oddone

import brainscroll.shared.generated.resources.Res
import brainscroll.shared.generated.resources.odd_rule_below10
import brainscroll.shared.generated.resources.odd_rule_digitsum
import brainscroll.shared.generated.resources.odd_rule_even
import brainscroll.shared.generated.resources.odd_rule_fibonacci
import brainscroll.shared.generated.resources.odd_rule_multiple3
import brainscroll.shared.generated.resources.odd_rule_multiple4
import brainscroll.shared.generated.resources.odd_rule_multiple5
import brainscroll.shared.generated.resources.odd_rule_multiple7
import brainscroll.shared.generated.resources.odd_rule_odd
import brainscroll.shared.generated.resources.odd_rule_prime
import brainscroll.shared.generated.resources.odd_rule_square
import brainscroll.shared.generated.resources.odd_rule_triangular
import brainscroll.shared.generated.resources.odd_rule_twodigits
import com.mehmtcan.brainscroll.game.oddone.OddRule
import org.jetbrains.compose.resources.StringResource

/** The name of a rule as the page and the hint say it (in both languages, see strings.xml). */
fun oddRuleName(rule: OddRule): StringResource = when (rule) {
    OddRule.Even -> Res.string.odd_rule_even
    OddRule.Odd -> Res.string.odd_rule_odd
    OddRule.MultipleOf5 -> Res.string.odd_rule_multiple5
    OddRule.Below10 -> Res.string.odd_rule_below10
    OddRule.TwoDigits -> Res.string.odd_rule_twodigits
    OddRule.MultipleOf3 -> Res.string.odd_rule_multiple3
    OddRule.MultipleOf4 -> Res.string.odd_rule_multiple4
    OddRule.Prime -> Res.string.odd_rule_prime
    OddRule.Square -> Res.string.odd_rule_square
    OddRule.MultipleOf7 -> Res.string.odd_rule_multiple7
    OddRule.DigitSumEven -> Res.string.odd_rule_digitsum
    OddRule.Triangular -> Res.string.odd_rule_triangular
    OddRule.Fibonacci -> Res.string.odd_rule_fibonacci
}
