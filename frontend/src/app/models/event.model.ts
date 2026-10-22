export enum EventType {
  PRODUCTION = 'PRODUCTION',
  MAINTENANCE = 'MAINTENANCE',
  QUALITY = 'QUALITY'
}

export interface EventRequest {
  eventId: string;
  eventType: EventType;
  description: string;
  timestamp: string;
}

export interface EventResponse {
  eventId: string;
  eventType: EventType;
  description: string;
  timestamp: string;
}
