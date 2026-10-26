/**
 * TrustPay Guardian formatting and masking utilities
 */

/**
 * Formats a monetary number in Indian Rupee format (en-IN): e.g. ₹ 1,00,000.00
 */
export function formatCurrency(amount: number): string {
  if (isNaN(amount)) return '₹ 0.00';
  return '₹ ' + amount.toLocaleString('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

/**
 * Masks an account number preserving prefix and last 4 digits: e.g. TPG0000000001 -> TPG******0001
 */
export function maskAccountNumber(accountNumber: string): string {
  if (!accountNumber || accountNumber.length < 8) return accountNumber || '';
  const prefix = accountNumber.substring(0, 3);
  const suffix = accountNumber.substring(accountNumber.length - 4);
  return `${prefix}${'*'.repeat(accountNumber.length - 7)}${suffix}`;
}

/**
 * Masks a person's full name to prevent identity enumeration: e.g. John Doe -> J*** D**
 */
export function maskHolderName(name: string): string {
  if (!name || name.trim().length === 0) return '';
  return name
    .trim()
    .split(/\s+/)
    .map((part) => {
      if (part.length <= 1) return part;
      return part[0] + '*'.repeat(Math.max(1, part.length - 1));
    })
    .join(' ');
}

/**
 * Formats ISO timestamp to a readable date time string
 */
export function formatDateTime(isoString: string): string {
  if (!isoString) return '';
  try {
    const d = new Date(isoString);
    return d.toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return isoString;
  }
}
