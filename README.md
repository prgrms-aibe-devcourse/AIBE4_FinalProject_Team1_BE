## Local Run (Docker Compose + Nginx)

### 사전 준비

- Docker Desktop 실행 중
- FE/BE 레포가 같은 부모 폴더 아래에 위치
```
Workspace/
├── AIBE4_FinalProject_Team1_BE
└── AIBE4_FinalProject_Team1_FE
```

---

### Run
```bash
docker compose up -d --build
```

빌드 완료 후 아래 URL로 접속할 수 있습니다.

| Service | URL |
|--|-----|
| Web App | http://localhost |
| Swagger UI | http://localhost/api/swagger-ui/index.html |
| OpenAPI JSON | http://localhost/api/v3/api-docs |

---

### Stop

`Ctrl + C`로 로그 출력을 종료한 뒤 아래 명령어로 컨테이너를 중지합니다.
```bash
docker compose down
```

## 팀 협업 컨벤션
### 커밋 메시지 유형

| 유형 | 의미 |
|------|------|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `style` | 코드 formatting, 세미콜론 누락 등 |
| `refactor` | 코드 리팩토링 |
| `test` | 테스트 코드 추가 |
| `chore` | 패키지 매니저 수정, 기타 수정 |
| `design` | CSS 등 UI 디자인 변경 |
| `comment` | 주석 추가 및 변경 |
| `rename` | 파일/폴더명 수정 또는 이동 |
| `remove` | 파일 삭제 |
| `!breaking change` | 커다란 API 변경 |
| `!hotfix` | 급한 버그 수정 |
| `assets` | 에셋 파일 추가 |

### 커밋 메시지 형식

**제목:**
```
type : 커밋메시지
```

**내용:**
```markdown
### 작업 내용
- 작업 내용 1
- 작업 내용 2
- 작업 내용 3
```