import { expect, test, type Page } from '@playwright/test'

const ME = {
  id: 1,
  email: 'ania@example.com',
  displayName: 'Ania Nowak',
  locale: 'pl',
  emailVerified: false,
  hasPassword: true,
  linkedLogins: [],
}

/** The smoke tests mock the API: they check the UI wiring, not the backend. */
async function mockApi(page: Page, options: { loggedIn: boolean }) {
  let loggedIn = options.loggedIn
  await page.route('**/api/public/auth/config', (route) =>
    route.fulfill({ json: { googleEnabled: false } }),
  )
  await page.route('**/api/auth/csrf', (route) => route.fulfill({ status: 204 }))
  await page.route('**/api/me', (route) =>
    loggedIn
      ? route.fulfill({ json: ME })
      : route.fulfill({
          status: 401,
          contentType: 'application/problem+json',
          json: { status: 401, code: 'UNAUTHORIZED' },
        }),
  )
  await page.route('**/api/auth/login', (route) => {
    loggedIn = true
    return route.fulfill({ status: 204 })
  })
}

test('anonymous visitor lands on the Polish login page and can switch to English', async ({
  page,
}) => {
  await mockApi(page, { loggedIn: false })

  await page.goto('/')

  await expect(page).toHaveURL(/\/login/)
  await expect(page.locator('html')).toHaveAttribute('lang', 'pl')
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Zaloguj się')

  await page.getByTestId('language-toggle').click()

  await expect(page.locator('html')).toHaveAttribute('lang', 'en')
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Log in')
})

test('logging in opens the app with the email verification banner', async ({ page }) => {
  await mockApi(page, { loggedIn: false })
  await page.goto('/login')

  await page.getByLabel('Adres e-mail').fill('ania@example.com')
  await page.getByLabel('Hasło', { exact: true }).fill('kot-w-butach-27')
  await page.getByRole('button', { name: 'Zaloguj się' }).click()

  await expect(page).toHaveURL(/\/items/)
  await expect(page.getByTestId('verify-banner')).toContainText('ania@example.com')
  // Desktop shows the sidebar ("Moje rzeczy"), mobile the bottom navigation ("Rzeczy"); only one is visible.
  const nav = page.getByRole('navigation', { name: 'Nawigacja główna' })
  await expect(nav.getByRole('link', { name: /^(Moje rzeczy|Rzeczy)$/ })).toBeVisible()
})

test('wrong credentials show the translated error', async ({ page }) => {
  await mockApi(page, { loggedIn: false })
  await page.route('**/api/auth/login', (route) =>
    route.fulfill({
      status: 401,
      contentType: 'application/problem+json',
      json: { status: 401, code: 'INVALID_CREDENTIALS' },
    }),
  )
  await page.goto('/login')

  await page.getByLabel('Adres e-mail').fill('ania@example.com')
  await page.getByLabel('Hasło', { exact: true }).fill('wrong-password')
  await page.getByRole('button', { name: 'Zaloguj się' }).click()

  await expect(page.getByRole('alert')).toContainText('Nie udało się zalogować')
})
