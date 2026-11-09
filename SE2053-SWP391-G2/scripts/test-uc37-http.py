"""HTTP/JSP checks against the isolated Tomcat started by prepare-uc37-http-test.ps1.
Creates temporary login accounts, then deactivates them; existing users/permissions are unchanged.
Login audit history is preserved. No financial fixture is committed.
"""
import hashlib
import http.cookiejar
import json
import os
from pathlib import Path
import re
import subprocess
import urllib.error
import urllib.parse
import urllib.request
import uuid

BASE = 'http://localhost:18087/cinema'
OUT = Path(os.environ.get('UC37_OUTPUT', 'D:/Ky9/SWP/outputs/uc37_20261106'))
OUT.mkdir(parents=True, exist_ok=True)
checks = []


def sql(query):
    result = subprocess.run(['sqlcmd', '-S', 'tcp:localhost,1433', '-E', '-d', 'cinemaDB', '-b', '-I',
                             '-W', '-h', '-1', '-Q', 'SET NOCOUNT ON; ' + query], capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(result.stderr + result.stdout)
    return result.stdout.strip()


def session():
    jar = http.cookiejar.CookieJar()
    return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar)), jar


def get(client, path, data=None):
    request = urllib.request.Request(BASE + path, data=urllib.parse.urlencode(data).encode() if data else None)
    try:
        response = client.open(request, timeout=30)
        return response.status, response.read(), response
    except urllib.error.HTTPError as error:
        return error.code, error.read(), error


def check(condition, name):
    if not condition:
        raise AssertionError(name)
    checks.append(name)


def login(email, password):
    client, jar = session()
    status, body, _ = get(client, '/login')
    token = re.search(r'name="csrfToken"\s+value="([^"]+)"', body.decode('utf-8'))
    check(status == 200 and token is not None, 'Login form/CSRF available')
    status, body, _ = get(client, '/login', {'identifier': email, 'password': password, 'csrfToken': token.group(1)})
    check(status == 200, 'Login and role landing page render')
    return client, jar


