export function isReadOnlyAccountant(roles?: string[] | null): boolean {
  const safeRoles = roles ?? []
  return safeRoles.includes('CONTADOR')
    && !safeRoles.includes('ADMIN')
    && !safeRoles.includes('BPO_ADMIN')
}

export function isGlobalBpoOperator(roles?: string[] | null): boolean {
  const safeRoles = roles ?? []
  return safeRoles.includes('BPO_ADMIN')
    || (safeRoles.includes('ADMIN') && safeRoles.includes('CONTADOR'))
}

export function isBpoOperator(roles?: string[] | null): boolean {
  const safeRoles = roles ?? []
  return safeRoles.includes('CONTADOR') || isGlobalBpoOperator(safeRoles)
}
