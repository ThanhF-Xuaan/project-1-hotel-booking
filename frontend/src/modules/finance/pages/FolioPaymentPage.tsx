import React, { useState, useCallback, useEffect, useRef, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  DollarSign,
  Search,
  RefreshCw,
  CheckCircle,
  CheckCircle2,
  XCircle,
  Receipt,
  Printer,
  ChevronLeft,
  ChevronRight,
  History,
} from 'lucide-react';
import Button from '../../../core/components/ui/Button';
import Input from '../../../core/components/ui/Input';
import CurrencyInput from '../../../core/components/ui/CurrencyInput';
import Select from '../../../core/components/ui/Select';
import Modal, { ConfirmModal } from '../../../core/components/ui/Modal';
import { formatCurrency } from '../../../core/utils/currency';
import { getApiErrorMessage, type ApiErrorInfo } from '../../../core/api/error';
import { isSessionExpiredError } from '../../../core/api/client';
import financeService from '../services/finance.service';
import bookingService from '../../booking/services/booking.service';
import CheckoutModal from '../components/payment/CheckoutModal';
import PaymentErrorModal from '../components/payment/PaymentErrorModal';
import PaymentStatusBadge from '../components/payment/PaymentStatusBadge';
import InvoicePaymentFlowModal from '../components/payment/InvoicePaymentFlowModal';
import HotelFolioReceipt from '../components/payment/HotelFolioReceipt';
import type {
  PaymentResponse,
  PaymentStatus,
  InvoiceResponse,
  InvoiceStatus,
  PaymentMethod,
  PaymentPurpose,
  GatewayMethod,
  PaymentUrlResponse,
} from '../types/finance.types';
import type { BookingResponse, BookingStatus } from '../../booking/types/booking.types';

/** Cấu hình ô nhập mã tham chiếu theo từng method (mục 1 — bảng 0.1) */
const REFERENCE_CONFIG: Record<
  PaymentMethod,
  { label: string; required: boolean; placeholder: string }
> = {
  CASH: { label: 'Số phiếu thu (tùy chọn)', required: false, placeholder: 'VD: PT-001' },
  BANK_TRANSFER: { label: 'Mã giao dịch ngân hàng *', required: true, placeholder: 'VD: FT260930001' },
  CREDIT_CARD: { label: 'Approval code (POS) *', required: true, placeholder: 'VD: AP778899' },
  DEBIT_CARD: { label: 'Approval code (POS) *', required: true, placeholder: 'VD: AP778899' },
  VNPAY: { label: 'Mã tham chiếu (tự ghi từ webhook)', required: false, placeholder: '' },
  MOMO: { label: 'Mã tham chiếu (tự ghi từ webhook)', required: false, placeholder: '' },
  ZALOPAY: { label: 'Mã tham chiếu', required: false, placeholder: '' },
  OTHER: { label: 'Mã tham chiếu / Biên lai', required: false, placeholder: 'VD: HD-001' },
};

const isGatewayMethod = (m: PaymentMethod): m is GatewayMethod => m === 'VNPAY';

