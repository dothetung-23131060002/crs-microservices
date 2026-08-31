import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import path from 'node:path';

const screenshotsDir = path.resolve(
  process.cwd(),
  process.env.BUOI08_SCREENSHOTS_DIR ?? path.join('bao-cao', 'screenshots'),
);

const failureScreenshotNames: Record<string, string> = {
  '01': '01-loi-chua-dang-nhap-vao-admin.png',
  '02': '02-loi-sai-mat-khau.png',
  '03': '03-loi-student-dang-nhap.png',
  '04': '04-loi-hydration-chuyen-nham-login.png',
  '05': '05-loi-admin-crud.png',
  '06': '06-loi-f5-chuyen-nham-login.png',
  '07': '07-loi-backend-tra-403.png',
  '08': '08-loi-courses-hien-thao-tac.png',
};

test.beforeAll(() => {
  mkdirSync(screenshotsDir, { recursive: true });
});

test.afterEach(async ({ page }, testInfo) => {
  if (testInfo.status === testInfo.expectedStatus || page.isClosed()) {
    return;
  }

  const scenarioNumber = testInfo.title.slice(0, 2);
  const filename = failureScreenshotNames[scenarioNumber] ?? `${scenarioNumber}-loi.png`;
  await page.screenshot({ path: path.join(screenshotsDir, filename), fullPage: true });
});

async function clearAuthStorage(page: Page) {
  await page.goto('/courses');
  await page.evaluate(() => {
    localStorage.removeItem('crs_token');
    localStorage.removeItem('crs_user');
  });
}

async function login(page: Page, username: string, password: string) {
  await page.goto('/login');
  await page.locator('input').nth(0).fill(username);
  await page.locator('input[type="password"]').fill(password);

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().endsWith('/api/auth/login') && response.request().method() === 'POST',
  );
  await page.getByRole('button', { name: 'Dang nhap' }).click();
  const loginResponse = await loginResponsePromise;

  expect(loginResponse.status()).toBe(200);
  await expect(page).toHaveURL('/courses');
}

test('01 chua dang nhap vao admin se ve login', async ({ page }) => {
  await clearAuthStorage(page);
  await page.goto('/admin/courses');

  await expect(page).toHaveURL('/login');
  await expect(page.getByRole('heading', { name: 'Dang nhap he thong CRS' })).toBeVisible();
  await page.screenshot({
    path: path.join(screenshotsDir, '01-chua-dang-nhap-vao-admin.png'),
    fullPage: true,
  });
});

test('02 sai mat khau hien dung thong bao', async ({ page }) => {
  await clearAuthStorage(page);
  await page.goto('/login');
  await page.locator('input').nth(0).fill('student1');
  await page.locator('input[type="password"]').fill('sai-mat-khau');

  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.url().endsWith('/api/auth/login') && response.request().method() === 'POST',
  );
  await page.getByRole('button', { name: 'Dang nhap' }).click();
  const loginResponse = await loginResponsePromise;

  expect(loginResponse.status()).toBe(401);
  await expect(page.getByText('Sai username hoac password', { exact: true })).toBeVisible();
  await page.screenshot({ path: path.join(screenshotsDir, '02-sai-mat-khau.png'), fullPage: true });
});

