import { TrackByFunction } from '@angular/core';

/**
 * Utilitários Genéricos de TrackBy para diretivas de renderização do Angular 21 (como @for)
 * Maximiza a perfomance em repetições de listas ao evitar reconstruções desnecessárias da árvore DOM
 */
export class TrackByUtils {
  
  /**
   * Fornece trackby genérico baseado no identificador único 'id' da entidade
   */
  static byId<T extends { id: string | number }>(): TrackByFunction<T> {
    return (index: number, item: T) => item.id;
  }

  /**
   * Fornece trackby genérico baseado em propriedade customizada
   */
  static byProperty<T>(prop: keyof T): TrackByFunction<T> {
    return (index: number, item: T) => item[prop];
  }

  /**
   * Fornece trackby genérico baseado unicamente no índice de repetição do item
   */
  static byIndex<T>(): TrackByFunction<T> {
    return (index: number) => index;
  }
}