export const FolioPaymentPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const urlBookingId = searchParams.get('bookingId');
  const [bookingNumberInput, setBookingNumberInput] = useState('');
  const [currentBooking, setCurrentBooking] = useState<BookingResponse | null>(null);
  const [payments, setPayments] = useState<PaymentResponse[]>([]);
  const [invoice, setInvoice] = useState<InvoiceResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // In ấn biên lai Folio
  const [isReceiptModalOpen, setIsReceiptModalOpen] = useState(false);
  const [selectedReceiptPayment, setSelectedReceiptPayment] = useState<PaymentResponse | null>(null);
  const [receiptBooking, setReceiptBooking] = useState<BookingResponse | null>(null);
  const [receiptInvoice, setReceiptInvoice] = useState<InvoiceResponse | null>(null);
  const activeFolioRequestIdRef = useRef(0);

  // Lịch sử giao dịch toàn hệ thống (LUÔN HIỂN THỊ, không phụ thuộc chọn phòng trước)
  const [globalPayments, setGlobalPayments] = useState<PaymentResponse[]>([]);
  const [globalTotalElements, setGlobalTotalElements] = useState(0);
  const [globalTotalPages, setGlobalTotalPages] = useState(1);
  const [globalPage, setGlobalPage] = useState(1);
  const [globalStatusFilter, setGlobalStatusFilter] = useState<'ALL' | PaymentStatus>('ALL');
  const [globalMethodFilter, setGlobalMethodFilter] = useState<'ALL' | PaymentMethod>('ALL');
  const [isLoadingGlobal, setIsLoadingGlobal] = useState(false);
  const [globalError, setGlobalError] = useState<string | null>(null);

  // Payment Modal
  const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
  const [isInvoicePaymentFlowOpen, setIsInvoicePaymentFlowOpen] = useState(false);
  const [payAmount, setPayAmount] = useState<number>(0);
  const [payMethod, setPayMethod] = useState<PaymentMethod>('CASH');
  const [payPurpose, setPayPurpose] = useState<PaymentPurpose>('FULL_PAYMENT');
  const [payProvider, setPayProvider] = useState('');
  const [payReference, setPayReference] = useState('');
  const [isProcessingPayment, setIsProcessingPayment] = useState(false);

  // Giai đoạn D — phiên thanh toán online (VNPay/MoMo) + popup lỗi §6
  const [activeGateway, setActiveGateway] = useState<GatewayMethod>('VNPAY');
  const [gatewaySession, setGatewaySession] = useState<PaymentUrlResponse | null>(null);
  const [isCheckoutModalOpen, setIsCheckoutModalOpen] = useState(false);
  const [paymentError, setPaymentError] = useState<ApiErrorInfo | null>(null);
  const [isCancellingPayment, setIsCancellingPayment] = useState(false);
  const referenceInputRef = useRef<HTMLInputElement>(null);

  // Tương tác chuyển trạng thái thanh toán
  const [actionConfirm, setActionConfirm] = useState<{
    type: 'complete' | 'cancel';
    payment: PaymentResponse;
  } | null>(null);
  const [isMutatingStatus, setIsMutatingStatus] = useState(false);

  // Refund Modal
  const [isRefundModalOpen, setIsRefundModalOpen] = useState(false);
  const [refundPaymentId, setRefundPaymentId] = useState<number | null>(null);
  const [refundAmount, setRefundAmount] = useState<number>(0);
  const [refundReason, setRefundReason] = useState('');
  const [isRefunding, setIsRefunding] = useState(false);

  // Invoicing Modal
  const [isGeneratingInvoice, setIsGeneratingInvoice] = useState(false);

  // ── Danh sách đơn đặt phòng — chọn nhanh, không cần nhập mã ───────────────
  const [bookingList, setBookingList] = useState<BookingResponse[]>([]);
  const [isListLoading, setIsListLoading] = useState(false);
  const [listError, setListError] = useState<string | null>(null);
  const [listFilterKeyword, setListFilterKeyword] = useState('');

  const fetchFolioData = useCallback(async (query: string) => {
    if (!query) return;
    const requestId = ++activeFolioRequestIdRef.current;
    const cleanInput = query.trim();

    setIsLoading(true);
    setErrorMessage(null);
    setReceiptBooking(null);
    setReceiptInvoice(null);
    setSelectedReceiptPayment(null);
    setIsReceiptModalOpen(false);

    if (
      currentBooking &&
      currentBooking.bookingNumber.toLowerCase() !== cleanInput.toLowerCase()
    ) {
      // Xóa hóa đơn/giao dịch cũ ngay khi chuyển phòng để tránh nhấp nháy dữ liệu lệch
      setInvoice(null);
      setPayments([]);
    }

    try {
      let booking: BookingResponse | null = null;

      // 1. Thử tìm nhanh trong danh sách bookingList theo mã booking (khớp chính xác hoặc chứa chuỗi)
      let matchedInList = bookingList.find(
        (b) => b.bookingNumber.toLowerCase() === cleanInput.toLowerCase()
      );
      if (!matchedInList) {
        matchedInList = bookingList.find((b) =>
          b.bookingNumber.toLowerCase().includes(cleanInput.toLowerCase())
        );
      }

      if (matchedInList) {
        booking = matchedInList;
      } else {
        // 2. Tra cứu theo mã booking từ API getByNumber (thử chuỗi gốc và viết hoa)
        try {
          const bRes = await bookingService.getByNumber(cleanInput);
          if (bRes.result) booking = bRes.result;
        } catch {
          try {
            const bResUpper = await bookingService.getByNumber(cleanInput.toUpperCase());
            if (bResUpper.result) booking = bResUpper.result;
          } catch {
            // 3. Thử tìm gần đúng theo mã booking qua API filter
            try {
              const filterRes = await bookingService.filter({
                bookingNumber: cleanInput,
                page: 1,
                size: 1,
              });
              if (filterRes.result?.content && filterRes.result.content.length > 0) {
                booking = filterRes.result.content[0];
              }
            } catch {
              // Bỏ qua
            }
          }
        }
      }

      if (activeFolioRequestIdRef.current !== requestId) return;

      if (booking) {
        setBookingNumberInput(booking.bookingNumber);
        setCurrentBooking(booking);

        // Load payments & invoice song song cho đúng booking đang chọn
        const [pRes, invRes] = await Promise.all([
          financeService.getPaymentsByBookingId(booking.id).catch(() => null),
          financeService.getInvoiceByBookingId(booking.id).catch(() => null),
        ]);

        if (activeFolioRequestIdRef.current !== requestId) return;

        setPayments(pRes?.result || []);
        setInvoice(invRes?.result || null);
      } else {
        setErrorMessage(
          `Không tìm thấy đơn đặt phòng với mã booking "${cleanInput}". Vui lòng kiểm tra lại mã đơn.`
        );
        setCurrentBooking(null);
        setPayments([]);
        setInvoice(null);
      }
    } catch (err: unknown) {
      if (activeFolioRequestIdRef.current !== requestId) return;
      console.error('Lỗi tra cứu Folio theo mã booking:', err);
      if (isSessionExpiredError(err)) {
        setErrorMessage('Phiên đăng nhập đã hết hạn — vui lòng đăng nhập lại để tra cứu Folio');
      } else {
        setErrorMessage('Không tra cứu được Folio — vui lòng kiểm tra lại mã đặt phòng');
      }
      setCurrentBooking(null);
      setPayments([]);
      setInvoice(null);
    } finally {
      if (activeFolioRequestIdRef.current === requestId) {
        setIsLoading(false);
      }
    }
  }, [bookingList, currentBooking]);

  const fetchBookingList = useCallback(async () => {
    setIsListLoading(true);
    setListError(null);
    try {
      // Lấy 50 đơn mới nhất để nhân viên tìm kiếm thuận tiện
      const res = await bookingService.filter({ page: 1, size: 50 });
      if (res.result) {
        setBookingList(res.result.content || []);
      }
    } catch (err: unknown) {
      console.error('Lỗi tải danh sách đơn đặt phòng:', err);
      if (isSessionExpiredError(err)) {
        setListError('Phiên đăng nhập đã hết hạn — vui lòng đăng nhập lại');
      } else {
        setListError('Không tải được danh sách đơn đặt phòng — vui lòng thử lại');
      }
    } finally {
      setIsListLoading(false);
    }
  }, []);

  const fetchGlobalPayments = useCallback(
    async (
      targetPage = globalPage,
      targetStatus = globalStatusFilter,
      targetMethod = globalMethodFilter
    ) => {
      setIsLoadingGlobal(true);
      setGlobalError(null);
      try {
        const res = await financeService.filterPayments({
          page: targetPage,
          pageSize: 10,
          sortBy: 'createdAt',
          sortDirection: 'DESC',
          status: targetStatus === 'ALL' ? undefined : targetStatus,
          paymentMethod: targetMethod === 'ALL' ? undefined : targetMethod,
        });
        if (res.result) {
          setGlobalPayments(res.result.content || []);
          setGlobalTotalElements(res.result.totalElements || 0);
          setGlobalTotalPages(Math.max(1, res.result.totalPages || 1));
        }
      } catch (err: unknown) {
        if (isSessionExpiredError(err)) {
          setGlobalError('Phiên đăng nhập đã hết hạn — vui lòng đăng nhập lại');
        } else {
          setGlobalError('Không tải được lịch sử giao dịch toàn hệ thống');
        }
      } finally {
        setIsLoadingGlobal(false);
      }
    },
    [globalPage, globalStatusFilter, globalMethodFilter]
  );

  useEffect(() => {
    let isMounted = true;
    const init = async () => {
      if (isMounted) {
        await fetchBookingList();
      }
    };
    void init();
    return () => {
      isMounted = false;
    };
  }, [fetchBookingList]);

  useEffect(() => {
    let isMounted = true;
    const loadGlobal = async () => {
      if (isMounted) {
        await fetchGlobalPayments(globalPage, globalStatusFilter, globalMethodFilter);
      }
    };
    void loadGlobal();
    return () => {
      isMounted = false;
    };
  }, [fetchGlobalPayments, globalPage, globalStatusFilter, globalMethodFilter]);

  // Tự đồng bộ dữ liệu khi quay lại trang qua nút Back trình duyệt (bfcache)
  useEffect(() => {
    const handlePageShow = (event: PageTransitionEvent) => {
      if (event.persisted) {
        void fetchGlobalPayments();
        if (currentBooking) {
          void fetchFolioData(currentBooking.bookingNumber);
        }
      }
    };
    window.addEventListener('pageshow', handlePageShow);
    return () => {
      window.removeEventListener('pageshow', handlePageShow);
    };
  }, [fetchGlobalPayments, fetchFolioData, currentBooking]);

  // Tự động load đơn theo bookingId từ query param (khi thanh toán VNPay chuyển hướng về)
  useEffect(() => {
    if (!urlBookingId) return;
    const bid = Number(urlBookingId);
    if (!bid || isNaN(bid)) return;

    let isMounted = true;
    const loadBookingFromParam = async () => {
      try {
        setIsLoading(true);
        const res = await bookingService.getById(bid);
        if (res.result && isMounted) {
          setBookingNumberInput(res.result.bookingNumber);
          setCurrentBooking(res.result);
          // Load payments
          const pRes = await financeService.getPaymentsByBookingId(res.result.id);
          if (pRes.result && isMounted) {
            setPayments(pRes.result);
          }
          // Load invoice
          try {
            const invRes = await financeService.getInvoiceByBookingId(res.result.id);
            if (invRes.result && isMounted) {
              setInvoice(invRes.result);
            }
          } catch {
            if (isMounted) setInvoice(null);
          }
        }
      } catch (err) {
        console.warn('Lỗi tự động tải booking theo urlParam bookingId:', err);
      } finally {
        if (isMounted) setIsLoading(false);
      }
    };

    void loadBookingFromParam();
    return () => {
      isMounted = false;
    };
  }, [urlBookingId]);

  // Lọc realtime danh sách booking theo cả MÃ BOOKING và SỐ PHÒNG (nhập từng số nhảy tìm kiếm luôn)
  const filteredBookingList = useMemo(() => {
    let list = bookingList;

    // 1. Nhập từng số/chữ ở ô mã booking nhảy tìm kiếm ngay lập tức
    if (bookingNumberInput.trim()) {
      const bKw = bookingNumberInput.trim().toLowerCase();
      list = list.filter((b) => b.bookingNumber.toLowerCase().includes(bKw));
    }

    // 2. Nhập số phòng ở ô dưới nhảy tìm kiếm theo số phòng ngay lập tức
    if (listFilterKeyword.trim()) {
      const kw = listFilterKeyword.trim().toLowerCase();
      const cleanKw = kw.replace(/^(phòng|phong|p\.?)\s*/i, '').trim();
      list = list.filter((b) => {
        const rooms = b.bookingDetails?.flatMap((d) => d.bookingRooms || []) || [];
        return rooms.some((r) => {
          if (!r.roomNumber) return false;
          const rNum = r.roomNumber.toLowerCase();
          return (
            rNum === kw ||
            rNum.includes(kw) ||
            (cleanKw ? rNum === cleanKw || rNum.includes(cleanKw) : false)
          );
        });
      });
    }

    return list;
  }, [bookingList, bookingNumberInput, listFilterKeyword]);

  // Tự động tìm kiếm backend và nhảy nạp Folio ngay khi nhập từng số mã booking
  useEffect(() => {
    const trimmed = bookingNumberInput.trim().toLowerCase();
    if (!trimmed) {
      return;
    }

    // Nếu in-memory chưa có đơn nào khớp, truy vấn backend sau 300ms
    const hasLocalMatch = bookingList.some((b) =>
      b.bookingNumber.toLowerCase().includes(trimmed)
    );
    if (!hasLocalMatch && trimmed.length >= 2) {
      const fetchTimer = setTimeout(async () => {
        try {
          const res = await bookingService.filter({
            bookingNumber: trimmed,
            page: 1,
            size: 50,
          });
          if (res.result?.content && res.result.content.length > 0) {
            setBookingList((prev) => {
              const existingIds = new Set(prev.map((b) => b.id));
              const newItems = res.result?.content.filter((b) => !existingIds.has(b.id)) || [];
              return [...prev, ...newItems];
            });
          }
        } catch (err) {
          console.warn('Lỗi tìm kiếm booking từ backend:', err);
        }
      }, 300);

      return () => clearTimeout(fetchTimer);
    }

    // Tự động nạp Folio khi khớp chính xác hoặc chỉ còn đúng 1 đơn khớp
    const folioTimer = setTimeout(() => {
      const exactMatch = bookingList.find(
        (b) => b.bookingNumber.toLowerCase() === trimmed
      );
      if (exactMatch) {
        if (currentBooking?.id !== exactMatch.id) {
          fetchFolioData(exactMatch.bookingNumber);
        }
      } else if (trimmed.length >= 3) {
        const matches = bookingList.filter((b) =>
          b.bookingNumber.toLowerCase().includes(trimmed)
        );
        if (matches.length === 1 && currentBooking?.id !== matches[0].id) {
          fetchFolioData(matches[0].bookingNumber);
        }
      }
    }, 350);

    return () => clearTimeout(folioTimer);
  }, [bookingNumberInput, bookingList, currentBooking, fetchFolioData]);

  // Tìm kiếm số phòng qua backend nếu danh sách in-memory chưa nạp
  const handleSearchByRoom = async () => {
    if (!listFilterKeyword.trim()) {
      await fetchBookingList();
      return;
    }
    const kw = listFilterKeyword.trim().toLowerCase();
    const cleanKw = kw.replace(/^(phòng|phong|p\.?)\s*/i, '').trim();

    const inMemoryMatches = bookingList.filter((b) => {
      const rooms = b.bookingDetails?.flatMap((d) => d.bookingRooms || []) || [];
      return rooms.some((r) => {
        if (!r.roomNumber) return false;
        const rNum = r.roomNumber.toLowerCase();
        return (
          rNum === kw ||
          rNum.includes(kw) ||
          (cleanKw ? rNum === cleanKw || rNum.includes(cleanKw) : false)
        );
      });
    });

    if (inMemoryMatches.length === 0) {
      setIsListLoading(true);
      try {
        const res = await bookingService.filter({ roomNumber: cleanKw || kw, page: 1, size: 50 });
        if (res.result?.content) {
          setBookingList((prev) => {
            const existingIds = new Set(prev.map((b) => b.id));
            const newItems = res.result?.content.filter((b) => !existingIds.has(b.id)) || [];
            return [...prev, ...newItems];
          });
        }
      } catch (err) {
        console.warn('Lỗi tìm kiếm theo số phòng từ backend:', err);
      } finally {
        setIsListLoading(false);
      }
    }
  };

  /** Click 1 dòng trong danh sách → nạp thẳng Folio của đơn đó */
  const handleSelectBooking = (booking: BookingResponse) => {
    setBookingNumberInput(booking.bookingNumber);
    fetchFolioData(booking.bookingNumber);
  };

  const bookingStatusBadge = (status: BookingStatus) => {
    switch (status) {
      case 'CONFIRMED':
        return (
          <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
            Đã xác nhận
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-red-50 text-red-700 border border-red-200">
            Đã hủy
          </span>
        );
      default:
        return (
          <span className="px-2.5 py-1 text-xs font-semibold rounded-full bg-amber-50 text-amber-700 border border-amber-200">
            No-show
          </span>
        );
    }
  };

  const totalCharges =
    invoice && Number(invoice.grandTotal) > 0
      ? Number(invoice.grandTotal)
      : currentBooking
        ? Number(currentBooking.totalAmount)
        : 0;
  const totalPaid = payments
    .filter((p) => p.status === 'SUCCESS')
    .reduce((sum, p) => sum + Number(p.totalAmount), 0);
  const balanceDue = Math.max(0, totalCharges - totalPaid);

  const handleOpenPaymentModal = () => {
    if (!currentBooking) return;
    setPayAmount(Math.max(0, balanceDue));
    setIsInvoicePaymentFlowOpen(true);
  };

  const handleSubmitPayment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentBooking || payAmount <= 0) return;

    // D2 — validate mã tham chiếu theo method ngay tại FE (BE cũng bắt, code 8007):
    // popup → giữ nguyên dữ liệu đã nhập → focus lại ô (§6.2)
    const refConfig = REFERENCE_CONFIG[payMethod];
    if (refConfig.required && !payReference.trim()) {
      setPaymentError({
        code: 8007,
        message: `Phương thức "${refConfig.label.replace(' *', '')}" bắt buộc nhập mã tham chiếu. Vui lòng nhập để tiếp tục.`,
      });
      referenceInputRef.current?.focus();
      return;
    }

    setIsProcessingPayment(true);
    try {
      if (isGatewayMethod(payMethod)) {
        // D3 — tạo phiên thanh toán online → mở CheckoutModal (countdown + link gateway)
        const res = await financeService.createVnPayPayment({
          bookingId: currentBooking.id,
          method: 'VNPAY',
          bankCode: 'NCB',
          amount: payAmount,
        });
        if (res.result) {
          setActiveGateway('VNPAY');
          setGatewaySession(res.result);
          setIsPaymentModalOpen(false);
          setIsCheckoutModalOpen(true);
        }
      } else {
        await financeService.createPayment({
          bookingId: currentBooking.id,
          totalAmount: payAmount,
          paymentMethod: payMethod,
          paymentPurpose: payPurpose,
          paymentProvider: payProvider || undefined,
          transactionReference: payReference || undefined,
        });

        setIsPaymentModalOpen(false);
        setSuccessMessage('Thanh toán đã được ghi nhận thành công!');
        fetchFolioData(currentBooking.bookingNumber);
        void fetchGlobalPayments();
      }
    } catch (err) {
      // §6 bước ① — popup lỗi (code + message từ ApiResponse), KHÔNG mất dữ liệu đã nhập
      console.error('Lỗi thanh toán:', err);
      setPaymentError(getApiErrorMessage(err, 'Không thể ghi nhận thanh toán'));
    } finally {
      setIsProcessingPayment(false);
    }
  };

  // §6 bước ②③④ — hủy payment dở (PENDING → CANCELLED) → reset state → khách làm lại từ đầu
  const handleCancelAndRestart = async () => {
    setIsCancellingPayment(true);
    try {
      if (gatewaySession?.paymentId) {
        try {
          await financeService.cancelPayment(gatewaySession.paymentId);
        } catch (err) {
          // Best-effort: payment có thể đã xử lý xong qua webhook — vẫn reset FE
          console.warn('Không hủy được payment (có thể đã xử lý):', err);
        }
      }
      setGatewaySession(null);
      setIsCheckoutModalOpen(false);
      setIsPaymentModalOpen(false);
      setPayReference('');
      setPayProvider('');
      setPaymentError(null);
      void fetchGlobalPayments();
      if (currentBooking) {
        void fetchFolioData(currentBooking.bookingNumber);
      }
    } finally {
      setIsCancellingPayment(false);
    }
  };

  const handleOpenRefundModal = (payment: PaymentResponse) => {
    setRefundPaymentId(payment.id);
    setRefundAmount(Number(payment.totalAmount));
    setRefundReason('Hoàn tiền trả phòng sớm');
    setIsRefundModalOpen(true);
  };

  const handleConfirmRefund = async () => {
    if (!refundPaymentId) return;
    setIsRefunding(true);
    try {
      await financeService.refundPayment(refundPaymentId, refundAmount, refundReason);
      setIsRefundModalOpen(false);
      setSuccessMessage('Hoàn tiền thành công!');
      await fetchGlobalPayments();
      if (currentBooking) {
        await fetchFolioData(currentBooking.bookingNumber);
      }
    } catch (err) {
      console.error('Lỗi hoàn tiền:', err);
      setPaymentError(getApiErrorMessage(err, 'Hoàn tiền thất bại'));
    } finally {
      setIsRefunding(false);
    }
  };

  const handleGenerateInvoice = async () => {
    if (!currentBooking) return;
    setIsGeneratingInvoice(true);
    try {
      const res = await financeService.createInvoice({
        bookingId: currentBooking.id,
        serviceFeeRate: currentBooking.serviceFeeRate,
      });
      if (res.result) {
        setInvoice(res.result);
        setSuccessMessage('Tạo / đồng bộ hóa đơn GTGT thành công!');
      }
    } catch (err) {
      console.error('Lỗi tạo hóa đơn:', err);
      setPaymentError(getApiErrorMessage(err, 'Không thể tạo hóa đơn GTGT'));
    } finally {
      setIsGeneratingInvoice(false);
    }
  };

  const handleIssueInvoice = async () => {
    if (!invoice || !currentBooking) return;
    try {
      const res = await financeService.issueInvoice(invoice.id);
      if (res.result) {
        setInvoice(res.result);
        setSuccessMessage('Hóa đơn đã được phát hành chính thức (DRAFT → ISSUED)!');
      }
    } catch (err) {
      console.error('Lỗi phát hành hóa đơn:', err);
      setPaymentError(getApiErrorMessage(err, 'Phát hành hóa đơn thất bại'));
    }
  };

  const handleConfirmStatusTransition = async () => {
    if (!actionConfirm) return;
    setIsMutatingStatus(true);
    try {
      if (actionConfirm.type === 'complete') {
        await financeService.completePayment(actionConfirm.payment.id);
        setSuccessMessage(
          `Đã xác nhận hoàn tất giao dịch ${actionConfirm.payment.transactionReference || `PAY-${actionConfirm.payment.id}`} (PENDING → SUCCESS)!`
        );
      } else {
        await financeService.cancelPayment(actionConfirm.payment.id);
        setSuccessMessage(
          `Đã hủy giao dịch ${actionConfirm.payment.transactionReference || `PAY-${actionConfirm.payment.id}`} (PENDING → CANCELLED) và mở khóa phiên thanh toán.`
        );
      }
      setActionConfirm(null);
      await fetchGlobalPayments();
      if (currentBooking) {
        await fetchFolioData(currentBooking.bookingNumber);
      }
    } catch (err: unknown) {
      setActionConfirm(null);
      setPaymentError(
        getApiErrorMessage(
          err,
          actionConfirm.type === 'complete'
            ? 'Không thể xác nhận hoàn tất giao dịch'
            : 'Không thể hủy giao dịch'
        )
      );
    } finally {
      setIsMutatingStatus(false);
    }
  };

  const bookingMetaById = useMemo(() => {
    const map = new Map<number, { booking: BookingResponse; roomNumbers: string[] }>();
    bookingList.forEach((b) => {
      const roomNumbers =
        b.bookingDetails
          ?.flatMap((d) => d.bookingRooms || [])
          .map((r) => r.roomNumber)
          .filter((rn): rn is string => Boolean(rn)) || [];
      map.set(b.id, { booking: b, roomNumbers });
    });
    return map;
  }, [bookingList]);

  const handleOpenReceiptForPayment = async (p: PaymentResponse) => {
    try {
      let targetBooking: BookingResponse | null =
        p.bookingId === currentBooking?.id
          ? currentBooking
          : bookingMetaById.get(p.bookingId)?.booking || null;

      if (!targetBooking) {
        const res = await bookingService.getById(p.bookingId);
        targetBooking = res.result || null;
      }

      if (!targetBooking) {
        setPaymentError({
          code: 404,
          message: 'Không tìm thấy thông tin đơn đặt phòng để hiển thị biên lai',
        });
        return;
      }

      const targetInvoice =
        p.bookingId === currentBooking?.id
          ? invoice
          : (await financeService.getInvoiceByBookingId(p.bookingId).catch(() => null))?.result ||
            null;

      setReceiptBooking(targetBooking);
      setReceiptInvoice(targetInvoice);
      setSelectedReceiptPayment(p);
      setIsReceiptModalOpen(true);
    } catch (err) {
      setPaymentError(getApiErrorMessage(err, 'Không tải được thông tin đơn đặt phòng để in biên lai'));
    }
  };

  const invoiceStatusBadge = (status: InvoiceStatus) => {
    switch (status) {
      case 'PAID':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
            Đã thanh toán (PAID)
          </span>
        );
      case 'ISSUED':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-blue-50 text-blue-700 border border-blue-200">
            Đã phát hành (ISSUED)
          </span>
        );
      case 'CANCELLED':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-red-50 text-red-700 border border-red-200">
            Đã hủy (CANCELLED)
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-50 text-amber-700 border border-amber-200">
            Nháp (DRAFT)
          </span>
        );
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-neutral-900">Quản lý Tài chính & Folio Khách hàng</h1>
        <p className="text-sm text-neutral-500 mt-1">
          Theo dõi công nợ, giao dịch thanh toán, phụ phí và phát hành hóa đơn GTGT (VAT Invoice)
        </p>
      </div>

      {successMessage && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-2xl flex items-center justify-between">
          <span>{successMessage}</span>
          <button onClick={() => setSuccessMessage(null)} className="text-sm font-semibold underline">
            Đóng
          </button>
        </div>
      )}

      {errorMessage && (
        <div className="p-4 bg-red-50 border border-red-200 text-red-700 rounded-2xl">
          {errorMessage}
        </div>
      )}

      {/* KHỐI 1: TRA CỨU & DANH SÁCH ĐƠN ĐẶT PHÒNG (GỘP GỌN 1 KHUNG) */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-xs overflow-hidden">
        <div className="p-4 border-b border-neutral-200 space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <span className="font-bold text-neutral-900">Danh sách &amp; Tra cứu Đơn đặt phòng</span>
              <span className="text-xs text-neutral-500 ml-2">
                ({filteredBookingList.length} đơn • Chọn 1 đơn để xem công nợ &amp; Hóa đơn GTGT)
              </span>
            </div>
            <Button
              variant="ghost"
              size="sm"
              onClick={fetchBookingList}
              disabled={isListLoading}
              title="Tải lại danh sách đơn đặt phòng mới nhất"
            >
              <RefreshCw className={`w-4 h-4 mr-1.5 ${isListLoading ? 'animate-spin' : ''}`} />
              Tải lại danh sách
            </Button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-12 gap-2.5 items-center">
            <div className="md:col-span-5">
              <Input
                placeholder="Tìm theo mã đơn (VD: BK-TEST-1005)..."
                value={bookingNumberInput}
                onChange={(e) => {
                  setBookingNumberInput(e.target.value);
                  if (!e.target.value.trim()) {
                    setErrorMessage(null);
                  }
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') fetchFolioData(bookingNumberInput);
                }}
                leftIcon={<Search className="w-4 h-4" />}
              />
            </div>
            <div className="md:col-span-4">
              <Input
                placeholder="Lọc theo số phòng (VD: 101, 301E)..."
                value={listFilterKeyword}
                onChange={(e) => setListFilterKeyword(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') handleSearchByRoom();
                }}
              />
            </div>
            <div className="md:col-span-3 flex items-center gap-2 justify-end">
              <Button
                variant="primary"
                onClick={() => fetchFolioData(bookingNumberInput)}
                disabled={isLoading || !bookingNumberInput.trim()}
                className="flex-1 md:flex-none"
                title="Tìm đơn theo mã đặt phòng ở ô bên cạnh và mở chi tiết Folio"
              >
                <Search className="w-4 h-4 mr-1.5" />
                Tìm &amp; Mở Folio
              </Button>
              {(bookingNumberInput || listFilterKeyword) && (
                <Button
                  variant="outline"
                  onClick={() => {
                    setBookingNumberInput('');
                    setListFilterKeyword('');
                    setErrorMessage(null);
                  }}
                >
                  Đặt lại
                </Button>
              )}
            </div>
          </div>
        </div>

        {listError && (
          <div className="mx-4 mt-4 p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-xl">
            {listError}
          </div>
        )}

        {isListLoading && filteredBookingList.length === 0 ? (
          <div className="p-6 text-center text-sm text-neutral-500">Đang tải danh sách...</div>
        ) : filteredBookingList.length === 0 && !listError ? (
          <div className="p-6 text-center text-sm text-neutral-500">
            {bookingNumberInput.trim()
              ? `Không tìm thấy đơn đặt phòng nào khớp mã "${bookingNumberInput}"`
              : listFilterKeyword.trim()
                ? `Không tìm thấy đơn đặt phòng nào cho số phòng "${listFilterKeyword}"`
                : 'Chưa có đơn đặt phòng nào — hãy tạo đơn ở mục Đặt phòng'}
          </div>
        ) : (
          <div className="overflow-auto max-h-80">
            <table className="w-full text-left border-collapse text-sm">
              <thead className="bg-neutral-50 border-b border-neutral-200 text-neutral-600 font-medium">
                <tr>
                  <th className="p-3">Phòng vật lý / Mã đơn</th>
                  <th className="p-3">Khách hàng</th>
                  <th className="p-3">Khách sạn</th>
                  <th className="p-3 text-right">Tổng tiền</th>
                  <th className="p-3 text-center">Trạng thái</th>
                  <th className="p-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {filteredBookingList.map((b) => {
                  const isActive = currentBooking?.bookingNumber === b.bookingNumber;
                  const hasName = b.guestName && b.guestName !== b.guestPhone;
                  return (
                    <tr
                      key={b.id}
                      role="button"
                      tabIndex={0}
                      onClick={() => handleSelectBooking(b)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault();
                          handleSelectBooking(b);
                        }
                      }}
                      className={`cursor-pointer transition-colors focus:outline-none focus:ring-2 focus:ring-red-500 ${isActive ? 'bg-red-50' : 'hover:bg-neutral-50'
                        }`}
                    >
                      <td className="p-3">
                        {(() => {
                          const assignedRooms = b.bookingDetails?.flatMap((d) => d.bookingRooms || []) || [];
                          const roomsWithInfo = assignedRooms.filter((r) => r.roomNumber);
                          if (roomsWithInfo.length > 0) {
                            return (
                              <div className="space-y-0.5">
                                <div className="flex flex-wrap gap-1">
                                  {roomsWithInfo.map((r, idx) => (
                                    <span
                                      key={r.id || idx}
                                      className="inline-flex items-center px-2 py-0.5 rounded-lg text-xs font-bold bg-red-50 text-red-700 border border-red-200"
                                    >
                                      Phòng {r.roomNumber}
                                    </span>
                                  ))}
                                </div>
                                <div className="text-[11px] font-mono text-neutral-400">
                                  {b.bookingNumber}
                                </div>
                              </div>
                            );
                          }
                          return (
                            <div>
                              <span className="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium bg-neutral-100 text-neutral-600">
                                Chưa xếp phòng
                              </span>
                              <div className="text-[11px] font-mono text-neutral-400 mt-0.5">
                                {b.bookingNumber}
                              </div>
                            </div>
                          );
                        })()}
                      </td>
                      <td className="p-3">
                        <div className="font-medium text-neutral-900">
                          {hasName ? b.guestName : b.guestPhone}
                        </div>
                        {hasName && (
                          <div className="text-xs text-neutral-500">{b.guestPhone}</div>
                        )}
                      </td>
                      <td className="p-3 text-neutral-700">{b.hotelName}</td>
                      <td className="p-3 text-right font-semibold text-neutral-900">
                        {formatCurrency(b.totalAmount)}
                      </td>
                      <td className="p-3 text-center">{bookingStatusBadge(b.status)}</td>
                      <td className="p-3 text-right">
                        <Button
                          variant={isActive ? 'secondary' : 'ghost'}
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleSelectBooking(b);
                          }}
                        >
                          {isActive ? 'Đang xem' : 'Mở Folio →'}
                        </Button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {currentBooking && (
        <div className="space-y-6">
          {/* THÔNG TIN PHÒNG VẬT LÝ & KHÁCH HÀNG CỦA HÓA ĐƠN FOLIO */}
          <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs flex flex-wrap items-center justify-between gap-4">
            <div className="flex flex-wrap items-center gap-3">
              <div className="space-y-1">
                <span className="text-xs text-neutral-500 font-medium block">Phòng nhận hóa đơn / lưu trú:</span>
                <div className="flex flex-wrap gap-1.5">
                  {(() => {
                    const assignedRooms = currentBooking.bookingDetails?.flatMap((d) => d.bookingRooms || []) || [];
                    const roomsWithInfo = assignedRooms.filter((r) => r.roomNumber);
                    if (roomsWithInfo.length > 0) {
                      return roomsWithInfo.map((r, idx) => (
                        <span
                          key={r.id || idx}
                          className="inline-flex items-center px-3 py-1 rounded-xl text-sm font-bold bg-red-50 text-red-700 border border-red-200"
                        >
                          Phòng {r.roomNumber}
                        </span>
                      ));
                    }
                    return (
                      <span className="inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-medium bg-neutral-100 text-neutral-600">
                        Chưa xếp phòng vật lý
                      </span>
                    );
                  })()}
                </div>
              </div>

              <div className="h-8 w-px bg-neutral-200 hidden sm:block mx-2" />

              <div className="text-sm">
                <div className="font-bold text-neutral-900">{currentBooking.guestName || currentBooking.guestPhone}</div>
                <div className="text-xs text-neutral-500">
                  SĐT: <span className="font-medium text-neutral-700">{currentBooking.guestPhone}</span> • Khách sạn: <span className="font-medium text-neutral-700">{currentBooking.hotelName}</span>
                </div>
              </div>
            </div>

            <div className="text-right text-xs text-neutral-500">
              <div>Mã đơn đặt phòng: <strong className="font-mono text-neutral-800">{currentBooking.bookingNumber}</strong></div>
              <div>Trạng thái đơn: <span className="font-semibold text-neutral-800">{currentBooking.status}</span></div>
            </div>
          </div>

          {/* FOLIO SUMMARY CARDS */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
              <span className="text-xs font-semibold text-neutral-500 uppercase tracking-wider">
                Tổng phát sinh (Charges)
              </span>
              <p className="text-2xl font-bold text-neutral-900 mt-1">
                {formatCurrency(totalCharges)}
              </p>
              <span className="text-xs text-neutral-400 mt-1 block">
                Bao gồm tiền phòng, phí dịch vụ & VAT
              </span>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
              <span className="text-xs font-semibold text-neutral-500 uppercase tracking-wider">
                Đã thanh toán (Paid)
              </span>
              <p className="text-2xl font-bold text-emerald-600 mt-1">
                {formatCurrency(totalPaid)}
              </p>
              <span className="text-xs text-neutral-400 mt-1 block">
                {payments.filter((p) => p.status === 'SUCCESS').length} giao dịch thành công
              </span>
            </div>

            <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
              <span className="text-xs font-semibold text-neutral-500 uppercase tracking-wider">
                Còn lại cần thu (Balance Due)
              </span>
              <p className={`text-2xl font-bold mt-1 ${balanceDue > 0 ? 'text-red-600' : 'text-emerald-600'}`}>
                {formatCurrency(Math.max(0, balanceDue))}
              </p>
              <span className="text-xs text-neutral-400 mt-1 block">
                {balanceDue < 0
                  ? `Đã thanh toán đủ (dư thu ${formatCurrency(Math.abs(balanceDue))})`
                  : balanceDue === 0
                    ? 'Đã quyết toán toàn bộ'
                    : 'Chưa tất toán'}
              </span>
            </div>
          </div>

          {/* ACTION BUTTONS */}
          <div className="flex flex-wrap gap-3">
            <Button variant="primary" onClick={handleOpenPaymentModal}>
              <DollarSign className="w-4 h-4 mr-2" />
              Thu tiền / Thanh toán mới
            </Button>

            {!invoice && (
              <Button
                variant="outline"
                onClick={handleGenerateInvoice}
                disabled={isGeneratingInvoice}
              >
                <Receipt className="w-4 h-4 mr-2" />
                {isGeneratingInvoice ? 'Đang tạo...' : 'Tạo hóa đơn GTGT'}
              </Button>
            )}

            {invoice && invoice.status === 'DRAFT' && (
              <Button variant="primary" onClick={handleIssueInvoice}>
                <CheckCircle className="w-4 h-4 mr-2" />
                Phát hành hóa đơn ({invoice.invoiceNumber})
              </Button>
            )}

            <Button
              variant="outline"
              onClick={() => {
                setReceiptBooking(currentBooking);
                setReceiptInvoice(invoice);
                setSelectedReceiptPayment(payments.find((p) => p.status === 'SUCCESS') || null);
                setIsReceiptModalOpen(true);
              }}
              className="border-neutral-300 hover:bg-neutral-50 text-neutral-800"
            >
              <Printer className="w-4 h-4 mr-2 text-neutral-600" />
              Xem &amp; In Biên lai Folio
            </Button>

            <Button
              variant="outline"
              onClick={() => fetchFolioData(currentBooking.bookingNumber)}
              disabled={isLoading}
              title="Đồng bộ lại công nợ, giao dịch và hóa đơn mới nhất của đơn đang xem"
            >
              <RefreshCw className={`w-4 h-4 mr-2 ${isLoading ? 'animate-spin' : ''}`} />
              Làm mới Folio
            </Button>
          </div>

          {/* CHI TIẾT HÓA ĐƠN GTGT & THÔNG TIN DỊCH VỤ */}
          {invoice ? (
            (() => {
              const effectiveSubTotal =
                Number(invoice.subTotal) > 0
                  ? Number(invoice.subTotal)
                  : Number(currentBooking.subtotalAmount || 0);
              const effectiveServiceFee =
                Number(invoice.grandTotal) > 0
                  ? Number(invoice.serviceFeeAmount)
                  : Number(currentBooking.serviceFeeAmount || 0);
              const effectiveVat =
                Number(invoice.grandTotal) > 0
                  ? Number(invoice.vatAmount)
                  : Number(currentBooking.totalVatAmount || 0);
              const effectiveGrandTotal =
                Number(invoice.grandTotal) > 0
                  ? Number(invoice.grandTotal)
                  : Number(currentBooking.totalAmount || totalCharges);
              const invoiceRemainingDue = Math.max(0, effectiveGrandTotal - totalPaid);
              const isInvoicePaid =
                invoice.status === 'PAID' || (effectiveGrandTotal > 0 && invoiceRemainingDue <= 0);
              const isBrokenZeroInvoice =
                (!invoice.details || invoice.details.length === 0) &&
                Number(invoice.grandTotal) === 0 &&
                effectiveGrandTotal > 0;

              const bookingFallbackRows = (() => {
                const rows: Array<{
                  key: string;
                  description: string;
                  quantity: number;
                  unitPrice: number;
                  vatRate: number;
                  vatAmount: number;
                  totalAmount: number;
                }> = [];
                const details = currentBooking.bookingDetails || [];
                details.forEach((d, dIdx) => {
                  const roomNums = (d.bookingRooms || [])
                    .map((r) => r.roomNumber)
                    .filter((n): n is string => Boolean(n));
                  const prefix = roomNums.length > 0 ? `Phòng ${roomNums.join(', ')} - ` : '';
                  const qty = d.quantity || 1;
                  const lineSub =
                    details.length > 0 ? Math.round(effectiveSubTotal / details.length) : effectiveSubTotal;
                  const lineVat =
                    details.length > 0 ? Math.round(effectiveVat / details.length) : effectiveVat;
                  const lineFee =
                    details.length > 0 ? Math.round(effectiveServiceFee / details.length) : effectiveServiceFee;
                  const vatRateVal =
                    lineSub > 0 && lineVat > 0 ? Math.round((lineVat / lineSub) * 100) : 0;

                  rows.push({
                    key: `bd-${d.id || dIdx}`,
                    description: `${prefix}${d.roomTypeName || 'Lưu trú phòng'} (${d.checkInDate} ➔ ${d.checkOutDate})`,
                    quantity: qty,
                    unitPrice: qty > 0 ? Math.round(lineSub / qty) : lineSub,
                    vatRate: vatRateVal,
                    vatAmount: lineVat,
                    totalAmount: lineSub + lineFee + lineVat,
                  });

                  (d.bookingRooms || []).forEach((rm, rmIdx) => {
                    (rm.charges || []).forEach((ch, chIdx) => {
                      rows.push({
                        key: `chg-${rmIdx}-${ch.id || chIdx}`,
                        description: `${rm.roomNumber ? `Phòng ${rm.roomNumber} - ` : ''}${ch.itemName}`,
                        quantity: ch.quantity || 1,
                        unitPrice: Number(ch.unitPrice),
                        vatRate: Number(ch.vatRate || 0),
                        vatAmount: Number(ch.vatAmount || 0),
                        totalAmount: Number(ch.totalAmount),
                      });
                    });
                  });
                });
                return rows;
              })();

              const serviceRows =
                invoice.details && invoice.details.length > 0
                  ? invoice.details.map((d) => ({
                      key: `inv-${d.id}`,
                      description: d.description,
                      quantity: d.quantity,
                      unitPrice: Number(d.unitPrice),
                      vatRate: Number(d.vatRate),
                      vatAmount: Number(d.vatAmount),
                      totalAmount: Number(d.totalAmount),
                    }))
                  : bookingFallbackRows;

              return (
                <div
                  role="button"
                  tabIndex={0}
                  onClick={() => {
                    if (isInvoicePaid) {
                      setReceiptBooking(currentBooking);
                      setSelectedReceiptPayment(payments.find((p) => p.status === 'SUCCESS') || null);
                      setIsReceiptModalOpen(true);
                    } else {
                      setIsInvoicePaymentFlowOpen(true);
                    }
                  }}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      if (isInvoicePaid) {
                        setReceiptBooking(currentBooking);
                        setSelectedReceiptPayment(payments.find((p) => p.status === 'SUCCESS') || null);
                        setIsReceiptModalOpen(true);
                      } else {
                        setIsInvoicePaymentFlowOpen(true);
                      }
                    }
                  }}
                  className={`bg-white rounded-2xl border shadow-xs p-6 space-y-4 cursor-pointer transition-all group ${
                    isInvoicePaid
                      ? 'border-emerald-200 hover:border-emerald-400 hover:shadow-md'
                      : 'border-neutral-200 hover:border-red-400 hover:shadow-md'
                  }`}
                >
                  <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4 border-b border-neutral-100 pb-4">
                    <div className="min-w-0 flex-1 space-y-1.5">
                      <div className="flex flex-wrap items-center gap-2">
                        <h3 className="text-lg font-bold text-neutral-900">
                          Hóa đơn GTGT: {invoice.invoiceNumber}
                        </h3>
                        {isInvoicePaid ? (
                          <span className="inline-flex items-center gap-1 text-xs px-2.5 py-0.5 rounded-full font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                            <CheckCircle2 className="w-3.5 h-3.5" />
                            Đã thanh toán thành công
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-xs px-2.5 py-0.5 rounded-full font-bold bg-red-50 text-red-700 border border-red-200">
                            <Receipt className="w-3.5 h-3.5" />
                            Chưa thanh toán đủ • Nhấp để thanh toán
                          </span>
                        )}
                        {invoiceStatusBadge(isInvoicePaid ? 'PAID' : invoice.status)}
                        {(() => {
                          const assignedRooms =
                            currentBooking.bookingDetails?.flatMap((d) => d.bookingRooms || []) || [];
                          const roomsWithInfo = assignedRooms.filter((r) => r.roomNumber);
                          return roomsWithInfo.map((r, idx) => (
                            <span
                              key={r.id || idx}
                              className="inline-flex items-center px-2.5 py-0.5 rounded-lg text-xs font-bold bg-neutral-100 text-neutral-700"
                            >
                              Phòng {r.roomNumber}
                            </span>
                          ));
                        })()}
                      </div>
                      <span className="text-xs text-neutral-500 block">
                        Đơn đặt phòng: <b className="font-mono text-neutral-800">{currentBooking.bookingNumber}</b> | Khách hàng:{' '}
                        <b className="text-neutral-800">{currentBooking.guestName}</b> | Ngày lập:{' '}
                        {new Date(invoice.createdAt).toLocaleDateString('vi-VN')}
                      </span>
                      {isBrokenZeroInvoice && (
                        <div className="inline-flex items-center gap-2 text-[11px] text-amber-700 bg-amber-50 border border-amber-200 px-2.5 py-1 rounded-lg">
                          <span>Chi tiết hóa đơn đang được khôi phục từ đơn đặt phòng.</span>
                          <button
                            type="button"
                            onClick={(e) => {
                              e.stopPropagation();
                              void handleGenerateInvoice();
                            }}
                            className="font-bold underline cursor-pointer"
                          >
                            Đồng bộ lưu vào DB
                          </button>
                        </div>
                      )}
                    </div>

                    <div className="shrink-0 flex items-center justify-end gap-2">
                      {isInvoicePaid ? (
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={(e) => {
                            e.stopPropagation();
                            setReceiptBooking(currentBooking);
                            setReceiptInvoice(invoice);
                            setSelectedReceiptPayment(
                              payments.find((p) => p.status === 'SUCCESS') || null
                            );
                            setIsReceiptModalOpen(true);
                          }}
                        >
                          <Printer className="w-4 h-4 mr-1.5" />
                          Xem &amp; In Hóa đơn
                        </Button>
                      ) : (
                        <Button
                          size="sm"
                          variant="primary"
                          onClick={(e) => {
                            e.stopPropagation();
                            setIsInvoicePaymentFlowOpen(true);
                          }}
                        >
                          <Receipt className="w-4 h-4 mr-1.5" />
                          Thanh toán hóa đơn này
                        </Button>
                      )}
                    </div>
                  </div>

                  {/* Chi tiết từng dòng dịch vụ / phòng trên hóa đơn */}
                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-xs">
                      <thead className="bg-neutral-50 text-neutral-600 font-semibold">
                        <tr>
                          <th className="p-3">Khoản mục / Dịch vụ</th>
                          <th className="p-3 text-center">SL</th>
                          <th className="p-3 text-right">Đơn giá</th>
                          <th className="p-3 text-right">VAT (%)</th>
                          <th className="p-3 text-right">Tiền thuế</th>
                          <th className="p-3 text-right">Thành tiền</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-neutral-100">
                        {serviceRows.length === 0 ? (
                          <tr>
                            <td colSpan={6} className="p-4 text-center text-neutral-400">
                              Chưa có hạng mục dịch vụ chi tiết cho hóa đơn này.
                            </td>
                          </tr>
                        ) : (
                          serviceRows.map((row) => (
                            <tr key={row.key}>
                              <td className="p-3 font-medium text-neutral-800">{row.description}</td>
                              <td className="p-3 text-center">{row.quantity}</td>
                              <td className="p-3 text-right">{formatCurrency(row.unitPrice)}</td>
                              <td className="p-3 text-right">{row.vatRate}%</td>
                              <td className="p-3 text-right">{formatCurrency(row.vatAmount)}</td>
                              <td className="p-3 text-right font-bold text-neutral-900">
                                {formatCurrency(row.totalAmount)}
                              </td>
                            </tr>
                          ))
                        )}
                      </tbody>
                      <tfoot className="bg-neutral-50/70 border-t border-neutral-200 text-xs">
                        <tr>
                          <td colSpan={5} className="p-2.5 text-right text-neutral-600">
                            Tạm tính (Trước thuế &amp; phí):
                          </td>
                          <td className="p-2.5 text-right font-semibold text-neutral-800">
                            {formatCurrency(effectiveSubTotal)}
                          </td>
                        </tr>
                        {effectiveServiceFee > 0 && (
                          <tr>
                            <td colSpan={5} className="p-2 text-right text-neutral-600">
                              Phí dịch vụ ({invoice.serviceFeeRate ?? currentBooking.serviceFeeRate ?? 0}%):
                            </td>
                            <td className="p-2 text-right font-semibold text-neutral-800">
                              +{formatCurrency(effectiveServiceFee)}
                            </td>
                          </tr>
                        )}
                        <tr>
                          <td colSpan={5} className="p-2 text-right text-neutral-600">
                            Thuế GTGT (VAT):
                          </td>
                          <td className="p-2 text-right font-semibold text-neutral-800">
                            +{formatCurrency(effectiveVat)}
                          </td>
                        </tr>
                        <tr className="border-t border-neutral-200">
                          <td colSpan={5} className="p-3 text-right font-bold text-neutral-900">
                            {isInvoicePaid ? 'Tổng hóa đơn (Đã thanh toán thành công):' : 'Tổng cộng hóa đơn:'}
                          </td>
                          <td className={`p-3 text-right font-black text-sm ${isInvoicePaid ? 'text-emerald-700' : 'text-red-600'}`}>
                            {formatCurrency(effectiveGrandTotal)}
                          </td>
                        </tr>
                        {totalPaid > 0 && (
                          <tr>
                            <td colSpan={5} className="px-3 py-1.5 text-right text-neutral-600">
                              Đã thanh toán:
                            </td>
                            <td className="px-3 py-1.5 text-right font-semibold text-emerald-700">
                              -{formatCurrency(totalPaid)}
                            </td>
                          </tr>
                        )}
                        <tr>
                          <td colSpan={5} className="px-3 pb-3 pt-1 text-right font-semibold text-neutral-700">
                            Còn lại cần thu:
                          </td>
                          <td
                            className={`px-3 pb-3 pt-1 text-right font-bold ${
                              invoiceRemainingDue > 0 ? 'text-red-600' : 'text-emerald-700'
                            }`}
                          >
                            {formatCurrency(invoiceRemainingDue)}
                          </td>
                        </tr>
                      </tfoot>
                    </table>
                  </div>
                </div>
              );
            })()
          ) : (
            <div className="bg-white rounded-2xl border border-neutral-200 shadow-xs p-5 space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-neutral-100 pb-3">
                <div>
                  <h3 className="text-base font-bold text-neutral-900 flex items-center gap-2">
                    <span>Bảng kê Dịch vụ &amp; Trạng thái Thanh toán</span>
                    {balanceDue <= 0 ? (
                      <span className="inline-flex items-center gap-1 text-xs px-2.5 py-0.5 rounded-full font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        Đã thanh toán thành công
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 text-xs px-2.5 py-0.5 rounded-full font-bold bg-amber-50 text-amber-700 border border-amber-200">
                        Chưa xuất Hóa đơn GTGT • Cần thu {formatCurrency(balanceDue)}
                      </span>
                    )}
                  </h3>
                  <p className="text-xs text-neutral-500 mt-0.5">
                    Đơn {currentBooking.bookingNumber} hiện chưa tạo Hóa đơn GTGT chính thức.
                  </p>
                </div>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => void handleGenerateInvoice()}
                  disabled={isGeneratingInvoice}
                >
                  <Receipt className="w-4 h-4 mr-1.5" />
                  {isGeneratingInvoice ? 'Đang tạo...' : 'Tạo hóa đơn GTGT ngay'}
                </Button>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
                {(currentBooking.bookingDetails || []).map((d, idx) => (
                  <div
                    key={d.id || idx}
                    className="p-3 bg-neutral-50 rounded-xl border border-neutral-200 flex items-center justify-between"
                  >
                    <div>
                      <div className="font-bold text-neutral-900">{d.roomTypeName}</div>
                      <div className="text-neutral-500">
                        {d.checkInDate} ➔ {d.checkOutDate} • SL: {d.quantity}
                      </div>
                    </div>
                    <span className="font-bold text-neutral-800">
                      {(d.bookingRooms || [])
                        .map((r) => r.roomNumber)
                        .filter(Boolean)
                        .map((rn) => `P.${rn}`)
                        .join(', ') || 'Chưa xếp phòng'}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* KHỐI 3: LỊCH SỬ GIAO DỊCH & THANH TOÁN TOÀN HỆ THỐNG (1 BẢNG DUY NHẤT, LUÔN HIỂN THỊ) */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-xs overflow-hidden">
        <div className="p-4 border-b border-neutral-200 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-red-50 text-red-600 flex items-center justify-center">
              <History className="w-5 h-5" />
            </div>
            <div>
              <div className="font-bold text-neutral-900 flex items-center gap-2">
                <span>Lịch sử Giao dịch &amp; Thanh toán</span>
                <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-neutral-100 text-neutral-700">
                  {globalTotalElements} giao dịch
                </span>
              </div>
              <p className="text-xs text-neutral-500">
                Lưu toàn bộ thanh toán của mọi phòng/đơn đặt phòng — luôn hiển thị &amp; tương tác trực tiếp trạng thái
              </p>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <select
              aria-label="Lọc theo phương thức thanh toán"
              value={globalMethodFilter}
              onChange={(e) => {
                setGlobalMethodFilter(e.target.value as 'ALL' | PaymentMethod);
                setGlobalPage(1);
              }}
              className="h-9 px-3 text-xs font-medium bg-white border border-neutral-200 rounded-xl text-neutral-700 focus:outline-none focus:ring-2 focus:ring-red-500"
            >
              <option value="ALL">Tất cả phương thức</option>
              <option value="VNPAY">VNPay Sandbox (VNPAY)</option>
              <option value="CASH">Tiền mặt (CASH)</option>
              <option value="BANK_TRANSFER">Chuyển khoản (BANK_TRANSFER)</option>
              <option value="CREDIT_CARD">Thẻ POS (CREDIT_CARD)</option>
              <option value="DEBIT_CARD">Thẻ ghi nợ (DEBIT_CARD)</option>
            </select>

            <Button
              variant="outline"
              size="sm"
              onClick={() => void fetchGlobalPayments()}
              disabled={isLoadingGlobal}
            >
              <RefreshCw className={`w-3.5 h-3.5 mr-1.5 ${isLoadingGlobal ? 'animate-spin' : ''}`} />
              Làm mới
            </Button>
          </div>
        </div>

        {/* Bộ lọc nhanh theo trạng thái toàn hệ thống */}
        <div className="px-4 py-2.5 bg-neutral-50/70 border-b border-neutral-100 flex flex-wrap items-center gap-1.5 text-xs">
          {(
            [
              { key: 'ALL', label: 'Tất cả' },
              { key: 'PENDING', label: 'PENDING (Đang chờ)' },
              { key: 'SUCCESS', label: 'SUCCESS (Thành công)' },
              { key: 'FAILED', label: 'FAILED (Thất bại)' },
              { key: 'CANCELLED', label: 'CANCELLED (Đã hủy)' },
              { key: 'REFUNDED', label: 'REFUNDED (Đã hoàn)' },
            ] as const
          ).map((tab) => {
            const active = globalStatusFilter === tab.key;
            return (
              <button
                key={tab.key}
                type="button"
                onClick={() => {
                  setGlobalStatusFilter(tab.key);
                  setGlobalPage(1);
                }}
                className={`px-2.5 py-1 rounded-xl font-semibold transition-colors cursor-pointer ${
                  active
                    ? 'bg-red-600 text-white shadow-2xs'
                    : 'bg-white border border-neutral-200 text-neutral-600 hover:bg-neutral-100'
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>

        {globalError && (
          <div className="m-4 p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-xl">
            {globalError}
          </div>
        )}

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead className="bg-neutral-50 border-b border-neutral-200 text-neutral-600 font-medium">
              <tr>
                <th className="p-3.5">Mã giao dịch</th>
                <th className="p-3.5">Đơn đặt phòng / Phòng</th>
                <th className="p-3.5">Phương thức</th>
                <th className="p-3.5 text-right">Số tiền (VNĐ)</th>
                <th className="p-3.5 text-center">Trạng thái</th>
                <th className="p-3.5">Thời gian</th>
                <th className="p-3.5 text-center">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100">
              {isLoadingGlobal && globalPayments.length === 0 ? (
                <tr>
                  <td colSpan={7} className="p-6 text-center text-neutral-500 text-sm">
                    Đang tải lịch sử giao dịch...
                  </td>
                </tr>
              ) : globalPayments.length === 0 ? (
                <tr>
                  <td colSpan={7} className="p-6 text-center text-neutral-400 text-sm">
                    Không có giao dịch nào khớp bộ lọc hiện tại.
                  </td>
                </tr>
              ) : (
                globalPayments.map((p) => {
                  const meta = bookingMetaById.get(p.bookingId);
                  const rooms = meta?.roomNumbers || [];
                  const bookingCode = p.bookingNumber || meta?.booking.bookingNumber || `#${p.bookingId}`;
                  const isCurrentBookingRow = currentBooking?.id === p.bookingId;
                  return (
                    <tr
                      key={`global-${p.id}`}
                      className={isCurrentBookingRow ? 'bg-red-50/35 hover:bg-red-50/60' : 'hover:bg-neutral-50/80'}
                    >
                      <td className="p-3.5">
                        <div className="font-mono font-semibold text-neutral-900">
                          {p.transactionReference || `PAY-${p.id}`}
                        </div>
                        {(p.gatewayTxnId || p.gatewayResponseCode) && (
                          <div className="text-[11px] text-neutral-500 mt-0.5 font-mono">
                            {p.gatewayTxnId ? `GW: ${p.gatewayTxnId}` : ''}
                            {p.gatewayTxnId && p.gatewayResponseCode ? ' • ' : ''}
                            {p.gatewayResponseCode ? `Mã ${p.gatewayResponseCode}` : ''}
                            {p.bankCode ? ` (${p.bankCode})` : ''}
                          </div>
                        )}
                      </td>
                      <td className="p-3.5">
                        <button
                          type="button"
                          onClick={() => {
                            setBookingNumberInput(bookingCode);
                            void fetchFolioData(bookingCode);
                          }}
                          className="text-left group cursor-pointer"
                          title="Nhấp để mở chi tiết Folio & Hóa đơn của đơn này"
                        >
                          <div className="font-mono text-xs font-bold text-red-600 group-hover:underline">
                            {bookingCode}
                          </div>
                          {rooms.length > 0 && (
                            <div className="flex flex-wrap gap-1 mt-0.5">
                              {rooms.map((rn, idx) => (
                                <span
                                  key={idx}
                                  className="px-1.5 py-0.2 rounded bg-neutral-100 text-neutral-700 text-[10px] font-bold"
                                >
                                  P.{rn}
                                </span>
                              ))}
                            </div>
                          )}
                        </button>
                      </td>
                      <td className="p-3.5 text-neutral-700 text-xs">
                        <div className="font-semibold">{p.paymentMethod}</div>
                        <div className="text-[11px] text-neutral-400">
                          {p.paymentProvider || p.paymentPurpose}
                        </div>
                      </td>
                      <td className="p-3.5 text-right font-bold text-neutral-900">
                        {formatCurrency(p.totalAmount)}
                      </td>
                      <td className="p-3.5 text-center">
                        <PaymentStatusBadge status={p.status} />
                      </td>
                      <td className="p-3.5 text-xs text-neutral-500">
                        {p.paidAt
                          ? new Date(p.paidAt).toLocaleString('vi-VN')
                          : p.createdAt
                            ? new Date(p.createdAt).toLocaleString('vi-VN')
                            : 'N/A'}
                      </td>
                      <td className="p-3.5 text-center">
                        <div className="flex flex-wrap items-center justify-center gap-1.5">
                          {p.status === 'PENDING' && (
                            <>
                              <button
                                type="button"
                                onClick={() => setActionConfirm({ type: 'complete', payment: p })}
                                className="text-xs text-emerald-700 hover:text-emerald-900 font-semibold px-2 py-1 rounded-lg bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 flex items-center gap-1 cursor-pointer"
                                title="Xác nhận giao dịch thành công (PENDING → SUCCESS)"
                              >
                                <CheckCircle2 className="w-3.5 h-3.5" />
                                Xác nhận
                              </button>
                              <button
                                type="button"
                                onClick={() => setActionConfirm({ type: 'cancel', payment: p })}
                                className="text-xs text-red-600 hover:text-red-800 font-semibold px-2 py-1 rounded-lg bg-red-50 hover:bg-red-100 border border-red-200 flex items-center gap-1 cursor-pointer"
                                title="Hủy giao dịch PENDING & mở khóa Redis"
                              >
                                <XCircle className="w-3.5 h-3.5" />
                                Hủy
                              </button>
                            </>
                          )}
                          {p.status === 'SUCCESS' && (
                            <>
                              <button
                                type="button"
                                onClick={() => void handleOpenReceiptForPayment(p)}
                                className="text-xs text-neutral-700 hover:text-neutral-900 font-semibold px-2 py-1 rounded-md hover:bg-neutral-100 flex items-center gap-1 border border-neutral-200 cursor-pointer"
                              >
                                <Printer className="w-3.5 h-3.5" />
                                Biên lai
                              </button>
                              <button
                                type="button"
                                onClick={() => handleOpenRefundModal(p)}
                                className="text-xs text-amber-700 hover:text-amber-900 font-semibold px-2 py-1 rounded-md bg-amber-50 hover:bg-amber-100 border border-amber-200 cursor-pointer"
                              >
                                Hoàn tiền
                              </button>
                            </>
                          )}
                          {(p.status === 'FAILED' || p.status === 'CANCELLED') && (
                            <button
                              type="button"
                              onClick={() => {
                                setBookingNumberInput(bookingCode);
                                void fetchFolioData(bookingCode);
                              }}
                              className="text-xs text-red-600 hover:text-red-800 font-semibold px-2 py-1 rounded-lg bg-red-50 hover:bg-red-100 border border-red-200 cursor-pointer"
                            >
                              Mở Folio
                            </button>
                          )}
                          {p.status === 'REFUNDED' && (
                            <button
                              type="button"
                              onClick={() => void handleOpenReceiptForPayment(p)}
                              className="text-xs text-neutral-600 hover:text-neutral-900 font-semibold px-2 py-1 rounded-md hover:bg-neutral-100 flex items-center gap-1 border border-neutral-200 cursor-pointer"
                            >
                              <Printer className="w-3.5 h-3.5" />
                              Biên lai
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        <div className="px-4 py-3 border-t border-neutral-100 bg-neutral-50/50 flex flex-wrap items-center justify-between gap-2 text-xs text-neutral-600">
          <span>
            Trang <strong>{globalPage}</strong> / <strong>{globalTotalPages}</strong> • Tổng{' '}
            <strong>{globalTotalElements}</strong> giao dịch
          </span>
          <div className="flex items-center gap-1.5">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setGlobalPage((prev) => Math.max(1, prev - 1))}
              disabled={globalPage <= 1 || isLoadingGlobal}
            >
              <ChevronLeft className="w-4 h-4 mr-1" />
              Trước
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setGlobalPage((prev) => Math.min(globalTotalPages, prev + 1))}
              disabled={globalPage >= globalTotalPages || isLoadingGlobal}
            >
              Sau
              <ChevronRight className="w-4 h-4 ml-1" />
            </Button>
          </div>
        </div>
      </div>

      {/* MODAL THANH TOÁN */}
      <Modal
        isOpen={isPaymentModalOpen}
        onClose={() => setIsPaymentModalOpen(false)}
        title="Ghi nhận Thanh toán Folio"
      >
        <form onSubmit={handleSubmitPayment} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">Số tiền thanh toán (VNĐ) *</label>
            <CurrencyInput
              value={payAmount}
              onChangeValue={(val) => setPayAmount(val)}
              placeholder="0"
              required
              className="font-bold text-neutral-900"
              suffix="VNĐ"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">Phương thức thanh toán</label>
              <Select<PaymentMethod>
                value={payMethod}
                onChange={(val) => setPayMethod(val as PaymentMethod)}
                options={[
                  { value: 'CASH', label: 'Tiền mặt tại quầy (Trực tiếp)' },
                  { value: 'VNPAY', label: 'Thẻ ATM nội địa (Cổng VNPAY)' },
                ]}
                size="lg"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">Mục đích thu</label>
              <Select<PaymentPurpose>
                value={payPurpose}
                onChange={(val) => setPayPurpose(val as PaymentPurpose)}
                options={[
                  { value: 'FULL_PAYMENT', label: 'Thanh toán toàn bộ' },
                  { value: 'DEPOSIT', label: 'Đặt cọc tiền phòng' },
                  { value: 'INCIDENTAL_DEPOSIT', label: 'Đặt cọc phát sinh' },
                ]}
                size="lg"
              />
            </div>
          </div>

          {isGatewayMethod(payMethod) ? (
            // D2 — VNPAY: KHÔNG nhập tay mã tham chiếu (webhook tự ghi) + hiện số tiền theo tổng đơn
            <div className="bg-blue-50 border border-blue-200 text-blue-700 rounded-2xl p-4 text-xs leading-relaxed">
              Thanh toán qua <b>Cổng VNPay</b>: mã tham chiếu sẽ
              <b> tự động ghi từ webhook</b> của cổng thanh toán (không nhập tay).
              <br />
              Số tiền thanh toán: <b className="text-blue-900">{formatCurrency(payAmount)}</b>{' '}
              — hệ thống tự mở phiên thanh toán có hiệu lực 10 phút.
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-1">Cổng / Đơn vị thanh toán</label>
                <Input
                  placeholder="VD: Vietcombank, Techcombank, VNPAY"
                  value={payProvider}
                  onChange={(e) => setPayProvider(e.target.value)}
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-neutral-700 mb-1">
                  {REFERENCE_CONFIG[payMethod].label}
                </label>
                <Input
                  ref={referenceInputRef}
                  placeholder={REFERENCE_CONFIG[payMethod].placeholder}
                  value={payReference}
                  onChange={(e) => setPayReference(e.target.value)}
                  error={
                    paymentError?.code === 8007 ? 'Mã tham chiếu là bắt buộc với phương thức này' : undefined
                  }
                />
              </div>
            </div>
          )}

          <div className="flex justify-end gap-3 pt-4 border-t border-neutral-100">
            <Button variant="outline" type="button" onClick={() => setIsPaymentModalOpen(false)}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" disabled={isProcessingPayment || payAmount <= 0}>
              {isProcessingPayment
                ? 'Đang xử lý...'
                : isGatewayMethod(payMethod)
                  ? 'Tạo phiên thanh toán'
                  : 'Xác nhận thu tiền'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* MODAL HOÀN TIỀN */}
      <Modal
        isOpen={isRefundModalOpen}
        onClose={() => setIsRefundModalOpen(false)}
        title="Xác nhận Hoàn tiền (Refund)"
      >
        <div className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">Số tiền hoàn (VNĐ) *</label>
            <CurrencyInput
              value={refundAmount}
              onChangeValue={(val) => setRefundAmount(val)}
              placeholder="0"
              suffix="VNĐ"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 mb-1">Lý do hoàn trả</label>
            <Input
              value={refundReason}
              onChange={(e) => setRefundReason(e.target.value)}
              placeholder="VD: Khách trả phòng sớm, hoàn cọc phòng..."
            />
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-neutral-100">
            <Button variant="outline" onClick={() => setIsRefundModalOpen(false)}>
              Hủy
            </Button>
            <Button
              variant="primary"
              onClick={handleConfirmRefund}
              disabled={isRefunding || refundAmount <= 0}
            >
              {isRefunding ? 'Đang hoàn...' : 'Xác nhận hoàn tiền'}
            </Button>
          </div>
        </div>
      </Modal>

      {/* MODAL PHIÊN THANH TOÁN GATEWAY (D3) — countdown + link + hủy/làm lại (§6) */}
      <CheckoutModal
        isOpen={isCheckoutModalOpen}
        onClose={() => setIsCheckoutModalOpen(false)}
        gateway={activeGateway}
        session={gatewaySession}
        amount={payAmount}
        onCancelAndRetry={handleCancelAndRestart}
        isCancelling={isCancellingPayment}
      />

      {/* POPUP LỖI THANH TOÁN (§6 — dùng core Modal, không toast) */}
      <PaymentErrorModal
        isOpen={paymentError !== null}
        code={paymentError?.code}
        message={paymentError?.message ?? ''}
        onRetry={handleCancelAndRestart}
        onClose={() => setPaymentError(null)}
        isRetrying={isCancellingPayment}
      />

      {/* MODAL HÓA ĐƠN & QUY TRÌNH THANH TOÁN SANDBOX */}
      <InvoicePaymentFlowModal
        isOpen={isInvoicePaymentFlowOpen}
        onClose={() => setIsInvoicePaymentFlowOpen(false)}
        booking={currentBooking}
        invoice={invoice}
        initialPaidAmount={totalPaid}
        initialBalanceDue={balanceDue}
        onPaymentSuccess={(payment) => {
          setSuccessMessage(
            payment.status === 'PENDING'
              ? `Giao dịch ${payment.transactionReference || `PAY-${payment.id}`} đã được ghi nhận (PENDING — chờ xác nhận).`
              : `Thanh toán ${payment.transactionReference || `PAY-${payment.id}`} đã được ghi nhận thành công!`
          );
          void fetchGlobalPayments();
          if (currentBooking) {
            void fetchFolioData(currentBooking.bookingNumber);
          }
        }}
        onPaymentsChanged={() => {
          void fetchGlobalPayments();
          if (currentBooking) {
            void fetchFolioData(currentBooking.bookingNumber);
          }
        }}
      />

      {/* MODAL XÁC NHẬN CHUYỂN TRẠNG THÁI GIAO DỊCH PENDING */}
      <ConfirmModal
        isOpen={actionConfirm !== null}
        onClose={() => setActionConfirm(null)}
        onConfirm={handleConfirmStatusTransition}
        isLoading={isMutatingStatus}
        variant={actionConfirm?.type === 'complete' ? 'success' : 'danger'}
        title={
          actionConfirm?.type === 'complete'
            ? 'Xác nhận hoàn tất thanh toán?'
            : 'Hủy giao dịch thanh toán đang chờ?'
        }
        description={
          actionConfirm
            ? actionConfirm.type === 'complete'
              ? `Chuyển giao dịch ${
                  actionConfirm.payment.transactionReference || `PAY-${actionConfirm.payment.id}`
                } (${formatCurrency(actionConfirm.payment.totalAmount)}) từ PENDING → SUCCESS, mở khóa phiên Redis và cập nhật trạng thái hóa đơn.`
              : `Chuyển giao dịch ${
                  actionConfirm.payment.transactionReference || `PAY-${actionConfirm.payment.id}`
                } (${formatCurrency(actionConfirm.payment.totalAmount)}) từ PENDING → CANCELLED và giải phóng khóa thanh toán để tạo phiên mới.`
            : undefined
        }
        confirmText={
          actionConfirm?.type === 'complete'
            ? 'Xác nhận (PENDING → SUCCESS)'
            : 'Hủy giao dịch (PENDING → CANCELLED)'
        }
      />

      {/* MODAL XEM VÀ IN BIÊN LAI FOLIO ĐẦY ĐỦ */}
      {isReceiptModalOpen && (
        <HotelFolioReceipt
          isOpen={isReceiptModalOpen}
          onClose={() => {
            setIsReceiptModalOpen(false);
            setSelectedReceiptPayment(null);
            setReceiptBooking(null);
            setReceiptInvoice(null);
          }}
          booking={receiptBooking || currentBooking}
          invoice={receiptBooking ? receiptInvoice : invoice}
          payment={selectedReceiptPayment}
          totalPaidAmount={
            !receiptBooking || receiptBooking.id === currentBooking?.id
              ? totalPaid
              : selectedReceiptPayment?.status === 'SUCCESS'
                ? Number(selectedReceiptPayment.totalAmount)
                : 0
          }
        />
      )}
    </div>
  );
};

export default FolioPaymentPage;
