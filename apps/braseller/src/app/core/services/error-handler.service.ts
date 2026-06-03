import { ErrorHandler, Injectable, NgZone, inject } from '@angular/core';

/**
 * Enterprise Global Error Handler
 * Intercepta exceções não tratadas em qualquer ponto do ciclo de execução do Angular 21,
 * evitando que falhas quebrem o fluxo do usuário ou causem memory leaks.
 */
@Injectable()
export class EnterpriseErrorHandler implements ErrorHandler {
  private zone = inject(NgZone);

  handleError(error: unknown): void {
    // Registra o erro no console de forma detalhada para auditoria
    console.error('*** [EMPRESA-CRITICO]: Exceção Angular Detectada ***', error);

    // Executa comportamento dentro da Angular Zone para garantir reatividade
    this.zone.run(() => {
      const errObj = error instanceof Error ? error : new Error(String(error));
      const message = errObj.message;
      this.sendToMonitoring(errObj, message);
    });
  }

  private sendToMonitoring(error: Error, msg: string): void {
    const logPackage = {
      timestamp: new Date().toISOString(),
      url: typeof window !== 'undefined' ? window.location.href : 'ssr-environment',
      userAgent: typeof navigator !== 'undefined' ? navigator.userAgent : 'node-backend',
      errorMessage: msg,
      stack: error.stack || 'Stack indisponível'
    };
    console.info('[APM-Telemetry]: Log de auditoria estruturado:', logPackage.errorMessage);
  }
}
