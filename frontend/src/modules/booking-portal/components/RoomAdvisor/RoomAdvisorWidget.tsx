import { HotelKnowledgePanel } from './HotelKnowledgePanel';
import React, { useState, useEffect, useRef } from 'react';
import {
  Sparkles,
  Bot,
  X,
  Send,
  RotateCcw,
  AlertTriangle,
  Info,
  Clock,
} from 'lucide-react';
import {
  sendAdvisorChat,
  sendAdvisorRefresh,
} from '../../services/roomAdvisorService';
import { RoomRecommendationCard } from './RoomRecommendationCard';
import type {
  AdvisorResultDto,
  AdvisorRecommendation,
  AdvisorSearchCriteria,
  ChatMessage,
  AdvisorClientError,
} from '../../types/roomAdvisor.types';

interface RoomAdvisorWidgetProps {
  onViewRoomDetails?: (recommendation: AdvisorRecommendation, criteria: AdvisorSearchCriteria) => void;
  isOpenExternal?: boolean;
  onCloseExternal?: () => void;
  onOpenExternal?: () => void;
}

const QUICK_PROMPTS = [
  'Tìm phòng 2 người lớn ở Hà Nội cuối tuần này',
  'Phòng gia đình 2 người lớn 1 trẻ em có ăn sáng',
  'Phòng cao cấp có ban công và bồn tắm',
  'Tìm phòng công tác có bàn làm việc giá dưới 1.5 triệu',
];

let messageIdCounter = 0;
function createMessageId(prefix: string): string {
  messageIdCounter += 1;
  return `${prefix}-${messageIdCounter}`;
}

const INITIAL_MESSAGE: ChatMessage = {
  id: 'welcome-msg',
  sender: 'assistant',
  text: 'Xin chào quý khách! Tôi là Trợ lý AI Hotel Grand. Hãy cho tôi biết nhu cầu đặt phòng của quý khách (địa điểm, ngày đi, ngày về, số người, ngân sách hoặc tiện ích mong muốn), tôi sẽ tìm những phòng phù hợp nhất!',
  timestamp: new Date(1700000000000),
};

