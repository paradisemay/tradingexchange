import type { ErrorCode } from "../types/api";

export const ERROR_CODE_MESSAGES: Record<ErrorCode, string> = {
  VALIDATION_ERROR: "Проверьте корректность введённых данных.",
  UNAUTHORIZED: "Сессия истекла. Войдите снова.",
  FORBIDDEN: "Операция запрещена для вашей роли.",
  INSTRUMENT_NOT_FOUND: "Инструмент не найден.",
  INSUFFICIENT_FUNDS: "Недостаточно средств для операции.",
  INSUFFICIENT_POSITION: "Недостаточно бумаг для продажи.",
  QUOTE_UNAVAILABLE: "Котировка временно недоступна.",
  CONFLICT: "Конфликт данных. Попробуйте ещё раз.",
  INTERNAL_ERROR: "Внутренняя ошибка сервиса."
};

export const fallbackErrorMessage = "Произошла ошибка. Попробуйте повторить позже.";
