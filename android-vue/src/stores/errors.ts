import axios from "axios";
import type { ApiErrorResponse } from "../types/api";
import { ERROR_CODE_MESSAGES, fallbackErrorMessage } from "../utils/errorCodes";

export const mapApiError = (error: unknown): string => {
  if (!axios.isAxiosError<ApiErrorResponse>(error)) {
    return fallbackErrorMessage;
  }
  const payload = error.response?.data;
  if (!payload?.errorCode) {
    return payload?.message ?? fallbackErrorMessage;
  }
  return ERROR_CODE_MESSAGES[payload.errorCode] ?? payload.message ?? fallbackErrorMessage;
};
