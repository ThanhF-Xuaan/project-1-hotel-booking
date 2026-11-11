/**
 * Utility functions for standardizing inclusion display to Vietnamese uppercase.
 * Single Source of Truth for unit formatting, frequency, target type, and source badge.
 */

export const formatInclusionUnit = (rawUnit?: string | null): string => {
  if (!rawUnit || !rawUnit.trim()) {
    return 'PHẦN'
  }

  const normalized = rawUnit.trim().toUpperCase()

  switch (normalized) {
    case 'PHAN':
    case 'PHẦN':
    case 'PORTION':
    case 'SET':
    case 'SUAT':
    case 'SUẤT':
      return 'PHẦN'
    case 'CHAI':
    case 'BOTTLE':
      return 'CHAI'
    case 'LON':
    case 'CAN':
      return 'LON'
    case 'KG':
    case 'KILOGRAM':
      return 'KG'
    case 'LUOT':
    case 'LƯỢT':
    case 'TURN':
      return 'LƯỢT'
    case 'CHUYEN':
    case 'CHUYẾN':
    case 'TRIP':
      return 'CHUYẾN'
    case 'NGAY':
    case 'NGÀY':
    case 'DAY':
      return 'NGÀY'
    case 'GOI':
    case 'GÓI':
    case 'PACKAGE':
      return 'GÓI'
    case 'HOP':
    case 'HỘP':
    case 'BOX':
      return 'HỘP'
    case 'DIA':
    case 'ĐĨA':
    case 'PLATE':
      return 'ĐĨA'
    case 'BO':
    case 'BỘ':
      return 'BỘ'
    case 'LY':
    case 'GLASS':
      return 'LY'
    case 'GIO':
    case 'GIỜ':
    case 'HOUR':
      return 'GIỜ'
    case 'VE':
    case 'VÉ':
    case 'TICKET':
      return 'VÉ'
    case 'KHACH':
    case 'KHÁCH':
    case 'PERSON':
    case 'PAX':
      return 'KHÁCH'
    case 'PHONG':
    case 'PHÒNG':
    case 'ROOM':
      return 'PHÒNG'
    default:
      return normalized
  }
}

export const formatInclusionFrequency = (freq?: string | null): string => {
  switch (freq) {
    case 'PER_NIGHT':
      return 'HÀNG NGÀY (MỖI ĐÊM)'
    case 'PER_STAY':
      return 'MỘT LẦN (CẢ KỲ NGHỈ)'
    default:
      return 'HÀNG NGÀY (MỖI ĐÊM)'
  }
}

export const formatInclusionTarget = (target?: string | null): string => {
  switch (target) {
    case 'PER_ROOM':
      return 'THEO PHÒNG'
    case 'PER_ADULT':
      return 'MỖI NGƯỜI LỚN'
    case 'PER_GUEST':
      return 'TẤT CẢ KHÁCH'
    default:
      return 'THEO PHÒNG'
  }
}

export const formatInclusionSource = (source?: string | null): string => {
  switch (source) {
    case 'CHAIN_MASTER':
      return 'CHUẨN CHUỖI'
    case 'HOTEL_MENU':
    default:
      return 'THỰC ĐƠN CƠ SỞ'
  }
}
