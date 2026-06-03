import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../config/environment';

/**
 * Interface genérica de resposta de API paginada de nível corporativo
 */
export interface PaginatedResult<T> {
  data: T[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

/**
 * Serviço de API Genérico e Altamente Tipado
 * Encapsula chamadas HTTP ao gateway de microsserviços, estendendo funcionalidades de:
 * 1. Serialização avançada de Query Parameters.
 * 2. Mapeamento de coleções DTO / Models do negócio.
 * 3. Centralização de erros HTTP para reatividade operacional.
 */
@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private http = inject(HttpClient);
  private baseUrl = environment.apiUrl;

  /**
   * GET estruturado
   */
  get<T>(path: string, params: Record<string, string | number | boolean | string[] | number[] | boolean[]> = {}, customHeaders?: HttpHeaders): Observable<T> {
    const httpParams = this.buildParams(params);
    return this.http.get<T>(`${this.baseUrl}/${path}`, { params: httpParams, headers: customHeaders }).pipe(
      catchError((error: HttpErrorResponse) => this.handleError(error))
    );
  }

  /**
   * POST de recursos
   */
  post<T>(path: string, body: unknown, customHeaders?: HttpHeaders): Observable<T> {
    return this.http.post<T>(`${this.baseUrl}/${path}`, body, { headers: customHeaders }).pipe(
      catchError((error: HttpErrorResponse) => this.handleError(error))
    );
  }

  /**
   * PUT de recursos completos
   */
  put<T>(path: string, body: unknown, customHeaders?: HttpHeaders): Observable<T> {
    return this.http.put<T>(`${this.baseUrl}/${path}`, body, { headers: customHeaders }).pipe(
      catchError((error: HttpErrorResponse) => this.handleError(error))
    );
  }

  /**
   * PATCH do estado parcial de entidades
   */
  patch<T>(path: string, body: unknown, customHeaders?: HttpHeaders): Observable<T> {
    return this.http.patch<T>(`${this.baseUrl}/${path}`, body, { headers: customHeaders }).pipe(
      catchError((error: HttpErrorResponse) => this.handleError(error))
    );
  }

  /**
   * DELETE de recursos centrais
   */
  delete<T>(path: string, customHeaders?: HttpHeaders): Observable<T> {
    return this.http.delete<T>(`${this.baseUrl}/${path}`, { headers: customHeaders }).pipe(
      catchError((error: HttpErrorResponse) => this.handleError(error))
    );
  }

  /**
   * GET formatado com retorno Mapeado por mapeador estrito à nível de arquitetura limpa
   */
  getWithMapper<T, R>(path: string, mapper: (dto: T) => R, params: Record<string, string | number | boolean | string[] | number[] | boolean[]> = {}): Observable<R> {
    return this.get<T>(path, params).pipe(
      map((dto: T) => mapper(dto)),
      catchError((error: HttpErrorResponse) => this.handleError(error))
    );
  }

  /**
   * Converte Record estático simples de Query Parameters em HttpParams reativos limpos
   */
  private buildParams(params: Record<string, string | number | boolean | string[] | number[] | boolean[]>): HttpParams {
    let httpParams = new HttpParams();
    Object.keys(params).forEach(key => {
      const val = params[key];
      if (val !== undefined && val !== null && (val as unknown) !== '') {
        if (Array.isArray(val)) {
          val.forEach(v => {
            httpParams = httpParams.append(key, String(v));
          });
        } else {
          httpParams = httpParams.set(key, String(val));
        }
      }
    });
    return httpParams;
  }

  /**
   * Central de tratamento de erros a nível de canal de transporte
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'Ocorreu um erro operacional insolúvel no transporte de rede.';
    
    if (error.error instanceof ErrorEvent) {
      // Erros do lado do client (ex: rede offline)
      errorMessage = `Erro de Conectividade: ${error.error.message}`;
    } else {
      // Erros de barramento / gateway de serviços corporativos (ex: 502, 403, 500)
      if (error.error && typeof error.error === 'object' && 'message' in error.error) {
        errorMessage = String((error.error as { message: string }).message);
      } else {
        errorMessage = `Código do Servidor: ${error.status} | Mensagem: ${error.statusText || 'Sem detalhes'}`;
      }
    }
    
    console.error(`[APISerivce-BoundaryError]: ${errorMessage}`, error);
    return throwError(() => new Error(errorMessage));
  }
}
