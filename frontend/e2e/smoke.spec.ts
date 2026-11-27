import { expect, test } from '@playwright/test'

test.beforeEach(async ({ page }) => {
  // The smoke test does not need a running backend.
  await page.route('**/api/public/ping', (route) =>
    route.fulfill({ json: { message: 'ok', locale: 'pl' } }),
  )
})

test('loads in Polish and switches to English', async ({ page }) => {
  await page.goto('/')

  await expect(page.locator('html')).toHaveAttribute('lang', 'pl')
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Wishka')
  await expect(page.getByTestId('api-status')).toContainText('Serwer działa')

  await page.getByTestId('language-switcher').selectOption('en')

  await expect(page.locator('html')).toHaveAttribute('lang', 'en')
  await expect(page.getByTestId('api-status')).toContainText('Server is running')
})

test('shows a translated error toast when the backend is unreachable', async ({ page }) => {
  await page.route('**/api/public/ping', (route) => route.abort('connectionrefused'))

  await page.goto('/')

  await expect(page.getByRole('alert')).toContainText('Brak połączenia')
})
