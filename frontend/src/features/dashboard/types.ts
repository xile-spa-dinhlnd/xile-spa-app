/** Phản hồi của `/actuator/health` (backend chỉ trả `status`, không lộ chi tiết). */
export interface HealthResponse {
  status: 'UP' | 'DOWN' | 'OUT_OF_SERVICE' | 'UNKNOWN'
}
