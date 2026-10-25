# UC14 - Huong dan test VNPay Sandbox local

## Test local khong can tunnel

`config.properties` dung chung da co san tai khoan VNPay Sandbox va `payment.vnpay.return-url=http://localhost:8080/SE2053-SWP391-G2/customer/booking/payment/return`.

1. Build/deploy nhu binh thuong, vao web bang `http://localhost:8080/SE2053-SWP391-G2/`, dang nhap Customer, dat ve va bam thanh toan VNPay.
2. Tren trang Sandbox chon Ngan hang NCB, the `9704198526191432198`, ten `NGUYEN VAN A`, ngay phat hanh `07/15`, OTP `123456`.
3. VNPay chuyen trinh duyet ve Return URL tren localhost. Tham so tra ve co chu ky HMAC nhu IPN nen backend xac minh chu ky va chot don ngay tai day (khong can VNPay goi IPN vao may local).

IPN khong can cau hinh khi test local. Neu deploy tren domain HTTPS cong khai, co the cau hinh IPN de VNPay thong bao ket qua truc tiep; IPN va Return URL deu chot don idempotent, giao dich chi duoc xu ly mot lan.

## 1. Chuan bi project va database

1. Cai JDK 21, Maven, Tomcat 10.x va SQL Server theo README.
2. Dam bao `SE2053-SWP391-G2/src/main/resources/ConnectDB.properties` ket noi duoc den database `cinemaDB`.
3. Chay `SE2053-SWP391-G2/sql/UC14_vnpay.sql` neu database chua co schema giao dich UC14.
4. Build WAR bang `mvn clean package` trong `SE2053-SWP391-G2`, sau do deploy/start Tomcat.

Voi Tomcat mac dinh, trang local la `http://localhost:8080/SE2053-SWP391-G2/`. Neu database chua duoc tao, lam theo thu tu khoi tao trong README; khong chay lai `cinemaDB.sql` tren database dang su dung.

## 2. Hieu thu tu nap config

`AppConfig` nap `config.properties.example`, sau do `config.properties` dung chung trong classpath se ghi de. File rieng ben ngoai, JVM system properties va environment variables co do uu tien cao hon theo thu tu nay.

- `config.properties` dung chung duoc theo doi boi Git; hien bat `auth.enabled=true` va chua cau hinh mail OTP cua nhom.
- `config.properties.example` la fallback cho cac key chua co trong config dung chung; URL mac dinh dung localhost va khong bat IPN.
- `ConnectDB.properties` la file ket noi SQL Server rieng, khong phai file cau hinh AppConfig.
- Thong tin merchant VNPay rieng can duoc dat trong file config ben ngoai hoac environment variables; khong commit key rieng vao repository.

## 3. Cau hinh VNPay local

Cach de Tomcat tu tim thay file la tao:

`<CATALINA_BASE>\conf\cineluxe\config.properties`

Trong VS Code Server Connector, `CATALINA_BASE` la gia tri `server.base.dir` trong cau hinh server. Neu can, xem log Tomcat luc khoi dong. Tao thu muc `conf\cineluxe` neu chua co.

Neu dung duong dan khac, tao file vi du `%USERPROFILE%\cinema-config\config.properties`, roi truyen JVM option `-Dapp.config=<duong-dan-tuyet-doi>`. Trong cau hinh Server Connector JSON, noi option nay vao `args.vm.override.string` va bat `args.override.boolean` thanh `true`; khong them vao `args.shutdown.vm.override.string`. Restart Tomcat sau khi doi config.

Them cac dong sau vao file rieng va thay placeholder bang thong tin Sandbox cua thanh vien. URL Return localhost duoc dung de VNPay dua trinh duyet ve website; de trong callback URL vi khong dung IPN khi test local:

```properties
payment.vnpay.tmn-code=<SANDBOX_TMN_CODE>
payment.vnpay.hash-secret=<SANDBOX_HASH_SECRET>
payment.vnpay.payment-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
payment.vnpay.return-url=http://localhost:8080/SE2053-SWP391-G2/customer/booking/payment/return
payment.vnpay.callback-url=
payment.vnpay.expire-minutes=10
```

Neu dung environment variables, ten key tuong ung la `APP_PAYMENT_VNPAY_TMN_CODE`, `APP_PAYMENT_VNPAY_HASH_SECRET`, `APP_PAYMENT_VNPAY_PAYMENT_URL`, `APP_PAYMENT_VNPAY_RETURN_URL` va `APP_PAYMENT_VNPAY_CALLBACK_URL`.

## 4. Tuy chon: dung tunnel/IPN voi URL cong khai

Chi can muc nay neu muon VNPay goi IPN vao may local, hoac muon truy cap website qua HTTPS tunnel. Neu chi test local theo muc tren, bo qua muc nay.

Tren Windows, cai `cloudflared` neu chua co:

```powershell
winget install --id Cloudflare.cloudflared -e
```

Mo PowerShell moi neu vua cai xong. Khi Tomcat dang chay o cong 8080, mo mot cua so PowerShell rieng:

```powershell
cloudflared tunnel --url http://localhost:8080
```

Giu cua so tunnel mo trong luc test. Lay hostname HTTPS duoc in ra, vi du `https://<ten-ngau-nhien>.trycloudflare.com`; gia tri `<TUNNEL_HOST>` trong config la `ten-ngau-nhien.trycloudflare.com`, khong gom `https://`. Moi lan khoi dong Quick Tunnel co the tao hostname moi.

Sau khi thay hostname trong config, restart Tomcat. Kiem tra website qua `https://<TUNNEL_HOST>/SE2053-SWP391-G2/`.

## 5. Dang ky IPN va test qua URL cong khai

1. Trong file config rieng, dat Return URL va callback URL thanh HTTPS tunnel/domain, bao gom context `/SE2053-SWP391-G2`.
2. Dang nhap Merchant Admin/SIT va dang ky IPN URL la gia tri `payment.vnpay.callback-url` trong file config.
3. Dang nhap website voi tai khoan Customer, tao don co tong thanh toan lon hon 0 va chon VNPay.
4. Hoan tat giao dich theo test case/thong tin thanh toan Sandbox VNPay cung cap. Return URL va IPN deu duoc xac minh va co the chot don; kiem tra trang lich su/don hang va log Tomcat.
5. Ket thuc test bang `Ctrl+C` trong cua so `cloudflared`.

Don co tong thanh toan 0 dong khong di qua VNPay; website xac nhan truc tiep theo luong booking 0 dong.

## Xu ly loi nhanh

- `cloudflared is not recognized`: neu dang dung tunnel, cai bang winget va mo PowerShell moi.
- `404` o Return/IPN: kiem tra WAR da deploy va URL co context `/SE2053-SWP391-G2`.
- `Invalid HTTPS ... URL`: chi ap dung khi dung tunnel/domain cong khai; Return URL va callback URL khi do phai dung HTTPS va hostname hien tai.
- Thieu `payment.vnpay.*`: kiem tra file config duoc dat dung vi tri, key khong trong va Tomcat da restart.
- Don van `Pending` sau khi Return: kiem tra Return URL trong config, chu ky giao dich va log Tomcat; neu dang dung IPN, kiem tra them callback URL va ket noi tunnel/domain.
- Loi schema giao dich: chay `SE2053-SWP391-G2/sql/UC14_vnpay.sql` tren database local.
