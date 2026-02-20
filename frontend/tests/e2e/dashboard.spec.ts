import { test, expect } from '@playwright/test';

// Helper to mock login and navigate to dashboard
async function loginAndGoToDashboard(page: any) {
  // Mock auth login
  await page.route('**/api/auth/login', async (route: any) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        token: 'eyJhbGciOiJIUzI1NiJ9.mock.token',
        name: 'Test Admin',
        email: 'admin@test.com',
        role: 'ADMIN',
      }),
    });
  });

  // Mock students list
  await page.route('**/api/students?**', async (route: any) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        content: [
          { id: 1, firstName: 'John', lastName: 'Doe', email: 'john@test.com', active: true, studentClass: '10', section: 'A' },
          { id: 2, firstName: 'Jane', lastName: 'Smith', email: 'jane@test.com', active: false, studentClass: '9', section: 'B' },
        ],
        totalPages: 1,
        totalElements: 2,
        number: 0,
        size: 10,
      }),
    });
  });

  // Mock stats endpoint
  await page.route('**/api/students/stats', async (route: any) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ total: 85, active: 15, inactive: 5 }),
    });
  });

  // Login first
  await page.goto('/login');
  await page.getByLabel('Email').fill('admin@test.com');
  await page.getByLabel('Password').fill('password123');
  await page.getByRole('button', { name: 'Sign In' }).click();
  await expect(page).toHaveURL('/dashboard');
}

test.describe('Dashboard - Stats Cards & Import/Export', () => {
  test('should display Total, Active, and Inactive stats cards', async ({ page }) => {
    await loginAndGoToDashboard(page);

    // Verify all 3 stats cards are visible with correct values
    await expect(page.getByText('Total Students')).toBeVisible();
    await expect(page.getByText('Active Students', { exact: true })).toBeVisible();
    await expect(page.getByText('Inactive Students', { exact: true })).toBeVisible();

    // Check the count values
    await expect(page.getByText('85')).toBeVisible();
    await expect(page.getByText('15', { exact: true })).toBeVisible();
    await expect(page.getByText('5', { exact: true })).toBeVisible();
  });

  test('should display Import and Export buttons', async ({ page }) => {
    await loginAndGoToDashboard(page);

    await expect(page.getByRole('button', { name: /Import/ })).toBeVisible();
    await expect(page.getByRole('button', { name: /Export/ })).toBeVisible();
  });

  test('should trigger CSV export on Export button click', async ({ page }) => {
    await loginAndGoToDashboard(page);

    // Mock the export CSV fetch call
    let exportCalled = false;
    await page.route('**/api/students/export/csv**', async (route: any) => {
      exportCalled = true;
      await route.fulfill({
        status: 200,
        contentType: 'text/csv',
        headers: { 'Content-Disposition': 'attachment; filename=students.csv' },
        body: 'firstName,lastName,email,phone,class,section,enrollmentDate,active\nJohn,Doe,john@test.com,,10,A,,true\n',
      });
    });

    await page.getByRole('button', { name: /Export/ }).click();

    // Wait briefly for the fetch to complete
    await page.waitForTimeout(1000);
    expect(exportCalled).toBe(true);
  });

  test('should show import result after CSV import', async ({ page }) => {
    await loginAndGoToDashboard(page);

    // Mock the import endpoint
    await page.route('**/api/students/import/csv', async (route: any) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ imported: 3, failed: 1, errors: ['Row 4: firstName and email are required'] }),
      });
    });

    // Re-mock stats to return updated values after import
    await page.route('**/api/students/stats', async (route: any) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ total: 88, active: 18, inactive: 5 }),
      });
    });

    // Trigger file upload by setting files on the hidden input
    const fileInput = page.locator('input[type="file"][accept=".csv"]');
    await fileInput.setInputFiles({
      name: 'students.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(
        'firstName,lastName,email\nAlice,Wonder,alice@test.com\nBob,Builder,bob@test.com\nClark,Kent,clark@test.com\n,,\n'
      ),
    });

    // Verify import result banner shows
    await expect(page.getByText('3 imported')).toBeVisible();
    await expect(page.getByText('1 failed')).toBeVisible();
    await expect(page.getByText('Row 4: firstName and email are required')).toBeVisible();
  });
});
