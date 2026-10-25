import axios, { type AxiosError } from 'axios';
import type { ApiResponse } from '../../../core/api/types';
import type {
  AdvisorChatRequestDto,
  AdvisorRefreshRequestDto,
  AdvisorResultDto,
  AdvisorClientError,
} from '../types/roomAdvisor.types';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

/**
 * Dedicated Axios client for Room Advisor.
 * Configured strictly with `withCredentials: true` to support anonymous `t12_session` HttpOnly cookie.
 */
const advisorClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 45000,
});

/**
 * Normalizes API errors into structured AdvisorClientError.
 */
function handleAdvisorError(error: unknown): never {
  if (axios.isAxiosError(error)) {
    const axiosErr = error as AxiosError<{
      code?: number;
      message?: string;
      error?: {
        reason?: string;
        retryable?: boolean;
        contractVersion?: string;
      };
    }>;

    const status = axiosErr.response?.status;
    const data = axiosErr.response?.data;
    const headers = axiosErr.response?.headers;

    // HTTP 429: Rate Limited
    if (status === 429) {
      let retryAfter = 10;
      const rawHeader = headers ? (headers['retry-after'] || headers['Retry-After']) : null;
      if (rawHeader) {
        const parsed = parseInt(String(rawHeader), 10);
        if (!isNaN(parsed) && parsed > 0) {
          retryAfter = parsed;
        }
      }

      const clientError: AdvisorClientError = {
        code: 429,
        reason: 'RATE_LIMITED',
        message: `Bạn đang gửi yêu cầu quá nhanh. Vui lòng đợi ${retryAfter} giây trước khi gửi tiếp.`,
        isRateLimited: true,
        retryAfterSeconds: retryAfter,
      };
      throw clientError;
    }

    // HTTP 404: Session Expired / Not Found
    if (status === 404) {
      const clientError: AdvisorClientError = {
        code: 404,
        reason: 'SESSION_NOT_FOUND',
        message: 'Phiên trò chuyện đã hết hạn. Hệ thống đang tự động bắt đầu phiên mới.',
        isSessionExpired: true,
      };
      throw clientError;
    }

    // HTTP 409: Session Version Conflict
    if (status === 409) {
      const clientError: AdvisorClientError = {
        code: 409,
        reason: 'SESSION_VERSION_CONFLICT',
        message: 'Dữ liệu phiên có thay đổi đồng thời. Đang đồng bộ lại phiên làm việc...',
        isSessionConflict: true,
      };
      throw clientError;
    }

    // Standard business or upstream error
    const message = data?.message || axiosErr.message || 'Không thể kết nối đến máy chủ trợ lý AI.';
    const clientError: AdvisorClientError = {
      code: status || 500,
      reason: data?.error?.reason || 'UNKNOWN_ERROR',
      message,
    };
    throw clientError;
  }

  throw {
    code: 500,
    message: error instanceof Error ? error.message : 'Lỗi không xác định khi giao tiếp với AI Advisor.',
  } as AdvisorClientError;
}

/**
 * Gửi tin nhắn ngôn ngữ tự nhiên đến Trợ lý AI Tìm Phòng (T12)
 */
export async function sendAdvisorChat(
  requestOrMessage: AdvisorChatRequestDto | string,
  sessionId?: string,
  sessionVersion?: number
): Promise<AdvisorResultDto> {
  const payload: AdvisorChatRequestDto =
    typeof requestOrMessage === 'string'
      ? {
          contractVersion: '2.0',
          message: requestOrMessage,
          ...(sessionId ? { sessionId } : {}),
          ...(typeof sessionVersion === 'number' ? { expectedSessionVersion: sessionVersion } : {}),
        }
      : requestOrMessage;

  try {
    const res = await advisorClient.post<ApiResponse<AdvisorResultDto>>(
      '/api/v1/public/ai/room-advisor/chat',
      payload
    );
    return res.data.result;
  } catch (err) {
    return handleAdvisorError(err);
  }
}

/**
 * Làm mới trạng thái phiên tư vấn AI
 */
export async function sendAdvisorRefresh(
  requestOrSessionId: AdvisorRefreshRequestDto | string,
  sessionVersion?: number
): Promise<AdvisorResultDto> {
  const payload: AdvisorRefreshRequestDto =
    typeof requestOrSessionId === 'string'
      ? {
          contractVersion: '2.0',
          sessionId: requestOrSessionId,
          ...(typeof sessionVersion === 'number' ? { expectedSessionVersion: sessionVersion } : {}),
        }
      : requestOrSessionId;

  try {
    const res = await advisorClient.post<ApiResponse<AdvisorResultDto>>(
      '/api/v1/public/ai/room-advisor/refresh',
      payload
    );
    return res.data.result;
  } catch (err) {
    return handleAdvisorError(err);
  }
}
