/**
 * 스마트 QR 메뉴판 비즈니스 로직
 * - 메뉴 렌더링 (백엔드 API 연동)
 * - 장바구니 관리 (SessionStorage)
 * - 주문 전송 및 최종 결제 연동
 */

// 1. 초기 메뉴 데이터 (API 로드 전 기본값 또는 빈 배열)
let menuData = [];

// 2. 전역 상태 관리
let cart = JSON.parse(sessionStorage.getItem('current_cart') || '[]');
let orderedItems = JSON.parse(sessionStorage.getItem('ordered_history') || '[]');

// URL에서 storePublicId 추출 (예: /qr_menu_order.html?storeId=...)
const urlParams = new URLSearchParams(window.location.search);
const storePublicId = "80b914dc-fd48-4b60-9f12-01ce4c116593"
// const storePublicId = urlParams.get('storeId') || 'default-store-id'; // 테스트용 기본값 설정 필요 시 수정

/**
 * 앱 초기화
 */
async function init() {
    await fetchMenuData();
    renderMenuGrid();
    updateUI();
    lucide.createIcons();
}

/**
 * 백엔드 API에서 메뉴 데이터 가져오기
 */
// const API_BASE = "http://localhost:8080";

async function fetchMenuData() {
    try {
        const url = `/api/menus/${storePublicId}/customer`;
        console.log("[menu] fetching:", url);

        const response = await fetch(url, {
            method: "GET",
            headers: { "Accept": "application/json" },
            // credentials: "include", // 쿠키/세션 쓰면 필요
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
    } catch (error) {
        console.error("Menu fetch error:", error);
        const grid = document.getElementById("menu-grid");
        if (grid) grid.innerHTML = '<p class="text-center text-gray-500">메뉴를 불러올 수 없습니다.</p>';
    }
}

/**
 * 메뉴 리스트 화면 렌더링
 */
function renderMenuGrid() {
    const grid = document.getElementById('menu-grid');
    if (!grid) return;

    grid.innerHTML = '';
    menuData.forEach(item => {
        const card = document.createElement('div');
        card.className = 'menu-card flex bg-white rounded-2xl p-4 border border-gray-100 shadow-sm gap-4 items-center';
        card.innerHTML = `
            <div class="w-16 h-16 bg-rose-50 rounded-2xl flex items-center justify-center text-3xl shadow-inner shrink-0">
                ${item.icon}
            </div>
            <div class="flex-1">
                <h3 class="font-bold text-gray-900 text-base">${item.name}</h3>
                <p class="text-xs text-gray-400 mt-1">${item.desc}</p>
                <p class="text-sm font-black text-rose-600 mt-2">${item.price.toLocaleString()}원</p>
            </div>
            <button onclick="addToCart('${item.id}')" class="bg-gray-900 text-white w-10 h-10 rounded-xl flex items-center justify-center shadow-lg active:bg-rose-600 transition-all">
                <i data-lucide="plus" class="w-5 h-5"></i>
            </button>
        `;
        grid.appendChild(card);
    });
    lucide.createIcons(); // 동적으로 추가된 아이콘 렌더링
}

/**
 * 장바구니에 아이템 추가
 */
function addToCart(id) {
    const item = menuData.find(m => m.id === id);
    if (!item) return;

    const existing = cart.find(c => c.id === id);

    if (existing) {
        existing.quantity += 1;
    } else {
        cart.push({ ...item, quantity: 1 });
    }

    saveState();
    updateUI();
}

/**
 * 장바구니 수량 조절
 */
function adjustCartQty(id, delta) {
    const idx = cart.findIndex(c => c.id === id);
    if (idx === -1) return;

    cart[idx].quantity += delta;
    if (cart[idx].quantity <= 0) {
        cart.splice(idx, 1);
    }
    saveState();
    updateUI();
}

/**
 * 장바구니 비우기
 */
function clearCart() {
    cart = [];
    saveState();
    updateUI();
}

/**
 * 주문 전송 (여러 번 가능)
 */
function placeOrder() {
    if (cart.length === 0) return;

    if (confirm("주방으로 주문을 전송하시겠습니까?")) {
        // 주문 내역 누적
        cart.forEach(cartItem => {
            const existing = orderedItems.find(o => o.id === cartItem.id);
            if (existing) {
                existing.quantity += cartItem.quantity;
            } else {
                orderedItems.push({...cartItem});
            }
        });

        // 장바구니 비우기 및 UI 갱신
        cart = [];
        saveState();
        updateUI();
        alert("주문이 성공적으로 접수되었습니다!");
    }
}

/**
 * 최종 결제하기 (백엔드 API 호출)
 */
async function goToPayment() {
    if (orderedItems.length === 0) {
        alert("주문한 내역이 없습니다.");
        return;
    }

    const total = orderedItems.reduce((acc, cur) => acc + (cur.price * cur.quantity), 0);

    if (confirm(`최종 결제 금액은 ${total.toLocaleString()}원입니다.\n결제를 진행하시겠습니까?`)) {

        // 서버 전송용 데이터 구성
        const paymentPayload = {
            tableId: "05", // 실제로는 URL 파라미터나 세션에서 가져와야 함
            orderList: orderedItems,
            totalAmount: total,
            timestamp: new Date().toISOString()
        };

        console.log("결제 API 요청 데이터:", paymentPayload);

        try {
            /** * [Spring Boot API 연동 예시]
             * const response = await fetch('/api/payment/execute', {
             * method: 'POST',
             * headers: { 'Content-Type': 'application/json' },
             * body: JSON.stringify(paymentPayload)
             * });
             * if (response.ok) { ... }
             */

            alert("결제가 완료되었습니다. 이용해주셔서 감사합니다!");

            // 모든 상태 초기화
            cart = [];
            orderedItems = [];
            saveState();
            window.location.reload();
        } catch (e) {
            console.error("Payment Error:", e);
            alert("결제 처리 중 통신 오류가 발생했습니다.");
        }
    }
}

/**
 * 세션 스토리지 저장
 */
function saveState() {
    sessionStorage.setItem('current_cart', JSON.stringify(cart));
    sessionStorage.setItem('ordered_history', JSON.stringify(orderedItems));
}

/**
 * UI 요소들 업데이트
 */
function updateUI() {
    const cartTotalEl = document.getElementById('cart-total');
    const orderedTotalEl = document.getElementById('ordered-total');
    const orderBtn = document.getElementById('order-btn');
    const payBtn = document.getElementById('pay-btn');

    const cartSum = cart.reduce((acc, cur) => acc + (cur.price * cur.quantity), 0);
    const orderSum = orderedItems.reduce((acc, cur) => acc + (cur.price * cur.quantity), 0);

    if(cartTotalEl) cartTotalEl.innerText = cartSum.toLocaleString() + '원';
    if(orderedTotalEl) orderedTotalEl.innerText = orderSum.toLocaleString() + '원';

    // 버튼 활성화 상태 제어
    if (orderBtn) toggleBtnState(orderBtn, cart.length > 0);
    if (payBtn) toggleBtnState(payBtn, orderedItems.length > 0);

    // 상세 내역 리스트 갱신
    renderDrawerList(cart, 'cart-list', true);
    renderDrawerList(orderedItems, 'ordered-list', false);

    lucide.createIcons();
}

function toggleBtnState(btn, isActive) {
    if (!btn) return;
    if (isActive) {
        btn.classList.remove('opacity-50', 'cursor-not-allowed');
        btn.disabled = false;
    } else {
        btn.classList.add('opacity-50', 'cursor-not-allowed');
        btn.disabled = true;
    }
}

/**
 * 드로어(상세내역) 리스트 렌더링
 */
function renderDrawerList(items, containerId, isCart) {
    const container = document.getElementById(containerId);
    if (!container) return;

    if (items.length === 0) {
        container.innerHTML = `<p class="text-sm text-gray-400 text-center py-4">${isCart ? '담은 메뉴가 없습니다.' : '주문 내역이 없습니다.'}</p>`;
        return;
    }

    container.innerHTML = '';
    items.forEach(item => {
        const div = document.createElement('div');
        div.className = 'flex items-center justify-between py-2';
        div.innerHTML = `
            <div class="flex items-center gap-3">
                <span class="text-xl">${item.icon}</span>
                <div>
                    <p class="text-sm font-bold text-gray-800">${item.name}</p>
                    <p class="text-xs text-gray-400">${(item.price * item.quantity).toLocaleString()}원</p>
                </div>
            </div>
            ${isCart ? `
                <div class="flex items-center gap-2 bg-gray-50 rounded-lg p-1">
                    <button onclick="adjustCartQty('${item.id}', -1)" class="w-6 h-6 flex items-center justify-center bg-white rounded border border-gray-200 text-xs">-</button>
                    <span class="text-xs font-bold w-4 text-center">${item.quantity}</span>
                    <button onclick="adjustCartQty('${item.id}', 1)" class="w-6 h-6 flex items-center justify-center bg-white rounded border border-gray-200 text-xs">+</button>
                </div>
            ` : `
                <span class="text-sm font-bold text-gray-500">${item.quantity}개</span>
            `}
        `;
        container.appendChild(div);
    });
}

/**
 * 드로어 열기/닫기
 */
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

// 윈도우 로드 시 시작
window.addEventListener('DOMContentLoaded', init);