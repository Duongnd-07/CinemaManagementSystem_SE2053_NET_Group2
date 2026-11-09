import fs from 'node:fs/promises';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const output = process.env.UC37_OUTPUT || 'D:/Ky9/SWP/outputs/uc37_20261106';
const playwright = process.env.UC37_PLAYWRIGHT || path.join(output, 'browser-tools/node_modules/playwright-core/index.mjs');
const { chromium } = await import(pathToFileURL(playwright));
const storage = JSON.parse(await fs.readFile(path.join(output, 'browser-session.json'), 'utf8'));
const browser = await chromium.launch({ executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true });
const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
const failures = [];
const checks = [];
function check(condition, name) {
    if (!condition) throw new Error(name);
    checks.push(name);
}
try {
    await context.addCookies(storage.cookies);
    const page = await context.newPage();
    page.on('pageerror', error => failures.push(error.message));
    const response = await page.goto('http://localhost:18087/cinema/manager/online-reports?from=2026-11-01&to=2026-11-30', { waitUntil: 'networkidle' });
    check(response.status() === 200, 'Report returns HTTP 200');
    await page.getByRole('heading', { name: 'Lịch sử bán vé online', exact: true }).waitFor();
    check(await page.locator('aside').getByRole('link', { name: 'Lịch sử bán vé online', exact: false }).count() === 1, 'Sidebar uses the new history label');
    check(await page.locator('.or-kpis > div').count() === 6, 'Six KPI cards');
    check(!(await page.locator('main').innerText()).includes('Giờ Việt Nam')
        && !(await page.locator('main').innerText()).includes('Ngày không bán được đơn vẫn hiển thị 0đ'), 'Unnecessary chart and currency descriptions removed');
    for (const field of ['movieId', 'showtimeId', 'bookingId', 'tab', 'status']) {
        check(await page.locator('[name="' + field + '"]').count() === 0, 'Removed filter: ' + field);
    }
    check(await page.getByRole('link', { name: 'Kiểm tra thanh toán', exact: true }).count() === 0
        && await page.getByRole('link', { name: 'Đổi / hoàn', exact: true }).count() === 0, 'Only paid-order history is shown');
    const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date());
    const week = new Date(today + 'T12:00:00Z');
    week.setUTCDate(week.getUTCDate() - 6);
    const [year, month] = today.split('-').map(Number);
    const presets = [
        ['Hôm nay', today],
        ['7 ngày gần nhất', week.toISOString().slice(0, 10)],
        ['Tháng này', today.slice(0, 7) + '-01'],
        ['Quý này', year + '-' + String(Math.floor((month - 1) / 3) * 3 + 1).padStart(2, '0') + '-01'],
        ['Năm này', year + '-01-01']
    ];
    for (const [label, from] of presets) {
        const link = page.getByRole('navigation', { name: 'Chọn nhanh khoảng ngày' }).getByRole('link', { name: label, exact: true });
        await link.click();
        await page.waitForLoadState('networkidle');
        check(await page.locator('#from').inputValue() === from && await page.locator('#to').inputValue() === today, 'Preset date boundaries: ' + label);
        const monthly = ['Quý này', 'Năm này'].includes(label);
        const chart = page.getByRole('region', { name: monthly ? 'Thực thu theo tháng' : 'Thực thu theo ngày', exact: true });
        const expectedBars = monthly ? month - Number(from.slice(5, 7)) + 1
            : Math.round((Date.parse(today) - Date.parse(from)) / 86400000) + 1;
        check(await chart.locator('.or-bar').count() === expectedBars, 'Correct day/month chart buckets: ' + label);
        const labels = await chart.locator('.or-bar span').allTextContents();
        check(labels.every(value => monthly ? /^\d{2}\/\d{4}$/.test(value) : /^\d{2}-\d{2}$/.test(value)), 'Correct chart label format: ' + label);
        const amounts = await chart.locator('tbody td.or-money').allTextContents();
        const sum = amounts.reduce((total, value) => total + Number(value.replace(/[^\d-]/g, '')), 0);
        const [collected, refund, net] = (await page.locator('.or-kpis dd').allTextContents())
            .slice(-3).map(value => Number(value.replace(/[^\d-]/g, '')));
        check(sum === net, 'Chart totals match net collections: ' + label);
        check(net === collected - refund, 'Net collections reconcile: ' + label);
    }
    const table = page.locator('table.or-table-wide');
    const rows = table.locator('tbody tr').filter({ has: page.locator('td a') });
    check(await rows.count() <= 10, 'History renders at most ten orders');
    const layout = await table.evaluate(table => {
        const headers = [...table.querySelectorAll('thead th')];
        const rows = [...table.querySelectorAll('tbody tr')].filter(row => row.children.length !== 1);
        return rows.every(row => [...row.children].every((cell, i) => {
            const header = headers[i].getBoundingClientRect();
            const rect = cell.getBoundingClientRect();
            const aligned = Math.abs(header.left - rect.left) < 1 && Math.abs(header.right - rect.right) < 1;
            const numeric = i < 2 || getComputedStyle(cell).textAlign === getComputedStyle(headers[i]).textAlign;
            return aligned && numeric && cell.textContent.trim().length > 0;
        }));
    });
    check(layout, 'All headers and cells align, numeric columns match, no blank cells');
    await page.screenshot({ path: path.join(output, 'report-desktop.png'), fullPage: true });
    const nextPage = page.getByRole('link', { name: 'Trang sau', exact: true });
    if (await nextPage.count()) {
        const firstIds = await rows.locator('td:first-child a').allTextContents();
        const summary = await page.locator('.or-kpis dd').allTextContents();
        await nextPage.click();
        const secondIds = await rows.locator('td:first-child a').allTextContents();
        check(secondIds.length > 0 && secondIds.length <= 10 && secondIds.every(id => !firstIds.includes(id)), 'Next page has at most ten distinct orders');
        check(JSON.stringify(await page.locator('.or-kpis dd').allTextContents()) === JSON.stringify(summary), 'Pagination preserves full-period totals');
        check(await page.locator('#from').inputValue() === year + '-01-01' && await page.locator('#to').inputValue() === today, 'Pagination preserves date filters');
        check(new URL(page.url()).searchParams.get('groupBy') === 'month'
            && await page.getByRole('heading', { name: 'Thực thu theo tháng', exact: true }).count() === 1, 'Pagination preserves monthly chart');
        await page.getByRole('link', { name: 'Trang trước', exact: true }).click();
        check(JSON.stringify(await rows.locator('td:first-child a').allTextContents()) === JSON.stringify(firstIds), 'Previous page restores the original orders');
    }
    const orderLink = rows.first().locator('a');
    if (await orderLink.count()) {
        await orderLink.click();
        await page.getByRole('heading', { name: /Đơn online #/ }).waitFor();
        check(await page.getByRole('table').count() >= 1, 'Paid order detail renders');
        check(await page.getByText('Kiểm tra thanh toán', { exact: true }).count() === 0, 'Detail has no payment inspection section');
        await page.getByRole('link', { name: 'Về lịch sử bán vé', exact: true }).click();
        check(await page.getByRole('heading', { name: 'Thực thu theo tháng', exact: true }).count() === 1, 'Detail return preserves monthly chart');
    }
    await page.setViewportSize({ width: 375, height: 812 });
    const width = await page.evaluate(() => ({ document: document.documentElement.scrollWidth, viewport: window.innerWidth }));
    check(width.document <= width.viewport + 1, '375px layout has no document horizontal overflow');
    const tableWidth = await table.evaluate(table => ({ table: table.getBoundingClientRect().width, container: table.parentElement.clientWidth }));
    check(tableWidth.table >= 1300 && tableWidth.container < tableWidth.table, 'Mobile table scrolls inside its region');
    await page.screenshot({ path: path.join(output, 'report-mobile.png'), fullPage: true });
    await page.getByLabel('Từ ngày', { exact: true }).fill('1900-01-01');
    await page.getByLabel('Đến ngày', { exact: true }).fill('1900-01-03');
    await page.getByRole('button', { name: 'Xem lịch sử', exact: true }).click();
    check((await page.locator('.or-kpis dd').allTextContents()).every(value => /^0đ?$/.test(value.trim())), 'Empty period displays zero in all KPIs');
    check(await table.getByText('Không có đơn online đã thanh toán trong kỳ.', { exact: true }).count() === 1, 'Empty history has a clear message');
    await page.getByLabel('Từ ngày', { exact: true }).fill('2026-11-07');
    await page.getByLabel('Đến ngày', { exact: true }).fill('2026-11-06');
    await page.getByRole('button', { name: 'Xem lịch sử', exact: true }).click();
    await page.getByRole('alert').waitFor();
    checks.push('Invalid dates show accessible validation feedback');
    check(failures.length === 0, 'No uncaught browser script errors: ' + failures.join('; '));
    await fs.writeFile(path.join(output, 'browser-results.json'), JSON.stringify({ passed: checks.length, checks }, null, 2));
    console.log('PASS: ' + checks.length + ' browser/layout checks');
} finally {
    await browser.close();
    await fs.unlink(path.join(output, 'browser-session.json')).catch(() => {});
}
