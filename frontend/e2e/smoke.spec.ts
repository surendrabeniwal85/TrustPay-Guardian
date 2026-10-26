import { test, expect } from '@playwright/test';

test.describe('TrustPay Guardian Customer Flow Smoke Test', () => {
  test('open home page, sign in as seeded customer, view dashboard, and log out', async ({ page }) => {
    // 1. Open home page
    await page.goto('/');
    await expect(page).toHaveTitle(/TrustPay Guardian/i);
    await expect(page.getByText(/Educational Digital Banking Platform/i)).toBeVisible();

    // 2. Navigate to login
    await page.getByRole('link', { name: /Sign In/i }).first().click();
    await expect(page).toHaveURL(/.*login/);

    // 3. Authenticate with seeded customer credentials
    await page.getByPlaceholder(/name@trustpay.internal/i).fill('customer@trustpay.internal');
    await page.getByPlaceholder(/Enter your password/i).fill('Customer@123');
    await page.getByRole('button', { name: /Sign In to Account/i }).click();

    // 4. Verify arrival on Customer Dashboard
    await expect(page).toHaveURL(/.*app\/dashboard/);
    await expect(page.getByText(/Total Ledger Balance/i)).toBeVisible();
    await expect(page.getByText(/Send Money/i)).toBeVisible();

    // 5. Sign out
    await page.getByRole('button', { name: /Sign Out/i }).click();
    await expect(page).toHaveURL(/.*login/);
  });
});
