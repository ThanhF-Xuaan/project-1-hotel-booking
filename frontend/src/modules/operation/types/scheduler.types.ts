export interface SystemSchedulerTaskDto {
  taskKey: string;
  taskName: string;
  category: 'INVENTORY' | 'BOOKING' | 'IDENTITY' | 'OPERATION' | string;
  cronOrInterval: string;
  shedLockName: string;
  hasDistributedLock: boolean;
  allowManualTrigger: boolean;
  automaticPatternMessage?: string;
  description?: string;
  lockStatus: 'IDLE' | 'RUNNING_LOCKED' | string;
  lockedUntil?: string | null;
}

export interface SchedulerTriggerResultDto {
  taskKey: string;
  taskName: string;
  success: boolean;
  message: string;
  processedCount?: number | null;
  executedAt: string;
}

export interface SystemSchedulerSearchDto {
  keyword?: string;
  category?: string;
  allowManualTrigger?: boolean;
}

