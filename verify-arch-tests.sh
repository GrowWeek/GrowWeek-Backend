#!/bin/bash
# Architecture Test Verification Script
# ArchitectureTest.kt의 5개 규칙을 21개 시나리오(20회 실행)로 검증
#
# 사용법: bash verify-arch-tests.sh

set -euo pipefail

PROJECT_DIR="/Users/imsubin/IdeaProjects/GrowWeek/GrowWeek-Backend/GrowWeek-Backend"
SRC_BASE="$PROJECT_DIR/server/src/main/kotlin/xyz/robinjoon/growweek"
RESULTS_FILE="$PROJECT_DIR/docs/architecture-test-verification.md"

cat > "$RESULTS_FILE" << 'HEADER'
# 아키텍처 테스트 검증 결과

검증일: 2026-02-18

| 시나리오 | 설명 | 기대 | 실제 | 결과 |
|---------|------|------|------|------|
HEADER

run_test() {
    local scenario="$1"
    local description="$2"
    local expected="$3"

    echo "=== Running scenario: $scenario - $description ==="

    cd "$PROJECT_DIR/server"
    local actual
    if ./gradlew test --tests "xyz.robinjoon.growweek.architecture.ArchitectureTest" --no-daemon 2>&1 > /tmp/arch-test-output-$scenario.txt 2>&1; then
        actual="PASS"
    else
        actual="FAIL"
    fi

    local result
    if [ "$expected" = "$actual" ]; then
        result="PASS"
    else
        result="FAIL"
    fi

    echo "  Expected: $expected, Actual: $actual, Result: $result"
    echo "| $scenario | $description | $expected | $actual | $result |" >> "$RESULTS_FILE"
}

create_file() {
    local filepath="$1"
    local content="$2"
    mkdir -p "$(dirname "$filepath")"
    echo "$content" > "$filepath"
}

delete_file() {
    rm -f "$1"
}

# ========== A1: Presentation → Infrastructure ==========
create_file "$SRC_BASE/task/presentation/rest/controller/LayerViolationA1.kt" \
'package xyz.robinjoon.growweek.task.presentation.rest.controller

import xyz.robinjoon.growweek.task.infrastructure.persistence.ExposedTaskRepository

class LayerViolationA1(val repo: ExposedTaskRepository)'

run_test "A1" "Presentation → Infrastructure" "FAIL"
delete_file "$SRC_BASE/task/presentation/rest/controller/LayerViolationA1.kt"

# ========== A2+A3: Presentation → domain.repository ==========
create_file "$SRC_BASE/task/presentation/rest/controller/LayerViolationA2.kt" \
'package xyz.robinjoon.growweek.task.presentation.rest.controller

import xyz.robinjoon.growweek.task.domain.repository.TaskRepository

class LayerViolationA2(val repo: TaskRepository)'

create_file "$SRC_BASE/task/presentation/rest/controller/LayerViolationA3.kt" \
'package xyz.robinjoon.growweek.task.presentation.rest.controller

import xyz.robinjoon.growweek.task.domain.repository.TaskRepository

class LayerViolationA3(val repo: TaskRepository)'

run_test "A2+A3" "Presentation → domain.repository" "FAIL"
delete_file "$SRC_BASE/task/presentation/rest/controller/LayerViolationA2.kt"
delete_file "$SRC_BASE/task/presentation/rest/controller/LayerViolationA3.kt"

# ========== A4: Application → Infrastructure ==========
create_file "$SRC_BASE/task/application/service/LayerViolationA4.kt" \
'package xyz.robinjoon.growweek.task.application.service

import xyz.robinjoon.growweek.task.infrastructure.persistence.ExposedTaskRepository

class LayerViolationA4(val repo: ExposedTaskRepository)'

run_test "A4" "Application → Infrastructure" "FAIL"
delete_file "$SRC_BASE/task/application/service/LayerViolationA4.kt"

# ========== A5: Application → Presentation ==========
create_file "$SRC_BASE/task/application/service/LayerViolationA5.kt" \
'package xyz.robinjoon.growweek.task.application.service

import xyz.robinjoon.growweek.task.presentation.rest.controller.TaskController

class LayerViolationA5(val controller: TaskController)'

run_test "A5" "Application → Presentation" "FAIL"
delete_file "$SRC_BASE/task/application/service/LayerViolationA5.kt"