test('03 student dang nhap thanh cong', async ({ page }) => {
  await clearAuthStorage(page);
  await login(page, 'student1', 'student123');

  await expect(page.getByText('Xin chao, student1 (STUDENT)', { exact: true })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Dang ky hoc phan' })).toBeVisible();
  await expect(page.locator('tbody tr').first()).toBeVisible();
  await page.screenshot({
    path: path.join(screenshotsDir, '03-student-dang-nhap-thanh-cong.png'),
    fullPage: true,
  });

  await page.getByRole('link', { name: 'Dang ky hoc phan' }).click();
  await expect(page).toHaveURL('/register-course');
  await expect(page.getByRole('heading', { name: 'Đăng ký học phần' })).toBeVisible();
});

test('04 student go tay admin se ve courses', async ({ page }) => {
  await clearAuthStorage(page);
  await login(page, 'student1', 'student123');

  // page.goto tao full document navigation, dung nghia voi thao tac go tay URL.
  await page.goto('/admin/courses');

  await expect(page).toHaveURL('/courses');
  await expect(page.getByText('Xin chao, student1 (STUDENT)', { exact: true })).toBeVisible();
  await page.screenshot({
    path: path.join(screenshotsDir, '04-student-bi-chan-admin.png'),
    fullPage: true,
  });
});

test('05 admin xem sua xoa duoc', async ({ page }) => {
  await clearAuthStorage(page);
  await login(page, 'student1', 'student123');
  await page.getByRole('button', { name: 'Dang xuat' }).click();
  await expect(page).toHaveURL('/login');

  const loggedOutStorage = await page.evaluate(() => ({
    token: localStorage.getItem('crs_token'),
    user: localStorage.getItem('crs_user'),
  }));
  expect(loggedOutStorage).toEqual({ token: null, user: null });

  await login(page, 'admin', 'admin123');
  await page.getByRole('link', { name: 'Quan tri mon hoc' }).click();

  await expect(page).toHaveURL('/admin/courses');
  await expect(page.getByText('Xin chao, admin (ADMIN)', { exact: true })).toBeVisible();
  await expect(page.getByRole('columnheader', { name: 'Thao tác' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Sửa' }).first()).toBeVisible();
  await expect(page.getByRole('button', { name: 'Xoá' }).first()).toBeVisible();
  await page.screenshot({ path: path.join(screenshotsDir, '05a-admin-xem-duoc.png'), fullPage: true });

  const uniqueSuffix = `${Date.now()}`;
  const createdName = `Buoi 08 Playwright ${uniqueSuffix}`;
  const updatedName = `${createdName} da sua`;
  let createdCourseId: number | undefined;
  let deleted = false;

  try {
    await page.locator('#tenMonHoc').fill(createdName);
    await page.locator('#soTinChi').fill('3');
    await page.locator('#soChoToiDa').fill('25');

    const createResponsePromise = page.waitForResponse(
      (response) => response.url().endsWith('/api/courses') && response.request().method() === 'POST',
    );
    await page.getByRole('button', { name: 'Thêm mới' }).click();
    const createResponse = await createResponsePromise;
    expect(createResponse.status()).toBe(201);
    createdCourseId = (await createResponse.json()).id as number;

    const searchBox = page.getByPlaceholder('Tìm kiếm theo tên môn học...');
    await searchBox.fill(createdName);
    const createdRow = page.getByRole('row').filter({ hasText: createdName });
    await expect(createdRow).toBeVisible();
    await createdRow.getByRole('button', { name: 'Sửa' }).click();

    await expect(page.getByRole('heading', { name: 'Sửa môn học' })).toBeVisible();
    await page.locator('#tenMonHoc').fill(updatedName);
    const updateResponsePromise = page.waitForResponse(
      (response) =>
        response.url().endsWith(`/api/courses/${createdCourseId}`) &&
        response.request().method() === 'PUT',
    );
    await page.getByRole('button', { name: 'Cập nhật' }).click();
    const updateResponse = await updateResponsePromise;
    expect(updateResponse.status()).toBe(200);

    await searchBox.fill(updatedName);
    const updatedRow = page.getByRole('row').filter({ hasText: updatedName });
    await expect(updatedRow).toBeVisible();
    await page.screenshot({
      path: path.join(screenshotsDir, '05b-admin-sua-thanh-cong.png'),
      fullPage: true,
    });

    page.once('dialog', (dialog) => dialog.accept());
    const deleteResponsePromise = page.waitForResponse(
      (response) =>
        response.url().endsWith(`/api/courses/${createdCourseId}`) &&
        response.request().method() === 'DELETE',
    );
    await updatedRow.getByRole('button', { name: 'Xoá' }).click();
    const deleteResponse = await deleteResponsePromise;
    expect(deleteResponse.status()).toBe(204);
    deleted = true;

    await expect(page.getByText('Không tìm thấy môn học nào phù hợp.')).toBeVisible();
    await page.screenshot({
      path: path.join(screenshotsDir, '05c-admin-xoa-thanh-cong.png'),
      fullPage: true,
    });
  } finally {
    if (createdCourseId && !deleted) {
      const token = await page.evaluate(() => localStorage.getItem('crs_token'));
      if (token) {
        await page.request.delete(`http://localhost:8080/api/courses/${createdCourseId}`, {
          headers: { Authorization: `Bearer ${token}` },
        });
      }
    }
  }
});

test('06 F5 van giu trang thai dang nhap', async ({ page }) => {
  await clearAuthStorage(page);
  await login(page, 'admin', 'admin123');
  await page.getByRole('link', { name: 'Quan tri mon hoc' }).click();
  await expect(page).toHaveURL('/admin/courses');

  await page.reload();

  await expect(page).toHaveURL('/admin/courses');
  await expect(page.getByText('Xin chao, admin (ADMIN)', { exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Quản lý môn học (Admin)' })).toBeVisible();
  await page.screenshot({
    path: path.join(screenshotsDir, '06-f5-giu-dang-nhap.png'),
    fullPage: true,
  });
});

test('07 token rac nhan 401 va tu dang xuat', async ({ page }) => {
  await clearAuthStorage(page);
  await login(page, 'admin', 'admin123');
  await page.getByRole('link', { name: 'Quan tri mon hoc' }).click();
  await expect(page).toHaveURL('/admin/courses');

  await page.evaluate(() => {
    localStorage.setItem('crs_token', 'chuoi-rac-buoi-08');
  });
  await page.locator('#tenMonHoc').fill(`Token rac ${Date.now()}`);
  await page.locator('#soTinChi').fill('3');
  await page.locator('#soChoToiDa').fill('20');

  const createResponsePromise = page.waitForResponse(
    (response) => response.url().endsWith('/api/courses') && response.request().method() === 'POST',
  );
  await page.getByRole('button', { name: 'Thêm mới' }).click();
  const createResponse = await createResponsePromise;
  await page.waitForTimeout(300);

  expect(createResponse.status()).toBe(401);
  await expect(page).toHaveURL('/login');
  const authStorage = await page.evaluate(() => ({
    token: localStorage.getItem('crs_token'),
    user: localStorage.getItem('crs_user'),
  }));
  expect(authStorage).toEqual({ token: null, user: null });
  await page.screenshot({
    path: path.join(screenshotsDir, '07-token-rac-tu-dang-xuat.png'),
    fullPage: true,
  });
});

test('08 courses cong khai khong co cot thao tac', async ({ page }) => {
  await clearAuthStorage(page);
  await page.goto('/courses');

  await expect(page.locator('tbody tr').first()).toBeVisible();
  await expect(page.getByRole('columnheader', { name: 'Thao tác' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Sửa' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Xoá' })).toHaveCount(0);
  await page.screenshot({
    path: path.join(screenshotsDir, '08-courses-khong-co-thao-tac.png'),
    fullPage: true,
  });
});
