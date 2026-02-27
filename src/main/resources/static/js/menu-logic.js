/**
 * 스마트 QR 메뉴판 비즈니스 로직
 * - 메뉴 렌더링 (백엔드 API 연동)
 * - 장바구니 관리 (SessionStorage)
 * - 결제 연동 (주문 단계 생략 버전)
 *
 * 변경점:
 *  - storePublicId/tablePublicId/token 하드코딩 제거 (URL query에서 파싱)
 *  - sessionStorage key 테이블 단위 분리
 */

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
    // 지금 단계에서는 storeName/tableName을 백엔드에서 안 주므로
    // 최소로 tablePublicId만 표시(원하면 "Table xx" 매핑은 나중에 API로)
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
// 3) 앱 초기화
// -------------------------
async function init() {
    lucide.createIcons();

    if (!storePublicId || !tablePublicId || !entryToken) {
        setError("유효하지 않은 QR 입니다. 새로 발급된 QR을 다시 스캔해 주세요.");
        disableOrdering();
        return;
    }

    setHeaderHints();

    await fetchMenuData();
    renderMenuGrid();
    updateUI();
    lucide.createIcons();
}

// -------------------------
// 4) 메뉴 API 호출
// -------------------------
async function fetchMenuData() {
    try {
        const url = `/api/menus/${storePublicId}/customer`;
        console.log("[menu] fetching:", url);

        const response = await fetch(url, {
            method: "GET",
            headers: { "Accept": "application/json" },
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
// 5) 메뉴 렌더링
// -------------------------
function renderMenuGrid() {
    const grid = document.getElementById('menu-grid');
    if (!grid) return;

    grid.innerHTML = '';

    menuData.forEach(item => {
        const card = document.createElement('div');
        card.className = 'menu-card flex bg-white rounded-2xl p-4 border border-gray-100 shadow-sm gap-4 items-center';

        const iconWrap = document.createElement('div');
        iconWrap.className = 'w-16 h-16 bg-rose-50 rounded-2xl flex items-center justify-center text-3xl shadow-inner shrink-0';
        iconWrap.textContent = item.icon ?? '';

        const body = document.createElement('div');
        body.className = 'flex-1';

        const title = document.createElement('h3');
        title.className = 'font-bold text-gray-900 text-base';
        title.textContent = item.name ?? '';

        const desc = document.createElement('p');
        desc.className = 'text-xs text-gray-400 mt-1';
        desc.textContent = item.desc ?? '';

        const price = document.createElement('p');
        price.className = 'text-sm font-black text-rose-600 mt-2';
        price.textContent = `${(item.price ?? 0).toLocaleString()}원`;

        body.append(title, desc, price);

        const btn = document.createElement('button');
        btn.className = 'bg-gray-900 text-white w-10 h-10 rounded-xl flex items-center justify-center shadow-lg active:bg-rose-600 transition-all';
        btn.addEventListener('click', () => addToCart(item.id));

        const plusIcon = document.createElement('i');
        plusIcon.setAttribute('data-lucide', 'plus');
        plusIcon.className = 'w-5 h-5';
        btn.appendChild(plusIcon);

        card.append(iconWrap, body, btn);
        grid.appendChild(card);
    });

    lucide.createIcons();
}

// -------------------------
// 6) 장바구니 로직
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
// 7) 결제(아직은 데모) - 하드코딩 제거
// -------------------------
async function goToPayment() {
    if (cart.length === 0) {
        alert("장바구니에 담긴 메뉴가 없습니다.");
        return;
    }

    const total = cart.reduce((acc, cur) => acc + (cur.price * cur.quantity), 0);

    if (confirm(`최종 결제 금액은 ${total.toLocaleString()}원입니다.\n결제를 진행하시겠습니까?`)) {
        const paymentPayload = {
            storePublicId,
            tablePublicId,

            token: entryToken,

            // 데모: 그대로 보내지만, 나중엔 menuPublicId+qty만 보내도록 바꾸는게 정석
            orderList: cart,
            totalAmount: total,
            timestamp: new Date().toISOString()
        };

        console.log("결제 API 요청 데이터:", paymentPayload);

        try {
            // TODO: 다음 단계에서 실제 결제 API 연동
            alert("결제가 완료되었습니다. 이용해주셔서 감사합니다!");

            cart = [];
            saveState();
            window.location.reload();
        } catch (e) {
            console.error("Payment Error:", e);
            alert("결제 처리 중 통신 오류가 발생했습니다.");
        }
    }
}

// -------------------------
// 8) UI 업데이트
// -------------------------
function updateUI() {
    const cartTotalEl = document.getElementById('cart-total');
    const payBtn = document.getElementById('pay-btn');

    const cartSum = cart.reduce((acc, cur) => acc + (cur.price * cur.quantity), 0);

    if (cartTotalEl) cartTotalEl.innerText = cartSum.toLocaleString() + '원';

    if (payBtn) {
        if (cart.length > 0) {
            payBtn.classList.remove('opacity-50', 'cursor-not-allowed');
            payBtn.disabled = false;
        } else {
            payBtn.classList.add('opacity-50', 'cursor-not-allowed');
            payBtn.disabled = true;
        }
    }

    renderCartDrawerList();
    lucide.createIcons();
}

function renderCartDrawerList() {
    const container = document.getElementById('cart-list');
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

    container.innerHTML = '';

    cart.forEach(item => {
        const row = document.createElement('div');
        row.className = 'flex items-center justify-between py-2';

        const left = document.createElement('div');
        left.className = 'flex items-center gap-4';

        const icon = document.createElement('span');
        icon.className = 'text-2xl';
        icon.textContent = item.icon ?? '';

        const info = document.createElement('div');

        const name = document.createElement('p');
        name.className = 'text-sm font-bold text-gray-800';
        name.textContent = item.name ?? '';

        const subtotal = document.createElement('p');
        subtotal.className = 'text-xs text-rose-600 font-bold';
        subtotal.textContent = `${((item.price ?? 0) * (item.quantity ?? 0)).toLocaleString()}원`;

        info.append(name, subtotal);
        left.append(icon, info);

        const right = document.createElement('div');
        right.className = 'flex items-center gap-2 bg-gray-50 rounded-xl p-1.5 border border-gray-100';

        const minus = document.createElement('button');
        minus.className = 'w-7 h-7 flex items-center justify-center bg-white rounded-lg border border-gray-200 text-sm font-bold active:bg-gray-100';
        minus.textContent = '-';
        minus.addEventListener('click', () => adjustCartQty(item.id, -1));

        const qty = document.createElement('span');
        qty.className = 'text-sm font-black w-6 text-center';
        qty.textContent = String(item.quantity ?? 0);

        const plus = document.createElement('button');
        plus.className = 'w-7 h-7 flex items-center justify-center bg-white rounded-lg border border-gray-200 text-sm font-bold active:bg-gray-100';
        plus.textContent = '+';
        plus.addEventListener('click', () => adjustCartQty(item.id, 1));

        right.append(minus, qty, plus);

        row.append(left, right);
        container.appendChild(row);
    });

    lucide.createIcons();
}

// -------------------------
// 9) 드로어
// -------------------------
function toggleDrawer() {
    const drawer = document.getElementById('drawer');
    const overlay = document.getElementById('drawer-overlay');
    if (!drawer || !overlay) return;

    if (drawer.classList.contains('translate-y-full')) {
        drawer.classList.remove('translate-y-full');
        overlay.classList.remove('hidden');
        setTimeout(() => overlay.classList.add('opacity-100'), 10);
    } else {
        drawer.classList.add('translate-y-full');
        overlay.classList.remove('opacity-100');
        setTimeout(() => overlay.classList.add('hidden'), 300);
    }
}

window.addEventListener('DOMContentLoaded', init);