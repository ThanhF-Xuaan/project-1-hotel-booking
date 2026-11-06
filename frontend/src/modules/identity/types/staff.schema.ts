import { z } from 'zod';

export const VIETNAMESE_NAME_REGEX = /^[\p{L}\s'-]+$/u;
export const VIETNAMESE_PHONE_REGEX = /^(0|\+84)(3[2-9]|5[6|8|9]|7[0|6-9]|8[1-9]|9[0-9])[0-9]{7}$/;
export const USERNAME_REGEX = /^[a-zA-Z0-9._-]{3,50}$/;

export interface StaffFormFieldErrors {
  username?: string;
  roleId?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  scopeEntityId?: string;
  password?: string;
  confirmPassword?: string;
}

export const staffBaseSchema = z
  .object({
    lastName: z
      .string()
      .trim()
      .min(1, 'Họ và tên đệm không được để trống.')
      .max(100, 'Họ và tên đệm tối đa 100 ký tự.')
      .refine((val) => !/^\d+$/.test(val), { message: 'Họ và tên đệm không được là số.' })
      .refine((val) => !/\d/.test(val), { message: 'Họ và tên đệm không được chứa chữ số.' })
      .refine((val) => VIETNAMESE_NAME_REGEX.test(val), {
        message: 'Họ và tên đệm chỉ được chứa chữ cái, không chứa ký tự đặc biệt.',
      }),

    firstName: z
      .string()
      .trim()
      .min(1, 'Tên không được để trống.')
      .max(100, 'Tên tối đa 100 ký tự.')
      .refine((val) => !/^\d+$/.test(val), { message: 'Tên không được là số.' })
      .refine((val) => !/\d/.test(val), { message: 'Tên không được chứa chữ số.' })
      .refine((val) => VIETNAMESE_NAME_REGEX.test(val), {
        message: 'Tên chỉ được chứa chữ cái, không chứa ký tự đặc biệt.',
      }),

    email: z
      .string()
      .trim()
      .min(1, 'Email liên hệ không được để trống.')
      .max(100, 'Email tối đa 100 ký tự.')
      .email('Định dạng email không hợp lệ (ví dụ: staff@utc.edu.vn).'),

    phone: z
      .string()
      .trim()
      .min(1, 'Số điện thoại không được để trống.')
      .refine((val) => VIETNAMESE_PHONE_REGEX.test(val), {
        message: 'Số điện thoại không hợp lệ (phải gồm 10 số bắt đầu bằng 03, 05, 07, 08, 09 hoặc +84).',
      }),

    roleId: z
      .union([z.number(), z.string()])
      .refine((val) => Boolean(val && Number(val) > 0), {
        message: 'Vui lòng chọn vai trò (Role) cho nhân sự.',
      }),

    scopeType: z.enum(['PROPERTY', 'REGION', 'CHAIN']),
    scopeEntityId: z.union([z.number(), z.string(), z.undefined()]),
  })
  .superRefine((data, ctx) => {
    if (data.scopeType === 'PROPERTY' && (!data.scopeEntityId || Number(data.scopeEntityId) <= 0)) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['scopeEntityId'],
        message: 'Vui lòng chọn khách sạn trực thuộc.',
      });
    }
    if (data.scopeType === 'REGION' && (!data.scopeEntityId || Number(data.scopeEntityId) <= 0)) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['scopeEntityId'],
        message: 'Vui lòng chọn khu vực quản lý.',
      });
    }
  });

export const staffCreateSchema = staffBaseSchema
  .and(
    z.object({
      username: z
        .string()
        .trim()
        .min(1, 'Tên đăng nhập không được để trống.')
        .min(3, 'Tên đăng nhập phải có tối thiểu 3 ký tự.')
        .max(50, 'Tên đăng nhập tối đa 50 ký tự.')
        .regex(
          USERNAME_REGEX,
          'Tên đăng nhập chỉ được chứa chữ cái, số, dấu chấm (.), gạch dưới (_) hoặc gạch ngang (-).'
        ),

      password: z
        .string()
        .trim()
        .min(1, 'Vui lòng thiết lập mật khẩu khởi tạo cho nhân viên.')
        .min(6, 'Mật khẩu khởi tạo phải có tối thiểu 6 ký tự.'),

      confirmPassword: z
        .string()
        .trim()
        .min(1, 'Vui lòng xác nhận mật khẩu khởi tạo.'),
    })
  )
  .superRefine((data, ctx) => {
    if (data.password !== data.confirmPassword) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['confirmPassword'],
        message: 'Mật khẩu xác nhận không khớp. Vui lòng kiểm tra lại.',
      });
    }
  });

export const staffEditSchema = staffBaseSchema
  .and(
    z.object({
      password: z.string().optional(),
    })
  )
  .superRefine((data, ctx) => {
    if (data.password && data.password.trim().length > 0 && data.password.trim().length < 6) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['password'],
        message: 'Mật khẩu cập nhật phải có tối thiểu 6 ký tự.',
      });
    }
  });

export type StaffCreateFormValues = z.infer<typeof staffCreateSchema>;
export type StaffEditFormValues = z.infer<typeof staffEditSchema>;

export interface StaffFormValidationInput {
  username?: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  roleId: number | string;
  scopeType: 'PROPERTY' | 'REGION' | 'CHAIN';
  scopeEntityId?: number | string;
  password?: string;
  confirmPassword?: string;
}

export interface StaffFormValidationResult {
  isValid: boolean;
  errors: StaffFormFieldErrors;
  firstError?: string;
}

/**
 * Hàm xác thực form nhân viên (dùng chung cho cả thêm mới và chỉnh sửa).
 * Trả về danh sách lỗi theo từng trường và thông báo lỗi đầu tiên.
 */
export const validateStaffForm = (
  input: StaffFormValidationInput,
  isEditing: boolean
): StaffFormValidationResult => {
  const result = isEditing ? staffEditSchema.safeParse(input) : staffCreateSchema.safeParse(input);

  if (result.success) {
    return {
      isValid: true,
      errors: {},
    };
  }

  const errors: StaffFormFieldErrors = {};
  let firstError: string | undefined;

  for (const issue of result.error.issues) {
    const fieldName = issue.path[0] as keyof StaffFormFieldErrors;
    if (fieldName && !errors[fieldName]) {
      errors[fieldName] = issue.message;
    }
    if (!firstError) {
      firstError = issue.message;
    }
  }

  return {
    isValid: false,
    errors,
    firstError,
  };
};

