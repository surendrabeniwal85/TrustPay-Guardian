import { describe, it, expect } from 'vitest';
import {
  formatCurrency,
  maskAccountNumber,
  maskHolderName,
  formatDateTime,
} from '../utils/formatters';

describe('Formatters and Masking Utilities', () => {
  it('formats currency using Indian Rupee grouping', () => {
    expect(formatCurrency(0)).toBe('₹ 0.00');
    expect(formatCurrency(500)).toBe('₹ 500.00');
    expect(formatCurrency(1000)).toBe('₹ 1,000.00');
    expect(formatCurrency(100000)).toBe('₹ 1,00,000.00');
    expect(formatCurrency(2500000.5)).toBe('₹ 25,00,000.50');
  });

  it('masks account number retaining prefix and last 4 digits', () => {
    expect(maskAccountNumber('TPG0000000001')).toBe('TPG******0001');
    expect(maskAccountNumber('TPG1234567890')).toBe('TPG******7890');
    expect(maskAccountNumber('SHORT')).toBe('SHORT');
  });

  it('masks holder full name', () => {
    expect(maskHolderName('John Doe')).toBe('J*** D**');
    expect(maskHolderName('Alice')).toBe('A****');
    expect(maskHolderName('Bob Smith Jr')).toBe('B** S**** J*');
    expect(maskHolderName('')).toBe('');
  });

  it('formats ISO timestamps safely', () => {
    const formatted = formatDateTime('2026-05-15T10:30:00Z');
    expect(formatted).toContain('2026');
    expect(formatted).toContain('May');
  });
});
