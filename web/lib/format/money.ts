interface CurrencyLike {
  code?: string;
  symbol?: string;
}

/** Every trip quotes and is paid in its own currency — never assume EGP. */
export function formatMoney(amount: number | undefined | null, currency?: CurrencyLike): string {
  if (amount == null) return "—";
  const formatted = new Intl.NumberFormat("en-US", { maximumFractionDigits: 0 }).format(amount);
  return currency?.symbol ? `${currency.symbol} ${formatted}` : `${formatted} ${currency?.code ?? ""}`.trim();
}
