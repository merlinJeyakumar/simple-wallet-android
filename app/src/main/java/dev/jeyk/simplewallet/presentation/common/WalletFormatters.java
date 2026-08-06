package dev.jeyk.simplewallet.presentation.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;

import dev.jeyk.simplewallet.domain.model.TransactionType;

public final class WalletFormatters {
    private WalletFormatters() {
    }

    public static String money(BigDecimal amount, Currency currency) {
        return currency.getCurrencyCode() + " " + decimal(amount, currency);
    }

    public static String signedMoney(
            BigDecimal amount,
            Currency currency,
            TransactionType type
    ) {
        String sign = type == TransactionType.CREDIT ? "+ " : "- ";
        return sign + money(amount.abs(), currency);
    }

    public static String dateTime(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("dd MMM yyyy, HH:mm", Locale.getDefault())
                .withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

    private static String decimal(BigDecimal amount, Currency currency) {
        int fractionDigits = Math.max(currency.getDefaultFractionDigits(), 0);
        DecimalFormat format = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance());
        format.setGroupingUsed(true);
        format.setMinimumFractionDigits(fractionDigits);
        format.setMaximumFractionDigits(fractionDigits);
        format.setRoundingMode(RoundingMode.HALF_EVEN);
        return format.format(amount);
    }
}
