export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface LoginResponse {
  token: string;
  email: string;
  nombre: string;
  apellido: string;
  rol: 'DUENO' | 'AGENTE';
  debeCambiarPassword: boolean;
}
