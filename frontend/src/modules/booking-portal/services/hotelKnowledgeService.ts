import axios from 'axios';
import { z } from 'zod';
const client = axios.create({ baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080', timeout: 15000 });
const citation = z.object({ sourceId: z.string(), version: z.number().int().positive(),
  chunkId: z.string().uuid(), category: z.string(), hotelId: z.number().int().positive().nullish(),
  effectiveFrom: z.string().nullish(), snippet: z.string(), similarityScore: z.number() });
const resultSchema = z.object({
  status: z.enum(['GROUNDED_ANSWER', 'INSUFFICIENT_EVIDENCE', 'SCOPE_REQUIRED', 'CONFLICTING_SOURCES', 'RAG_TEMP_UNAVAILABLE', 'OUT_OF_SCOPE']),
  answer: z.string(), citations: z.array(citation), warnings: z.array(z.string()), disclaimers: z.array(z.string()),
});
export type HotelKnowledgeResult = z.infer<typeof resultSchema>;
const hotelSchema = z.object({ id: z.number().int().min(1).max(32767), name: z.string(), status: z.string() });
export type KnowledgeHotel = z.infer<typeof hotelSchema>;

export async function loadKnowledgeHotels(signal: AbortSignal): Promise<KnowledgeHotel[]> {
  const { data } = await client.post('/api/v1/public/hotels/filter', { status: 'ACTIVE', page: 0, pageSize: 50 }, { signal });
  return z.array(hotelSchema).parse(data.result.content).filter(hotel => hotel.status === 'ACTIVE');
}
export async function askHotelKnowledge(question: string, hotelId: number | undefined, signal: AbortSignal): Promise<HotelKnowledgeResult> {
  const { data } = await client.post('/api/v1/public/ai/room-advisor/knowledge/ask', { question, hotelId, language: 'vi', topK: 5 }, { signal });
  const result = resultSchema.parse(data.result);
  if (result.citations.some(source => source.hotelId != null && source.hotelId !== hotelId)
      || (result.status === 'GROUNDED_ANSWER' && result.citations.length === 0)) throw new Error('Invalid knowledge evidence');
  return result;
}