# ========== A6: Domain → Application ==========
create_file "$SRC_BASE/task/domain/model/LayerViolationA6.kt" \
'package xyz.robinjoon.growweek.task.domain.model

import xyz.robinjoon.growweek.task.application.service.CreateTaskService

class LayerViolationA6(val service: CreateTaskService)'

run_test "A6" "Domain → Application" "FAIL"
delete_file "$SRC_BASE/task/domain/model/LayerViolationA6.kt"

# ========== A7: Domain → Infrastructure ==========
create_file "$SRC_BASE/task/domain/model/LayerViolationA7.kt" \
'package xyz.robinjoon.growweek.task.domain.model

import xyz.robinjoon.growweek.task.infrastructure.persistence.ExposedTaskRepository

class LayerViolationA7(val repo: ExposedTaskRepository)'

run_test "A7" "Domain → Infrastructure" "FAIL"
delete_file "$SRC_BASE/task/domain/model/LayerViolationA7.kt"

# ========== A8: Domain → Presentation ==========
create_file "$SRC_BASE/task/domain/model/LayerViolationA8.kt" \
'package xyz.robinjoon.growweek.task.domain.model

import xyz.robinjoon.growweek.task.presentation.rest.controller.TaskController

class LayerViolationA8(val controller: TaskController)'

run_test "A8" "Domain → Presentation" "FAIL"
delete_file "$SRC_BASE/task/domain/model/LayerViolationA8.kt"

# ========== A9: Infrastructure → Presentation ==========
create_file "$SRC_BASE/task/infrastructure/persistence/LayerViolationA9.kt" \
'package xyz.robinjoon.growweek.task.infrastructure.persistence

import xyz.robinjoon.growweek.task.presentation.rest.controller.TaskController

class LayerViolationA9(val controller: TaskController)'

run_test "A9" "Infrastructure → Presentation" "FAIL"
delete_file "$SRC_BASE/task/infrastructure/persistence/LayerViolationA9.kt"

# ========== A10: Infrastructure → Application ==========
create_file "$SRC_BASE/task/infrastructure/persistence/LayerViolationA10.kt" \
'package xyz.robinjoon.growweek.task.infrastructure.persistence

import xyz.robinjoon.growweek.task.application.service.CreateTaskService

class LayerViolationA10(val service: CreateTaskService)'

run_test "A10" "Infrastructure → Application" "FAIL"
delete_file "$SRC_BASE/task/infrastructure/persistence/LayerViolationA10.kt"

# ========== B1: Presentation → Application (허용) ==========
create_file "$SRC_BASE/task/presentation/rest/controller/LayerAllowedB1.kt" \
'package xyz.robinjoon.growweek.task.presentation.rest.controller

import xyz.robinjoon.growweek.task.application.usecase.CreateTaskUseCase

class LayerAllowedB1(val useCase: CreateTaskUseCase)'

run_test "B1" "Presentation → Application (허용)" "PASS"
delete_file "$SRC_BASE/task/presentation/rest/controller/LayerAllowedB1.kt"

# ========== B2: Application → Domain (허용) ==========
create_file "$SRC_BASE/task/application/service/LayerAllowedB2.kt" \
'package xyz.robinjoon.growweek.task.application.service

import xyz.robinjoon.growweek.task.domain.model.Task

class LayerAllowedB2(val task: Task)'

run_test "B2" "Application → Domain (허용)" "PASS"
delete_file "$SRC_BASE/task/application/service/LayerAllowedB2.kt"

# ========== B3: Infrastructure → Domain (허용) ==========
create_file "$SRC_BASE/task/infrastructure/persistence/LayerAllowedB3.kt" \
'package xyz.robinjoon.growweek.task.infrastructure.persistence

import xyz.robinjoon.growweek.task.domain.repository.TaskRepository

class LayerAllowedB3(val repo: TaskRepository)'

run_test "B3" "Infrastructure → Domain (허용)" "PASS"
delete_file "$SRC_BASE/task/infrastructure/persistence/LayerAllowedB3.kt"

# ========== C1: Presentation → domain.model (DTO 변환 예외, 허용) ==========
create_file "$SRC_BASE/task/presentation/rest/controller/ExceptionAllowedC1.kt" \
'package xyz.robinjoon.growweek.task.presentation.rest.controller

import xyz.robinjoon.growweek.task.domain.model.TaskStatus

