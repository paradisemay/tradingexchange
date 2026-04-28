import BigNumber from "bignumber.js";

export const toBig = (value: string): BigNumber => new BigNumber(value);

export const mulMoney = (a: string, b: string): string => toBig(a).multipliedBy(b).toFixed();

export const formatMoney = (value: string | null, currency: string): string => {
  if (value === null) {
    return "N/A";
  }
  return new Intl.NumberFormat("ru-RU", {
    style: "currency",
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 8
  }).format(Number(value));
};

export const formatQuantity = (value: string | null): string => {
  if (value === null) {
    return "-";
  }
  return new Intl.NumberFormat("ru-RU", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 8
  }).format(Number(value));
};
