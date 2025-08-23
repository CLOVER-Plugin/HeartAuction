# HeartAuction - 마인크래프트 하트 경매 플러그인

[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://adoptium.net/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green.svg)](https://papermc.io/)
[![Paper](https://img.shields.io/badge/Paper-API-blue.svg)](https://papermc.io/)
[![License](https://img.shields.io/badge/License-Educational-yellow.svg)](LICENSE)

마인크래프트 서버에서 하트 경매 게임을 진행할 수 있는 종합적인 게임 플러그인입니다. 평화시간과 PVP 단계를 거쳐 전략적 게임플레이를 제공하며, 다양한 미니게임과 시스템을 통해 플레이어들의 참여를 유도합니다.

## 📋 목차

- [요구사항](#-요구사항)
- [게임 구조](#-게임-구조)
- [설정 파일](#️-설정-파일)
- [코드 구조](#️-코드-구조)
- [설치 및 사용법](#-설치-및-사용법)
- [주요 기능](#-주요-기능)
- [문제 해결](#️-문제-해결)
- [라이센스](#-라이센스)
- [기여하기](#-기여하기)
- [문의](#-문의)

## 🚀 요구사항

### 서버 환경
- **Java**: Java 21 이상
- **마인크래프트 버전**: 1.21.1 (Paper API)
- **플러그인**: PlaceholderAPI (선택사항, 권장)

### Dependencies
```xml
<dependencies>
    <dependency>
        <groupId>io.papermc.paper</groupId>
        <artifactId>paper-api</artifactId>
        <version>1.21.1-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>me.clip</groupId>
        <artifactId>placeholderapi</artifactId>
        <version>2.11.5</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### 권한
- `heart.op`: 운영자 권한 (기본값: op)
- `heart.admin`: 관리자 권한 (기본값: op)

## 🎮 게임 구조

### 라운드 흐름
1. **게임 시작** (`/게임시작`)
   - 모든 플레이어 체력을 2칸(4.0)으로 설정
   - 평화시간 시작 (기본 30분)
   - PVP 비활성화, 인벤토리 보존 활성화
   - 경매 시스템 시작

2. **평화시간**
   - 경매가 주기적으로 진행 (기본 3분 간격)
   - 미션 시스템 활성화
   - 상점 이용 가능

3. **PVP 단계**
   - 평화시간 종료 후 자동 시작
   - 모든 플레이어를 PVP 존으로 텔레포트
   - 경기장이 점진적으로 축소 (흰색 콘크리트 벽)
   - 최종 반지름까지 축소 후 게임 종료

### 주요 명령어
| 명령어 | 설명 | 권한 |
|--------|------|------|
| `/운영자 <닉네임>` | 지정 유저를 운영자로 설정/해제 | `heart.op` |
| `/게임시작` | 게임 시작 및 평화시간 스케줄 시작 | `heart.admin` |
| `/게임종료` | 게임 즉시 종료 및 모든 설정 초기화 | `heart.admin` |
| `/pvp시작` | 평화시간을 건너뛰고 즉시 PVP 시작 | `heart.admin` |
| `/미션지급 [닉]` | 랜덤 미션 지급 | `heart.admin` |
| `/미션포기` | 다이아 1개 지불로 현재 미션 포기 후 새 미션 지급 | 모든 플레이어 |
| `/경매` | 현재 진행될 경매에 참여/입찰 대기 등록 | 모든 플레이어 |
| `/spawn` | 설정된 스폰으로 이동 | 모든 플레이어 |
| `/꽃도박 <꽃개수>` | 꽃 도박 실행 | 모든 플레이어 |
| `/달걀도박 <닭개수>` | 달걀 도박 실행 | 모든 플레이어 |

### 시스템
- **경매 시스템**: 평화시간 중 주기적으로 진행, 다이아몬드로 입찰
- **미션 시스템**: 16가지 다양한 미션, 완료 시 다이아몬드 보상
- **PVP 존 관리**: 원형 경기장, 점진적 축소, 외곽 지역 데미지
- **도박 시스템**: 꽃 도박, 달걀 도박으로 다이아몬드 획득
- **상점 시스템**: 무기, 방어구, 포션 등 구매

## ⚙️ 설정 파일

### config.yml
```yaml
# 게임 기본 설정
peace-minutes: 30                  # 평화시간(분)
auction-interval-seconds: 180      # 평화시간 중 경매 간격(초)
auction-pre-title-seconds: 30      # 각 경매 시작 전 타이틀 예고(초)

# 스폰 좌표
spawn:
  world: world
  x: 0.5
  y: 100.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0

# PVP 존 설정
pvp-zone:
  world: world
  center-x: 100.5
  center-y: 80.0
  center-z: 100.5
  radius: 75          # 초기 반지름
  shrink-every-seconds: 60   # 1분마다 1블럭씩 축소 (사용하지 않음)
  damage-check-ticks: 10     # 외곽(파티클 영역) 입장 시 10틱마다 반칸 피해 (사용하지 않음)
  final-radius: 5            # 최종 축소될 반지름
  shrink-duration: 600       # 축소에 걸리는 시간(초) - 10분

# 상점 NPC 이름 및 위치(선택)
shop-npc:
  name: "아이템 상점"
  world: world
  x: 105.5
  y: 80.0
  z: 100.5

# 도박 설정
flower-gamble:
  center:
    world: world
    x: 200
    y: 80
    z: 200
  size: 5       # 5x5
  chest:
    world: world
    y: 80
    z: 206      # 5x5 앞쪽 상자 위치

egg-gamble:
  pit-origin:
    world: world
    x: 220 # 최소 좌표 (왼쪽 밑에 꼭지점 좌표?)
    y: 70
    z: 220
  size-x: 3
  size-y: 5
  size-z: 3
  chest:
    x: 220
    y: 70
    z: 224
```

### shop.yml
- 무기, 방어구, 포션 등의 가격 설정
- GUI 슬롯 배치 설정

### reward.yml
- 미션 완료 시 보상 다이아몬드 수량 설정

### bet.yml
- 꽃 도박, 달걀 도박의 배율 설정

## 🏗️ 코드 구조

```
src/main/java/yd/kingdom/heartAuction/
├── HeartAuction.java              # 메인 플러그인 클래스
├── command/                       # 명령어 처리
│   ├── AdminCommand.java         # 운영자 설정
│   ├── GameStartCommand.java     # 게임 시작
│   ├── GameEndCommand.java       # 게임 종료
│   ├── PvpStartCommand.java      # PVP 시작
│   ├── MissionGiveCommand.java   # 미션 지급
│   ├── MissionForfeitCommand.java # 미션 포기
│   ├── AuctionJoinCommand.java   # 경매 참여
│   ├── SpawnCommand.java         # 스폰 이동
│   ├── FlowerBetCommand.java     # 꽃 도박
│   └── EggBetCommand.java        # 달걀 도박
├── listener/                      # 이벤트 리스너
│   ├── AuctionChatListener.java  # 경매 채팅
│   ├── ChatListener.java         # 일반 채팅
│   ├── DiamondOreGuard.java     # 다이아몬드 광석 보호
│   ├── InteractListener.java     # 상호작용
│   ├── InventoryListener.java    # 인벤토리
│   └── PvpZoneListener.java      # PVP 존
├── manager/                       # 핵심 관리자
│   ├── AdminManager.java         # 운영자 관리
│   ├── AuctionManager.java       # 경매 관리
│   ├── GambleEggManager.java     # 달걀 도박
│   ├── GambleFlowerManager.java  # 꽃 도박
│   ├── GameManager.java          # 게임 진행
│   ├── MissionManager.java       # 미션 관리
│   ├── PvpZoneManager.java       # PVP 존 관리
│   └── ShopManager.java          # 상점 관리
├── papi/                         # PlaceholderAPI 확장
│   └── PeacePlaceholder.java     # 평화시간 플레이스홀더
└── util/                         # 유틸리티
    ├── Items.java                # 아이템 관련
    ├── Locations.java            # 위치 관련
    ├── Tasker.java               # 태스크 관리
    └── Texts.java                # 텍스트 관련
```

## 📥 설치 및 사용법

### 설치
1. **빌드**
   ```bash
   mvn clean package
   ```

2. **배포**
   - `target/heartauction-1.0-SNAPSHOT.jar`를 서버의 `plugins` 폴더에 복사
   - 서버 재시작

3. **초기 설정**
   - `config.yml`에서 PVP 존 좌표, 스폰 좌표 등 설정
   - `shop.yml`에서 상점 아이템 가격 조정
   - `reward.yml`에서 미션 보상 설정

### 사용법
1. **게임 시작**: `/게임시작` 명령어로 게임 시작
2. **플레이어 참여**: `/경매` 명령어로 경매 참여 등록
3. **미션 수행**: 자동으로 미션이 지급되며, 완료 시 보상 획득
4. **PVP 단계**: 평화시간 종료 후 자동으로 PVP 존으로 이동
5. **게임 종료**: 경기장 축소 완료 또는 `/게임종료` 명령어

## ✨ 주요 기능

### 🏆 경매 시스템
- 평화시간 중 주기적으로 진행
- 다이아몬드로 입찰하여 아이템 획득
- 자동 경매 진행 및 결과 처리
- 경매 참여자 등록 및 입찰 관리

### 🎯 미션 시스템
- 16가지 다양한 미션 (제작, 수집, 조합 등)
- 랜덤 미션 지급 및 완료 시 다이아몬드 보상
- 미션 포기 기능 (다이아몬드 1개 소모)
- 미션 종류:
  - 양털 5개 가져오기
  - 건초더미 1개 만들어오기
  - 썩은고기 5개 가져오기
  - 침대 2개 만들기
  - 씨앗 15개 가져오기
  - 색깔 염료 5개(서로 다른 색)
  - 그림 1개 만들어오기
  - 보트 3종류 만들기
  - 먹을 것 5개(서로 다른 종류)
  - 몬스터 관련 아이템 2개
  - 황금사과 1개 만들어오기
  - 참나무 묘목 30개 가져오기
  - 조약돌 한 세트(64) 가져오기
  - 랜턴 5개 만들기
  - 쿠키 2개 만들기
  - 아이템 액자 5개 만들기

### ⚔️ PVP 존 관리
- 원형 경기장에서 점진적 축소
- 흰색 콘크리트 벽으로 경계 표시
- 외곽 지역 체류 시 데미지
- 안전 반경 안내 메시지 (25블럭, 5블럭)

### 🎰 도박 시스템
- **꽃 도박**: 꽃 개수에 따른 배율로 다이아몬드 획득
  - 2개: 1.0배, 3개: 2.0배, 4개: 3.0배, 5개: 4.0배, 6개: 5.0배, 7개: 8.0배
- **달걀 도박**: 닭 개수에 따른 배율로 다이아몬드 획득
  - 2개: 1.0배, 3개: 2.0배, 4개: 3.0배, 5개: 4.0배, 6개: 5.0배, 7개: 6.0배, 8개: 7.0배, 9개: 8.0배, 10개: 20.0배

### 🛒 상점 시스템
- 무기, 방어구, 포션 등 구매
- 다이아몬드로 결제
- GUI 기반 상점 인터페이스
- 제공 아이템:
  - 검: 철검(15), 다이아검(20), 네더라이트검(30)
  - 방어구: 철/다이아/네더라이트 헬멧, 갑옷, 레깅스, 부츠
  - 포션: 즉시 치유(II) 스플래시 포션(4)
  - 황금 사과: 인챈트된 황금 사과(7)

### 🛡️ 보안 기능
- 다이아몬드 광석 보호
- 운영자 권한 관리
- 게임 상태별 접근 제어
- PlaceholderAPI 연동 (`%peace_time%`)

## 🛠️ 문제 해결

### 일반적인 문제
1. **플러그인 로드 실패**
   - Java 21 이상 설치 확인
   - Paper 서버 사용 확인
   - 의존성 플러그인 설치 확인

2. **경매 시스템 오류**
   - `config.yml`의 경매 간격 설정 확인
   - 데이터베이스 연결 상태 확인

3. **PVP 존 문제**
   - `config.yml`의 PVP 존 좌표 설정 확인
   - 월드 이름 및 좌표 정확성 확인

4. **미션 시스템 오류**
   - `reward.yml` 파일 형식 확인
   - 미션 풀 설정 확인

### 디버깅
- 서버 콘솔에서 오류 메시지 확인
- 플러그인 로그 확인
- `heart.admin` 권한으로 디버그 명령어 사용

### 로그 확인
```
[HeartAuction] 플러그인 활성화됨
[HeartAuction] PlaceholderAPI hooked: %peace_time% placeholder registered successfully.
[HeartAuction] HeartAuction enabled.
```

## 📄 라이센스

이 프로젝트는 개인/교육용으로 개발되었습니다. 상업적 사용 시 별도 문의가 필요합니다.

## 🤝 기여하기

### 기여 방법
1. 이슈 등록으로 버그 리포트 또는 기능 제안
2. Pull Request로 코드 개선 제안
3. 문서 개선 및 번역 기여

### 개발 환경 설정
1. Java 21 설치
2. Maven 설치
3. IDE 설정 (IntelliJ IDEA 권장)
4. Paper API 의존성 추가

### 코딩 스타일
- Java 표준 코딩 컨벤션 준수
- 한글 주석 사용
- 명확한 변수명과 메서드명 사용
- 동시성 처리를 위한 Atomic 변수 사용

### 빌드 및 테스트
```bash
# 프로젝트 빌드
mvn clean package

# 테스트 실행
mvn test

# 의존성 확인
mvn dependency:tree
```

## 📞 문의

- **이슈 등록**: [GitHub Issues](https://github.com/your-username/HeartAuction/issues)를 통한 버그 리포트 및 기능 제안
- **개발자 연락**: 프로젝트 메인테이너에게 직접 문의
- **커뮤니티**: 마인크래프트 서버 관리자 커뮤니티 활용

## 🔄 업데이트 내역

### v1.0-SNAPSHOT
- 초기 버전 릴리즈
- 기본 게임 시스템 구현
- 경매, 미션, PVP 존 시스템
- 도박 및 상점 시스템
- PlaceholderAPI 연동

## 📚 추가 자료

- [Paper API 문서](https://papermc.io/javadocs/)
- [PlaceholderAPI 문서](https://github.com/PlaceholderAPI/PlaceholderAPI/wiki)
- [마인크래프트 플러그인 개발 가이드](https://www.spigotmc.org/wiki/spigot-plugin-development/)

---

**HeartAuction**은 마인크래프트 서버에서 하트 경매 게임을 진행할 수 있도록 설계된 종합적인 게임 플러그인입니다. 

⭐ 이 프로젝트가 도움이 되었다면 스타를 눌러주세요!