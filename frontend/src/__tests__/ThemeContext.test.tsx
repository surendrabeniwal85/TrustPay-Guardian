import { render, screen, act } from '@testing-library/react';
import { describe, it, expect, beforeEach } from 'vitest';
import { ThemeProvider, useTheme } from '../context/ThemeContext';

const TestComponent = () => {
  const { theme, toggleTheme, setTheme } = useTheme();
  return (
    <div>
      <span data-testid="theme-value">{theme}</span>
      <button onClick={toggleTheme}>Toggle</button>
      <button onClick={() => setTheme('light')}>Set Light</button>
    </div>
  );
};

describe('ThemeContext', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-bs-theme');
  });

  it('initializes and renders default theme', () => {
    render(
      <ThemeProvider>
        <TestComponent />
      </ThemeProvider>
    );

    const themeEl = screen.getByTestId('theme-value');
    expect(themeEl.textContent).toMatch(/light|dark/);
  });

  it('toggles theme and updates document attribute and localStorage', () => {
    render(
      <ThemeProvider>
        <TestComponent />
      </ThemeProvider>
    );

    const toggleBtn = screen.getByText('Toggle');
    const initialTheme = screen.getByTestId('theme-value').textContent;

    act(() => {
      toggleBtn.click();
    });

    const newTheme = screen.getByTestId('theme-value').textContent;
    expect(newTheme).not.toBe(initialTheme);
    expect(document.documentElement.getAttribute('data-bs-theme')).toBe(newTheme);
    expect(localStorage.getItem('tpg-theme')).toBe(newTheme);
  });
});