token = uuid.uuid4().hex[:12]
password = uuid.uuid4().hex + '!aA1'
salt = os.urandom(16)
password_hash = salt.hex() + ':' + hashlib.pbkdf2_hmac('sha256', password.encode(), salt, 65536, 32).hex()
emails = {role: 'uc37-' + token + '-' + role.lower() + '@example.invalid' for role in ['Manager', 'Staff', 'Customer', 'Admin']}
ids = []
try:
    for index, (role, email) in enumerate(emails.items()):
        phone = '9' + str(int(token, 16))[-10:] + str(index)
        ids.append(int(sql("INSERT INTO [User](FullName,Email,Phone,PasswordHash,Role,Status) "
                           "VALUES (N'UC37 HTTP Test', '%s', '%s', '%s', '%s', 'Active'); SELECT SCOPE_IDENTITY();"
                           % (email, phone, password_hash, role))))
    anonymous, _ = session()
    status, _, response = get(anonymous, '/manager/online-reports?export=csv')
    check(status == 200 and '/login' in response.url, 'Anonymous export redirects to login')
    manager, jar = login(emails['Manager'], password)
    query = '?from=2026-11-01&to=2026-11-30'
    for tab in ['orders', 'adjustments', 'payments']:
        status, body, _ = get(manager, '/manager/online-reports' + query + '&tab=' + tab)
        (OUT / ('http-' + tab + '.html')).write_bytes(body)
        check(status == 200 and 'Lịch sử bán vé online'.encode() in body, 'Paid-order history renders, including legacy tab URL: ' + tab)
        check(b'Cache-Control' not in body and b'or-kpis' in body, 'Report has summary and no error fallback: ' + tab)
        check('Kiểm tra thanh toán'.encode() not in body and 'Đổi / hoàn'.encode() not in body,
              'Removed sections cannot be reopened by old query parameters: ' + tab)
        check(len(re.findall(rb'<dt>', body)) == 6, 'Six summary cards: ' + tab)
        status, body, response = get(manager, '/manager/online-reports' + query + '&tab=' + tab + '&export=csv')
        check(status == 200 and response.headers.get_content_type() == 'text/csv' and body.startswith(b'\xef\xbb\xbf'),
              'UTF-8 CSV export: ' + tab)
        check('"Đã thu","Hoàn tiền và bù ghế","Thực thu"'.encode() in body, 'CSV has cash adjustment columns: ' + tab)
        (OUT / ('http-' + tab + '.csv')).write_bytes(body)
    online_id = sql("SELECT TOP 1 b.BookingID FROM Booking b WHERE b.Channel='Online' AND b.Status='Paid' "
                    "AND b.PaidAt IS NOT NULL AND (b.FinalAmount=0 OR EXISTS (SELECT 1 FROM OnlinePayment p "
                    "WHERE p.BookingID=b.BookingID AND p.Status='Paid' AND p.Amount=b.FinalAmount "
                    "AND p.ReceivedAmount=b.FinalAmount AND p.PaidAt IS NOT NULL)) ORDER BY b.BookingID")
    counter_id = sql("SELECT TOP 1 BookingID FROM Booking WHERE Channel='Counter' ORDER BY BookingID")
    status, body, _ = get(manager, '/manager/online-reports?id=' + online_id)
    (OUT / 'http-detail.html').write_bytes(body)
    check(status == 200 and ('Đơn online #' + online_id).encode() in body, 'Paid online order detail renders')
    check('Kiểm tra thanh toán'.encode() not in body and 'Đổi / hoàn'.encode() not in body,
          'Order detail excludes removed sections')
    check(get(manager, '/manager/online-reports?id=' + counter_id)[0] == 404, 'Counter order blocked from online detail')
    check(get(manager, '/manager/online-reports?from=2026-02-30')[0] == 400, 'Malformed date returns validation page')
    check(get(manager, '/manager/online-reports?from=2026-11-07&to=2026-11-06')[0] == 400, 'Reversed range rejected')
    check(get(manager, '/manager/online-reports?bookingId=1%20OR%201=1')[0] == 200, 'Removed booking filter ignored')
    check(get(manager, '/manager/online-reports?tab=payments&status=unknown')[0] == 200, 'Removed tab/status filters ignored')
    check(get(manager, '/manager/online-reports?id=1%20OR%201=1')[0] == 400, 'Malformed detail ID rejected')
    check(get(manager, '/manager/online-reports?export=pdf')[0] == 400, 'Unsupported export rejected')
    check(get(manager, '/manager/online-reports?from=1900-01-01&to=1900-01-02')[0] == 200, 'Empty report renders correctly')
    for path in ['/manager/reports', '/manager/shifts', '/manager/counter-history']:
        check(get(manager, path)[0] == 200, 'Existing management page still renders: ' + path)
    for role in ['Staff', 'Customer']:
        client, _ = login(emails[role], password)
        for suffix in ['', '?export=csv', '?id=' + online_id]:
            check(get(client, '/manager/online-reports' + suffix)[0] == 403, role + ' denied direct report action ' + suffix)
    admin, _ = login(emails['Admin'], password)
    status, body, _ = get(admin, '/admin/permissions')
    check(status == 200 and b'VIEW_ONLINE_REPORTS' in body, 'UC-31 exposes the new configurable permission')
    # Session is sensitive and only retained for the local optional browser QA; it is deleted after that check.
    (OUT / 'browser-session.json').write_text(json.dumps({'cookies': [
        {'name': c.name, 'value': c.value, 'domain': 'localhost', 'path': c.path, 'httpOnly': True,
         'secure': False, 'sameSite': 'Lax'} for c in jar]}), encoding='utf-8')
    if os.environ.get('UC37_BROWSER') == '1':
        subprocess.run(['node', str(Path(__file__).with_name('test-uc37-browser.mjs'))], check=True)
    result = {'passed': len(checks), 'checks': checks, 'test_user_ids': ids,
              'financial_data_committed': False, 'accounts_deactivated_after_test': True}
    (OUT / 'http-results.json').write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
    print('PASS: %d HTTP/JSP/auth checks' % len(checks))
finally:
    (OUT / 'browser-session.json').unlink(missing_ok=True)
    if ids:
        sql("UPDATE [User] SET Status='Deactivated' WHERE UserID IN (%s) AND Email LIKE 'uc37-%s-%%@example.invalid'"
            % (','.join(map(str, ids)), token))