export const RoomAdvisorWidget: React.FC<RoomAdvisorWidgetProps> = ({
  onViewRoomDetails,
  isOpenExternal,
  onCloseExternal,
  onOpenExternal,
}) => {
  const [mode, setMode] = useState<'rooms' | 'knowledge'>('rooms');
  const [internalIsOpen, setInternalIsOpen] = useState(false);
  const isOpen = typeof isOpenExternal === 'boolean' ? isOpenExternal : internalIsOpen;

  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [sessionId, setSessionId] = useState<string | undefined>();
  const [sessionVersion, setSessionVersion] = useState<number | undefined>();
  const [cooldownSeconds, setCooldownSeconds] = useState<number>(0);

  const [messages, setMessages] = useState<ChatMessage[]>([INITIAL_MESSAGE]);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleOpen = () => {
    setInternalIsOpen(true);
    onOpenExternal?.();
  };

  const handleClose = () => {
    setInternalIsOpen(false);
    onCloseExternal?.();
  };

  // Auto scroll to bottom of chat
  useEffect(() => {
    if (isOpen) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
      inputRef.current?.focus();
    }
  }, [messages, isOpen, isLoading]);

  // Rate-limiting cooldown countdown timer
  useEffect(() => {
    if (cooldownSeconds <= 0) return;

    const timer = setInterval(() => {
      setCooldownSeconds((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [cooldownSeconds]);

  const handleSendMessage = async (textToSend?: string) => {
    const text = (textToSend || inputMessage).trim();
    if (!text || isLoading || cooldownSeconds > 0) return;

    const userMessage: ChatMessage = {
      id: createMessageId('user'),
      sender: 'user',
      text,
      timestamp: new Date(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInputMessage('');
    setIsLoading(true);

    try {
      const result: AdvisorResultDto = await sendAdvisorChat({
        contractVersion: '2.0',
        message: text,
        sessionId,
        expectedSessionVersion: sessionVersion,
      });

      if (result.sessionId) {
        setSessionId(result.sessionId);
      }
      if (typeof result.sessionVersion === 'number') {
        setSessionVersion(result.sessionVersion);
      }

      const botMessage: ChatMessage = {
        id: createMessageId('bot'),
        sender: 'assistant',
        text: result.reply,
        timestamp: new Date(),
        result,
      };

      setMessages((prev) => [...prev, botMessage]);
    } catch (err: unknown) {
      const clientErr = err as AdvisorClientError;

      if (clientErr.isRateLimited && clientErr.retryAfterSeconds) {
        setCooldownSeconds(clientErr.retryAfterSeconds);
      }

      const errorMessage: ChatMessage = {
        id: createMessageId('err'),
        sender: 'system',
        text: clientErr.message || 'Hệ thống đang bận. Vui lòng thử lại sau giây lát.',
        timestamp: new Date(),
        isError: true,
        rateLimitCountdown: clientErr.retryAfterSeconds,
      };

      setMessages((prev) => [...prev, errorMessage]);

      if (clientErr.isSessionExpired) {
        setSessionId(undefined);
        setSessionVersion(undefined);
      }

      if (clientErr.isSessionConflict && sessionId) {
        try {
          const resynced = await sendAdvisorRefresh({
            contractVersion: '2.0',
            sessionId,
          });
          if (typeof resynced.sessionVersion === 'number') {
            setSessionVersion(resynced.sessionVersion);
          }
          setMessages((prev) => [
            ...prev,
            {
              id: createMessageId('resync'),
              sender: 'system',
              text: 'Đã tự động đồng bộ lại phiên tư vấn. Vui lòng gửi lại yêu cầu của bạn.',
              timestamp: new Date(),
            },
          ]);
        } catch {
          setSessionId(undefined);
          setSessionVersion(undefined);
        }
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleResetSession = () => {
    setSessionId(undefined);
    setSessionVersion(undefined);
    setCooldownSeconds(0);
    setMessages([
      INITIAL_MESSAGE,
      {
        id: createMessageId('sys-reset'),
        sender: 'system',
        text: 'Phiên tư vấn đã được làm mới. Quý khách có thể bắt đầu yêu cầu mới.',
        timestamp: new Date(),
      },
    ]);
  };

  const handleRefreshRecommendations = async () => {
    if (!sessionId || isLoading || cooldownSeconds > 0) return;

    setIsLoading(true);
    try {
      const result = await sendAdvisorRefresh({
        contractVersion: '2.0',
        sessionId,
        expectedSessionVersion: sessionVersion,
      });

      if (typeof result.sessionVersion === 'number') {
        setSessionVersion(result.sessionVersion);
      }

      const botMessage: ChatMessage = {
        id: createMessageId('refresh'),
        sender: 'assistant',
        text: result.reply || 'Đã làm mới thông tin giá và phòng trống.',
        timestamp: new Date(),
        result,
      };

      setMessages((prev) => [...prev, botMessage]);
    } catch (err: unknown) {
      const clientErr = err as AdvisorClientError;
      if (clientErr.isRateLimited && clientErr.retryAfterSeconds) {
        setCooldownSeconds(clientErr.retryAfterSeconds);
      }
      setMessages((prev) => [
        ...prev,
        {
          id: createMessageId('refresh-err'),
          sender: 'system',
          text: clientErr.message || 'Không thể làm mới kết quả.',
          timestamp: new Date(),
          isError: true,
        },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      {/* Floating Launcher Button */}
      {!isOpen && (
        <button
          onClick={handleOpen}
          aria-label="Mở Trợ lý Tìm phòng AI"
          className="fixed bottom-6 right-6 z-50 flex items-center gap-2 bg-[#EE0000] hover:bg-red-700 text-white font-medium px-4 py-3 rounded-full shadow-lg hover:shadow-xl transition-all duration-200 transform hover:-translate-y-0.5 focus:outline-none focus:ring-4 focus:ring-red-300"
        >
          <Sparkles className="w-5 h-5 animate-pulse text-amber-200" />
          <span className="text-sm font-semibold tracking-wide">Trợ lý AI Tìm phòng</span>
        </button>
      )}

      {/* Floating Chat Drawer / Window */}
      {isOpen && (
        <div
          role="dialog"
          aria-labelledby="advisor-dialog-title"
          className="fixed bottom-4 right-4 sm:bottom-6 sm:right-6 z-50 w-[calc(100vw-2rem)] sm:w-[480px] md:w-[540px] max-h-[85vh] h-[720px] bg-white rounded-2xl shadow-2xl border border-neutral-200 flex flex-col overflow-hidden animate-in fade-in slide-in-from-bottom-5 duration-200"
        >
          {/* Header */}
          <div className="bg-neutral-900 text-white px-4 py-3.5 flex items-center justify-between border-b border-neutral-800">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-full bg-[#EE0000] flex items-center justify-center text-white shadow-sm">
                <Bot className="w-5 h-5" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h2 id="advisor-dialog-title" className="font-semibold text-sm sm:text-base text-white">
                    Trợ lý AI Tìm phòng
                  </h2>
                  <span className="text-[10px] bg-red-950 text-red-300 border border-red-800 px-1.5 py-0.5 rounded font-mono font-medium">
                    T12 R1
                  </span>
                </div>
                <p className="text-xs text-neutral-400">Tư vấn thông minh & giá trực tiếp</p>
              </div>
            </div>

            <div className="flex items-center gap-1">
              {sessionId && (
                <button
                  onClick={handleResetSession}
                  title="Làm mới phiên hội thoại"
                  aria-label="Làm mới phiên"
                  className="p-1.5 text-neutral-400 hover:text-white hover:bg-neutral-800 rounded-lg transition-colors"
                >
                  <RotateCcw className="w-4 h-4" />
                </button>
              )}
              <button
                onClick={handleClose}
                aria-label="Đóng Trợ lý"
                className="p-1.5 text-neutral-400 hover:text-white hover:bg-neutral-800 rounded-lg transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>

          <div className="flex gap-2 p-2 border-b">
            <button type="button" aria-pressed={mode === 'rooms'} onClick={() => setMode('rooms')} className="rounded border px-3 py-2 text-sm">Tìm phòng</button>
            <button type="button" aria-pressed={mode === 'knowledge'} onClick={() => setMode('knowledge')} className="rounded border px-3 py-2 text-sm">Chính sách &amp; tiện ích</button>
          </div>
          {mode === 'knowledge' && <HotelKnowledgePanel />}
          <div className={mode === 'rooms' ? 'contents' : 'hidden'}>
          {/* Chat Messages Body */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-neutral-50">
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`flex flex-col ${
                  msg.sender === 'user' ? 'items-end' : 'items-start'
                }`}
              >
                {/* Message Bubble */}
                <div
                  className={`max-w-[90%] rounded-2xl px-4 py-3 text-sm leading-relaxed shadow-sm ${
                    msg.sender === 'user'
                      ? 'bg-[#EE0000] text-white rounded-br-xs'
                      : msg.sender === 'system'
                      ? msg.isError
                        ? 'bg-red-50 text-red-800 border border-red-200 rounded-xl'
                        : 'bg-amber-50 text-amber-800 border border-amber-200 rounded-xl'
                      : 'bg-white text-neutral-900 border border-neutral-200 rounded-bl-xs'
                  }`}
                >
                  {/* System Error Icon */}
                  {msg.sender === 'system' && (
                    <div className="flex items-center gap-1.5 font-semibold text-xs mb-1">
                      {msg.isError ? (
                        <>
                          <AlertTriangle className="w-3.5 h-3.5 text-red-600" />
                          <span>Thông báo lỗi</span>
                        </>
                      ) : (
                        <>
                          <Info className="w-3.5 h-3.5 text-amber-600" />
                          <span>Hệ thống</span>
                        </>
                      )}
                    </div>
                  )}

                  <p className="whitespace-pre-line">{msg.text}</p>

                  {/* Recommendation Cards Section */}
                  {msg.result?.recommendations && msg.result.recommendations.length > 0 && (
                    <div className="mt-3 pt-3 border-t border-neutral-100 space-y-3">
                      <div className="flex items-center justify-between text-xs text-neutral-500 font-medium">
                        <span>
                          Gợi ý phù hợp ({msg.result.recommendations.length} phòng)
                        </span>
                        {sessionId && (
                          <button
                            onClick={handleRefreshRecommendations}
                            disabled={isLoading}
                            className="text-[#EE0000] hover:underline flex items-center gap-1"
                          >
                            <RotateCcw className="w-3 h-3" />
                            Cập nhật giá
                          </button>
                        )}
                      </div>

                      <div className="space-y-3">
                        {msg.result.recommendations.map((rec) => (
                          <RoomRecommendationCard
                            key={rec.recommendationId}
                            recommendation={rec}
                            onViewDetails={(r) => {
                              if (msg.result?.criteria) {
                                onViewRoomDetails?.(r, msg.result.criteria);
                              }
                              handleClose();
                            }}
                          />
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Result Warnings */}
                  {msg.result?.warnings && msg.result.warnings.length > 0 && (
                    <div className="mt-2 text-xs text-amber-700 bg-amber-50 p-2 rounded border border-amber-200">
                      {msg.result.warnings.map((w, idx) => (
                        <p key={idx} className="flex items-center gap-1">
                          <AlertTriangle className="w-3 h-3 shrink-0" />
                          <span>{w}</span>
                        </p>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            ))}

            {/* Loading Indicator */}
            {isLoading && (
              <div className="flex items-center gap-2 text-neutral-500 text-xs py-2 px-3 bg-white rounded-xl border border-neutral-200 w-fit">
                <Sparkles className="w-4 h-4 text-[#EE0000] animate-spin" />
                <span>Trợ lý AI đang tìm phòng phù hợp nhất...</span>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* Quick Prompts */}
          {messages.length <= 2 && !isLoading && (
            <div className="p-2.5 bg-neutral-100 border-t border-neutral-200 flex gap-2 overflow-x-auto no-scrollbar">
              {QUICK_PROMPTS.map((prompt, idx) => (
                <button
                  key={idx}
                  onClick={() => handleSendMessage(prompt)}
                  className="shrink-0 text-xs bg-white text-neutral-700 hover:text-[#EE0000] hover:border-red-300 border border-neutral-200 px-3 py-1.5 rounded-full transition-colors font-medium shadow-2xs"
                >
                  {prompt}
                </button>
              ))}
            </div>
          )}

          {/* Cooldown Banner */}
          {cooldownSeconds > 0 && (
            <div className="bg-amber-50 border-t border-amber-200 px-4 py-2 flex items-center justify-between text-xs text-amber-800">
              <div className="flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-amber-600 animate-pulse" />
                <span>Bạn đang gửi yêu cầu quá nhanh. Vui lòng chờ:</span>
              </div>
              <span className="font-bold text-amber-900 bg-amber-200 px-2 py-0.5 rounded font-mono">
                {cooldownSeconds}s
              </span>
            </div>
          )}

          {/* Input Footer */}
          <div className="p-3 bg-white border-t border-neutral-200 flex items-center gap-2">
            <input
              ref={inputRef}
              type="text"
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault();
                  handleSendMessage();
                }
              }}
              placeholder={
                cooldownSeconds > 0
                  ? `Vui lòng đợi ${cooldownSeconds}s trước khi gửi tiếp...`
                  : 'Nhập nhu cầu: Ngày đi, số người, địa điểm...'
              }
              disabled={isLoading || cooldownSeconds > 0}
              className="flex-1 bg-neutral-100 border border-neutral-200 rounded-xl px-3.5 py-2.5 text-sm text-neutral-900 placeholder:text-neutral-400 focus:outline-none focus:ring-2 focus:ring-[#EE0000] focus:bg-white transition-all disabled:opacity-50"
            />
            <button
              onClick={() => handleSendMessage()}
              disabled={!inputMessage.trim() || isLoading || cooldownSeconds > 0}
              aria-label="Gửi tin nhắn"
              className="bg-[#EE0000] hover:bg-red-700 disabled:bg-neutral-300 text-white p-2.5 rounded-xl transition-all shadow-sm focus:outline-none focus:ring-2 focus:ring-red-400"
            >
              <Send className="w-4 h-4" />
            </button>
          </div>
          </div>
        </div>
      )}
    </>
  );
};
