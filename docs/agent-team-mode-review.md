# Claude Code 에이전트 팀 모드 사용 후기

> 작성일: 2026-02-19
> 적용 이슈: [#21 - common 공유 계약 비대화 방지 구조 재검토](https://github.com/GrowWeek/GrowWeek-Backend/pull/29)

## 1. 개요

GitHub Issue #21 해결에 Claude Code의 에이전트 팀 모드를 처음 적용한 기록입니다.
팀 구성, 워크플로우, 소요 시간, 장단점을 정리합니다.

## 2. 팀 구성

B안(연구 + 구현 분리)을 기반으로 5명 구성:

| 역할 | 모델 | 담당 | 설명 |
|------|------|------|------|
| **리더 (team-lead)** | Opus | 조율 전용 | 태스크 정의, 의존성 관리, 팀원 스폰/할당, 사용자와 소통 |
| **researcher** | Opus | 구조 분석 + 설계 문서 | 프로젝트 탐색, 설계 전략 수립, `docs/` 설계 문서 작성 |
| **doc-writer** | Sonnet | 문서 갱신 | ARCHITECTURE.md, SKILL.md 문서 갱신 |
| **impl-task** | Sonnet | task BC 리팩터링 + 빌드 검증 | task BC 계약 파일 이동, import 갱신, 최종 빌드 테스트 |
| **impl-retro-member** | Sonnet | retro+member BC 리팩터링 | retrospective, member BC 계약 파일 이동, import 갱신 |

### 모델 선택 기준

- **researcher만 Opus**: 아키텍처 분석 및 설계 의사결정은 깊은 추론이 필요
- **나머지는 Sonnet**: 문서 갱신, 파일 이동, import 변경 등 정형화된 작업에는 Sonnet이 충분

## 3. 태스크 의존성 구조

```
#1 연구/설계 (researcher)
  ├─→ #2 문서 갱신 (doc-writer)
  ├─→ #4 task BC 리팩터링 (impl-task)        ─→ #6 빌드 검증 (impl-task)
  └─→ #5 retro+member 리팩터링 (impl-retro-member) ─↗
```

- #1 완료 후 #2, #4, #5를 **병렬 실행**
- #4, #5 완료 후 #6(빌드 검증)을 **순차 실행**
- #6은 유휴 상태였던 impl-task에게 추가 할당

## 4. 실행 흐름

### Phase 1: 연구 (순차)
1. researcher 스폰 → Task #1 수행
2. 프로젝트 구조 전체 탐색, BC별 common 의존 관계 분석
3. `docs/issue-21-common-contract-strategy.md` 설계 문서 작성
4. **리더가 설계 결과를 사용자에게 공유 → 승인 획득**

### Phase 2: 문서 + 리팩터링 (병렬)
5. doc-writer, impl-task, impl-retro-member 3명 동시 스폰
6. doc-writer: ARCHITECTURE.md, SKILL.md 4개 파일 갱신
7. impl-task: `common/contract/task/` 생성, 4개 파일 이동, 8개 파일 import 갱신
8. impl-retro-member: `common/contract/retrospective/`, `common/contract/member/` 생성, 2개 파일 이동, 9개 파일 import 갱신

### Phase 3: 검증 (순차)
9. impl-task에게 Task #6 할당 → `./gradlew build` 실행
10. 273/275 테스트 통과 확인 (실패 2건은 기존 환경변수 이슈)

### Phase 4: 마무리
11. 리더가 `ticket/#21` 브랜치 생성, 커밋, 푸시, PR 생성
12. 팀원 전원 shutdown → 팀 리소스 정리

## 5. 산출물

| 산출물 | 파일 수 | 내용 |
|--------|---------|------|
| 설계 문서 | 1 | `docs/issue-21-common-contract-strategy.md` |
| 문서 갱신 | 4 | ARCHITECTURE.md, domain/event/infrastructure SKILL.md |
| 파일 이동 | 6 | common/contract/{task,retrospective,member}/ 하위 |
| import 갱신 | ~17 | 메인 소스 + 테스트 파일 |
| PR | 1 | [#29](https://github.com/GrowWeek/GrowWeek-Backend/pull/29) |

## 6. 장점

### 병렬 처리 효과
- Phase 2에서 doc-writer, impl-task, impl-retro-member가 동시 작업
- 서로 다른 파일을 수정하므로 충돌 없이 병렬 진행 가능

### 역할 분리의 명확성
- researcher의 설계 문서가 이후 모든 작업의 기준점 역할
- 리더는 조율에만 집중하여 사용자와의 소통 품질 유지

### 유휴 팀원 재활용
- impl-task가 리팩터링 완료 후 유휴 → 빌드 검증(Task #6) 추가 할당
- 별도 에이전트를 스폰하지 않아 리소스 절약

## 7. 주의점 및 개선 포인트

### 동시 작업 시 파일 충돌 주의
- impl-task와 impl-retro-member가 동시에 `common/` 하위에서 작업
- `common/port/` 디렉토리 삭제 타이밍 등 조율 필요 → 프롬프트에 명시적 가이드 포함으로 해결

### 리더 컨텍스트 관리
- 팀원 idle 알림이 빈번하게 도착하여 컨텍스트를 소비
- 실질적 작업 보고와 idle 알림을 구분하여 처리 필요

### 태스크 의존성 설계가 중요
- 의존성을 잘못 설정하면 불필요한 대기 발생
- 이번에는 #2(문서)와 #4, #5(리팩터링)의 의존성을 해제하여 병렬 실행 가능하게 조정

### 모델 선택 전략
- 연구/설계처럼 깊은 판단이 필요한 작업 → Opus
- 정형화된 파일 이동, import 변경, 문서 갱신 → Sonnet으로 충분
- 비용 효율을 고려한 모델 분배가 중요

## 8. 결론

에이전트 팀 모드는 **역할이 명확하고 파일 영역이 분리되는 작업**에 효과적이었습니다.
특히 연구 → 구현의 순차 흐름과, 구현 내 BC별 병렬 처리의 조합이 자연스러웠습니다.

다만 **리더의 조율 비용**(태스크 설계, 프롬프트 작성, idle 알림 처리)이 존재하므로,
단순한 작업에는 단일 에이전트가 더 효율적일 수 있습니다.
