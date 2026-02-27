// -------------------------
// 0) QR 파라미터 파싱 (백엔드가 생성하는 URL 기준)
//   /qr_menu_order.html?s={storePublicId}&t={tablePublicId}&token={entryToken}
// -------------------------
const urlParams = new URLSearchParams(window.location.search);
const storePublicId = urlParams.get("s");
const tablePublicId = urlParams.get("t");
const entryToken = urlParams.get("token");

// 테이블별 장바구니 분리 저장
const CART_KEY = (storePublicId && tablePublicId)
    ? `current_cart::${storePublicId}::${tablePublicId}`
    : "current_cart::invalid";

// -------------------------
// 1) 초기 메뉴 데이터 / 상태
// -------------------------
let menuData = [];
let cart = JSON.parse(sessionStorage.getItem(CART_KEY) || "[]");

// -------------------------
// 2) UI 유틸
// -------------------------
function setError(message) {
    const box = document.getElementById("error-box");
    if (!box) return;
    box.textContent = message;
    box.classList.remove("hidden");
}

function clearError() {
    const box = document.getElementById("error-box");
    if (!box) return;
    box.textContent = "";
    box.classList.add("hidden");
}

function setHeaderHints() {
    const tableEl = document.getElementById("table-name");
    const subEl = document.getElementById("subtitle");

    if (tableEl) tableEl.textContent = tablePublicId ? `Table (${tablePublicId})` : "Table";
    if (subEl) subEl.textContent = "QR로 입장했습니다";
}

function disableOrdering() {
    const payBtn = document.getElementById("pay-btn");
    if (payBtn) {
        payBtn.disabled = true;
        payBtn.classList.add("opacity-50", "cursor-not-allowed");
    }
    const grid = document.getElementById("menu-grid");
    if (grid) grid.innerHTML = "";
}

// -------------------------
// 3) QR 입장(세션 생성)
// -------------------------
async function enterTableSession() {
    const response = await fetch("/api/table-sessions/enter", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Accept": "application/json",
            // 컨트롤러가 요구하는 헤더명 그대로
            "X-Table-Entry-Token": entryToken,
        },
        // ✅ 서버의 Set-Cookie를 브라우저가 저장/동봉할 수 있게
        credentials: "include",
        body: JSON.stringify({
            storePublicId,
            tablePublicId,
        }),
    });

    if (!response.ok) {
        const text = await response.text().catch(() => "");
        throw new Error(`enter 실패: HTTP ${response.status} ${response.statusText} :: ${text}`);
    }

    // 응답 바디가 필요 없으면 굳이 사용 안 해도 됨
    return response.json().catch(() => ({}));
}

// -------------------------
// 4) 앱 초기화
// -------------------------
async function init() {
    lucide.createIcons();

    if (!storePublicId || !tablePublicId || !entryToken) {
        setError("유효하지 않은 QR 입니다. 새로 발급된 QR을 다시 스캔해 주세요.");
        disableOrdering();
        return;
    }

    setHeaderHints();

    try {
        await enterTableSession();
        clearError();
    } catch (e) {
        console.error("Enter error:", e);
        setError("QR 인증에 실패했습니다. 새로 발급된 QR을 다시 스캔해 주세요.");
        disableOrdering();
        return;
    }

    await fetchMenuData();
    renderMenuGrid();
    updateUI();
    lucide.createIcons();
}

// -------------------------
// 5) 메뉴 API 호출
// -------------------------
async function fetchMenuData() {
    try {
        const url = `/api/menus/${storePublicId}/customer`;
        console.log("[menu] fetching:", url);

        const response = await fetch(url, {
            method: "GET",
            headers: { "Accept": "application/json" },
            // ✅ 메뉴 API도 인증/세션이 필요할 수 있어 포함(필요 없으면 있어도 무해)
            credentials: "include",
        });

        if (!response.ok) {
            const text = await response.text().catch(() => "");
            throw new Error(`HTTP ${response.status} ${response.statusText} :: ${text}`);
        }

        const data = await response.json();
        const list = Array.isArray(data) ? data : (data.data ?? data.content ?? []);

        menuData = list.map(item => ({
            id: item.menuPublicId,
            name: item.name,
            price: item.basePrice,
            desc: item.ingredientsJson?.description ?? "",
            icon: "🍽️",
        }));

        clearError();
    } catch (error) {
        console.error("Menu fetch error:", error);
        setError("메뉴를 불러올 수 없습니다.");
        const grid = document.getElementById("menu-grid");
        if (grid) grid.innerHTML = '<p class="text-center text-gray-500 py-10">메뉴를 불러올 수 없습니다.</p>';
        disableOrdering();
    }
}

