import { apiClient } from '../../../core/api/client';
import type {
  SystemSchedulerTaskDto,
  SchedulerTriggerResultDto,
  SystemSchedulerSearchDto,
} from '../types/scheduler.types';

export const schedulerService = {
  getSchedulers: async (
    searchDto?: SystemSchedulerSearchDto
  ): Promise<SystemSchedulerTaskDto[]> => {
    const res = await apiClient.post<SystemSchedulerTaskDto[]>(
      '/api/v1/operation/schedulers/filter',
      searchDto ?? {}
    );
    return res.result || [];
  },

  triggerScheduler: async (
    taskKey: string
  ): Promise<SchedulerTriggerResultDto> => {
    const res = await apiClient.post<SchedulerTriggerResultDto>(
      `/api/v1/operation/schedulers/${taskKey}/trigger`
    );
    return res.result;
  },
};

export default schedulerService;