class ExceptionAllowedC1(val status: TaskStatus)'

run_test "C1" "Presentation → domain.model (DTO 변환 예외)" "PASS"
delete_file "$SRC_BASE/task/presentation/rest/controller/ExceptionAllowedC1.kt"

# ========== C2: 임의 레이어 → common (공유 모듈 예외, 허용) ==========
create_file "$SRC_BASE/task/presentation/rest/controller/ExceptionAllowedC2.kt" \
'package xyz.robinjoon.growweek.task.presentation.rest.controller

import xyz.robinjoon.growweek.common.domain.MemberId

class ExceptionAllowedC2(val memberId: MemberId)'

run_test "C2" "임의 레이어 → common (공유 모듈 예외)" "PASS"
delete_file "$SRC_BASE/task/presentation/rest/controller/ExceptionAllowedC2.kt"

# ========== D1: member → task ==========
create_file "$SRC_BASE/member/application/service/BcViolationD1.kt" \
'package xyz.robinjoon.growweek.member.application.service

import xyz.robinjoon.growweek.task.domain.model.TaskStatus

class BcViolationD1(val status: TaskStatus)'

run_test "D1" "member → task (BC 격리 위반)" "FAIL"
delete_file "$SRC_BASE/member/application/service/BcViolationD1.kt"

# ========== D2: task → retrospective ==========
create_file "$SRC_BASE/task/application/service/BcViolationD2.kt" \
'package xyz.robinjoon.growweek.task.application.service

import xyz.robinjoon.growweek.retrospective.domain.model.Retrospective

class BcViolationD2(val retro: Retrospective)'

run_test "D2" "task → retrospective (BC 격리 위반)" "FAIL"
delete_file "$SRC_BASE/task/application/service/BcViolationD2.kt"

# ========== D3: retrospective → member ==========
create_file "$SRC_BASE/retrospective/application/service/BcViolationD3.kt" \
'package xyz.robinjoon.growweek.retrospective.application.service

import xyz.robinjoon.growweek.member.domain.model.Member

class BcViolationD3(val member: Member)'

run_test "D3" "retrospective → member (BC 격리 위반)" "FAIL"
delete_file "$SRC_BASE/retrospective/application/service/BcViolationD3.kt"

# ========== E1: member → common (허용) ==========
create_file "$SRC_BASE/member/application/service/BcAllowedE1.kt" \
'package xyz.robinjoon.growweek.member.application.service

import xyz.robinjoon.growweek.common.domain.MemberId

class BcAllowedE1(val memberId: MemberId)'

run_test "E1" "member → common (BC 허용)" "PASS"
delete_file "$SRC_BASE/member/application/service/BcAllowedE1.kt"

# ========== F1: Domain → Spring ==========
create_file "$SRC_BASE/task/domain/model/DomainViolationF1.kt" \
'package xyz.robinjoon.growweek.task.domain.model

import org.springframework.stereotype.Service

@Service
class DomainViolationF1'

run_test "F1" "Domain → Spring (순수성 위반)" "FAIL"
delete_file "$SRC_BASE/task/domain/model/DomainViolationF1.kt"

# ========== F2: Domain → Jakarta ==========
create_file "$SRC_BASE/task/domain/model/DomainViolationF2.kt" \
'package xyz.robinjoon.growweek.task.domain.model

import jakarta.annotation.PostConstruct

class DomainViolationF2 {
    @PostConstruct
    fun init() {}
}'

run_test "F2" "Domain → Jakarta (순수성 위반)" "FAIL"
delete_file "$SRC_BASE/task/domain/model/DomainViolationF2.kt"

# ========== Final cleanup check ==========
echo ""
echo "=== Final cleanup check ==="
REMAINING=$(find "$SRC_BASE" -name "LayerViolation*.kt" -o -name "LayerAllowed*.kt" -o -name "ExceptionAllowed*.kt" -o -name "BcViolation*.kt" -o -name "BcAllowed*.kt" -o -name "DomainViolation*.kt" 2>/dev/null)
if [ -z "$REMAINING" ]; then
    echo "All scenario files cleaned up successfully."
else
    echo "WARNING: Remaining files found:"
    echo "$REMAINING"
fi

echo ""
echo "=== Verification complete! Results written to $RESULTS_FILE ==="
echo ""
cat "$RESULTS_FILE"