// -------------------------
// 6) 메뉴 렌더링
// -------------------------
function renderMenuGrid() {
    const grid = document.getElementById("menu-grid");
    if (!grid) return;

    grid.innerHTML = "";

    menuData.forEach(item => {
        const card = document.createElement("div");
        card.className =
            "menu-card flex bg-white rounded-2xl p-4 border border-gray-100 shadow-sm gap-4 items-center";

        const iconWrap = document.createElement("div");
        iconWrap.className =
            "w-16 h-16 bg-rose-50 rounded-2xl flex items-center justify-center text-3xl shadow-inner shrink-0";
        iconWrap.textContent = item.icon ?? "";

        const body = document.createElement("div");
        body.className = "flex-1";

        const title = document.createElement("h3");
        title.className = "font-bold text-gray-900 text-base";
        title.textContent = item.name ?? "";

        const desc = document.createElement("p");
        desc.className = "text-xs text-gray-400 mt-1";
        desc.textContent = item.desc ?? "";

        const price = document.createElement("p");
        price.className = "text-sm font-black text-rose-600 mt-2";
        price.textContent = `${(item.price ?? 0).toLocaleString()}원`;

        body.append(title, desc, price);

        const btn = document.createElement("button");
        btn.className =
            "bg-gray-900 text-white w-10 h-10 rounded-xl flex items-center justify-center shadow-lg active:bg-rose-600 transition-all";
        btn.addEventListener("click", () => addToCart(item.id));

        const plusIcon = document.createElement("i");
        plusIcon.setAttribute("data-lucide", "plus");
        plusIcon.className = "w-5 h-5";
        btn.appendChild(plusIcon);

        card.append(iconWrap, body, btn);
        grid.appendChild(card);
    });

    lucide.createIcons();
}

// -------------------------
// 7) 장바구니 로직
// -------------------------
function addToCart(id) {
    const item = menuData.find(m => m.id === id);
    if (!item) return;

    const existing = cart.find(c => c.id === id);

    if (existing) existing.quantity += 1;
    else cart.push({ ...item, quantity: 1 });

    saveState();
    updateUI();
}

function adjustCartQty(id, delta) {
    const idx = cart.findIndex(c => c.id === id);
    if (idx === -1) return;

    cart[idx].quantity += delta;
    if (cart[idx].quantity <= 0) cart.splice(idx, 1);

    saveState();
    updateUI();
}

function clearCart() {
    cart = [];
    saveState();
    updateUI();
}

function saveState() {
    sessionStorage.setItem(CART_KEY, JSON.stringify(cart));
}

