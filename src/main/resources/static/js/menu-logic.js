/**
 * 스마트 QR 메뉴판 비즈니스 로직
 * - 메뉴 렌더링 (백엔드 API 연동)
 * - 장바구니 관리 (SessionStorage)
 * - 결제 연동 (주문 단계 생략 버전)
 */

// 1. 초기 메뉴 데이터
let menuData = [];

// 2. 전역 상태 관리
let cart = JSON.parse(sessionStorage.getItem('current_cart') || '[]');

// URL에서 storePublicId 추출
const urlParams = new URLSearchParams(window.location.search);
const storePublicId = "80b914dc-fd48-4b60-9f12-01ce4c116593";

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
 * 백엔드 API에서 메뉴 데이터 가져오기 (수정하지 않음)
 */
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
    } catch (error) {
        console.error("Menu fetch error:", error);
        const grid = document.getElementById("menu-grid");
        if (grid) grid.innerHTML = '<p class="text-center text-gray-500 py-10">메뉴를 불러올 수 없습니다.</p>';
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
    lucide.createIcons();
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
 * 최종 결제하기 (장바구니의 내용을 바로 서버로 전송)
 */
async function goToPayment() {
    if (cart.length === 0) {
        alert("장바구니에 담긴 메뉴가 없습니다.");
        return;
    }

    const total = cart.reduce((acc, cur) => acc + (cur.price * cur.quantity), 0);

    if (confirm(`최종 결제 금액은 ${total.toLocaleString()}원입니다.\n결제를 진행하시겠습니까?`)) {

        // 서버 전송용 데이터 구성
        const paymentPayload = {
            tableId: "05",
            orderList: cart, // 이제 확정 내역이 아닌 장바구니(cart)를 보냄
            totalAmount: total,
            timestamp: new Date().toISOString()
        };

        console.log("결제 API 요청 데이터:", paymentPayload);

        try {
            // 여기에 Spring Boot 결제 API 연동 (fetch 호출)
            alert("결제가 완료되었습니다. 이용해주셔서 감사합니다!");

            // 결제 성공 후 장바구니 초기화
            cart = [];
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
}

/**
 * UI 요소들 업데이트
 */
function updateUI() {
    const cartTotalEl = document.getElementById('cart-total');
    const payBtn = document.getElementById('pay-btn');

    const cartSum = cart.reduce((acc, cur) => acc + (cur.price * cur.quantity), 0);

    if(cartTotalEl) cartTotalEl.innerText = cartSum.toLocaleString() + '원';

    // 결제 버튼 활성화 상태 제어 (장바구니에 메뉴가 1개 이상일 때)
    if (payBtn) {
        if (cart.length > 0) {
            payBtn.classList.remove('opacity-50', 'cursor-not-allowed');
            payBtn.disabled = false;
        } else {
            payBtn.classList.add('opacity-50', 'cursor-not-allowed');
            payBtn.disabled = true;
        }
    }

    // 상세 내역 리스트 갱신
    renderCartDrawerList();

    lucide.createIcons();
}

/**
 * 장바구니 상세 내역 리스트 렌더링
 */
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
        return;
    }

    container.innerHTML = '';
    cart.forEach(item => {
        const div = document.createElement('div');
        div.className = 'flex items-center justify-between py-2';
        div.innerHTML = `
            <div class="flex items-center gap-4">
                <span class="text-2xl">${item.icon}</span>
                <div>
                    <p class="text-sm font-bold text-gray-800">${item.name}</p>
                    <p class="text-xs text-rose-600 font-bold">${(item.price * item.quantity).toLocaleString()}원</p>
                </div>
            </div>
            <div class="flex items-center gap-2 bg-gray-50 rounded-xl p-1.5 border border-gray-100">
                <button onclick="adjustCartQty('${item.id}', -1)" class="w-7 h-7 flex items-center justify-center bg-white rounded-lg border border-gray-200 text-sm font-bold active:bg-gray-100">-</button>
                <span class="text-sm font-black w-6 text-center">${item.quantity}</span>
                <button onclick="adjustCartQty('${item.id}', 1)" class="w-7 h-7 flex items-center justify-center bg-white rounded-lg border border-gray-200 text-sm font-bold active:bg-gray-100">+</button>
            </div>
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