// -------------------------
// 8) 결제(/api/orders)
// -------------------------
async function goToPayment() {
    if (cart.length === 0) {
        alert("장바구니에 담긴 메뉴가 없습니다.");
        return;
    }

    const total = cart.reduce((acc, cur) => acc + (cur.price * cur.quantity), 0);

    if (!confirm(`최종 결제 금액은 ${total.toLocaleString()}원입니다.\n결제를 진행하시겠습니까?`)) {
        return;
    }

    const paymentPayload = {
        storePublicId,
        tablePublicId,
        token: entryToken, // 백엔드에서 불필요하면 제거 가능(현재는 유지)
        items: cart.map((item) => ({
            menuPublicId: item.id,
            quantity: item.quantity,
        })),
    };

    console.log("결제 API 요청:", paymentPayload);

    try {
        const idempotencyKey =
            (window.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(36).slice(2)}`);

        const response = await fetch("/api/orders", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Idempotency-Key": idempotencyKey,
                "Accept": "application/json",
            },
            credentials: "include",
            body: JSON.stringify(paymentPayload),
        });

        if (!response.ok) {
            const text = await response.text().catch(() => "");
            let message = "결제 처리 실패";
            try {
                const parsed = JSON.parse(text);
                message = parsed?.message ?? message;
            } catch (_) {
                if (text) message = text;
            }
            throw new Error(message);
        }

        const result = await response.json().catch(() => ({}));
        console.log("결제 성공:", result);

        alert("결제가 완료되었습니다. 이용해주셔서 감사합니다!");

        cart = [];
        saveState();
        window.location.reload();
    } catch (e) {
        console.error("Payment Error:", e);
        alert(`결제 처리 중 오류가 발생했습니다: ${e.message}`);
    }
}

// -------------------------
// 9) UI 업데이트
// -------------------------
function updateUI() {
    const cartTotalEl = document.getElementById("cart-total");
    const payBtn = document.getElementById("pay-btn");

    const cartSum = cart.reduce((acc, cur) => acc + (cur.price * cur.quantity), 0);

    if (cartTotalEl) cartTotalEl.innerText = cartSum.toLocaleString() + "원";

    if (payBtn) {
        if (cart.length > 0) {
            payBtn.classList.remove("opacity-50", "cursor-not-allowed");
            payBtn.disabled = false;
        } else {
            payBtn.classList.add("opacity-50", "cursor-not-allowed");
            payBtn.disabled = true;
        }
    }

    renderCartDrawerList();
    lucide.createIcons();
}

function renderCartDrawerList() {
    const container = document.getElementById("cart-list");
    if (!container) return;

    if (cart.length === 0) {
        container.innerHTML = `
      <div class="flex flex-col items-center justify-center py-10 opacity-30">
        <i data-lucide="shopping-cart" class="w-12 h-12 mb-2"></i>
        <p class="text-sm">장바구니가 비어있습니다.</p>
      </div>
    `;
        lucide.createIcons();
        return;
    }

    container.innerHTML = "";

    cart.forEach(item => {
        const row = document.createElement("div");
        row.className = "flex items-center justify-between py-2";

        const left = document.createElement("div");
        left.className = "flex items-center gap-4";

        const icon = document.createElement("span");
        icon.className = "text-2xl";
        icon.textContent = item.icon ?? "";

        const info = document.createElement("div");

        const name = document.createElement("p");
        name.className = "text-sm font-bold text-gray-800";
        name.textContent = item.name ?? "";

        const subtotal = document.createElement("p");
        subtotal.className = "text-xs text-rose-600 font-bold";
        subtotal.textContent = `${((item.price ?? 0) * (item.quantity ?? 0)).toLocaleString()}원`;

        info.append(name, subtotal);
        left.append(icon, info);

        const right = document.createElement("div");
        right.className =
            "flex items-center gap-2 bg-gray-50 rounded-xl p-1.5 border border-gray-100";

        const minus = document.createElement("button");
        minus.className =
            "w-7 h-7 flex items-center justify-center bg-white rounded-lg border border-gray-200 text-sm font-bold active:bg-gray-100";
        minus.textContent = "-";
        minus.addEventListener("click", () => adjustCartQty(item.id, -1));

        const qty = document.createElement("span");
        qty.className = "text-sm font-black w-6 text-center";
        qty.textContent = String(item.quantity ?? 0);

        const plus = document.createElement("button");
        plus.className =
            "w-7 h-7 flex items-center justify-center bg-white rounded-lg border border-gray-200 text-sm font-bold active:bg-gray-100";
        plus.textContent = "+";
        plus.addEventListener("click", () => adjustCartQty(item.id, 1));

        right.append(minus, qty, plus);

        row.append(left, right);
        container.appendChild(row);
    });

    lucide.createIcons();
}

// -------------------------
// 10) 드로어
// -------------------------
function toggleDrawer() {
    const drawer = document.getElementById("drawer");
    const overlay = document.getElementById("drawer-overlay");
    if (!drawer || !overlay) return;

    if (drawer.classList.contains("translate-y-full")) {
        drawer.classList.remove("translate-y-full");
        overlay.classList.remove("hidden");
        setTimeout(() => overlay.classList.add("opacity-100"), 10);
    } else {
        drawer.classList.add("translate-y-full");
        overlay.classList.remove("opacity-100");
        setTimeout(() => overlay.classList.add("hidden"), 300);
    }
}

window.addEventListener("DOMContentLoaded", init);